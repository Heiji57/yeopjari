package com.yeopjari.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yeopjari.app.platform.HandlerTicker
import com.yeopjari.app.platform.LoggingNavigator
import com.yeopjari.app.platform.PleosDriveSource
import com.yeopjari.app.platform.PleosNavigator
import com.yeopjari.app.platform.RouteNavigator
import com.yeopjari.app.platform.Speaker
import com.yeopjari.core.coach.VoiceCoach
import com.yeopjari.core.detect.DriveEventDetector
import com.yeopjari.core.model.DriveFrame
import com.yeopjari.core.model.DriverMode
import com.yeopjari.core.model.EventType
import com.yeopjari.core.model.Gear
import com.yeopjari.core.model.ManeuverType
import com.yeopjari.core.model.PlannedSpot
import com.yeopjari.core.scenario.DriveSignalSource
import com.yeopjari.core.scenario.Scenario
import com.yeopjari.core.scenario.ScenarioReplaySource
import com.yeopjari.core.sync.HttpYeopjariApi
import com.yeopjari.core.sync.ParkingAdviceDto
import com.yeopjari.core.sync.ReportDto
import com.yeopjari.core.sync.RoutePreviewDto
import com.yeopjari.core.sync.SeniorInsightDto
import com.yeopjari.core.sync.TensionSpotDto
import com.yeopjari.core.sync.TripRecorder
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/** 화면 상태. P단(예습) → D단(주행) → P단(리포트). */
sealed interface Screen {
    data class Loading(val message: String) : Screen

    data class Preview(
        val preview: RoutePreviewDto,
        val parking: ParkingAdviceDto?,
        val insights: List<SeniorInsightDto>,
        val chosenEasy: Boolean = false,
        /** 버튼을 눌러 받아들인 제안 (SeniorInsightDto.type) */
        val handledInsights: Set<String> = emptySet(),
    ) : Screen {
        /** 지금 고른 길의 긴장 구간 (쉬운 길을 골랐으면 쉬운 길 기준) */
        val activeSpots: List<TensionSpotDto>
            get() = if (chosenEasy) preview.easyRoute?.spots.orEmpty() else preview.spots

        /** 지금 고른 길의 걸리는 시간 */
        val activeMinutes: Int
            get() = preview.minutes + (if (chosenEasy) preview.easyRoute?.extraMinutes ?: 0 else 0)
    }

    data class Driving(
        val nextText: String,
        val distanceM: Int,
        val speedKph: Int,
        val lastVoice: String?,
        /** 다음 안내 지점 종류 (화살표 아이콘용). 없으면 직진 */
        val nextType: ManeuverType? = null,
        /** 다음 안내 지점이 예습한 긴장 구간인지 */
        val spotAhead: Boolean = false,
        val spotsPassed: Int = 0,
        val spotsPlanned: Int = 0,
        val arrived: Boolean = false,
        val gear: Gear = Gear.D,
        /** 다음 안내 지점 index (지도에서 차 위치 계산용). 모두 지나면 null */
        val nextIndex: Int? = 0,
    ) : Screen

    data class Report(val report: ReportDto) : Screen

    data class Error(val message: String) : Screen
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val api = HttpYeopjariApi(BuildConfig.API_BASE)
    private val speaker = Speaker(app)
    private val navigator: RouteNavigator = if (BuildConfig.USE_PLEOS_SDK) PleosNavigator(app) else LoggingNavigator()

    /** 서버 호출과 이벤트 업로드는 한 줄로 순서대로 (이벤트 순서가 섞이지 않게). */
    private val io = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    var profile by mutableStateOf(Profiles.SOYEON)
        private set
    var screen by mutableStateOf<Screen>(Screen.Loading("길을 살펴보는 중이에요"))
        private set
    var toast by mutableStateOf<String?>(null)
        private set

    private var scenario: Scenario = profile.scenario()
    private var planned: List<PlannedSpot> = emptyList()
    private var source: DriveSignalSource? = null
    private var detector: DriveEventDetector? = null
    private var coach: VoiceCoach? = null
    private var recorder: TripRecorder? = null
    private var finishing = false
    private var spotsPassed = 0

    /** 지금 길의 안내 지점들 (지도 그리기용) */
    val route: List<com.yeopjari.core.model.Maneuver> get() = scenario.route

    /** 주행 중 예습한 긴장 구간이 있는 안내 지점 index */
    val plannedIndexes: Set<Int> get() = planned.map { it.maneuverIndex }.toSet()

    /** 예습한 긴장 구간 제목 (안내 지점 index → 제목) */
    val plannedTitles: Map<Int, String> get() = planned.associate { it.maneuverIndex to it.title }

    /** 지금 길의 목적지 이름 (화면 표시용) */
    val destinationName: String get() = scenario.destinationName

    init {
        loadPreview()
    }

    fun switchProfile(next: Profile) {
        if (screen is Screen.Driving) return
        profile = next
        loadPreview()
    }

    /** ① 출발 전(P) 길 예습 + 목적지 주차 난이도 + (시니어) 차 안 제안 */
    fun loadPreview() {
        stopSource()
        scenario = profile.scenario()
        screen = Screen.Loading("길을 살펴보는 중이에요")
        toast = null
        viewModelScope.launch {
            val result = withContext(io) {
                runCatching {
                    val p = profile
                    val preview = api.previewRoute(
                        p.driverId, "RECOMMENDED", p.fastMinutes, scenario.route,
                        if (p.easyRoute != null) "COMFORTABLE" else null, p.easyMinutes, p.easyRoute,
                    )
                    val parking = scenario.parkingPlaceId?.let { id ->
                        // 목적지 건물 주차장 기준으로 묻는다 (시연: ○○몰 → 지하주차장)
                        runCatching { api.parkingAdvice(if (id == "mall_flat") "mall_b2" else id) }.getOrNull()
                    }
                    val insights = if (p.mode == DriverMode.SENIOR) {
                        runCatching { api.insights(p.driverId) }.getOrDefault(emptyList())
                    } else emptyList()
                    Screen.Preview(preview, parking, insights)
                }
            }
            screen = result.getOrElse { Screen.Error("서버에 연결하지 못했어요 (${BuildConfig.API_BASE})\n${it.message}") }
            (screen as? Screen.Preview)?.let { if (profile.mode == DriverMode.SENIOR) speakInsights(it) }
        }
    }

    fun chooseEasyRoute() {
        val s = screen as? Screen.Preview ?: return
        val easy = s.preview.easyRoute ?: return
        navigator.applyRouteOption(easy.routeOption)
        // 시연: 쉬운 길 시나리오로 바꿔서, 예습·음성·리포트가 모두 쉬운 길 기준이 되게
        profile.easyScenario?.let { scenario = it() }
        screen = s.copy(chosenEasy = true)
        say(if (easy.spots.isEmpty()) "편한 길로 안내할게요" else "편한 길로 안내할게요. 긴장 구간은 ${easy.spots.size}곳이에요")
    }

    /** 쉬운 길을 골랐다가 다시 빠른 길로 */
    fun chooseFastRoute() {
        val s = screen as? Screen.Preview ?: return
        if (!s.chosenEasy) return
        navigator.applyRouteOption("RECOMMENDED")
        scenario = profile.scenario()
        screen = s.copy(chosenEasy = false)
        say("빠른 길로 안내할게요")
    }

    fun onInsightAction(insight: SeniorInsightDto) {
        val s = screen as? Screen.Preview ?: return
        if (insight.type in s.handledInsights) return
        when (insight.action) {
            "ADD_WAYPOINT_REPAIR_SHOP" -> {
                navigator.addRepairShopWaypoint()
                say("가는 길에 정비소를 들를게요")
            }
            // 앱에 예약 기능은 없으니 약속하지 않고 권하기만 한다
            "PLAN_DAYTIME" -> say(
                if (insight.message.contains("병원")) "좋아요. 병원 예약하실 때 오전 시간을 먼저 물어보세요"
                else "좋아요. 다음엔 밝을 때 다녀오면 더 편해요",
            )
            else -> return
        }
        screen = s.copy(handledInsights = s.handledInsights + insight.type)
    }

    /** ② 출발. 시연은 시나리오 재생, 실차는 Pleos 데이터. */
    fun startDriving() {
        val s = screen as? Screen.Preview ?: return
        planned = s.activeSpots.map { PlannedSpot(it.maneuverIndex, it.type, it.title, it.tip) }
        screen = Screen.Loading("출발 준비 중이에요")
        viewModelScope.launch {
            val tripId = withContext(io) {
                runCatching {
                    api.startTrip(profile.driverId, scenario.destinationName, profile.destinationCategory,
                        scenario.parkingPlaceId, planned.size)
                }
            }.getOrElse {
                screen = Screen.Error("주행을 시작하지 못했어요\n${it.message}")
                return@launch
            }
            recorder = TripRecorder(api, tripId)
            detector = DriveEventDetector(planned, scenario.parkingPlaceId)
            coach = VoiceCoach(profile.mode, planned)
            finishing = false
            spotsPassed = 0
            screen = Screen.Driving("출발해요", 0, 0, null, spotsPlanned = planned.size, gear = Gear.P)
            say(if (planned.isEmpty()) "출발해요. 오늘은 편한 길이에요" else "출발해요. 예습한 곳 ${planned.size}곳, 같이 지나가요")

            source = if (BuildConfig.USE_PLEOS_SDK) PleosDriveSource(getApplication<Application>())
            else ScenarioReplaySource(scenario, HandlerTicker(), speed = 6.0)
            source?.start(::onFrame)
        }
    }

    private fun onFrame(frame: DriveFrame) {
        val d = detector ?: return
        val events = d.onFrame(frame)
        if (events.isNotEmpty()) {
            val r = recorder
            viewModelScope.launch(io) { r?.record(events) }
        }
        spotsPassed += events.count { it.type == EventType.TENSION_SPOT_PASSED }
        val line = coach?.onFrame(frame)
        if (line != null) say(line)

        val current = screen as? Screen.Driving
        if (current != null) {
            val g = frame.guidance
            // 목적지에 처음 도착한 순간 한 번
            if (g?.arrived == true && !current.arrived) say("도착했어요. 주차도 천천히 해요")
            val next = g?.next
            screen = current.copy(
                nextText = when {
                    g == null -> current.nextText // 안내 정보가 없는 프레임(주차 마무리 등)은 그대로
                    g.arrived -> "목적지 도착 · 주차 중"
                    next != null -> "${next.roadName} ${next.type.korean}"
                    else -> "직진"
                },
                distanceM = g?.distanceToNextM ?: 0,
                speedKph = frame.vehicle.speedKph.toInt(),
                // 도착하면 주행 중에 했던 말은 지운다 (주차 화면에 지난 안내가 남지 않게)
                lastVoice = line ?: if (g?.arrived == true) null else current.lastVoice,
                nextType = if (g == null) current.nextType else next?.type,
                spotAhead = next != null && planned.any { it.maneuverIndex == next.index },
                spotsPassed = spotsPassed,
                arrived = g?.arrived ?: current.arrived,
                gear = frame.vehicle.gear,
                nextIndex = if (g == null) current.nextIndex else next?.index,
            )
        }
        if (d.finishedParking && !finishing) finishTrip(frame.vehicle.atMs)
    }

    /** ③ 도착 후(P) 리포트 */
    private fun finishTrip(atMs: Long) {
        finishing = true
        stopSource()
        val d = detector ?: return
        val r = recorder ?: return
        screen = Screen.Loading("오늘 운전을 정리하고 있어요")
        viewModelScope.launch {
            val report = withContext(io) {
                runCatching {
                    r.record(d.finish(atMs))
                    r.finish()
                }
            }
            screen = report.fold(
                onSuccess = { Screen.Report(it) },
                onFailure = { Screen.Error("리포트를 받지 못했어요. 연결되면 다시 시도할게요\n${it.message}") },
            )
            report.getOrNull()?.let { say(it.title) }
        }
    }

    fun dismissToast() {
        toast = null
    }

    private fun speakInsights(p: Screen.Preview) {
        p.insights.firstOrNull { !it.positive }?.let { say(it.message) }
    }

    private fun say(text: String) {
        toast = text
        speaker.speak(text)
    }

    private fun stopSource() {
        source?.stop()
        source = null
    }

    override fun onCleared() {
        stopSource()
        speaker.shutdown()
        io.close()
    }
}
