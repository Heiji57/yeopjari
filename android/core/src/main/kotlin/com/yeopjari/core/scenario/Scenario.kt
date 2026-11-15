package com.yeopjari.core.scenario

import com.yeopjari.core.model.DriveFrame
import com.yeopjari.core.model.ExteriorLights
import com.yeopjari.core.model.Gear
import com.yeopjari.core.model.Guidance
import com.yeopjari.core.model.Maneuver
import com.yeopjari.core.model.TurnSignal
import com.yeopjari.core.model.VehicleState

/**
 * 시연용 주행 시나리오. 프레임 목록 + 경로.
 * 에뮬레이터가 속도·위치 주행을 못 해도 언제나 같은 결과로 시연할 수 있게 해 준다.
 */
data class Scenario(
    val name: String,
    val route: List<Maneuver>,
    val frames: List<DriveFrame>,
    val parkingPlaceId: String?,
    val destinationName: String,
)

/**
 * 시나리오를 짧게 쓰기 위한 빌더. 시각은 가상 시각(1프레임 = 1초)이다.
 *
 * ```
 * scenario("소연이의 첫 강변북로", route, "mall_flat", "○○몰") {
 *     park(2); shift(Gear.D)
 *     approach(0, fromM = 300, kph = 30, signalAtM = 120)
 *     arrive(); reverse(2); park(1)
 * }
 * ```
 */
class ScenarioBuilder(private val route: List<Maneuver>) {
    companion object {
        const val MAX_KPH_CHANGE_PER_FRAME = 8.0
    }

    private val frames = mutableListOf<DriveFrame>()
    private var t = 0L
    private var gear = Gear.P
    private var speed = 0.0
    private var lights = ExteriorLights.AUTO
    private var telltales = emptySet<String>()
    private var nextIndex = 0

    private fun emit(
        guidance: Guidance?,
        signal: TurnSignal = TurnSignal.OFF,
        brake: Int = 0,
        stepMs: Long = 1_000,
    ) {
        frames += DriveFrame(VehicleState(t, gear, speed, brake, signal, lights, telltales), guidance)
        t += stepMs
    }

    private fun currentGuidance(distance: Int): Guidance? =
        route.getOrNull(nextIndex)?.let { Guidance(it, distance) }

    fun park(seconds: Int = 1) {
        gear = Gear.P; speed = 0.0
        repeat(seconds) { emit(currentGuidance(route.getOrNull(nextIndex)?.distanceFromPrevM ?: 0), brake = 30) }
    }

    fun shift(to: Gear) {
        gear = to
        emit(currentGuidance(route.getOrNull(nextIndex)?.distanceFromPrevM ?: 0), brake = 30)
    }

    fun lights(mode: ExteriorLights) { lights = mode }

    fun telltale(name: String) { telltales = telltales + name }

    /**
     * 안내 지점 [index] 까지 [fromM] 에서 0m 까지 [kph] 로 접근한 뒤 통과한다.
     *
     * @param signalAtM 이 거리부터 맞는 방향 깜빡이를 켠다 (null 이면 안 켬)
     * @param stepM 한 프레임에 줄어드는 거리
     * @param hardBrakeAtM 이 거리에서 급제동 (테스트용)
     * @param nearM 이 거리 안쪽부터는 [nearStepM] 씩 줄인다 (음성 안내가 나오는 구간을 실제 속도에 가깝게 늘려서,
     *   빨리 감기 재생에서도 말이 화면보다 늦지 않게)
     * @param nearStepM null 이면 끝까지 [stepM]
     */
    fun approach(
        index: Int,
        fromM: Int,
        kph: Int,
        signalAtM: Int? = null,
        stepM: Int = 50,
        hardBrakeAtM: Int? = null,
        nearM: Int = 300,
        nearStepM: Int? = null,
    ) {
        require(index == nextIndex) { "안내 지점은 순서대로: 기대 $nextIndex, 받은 $index" }
        val m = route[index]
        val target = kph.toDouble()
        var d = fromM
        while (d > 0) {
            // 실제 차처럼 1초에 최대 8km/h 씩 속도를 바꾼다 (급제동으로 잡히지 않게)
            speed = if (speed < target) minOf(target, speed + MAX_KPH_CHANGE_PER_FRAME)
            else maxOf(target, speed - MAX_KPH_CHANGE_PER_FRAME)
            val signal = if (signalAtM != null && d <= signalAtM) m.type.expectedSignal else TurnSignal.OFF
            if (hardBrakeAtM != null && d == hardBrakeAtM) {
                val before = speed
                speed = (before - 25).coerceAtLeast(5.0)
                emit(Guidance(m, d), signal, brake = 90, stepMs = 700)
                speed = before
            } else {
                emit(Guidance(m, d), signal)
            }
            d -= if (nearStepM != null && d <= nearM) nearStepM else stepM
        }
        emit(Guidance(m, 0), if (signalAtM != null) m.type.expectedSignal else TurnSignal.OFF)
        nextIndex++
    }

    /** 다음 안내 지점이 없을 때까지 가다가 목적지 도착. */
    fun arrive() {
        while (speed > 12) {
            speed -= MAX_KPH_CHANGE_PER_FRAME
            emit(Guidance(null, 0), brake = 15)
        }
        speed = 10.0
        emit(Guidance(null, 0, arrived = true), brake = 20)
        speed = 0.0
        emit(Guidance(null, 0, arrived = true), brake = 40)
    }

    /** 주차하며 R 을 [times] 번 넣는다. */
    fun reverse(times: Int) {
        repeat(times) {
            gear = Gear.R; speed = 3.0
            emit(Guidance(null, 0, arrived = true), stepMs = 4_000)
            emit(Guidance(null, 0, arrived = true), brake = 30, stepMs = 2_000)
            gear = Gear.D; speed = 2.0
            emit(Guidance(null, 0, arrived = true), stepMs = 3_000)
            emit(Guidance(null, 0, arrived = true), brake = 30, stepMs = 2_000)
        }
        speed = 0.0
    }

    fun build(name: String, parkingPlaceId: String?, destinationName: String) =
        Scenario(name, route, frames.toList(), parkingPlaceId, destinationName)
}

fun scenario(
    name: String,
    route: List<Maneuver>,
    parkingPlaceId: String?,
    destinationName: String,
    block: ScenarioBuilder.() -> Unit,
): Scenario = ScenarioBuilder(route).apply(block).build(name, parkingPlaceId, destinationName)
