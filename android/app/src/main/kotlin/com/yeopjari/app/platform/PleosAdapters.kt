package com.yeopjari.app.platform

import android.content.Context
import android.os.SystemClock
import com.yeopjari.core.model.DriveFrame
import com.yeopjari.core.model.ExteriorLights
import com.yeopjari.core.model.Gear
import com.yeopjari.core.model.Guidance
import com.yeopjari.core.model.Maneuver
import com.yeopjari.core.model.TurnSignal
import com.yeopjari.core.model.VehicleState
import com.yeopjari.core.scenario.DriveSignalSource

/**
 * ─────────────────────────────────────────────────────────────────────────
 *  Pleos SDK 를 만지는 곳은 이 파일 하나뿐이다.
 *  SDK 접근 승인 → gradle.properties 에 저장소·의존성 입력 → 아래 TODO 를 채운다.
 *  나머지 앱 코드는 DriveSignalSource / RouteNavigator 만 보므로 바꿀 필요가 없다.
 * ─────────────────────────────────────────────────────────────────────────
 *
 * 값이 들어오는 대로 on…() 을 부르면, 최신 값을 모아 DriveFrame 하나로 내보낸다.
 * 프레임은 값이 바뀔 때마다 + 1초마다 한 번 (시간 기반 판단: 급브레이크, 밤 운전).
 */
class PleosDriveSource(private val context: Context) : DriveSignalSource {

    private var listener: ((DriveFrame) -> Unit)? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    @Volatile private var gear = Gear.P
    @Volatile private var speedKph = 0.0
    @Volatile private var brakePct = 0
    @Volatile private var signal = TurnSignal.OFF
    @Volatile private var lights = ExteriorLights.AUTO
    @Volatile private var telltales: Set<String> = emptySet()
    @Volatile private var guidance: Guidance? = null

    private val heartbeat = object : Runnable {
        override fun run() {
            emit()
            handler.postDelayed(this, 1_000)
        }
    }

    override fun start(listener: (DriveFrame) -> Unit) {
        this.listener = listener
        // TODO(Vehicle SDK): 초기화 후 아래 값의 변경 리스너를 등록한다.
        //   Motion      : 기어(P/R/N/D) → onGear, 브레이크 페달(0~100%) → onBrake, 차속 → onSpeed
        //   Convenience : 방향지시등(Off/Left/Right Blink) → onTurnSignal, 외부등 모드(Off/Auto/Tail/Low beam) → onLights
        //   Safety      : 경고등(Telltales, 예: TPMS) → onTelltales
        //   (Virtual Console 의 카드 이름과 같다. 차속 카드가 없으면 버추얼 콘솔에선 시나리오 모드로 시연)
        //
        // TODO(NaviHelper SDK): initialize() 후 addListener(NaviHelperEventListener) 등록,
        //   getTBTInfo() 콜백 → onGuidance(다음 안내 지점, 남은 거리)
        //   도착 콜백 → onGuidance(null, 0, arrived = true)
        handler.post(heartbeat)
    }

    override fun stop() {
        handler.removeCallbacks(heartbeat)
        // TODO(SDK): 리스너 해제, NaviHelper.removeListener(), release()
        listener = null
    }

    // ── SDK 콜백에서 부르는 함수들 ──
    fun onGear(value: Gear) { gear = value; emit() }
    fun onSpeed(kph: Double) { speedKph = kph }
    fun onBrake(pct: Int) { brakePct = pct; emit() }
    fun onTurnSignal(value: TurnSignal) { signal = value; emit() }
    fun onLights(value: ExteriorLights) { lights = value }
    fun onTelltales(names: Set<String>) { telltales = names; emit() }
    fun onGuidance(next: Maneuver?, distanceM: Int, arrived: Boolean = false) {
        guidance = Guidance(next, distanceM, arrived); emit()
    }

    private fun emit() {
        val state = VehicleState(SystemClock.elapsedRealtime(), gear, speedKph, brakePct, signal, lights, telltales)
        handler.post { listener?.invoke(DriveFrame(state, guidance)) }
    }
}

/** Pleos NaviHelper 로 경로 옵션 변경·경유지 추가. */
class PleosNavigator(private val context: Context) : RouteNavigator {
    override fun applyRouteOption(option: String) {
        // TODO(NaviHelper SDK): naviHelper.changeRouteOption(RouteOption.valueOf(option))
        //   (목적지 설정 전이면 requestRoute(RouteInfo(..., routeOption = RouteOption.valueOf(option))))
    }

    override fun addRepairShopWaypoint() {
        // TODO(NaviHelper SDK): naviHelper.addWaypoint(RequestWaypointInfo(경도, 위도, "정비소 이름", WaypointIndex.FIRST, ...))
        //   정비소 좌표는 처음엔 고정값, 나중에 지도 검색 API 로.
    }
}
