package com.yeopjari.core.model

/** 같은 엔진, 모드만 다름. 서버 DriverMode 와 이름이 같다. */
enum class DriverMode { NOVICE, SENIOR }

enum class Gear { P, R, N, D }

enum class TurnSignal { OFF, LEFT, RIGHT }

enum class ExteriorLights { OFF, AUTO, TAIL, LOW_BEAM }

/**
 * 차량 상태 한 순간. Pleos Vehicle SDK(또는 시나리오 재생기)에서 온 값을 이 모양으로 맞춘다.
 *
 * @param atMs 주행 시작 기준 시각(ms). 시나리오는 가상 시각, 실차는 SystemClock.elapsedRealtime().
 */
data class VehicleState(
    val atMs: Long,
    val gear: Gear,
    val speedKph: Double,
    val brakePct: Int = 0,
    val turnSignal: TurnSignal = TurnSignal.OFF,
    val lights: ExteriorLights = ExteriorLights.AUTO,
    val telltales: Set<String> = emptySet(),
)

/** 서버 ManeuverType 과 이름이 같다. */
enum class ManeuverType {
    STRAIGHT, TURN_LEFT, TURN_RIGHT, UTURN, MERGE, EXIT, ROUNDABOUT, SCHOOL_ZONE;

    /** 깜빡이를 켜야 하는 안내인지 */
    val needsSignal: Boolean get() = this in setOf(TURN_LEFT, TURN_RIGHT, UTURN, MERGE, EXIT)

    val expectedSignal: TurnSignal
        get() = when (this) {
            TURN_LEFT, UTURN -> TurnSignal.LEFT
            TURN_RIGHT, MERGE, EXIT -> TurnSignal.RIGHT
            else -> TurnSignal.OFF
        }

    val korean: String
        get() = when (this) {
            STRAIGHT -> "직진"; TURN_LEFT -> "좌회전"; TURN_RIGHT -> "우회전"; UTURN -> "유턴"
            MERGE -> "합류"; EXIT -> "진출"; ROUNDABOUT -> "회전교차로"; SCHOOL_ZONE -> "스쿨존"
        }
}

/** 경로 위 안내 지점. 서버 /api/route/preview 요청의 maneuvers 원소와 같은 모양. */
data class Maneuver(
    val index: Int,
    val type: ManeuverType,
    val distanceFromPrevM: Int,
    val lanesToCross: Int = 0,
    val targetLane: Int = 0,
    val speedLimitBefore: Int = 0,
    val speedLimitAfter: Int = 0,
    val roadName: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
)

/**
 * 내비 안내 상태. 실차에서는 NaviHelper getTBTInfo / 경로 콜백에서 만든다.
 *
 * @param next 다음 안내 지점 (없으면 null)
 * @param distanceToNextM 다음 안내 지점까지 남은 거리
 * @param arrived 목적지 도착 안내가 나왔는지
 */
data class Guidance(val next: Maneuver?, val distanceToNextM: Int, val arrived: Boolean = false)

/** 차량 앱이 받는 한 프레임 = 차량 상태 + 내비 안내. */
data class DriveFrame(val vehicle: VehicleState, val guidance: Guidance?)

/** 서버 EventType 과 이름이 같다. attrs 규칙은 서버 EventType.java 주석 참고. */
enum class EventType {
    TENSION_SPOT_PASSED, MERGE_SUCCESS, TURN_SIGNAL_USED, TURN_SIGNAL_MISSED,
    HARD_BRAKE, REVERSE, PARKING_DONE, NIGHT_DRIVING, TELLTALE_ON,
}

data class TripEvent(val type: EventType, val atMs: Long, val attrs: Map<String, String> = emptyMap())

/** 길 예습에서 받은 긴장 구간 중, 주행 중 지나갔는지 확인할 것. */
data class PlannedSpot(val maneuverIndex: Int, val spotType: String, val title: String, val tip: String)
