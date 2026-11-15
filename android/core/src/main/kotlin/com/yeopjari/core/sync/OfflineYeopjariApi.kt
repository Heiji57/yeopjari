package com.yeopjari.core.sync

import com.yeopjari.core.model.EventType
import com.yeopjari.core.model.Maneuver
import com.yeopjari.core.model.ManeuverType
import com.yeopjari.core.model.TripEvent
import kotlin.math.roundToInt

/**
 * 서버 없이 앱만 설치해도 시연이 끝까지 흘러가도록, 서버 규칙을 간단히 옮긴 내장 시연 데이터.
 * (서버 DemoSeed 의 김씨 가족과 같은 숫자: 지난 깜빡이 83%, 주차 후진 평균 5번, TPMS 5일째 …)
 *
 * 서버가 있으면 [FallbackYeopjariApi] 가 서버를 쓰고, 첫 호출이 실패하면 이 구현으로 바꾼다.
 */
class OfflineYeopjariApi : YeopjariApi {

    private class OfflineTrip(val driverId: String, val planned: Int) {
        val events = mutableListOf<TripEvent>()
        var report: ReportDto? = null
    }

    private val trips = mutableMapOf<String, OfflineTrip>()
    private var seq = 0

    /** 운전자별 지난 기록 (세션 안에서 이어진다) */
    private val lastSignal = mutableMapOf("soyeon" to 83)
    private val lastReverse = mutableMapOf<String, Pair<Int, Boolean>>("soyeon" to (5 to false)) // (값, 같은 주차장인지)
    private val roadsMerged = mutableMapOf("soyeon" to mutableSetOf<String>())

    // ───────── 길 예습 ─────────

    override fun previewRoute(
        driverId: String, fastOption: String, fastMinutes: Int, fast: List<Maneuver>,
        easyOption: String?, easyMinutes: Int?, easy: List<Maneuver>?,
    ): RoutePreviewDto {
        val senior = driverId == "father"
        val fastSpots = detect(fast, senior, driverId)
        var offer: EasyRouteDto? = null
        if (easy != null && easyMinutes != null && easy.isNotEmpty()) {
            val easySpots = detect(easy, senior, driverId)
            val extra = easyMinutes - fastMinutes
            if (extra in 0..10 && easySpots.sumOf { it.severity } < fastSpots.sumOf { it.severity }) {
                offer = EasyRouteDto(
                    easyOption ?: "COMFORTABLE", extra, fastSpots.size, easySpots.size,
                    "쉬운 길 +${extra}분 · 긴장 구간 ${fastSpots.size}→${easySpots.size}", easySpots,
                )
            }
        }
        return RoutePreviewDto(fastMinutes, fastSpots, offer)
    }

    /** 서버 TensionSpotDetector 의 규칙 (개인화는 "처음 가는 길 합류 +1" 만) */
    private fun detect(route: List<Maneuver>, senior: Boolean, driverId: String): List<TensionSpotDto> {
        val out = mutableListOf<TensionSpotDto>()
        route.forEachIndexed { i, m ->
            val next = route.getOrNull(i + 1)
            val spot: Triple<String, Int, Pair<String, String>>? = when (m.type) {
                ManeuverType.MERGE -> {
                    val quick = m.lanesToCross >= 2 && next != null && next.distanceFromPrevM <= 800
                    val firstTime = m.roadName !in roadsMerged[driverId].orEmpty()
                    val prefix = if (firstTime) "처음 가는 길이에요. " else ""
                    if (quick) {
                        Triple("MERGE_THEN_LANE_CHANGE", 3, (if (m.targetLane > 0) "합류 후 바로 ${m.targetLane}차로" else "합류 후 바로 차로 변경") to
                            "${prefix}합류 500m 전부터 오른쪽 차로에서 기다렸다가, 합류 후엔 한 차로씩 천천히 옮겨요.")
                    } else {
                        Triple("MERGE", if (firstTime) 3 else 2, "${m.roadName.ifBlank { "도로" }} 합류" to "${prefix}합류 차로 끝까지 가속해서 옆 차 속도에 맞춰요.")
                    }
                }
                ManeuverType.TURN_LEFT, ManeuverType.TURN_RIGHT ->
                    if (next != null && (next.type == ManeuverType.TURN_LEFT || next.type == ManeuverType.TURN_RIGHT) && next.distanceFromPrevM <= 150) {
                        val gap = maxOf(10, (next.distanceFromPrevM / 10f).roundToInt() * 10)
                        Triple("QUICK_SUCCESSIVE_TURNS", if (next.distanceFromPrevM <= 100) 3 else 2,
                            "${m.type.korean} 후 ${gap}m ${next.type.korean}" to "첫 회전 전에, 바로 다음 ${next.type.korean} 할 차로를 미리 정해 둬요.")
                    } else null
                ManeuverType.SCHOOL_ZONE -> Triple("SCHOOL_ZONE", 2,
                    (if (m.speedLimitBefore > 0 && m.speedLimitAfter > 0) "스쿨존 ${m.speedLimitBefore}→${m.speedLimitAfter}" else "스쿨존") to
                        "들어가기 전에 미리 속도를 줄이고, 갓길의 아이들을 살펴요.")
                ManeuverType.UTURN -> Triple("UTURN", if (senior) 3 else 2, "유턴 구간" to "맞은편 차가 다 지나간 뒤 천천히 돌아요.")
                ManeuverType.ROUNDABOUT -> Triple("ROUNDABOUT", if (senior) 3 else 2, "회전교차로" to "안에 있는 차가 먼저예요. 왼쪽을 보고 들어가요.")
                else -> if (m.speedLimitAfter > 0 && m.speedLimitBefore - m.speedLimitAfter >= 20) {
                    Triple("SPEED_DROP", 1, "제한속도 ${m.speedLimitBefore}→${m.speedLimitAfter}" to "단속 구간 전에 미리 속도를 줄여요.")
                } else null
            }
            spot?.let { (type, sev, text) ->
                out += TensionSpotDto("spot-${m.index}", m.index, type, sev, text.first, text.second, m.roadName)
            }
        }
        return out.sortedWith(compareByDescending<TensionSpotDto> { it.severity }.thenBy { it.maneuverIndex })
            .take(3).sortedBy { it.maneuverIndex }
    }

    override fun parkingAdvice(placeId: String): ParkingAdviceDto = when (placeId) {
        "mall_b2" -> ParkingAdviceDto(4, "○○몰 지하주차장", "공영 평면주차장", 1, 3,
            "○○몰 지하주차장 ★★★★ (후진 평균 5번) → 도보 3분 공영 평면주차장 ★ 추천")
        "hospital" -> ParkingAdviceDto(2, "한빛병원 주차장", null, null, null, "한빛병원 주차장 ★★ (후진 평균 1번)")
        else -> ParkingAdviceDto(0, placeId, null, null, null, "$placeId (아직 기록이 적어요)")
    }

    override fun insights(driverId: String): List<SeniorInsightDto> =
        if (driverId != "father") emptyList() else listOf(
            SeniorInsightDto("TPMS", "타이어 공기압 경고 5일째예요. 가는 길에 정비소 들를까요?", "ADD_WAYPOINT_REPAIR_SHOP", false),
            SeniorInsightDto("NIGHT_DRIVING", "밤 운전이 늘었어요. 병원 예약은 오전으로 해볼까요?", "PLAN_DAYTIME", false),
            SeniorInsightDto("STABLE", "이번 주 운전, 안정적이었어요.", "NONE", true),
        )

    // ───────── 주행 · 리포트 ─────────

    override fun startTrip(driverId: String, destinationName: String, destinationCategory: String,
                           placeId: String?, tensionSpotsPlanned: Int): String {
        val id = "offline-${++seq}"
        trips[id] = OfflineTrip(driverId, tensionSpotsPlanned)
        return id
    }

    override fun sendEvents(tripId: String, events: List<TripEvent>) {
        val t = trips[tripId] ?: error("주행 없음: $tripId")
        check(t.report == null) { "이미 끝난 주행이에요: $tripId" }
        t.events += events
    }

    override fun finishTrip(tripId: String): ReportDto {
        val t = trips[tripId] ?: error("주행 없음: $tripId")
        t.report?.let { return it }
        val ev = t.events
        fun count(type: EventType) = ev.count { it.type == type }
        val items = mutableListOf<ReportItemDto>()

        if (t.planned > 0) {
            val passed = minOf(count(EventType.TENSION_SPOT_PASSED), t.planned)
            items += ReportItemDto("tension", "긴장 구간", "$passed/${t.planned}", "NONE", true,
                if (passed == t.planned) "모두 지나왔어요" else "${passed}곳 지나왔어요")
        }

        val used = count(EventType.TURN_SIGNAL_USED)
        val total = used + count(EventType.TURN_SIGNAL_MISSED)
        val signal = if (total == 0) null else (used * 100f / total).roundToInt()
        if (signal != null) {
            val prev = lastSignal[t.driverId]
            val trend = prev?.let { trend(signal, it, 3) } ?: "NONE"
            items += ReportItemDto("signal", "깜빡이", "$signal%", trend, trend != "DOWN",
                prev?.let { "지난번 $it%" } ?: "회전·합류 때 켠 비율")
            lastSignal[t.driverId] = signal
        }

        val parking = ev.lastOrNull { it.type == EventType.PARKING_DONE }
        val reverse = parking?.attrs?.get("reverseCount")?.toIntOrNull()
        if (reverse != null) {
            val base = lastReverse[t.driverId]
            if (base != null) {
                val trend = trend(reverse, base.first, 0)
                val note = when (trend) {
                    "DOWN" -> if (base.second) "같은 주차장 지난번보다 줄었어요" else "평소보다 줄었어요"
                    "UP" -> "오늘은 자리가 좁았나 봐요"
                    else -> if (base.second) "지난번과 비슷해요" else "평소와 비슷해요"
                }
                items += ReportItemDto("parking", "주차 후진", "${base.first}→${reverse}번", trend, trend != "UP", note)
            } else {
                items += ReportItemDto("parking", "주차 후진", "${reverse}번", "NONE", true, "첫 기록이에요")
            }
            lastReverse[t.driverId] = reverse to true
        }

        val known = roadsMerged.getOrPut(t.driverId) { mutableSetOf() }
        val milestones = ev.filter { it.type == EventType.MERGE_SUCCESS }
            .mapNotNull { it.attrs["road"]?.takeIf { r -> r.isNotBlank() } }
            .distinct().filter { it !in known }.map { "첫 $it 합류 성공" }
        ev.filter { it.type == EventType.MERGE_SUCCESS }.mapNotNullTo(known) { it.attrs["road"] }

        val title = when {
            t.driverId == "father" -> "오늘 운전, 차분했어요"
            milestones.isNotEmpty() -> "오늘 ${milestones.first()}!"
            else -> "오늘도 한 뼘 늘었어요"
        }
        val next = when {
            signal != null && signal < 90 -> "다음엔 회전 50m 전에 깜빡이를 먼저 켜볼까요?"
            count(EventType.HARD_BRAKE) >= 2 -> "앞차와 거리를 한 칸 더 두면 브레이크가 부드러워져요."
            reverse != null && reverse >= 4 -> "다음엔 출발 전에 평면 주차장을 먼저 골라볼까요?"
            else -> "오늘처럼만 가면 돼요. 다음엔 조금 더 먼 길도 괜찮아요."
        }
        return ReportDto(tripId, title, items, milestones, next).also { t.report = it }
    }

    private fun trend(current: Int, previous: Int, threshold: Int) = when {
        current - previous > threshold -> "UP"
        previous - current > threshold -> "DOWN"
        else -> "SAME"
    }
}

/**
 * 서버를 먼저 쓰고, 첫 길 예습 호출이 실패하면 그 뒤로는 [OfflineYeopjariApi] 를 쓴다.
 * 한 번 정해지면 바꾸지 않는다 (주행 도중 서버·내장 데이터가 섞이지 않게).
 */
class FallbackYeopjariApi(
    private val primary: YeopjariApi,
    private val offline: YeopjariApi = OfflineYeopjariApi(),
) : YeopjariApi {

    @Volatile
    var isOffline: Boolean = false
        private set
    private var decided = false

    private fun api(): YeopjariApi = if (isOffline) offline else primary

    override fun previewRoute(driverId: String, fastOption: String, fastMinutes: Int, fast: List<Maneuver>,
                              easyOption: String?, easyMinutes: Int?, easy: List<Maneuver>?): RoutePreviewDto {
        if (!decided) {
            decided = true
            try {
                return primary.previewRoute(driverId, fastOption, fastMinutes, fast, easyOption, easyMinutes, easy)
            } catch (e: Exception) {
                isOffline = true
            }
        }
        return api().previewRoute(driverId, fastOption, fastMinutes, fast, easyOption, easyMinutes, easy)
    }

    override fun startTrip(driverId: String, destinationName: String, destinationCategory: String,
                           placeId: String?, tensionSpotsPlanned: Int): String =
        api().startTrip(driverId, destinationName, destinationCategory, placeId, tensionSpotsPlanned)

    override fun sendEvents(tripId: String, events: List<TripEvent>) = api().sendEvents(tripId, events)
    override fun finishTrip(tripId: String): ReportDto = api().finishTrip(tripId)
    override fun parkingAdvice(placeId: String): ParkingAdviceDto = api().parkingAdvice(placeId)
    override fun insights(driverId: String): List<SeniorInsightDto> = api().insights(driverId)
}
