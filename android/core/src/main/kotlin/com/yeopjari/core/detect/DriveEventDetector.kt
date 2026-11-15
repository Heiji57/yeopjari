package com.yeopjari.core.detect

import com.yeopjari.core.model.DriveFrame
import com.yeopjari.core.model.EventType
import com.yeopjari.core.model.ExteriorLights
import com.yeopjari.core.model.Gear
import com.yeopjari.core.model.Maneuver
import com.yeopjari.core.model.ManeuverType
import com.yeopjari.core.model.PlannedSpot
import com.yeopjari.core.model.TripEvent
import com.yeopjari.core.model.TurnSignal

/**
 * 프레임을 받아 서버로 보낼 주행 이벤트를 만든다. 평가하지 않고 "무슨 일이 있었는지"만 기록한다.
 *
 * - 깜빡이: 회전·합류 지점 150m 전부터 지날 때까지 맞는 방향 깜빡이를 켰는지
 * - 긴장 구간: 지났는지, 부드러웠는지 (그 사이 급브레이크 없음 + 깜빡이 켬)
 * - 급브레이크: 1초 안에 15km/h 이상 감속, 또는 30km/h 이상에서 브레이크 80% 이상
 * - 주차: 도착 안내(또는 첫 R) 후부터 P단까지 후진 횟수·브레이크 횟수·걸린 시간
 * - 밤 운전: 전조등(하향등) 켜고 달린 시간
 * - 경고등: TPMS 등 처음 켜진 경고등
 */
class DriveEventDetector(
    planned: List<PlannedSpot>,
    private val parkingPlaceId: String?,
) {
    companion object {
        const val SIGNAL_WINDOW_M = 150
        const val HARD_BRAKE_DROP_KPH = 15.0
        const val HARD_BRAKE_WINDOW_MS = 1_000L
        const val HARD_BRAKE_PEDAL_PCT = 80
        const val HARD_BRAKE_MIN_SPEED_KPH = 30.0
        const val HARD_BRAKE_COOLDOWN_MS = 3_000L
        const val BRAKE_PRESS_PCT = 10
    }

    private val spotsByIndex = planned.associateBy { it.maneuverIndex }

    private var prev: DriveFrame? = null
    private var current: Maneuver? = null
    private var signalSeenForCurrent = false
    private var hardBrakeSinceCurrent = false
    private var lastHardBrakeAt = Long.MIN_VALUE / 2

    private var parking: ParkingSession? = null
    private var parkingDone = false
    private var nightMs = 0L
    private val telltalesSeen = mutableSetOf<String>()

    /** 주차까지 마치고 P단에 들어갔는지. 앱은 이걸 보고 도착 리포트를 요청한다. */
    var finishedParking: Boolean = false
        private set

    private class ParkingSession(val startedAt: Long) {
        var reverses = 0
        var brakes = 0
    }

    fun onFrame(frame: DriveFrame): List<TripEvent> {
        val out = mutableListOf<TripEvent>()
        val v = frame.vehicle
        val p = prev
        val g = frame.guidance

        // 1) 안내 지점 통과 (안내 정보가 없는 프레임은 건너뛴다)
        if (g != null) {
            val next = g.next
            val before = current
            if (before != null && (next == null || next.index != before.index)) {
                out += onPassed(before, v.atMs)
                current = null
            }
            if (next != null && next.index != before?.index) {
                current = next
                signalSeenForCurrent = false
                hardBrakeSinceCurrent = false
            }
            val now = current
            if (now != null && g.distanceToNextM <= SIGNAL_WINDOW_M &&
                v.turnSignal != TurnSignal.OFF && v.turnSignal == now.type.expectedSignal
            ) {
                signalSeenForCurrent = true
            }
        }

        // 2) 급브레이크
        if (p != null && v.gear == Gear.D) {
            val dt = v.atMs - p.vehicle.atMs
            val drop = p.vehicle.speedKph - v.speedKph
            val byDecel = dt in 1..HARD_BRAKE_WINDOW_MS && drop >= HARD_BRAKE_DROP_KPH
            val byPedal = v.brakePct >= HARD_BRAKE_PEDAL_PCT && p.vehicle.speedKph >= HARD_BRAKE_MIN_SPEED_KPH
            if ((byDecel || byPedal) && v.atMs - lastHardBrakeAt >= HARD_BRAKE_COOLDOWN_MS) {
                lastHardBrakeAt = v.atMs
                hardBrakeSinceCurrent = true
                out += TripEvent(EventType.HARD_BRAKE, v.atMs, mapOf("speedKph" to p.vehicle.speedKph.toInt().toString()))
            }
        }

        // 3) 주차
        if (!parkingDone) {
            if (parking == null && (g?.arrived == true || (v.gear == Gear.R && p?.vehicle?.gear != Gear.R))) {
                parking = ParkingSession(v.atMs)
            }
            parking?.let { s ->
                if (v.gear == Gear.R && p != null && p.vehicle.gear != Gear.R) {
                    s.reverses++
                    out += TripEvent(EventType.REVERSE, v.atMs)
                }
                if (v.brakePct >= BRAKE_PRESS_PCT && (p == null || p.vehicle.brakePct < BRAKE_PRESS_PCT)) {
                    s.brakes++
                }
                if (v.gear == Gear.P && p != null && p.vehicle.gear != Gear.P) {
                    parkingDone = true
                    finishedParking = true
                    val attrs = buildMap {
                        parkingPlaceId?.let { put("placeId", it) }
                        put("reverseCount", s.reverses.toString())
                        put("brakeCount", s.brakes.toString())
                        put("durationSec", ((v.atMs - s.startedAt) / 1000).toString())
                    }
                    out += TripEvent(EventType.PARKING_DONE, v.atMs, attrs)
                }
            }
        }

        // 4) 밤 운전
        if (p != null && v.lights == ExteriorLights.LOW_BEAM && v.speedKph > 5) {
            nightMs += (v.atMs - p.vehicle.atMs).coerceAtLeast(0)
        }

        // 5) 경고등
        for (t in v.telltales) {
            if (telltalesSeen.add(t)) {
                out += TripEvent(EventType.TELLTALE_ON, v.atMs, mapOf("name" to t))
            }
        }

        prev = frame
        return out
    }

    /** 주행 끝(리포트 요청 직전)에 한 번. 쌓아 둔 밤 운전 시간 등을 내보낸다. */
    fun finish(atMs: Long): List<TripEvent> {
        val minutes = (nightMs / 60_000).toInt()
        nightMs = 0
        return if (minutes >= 1) listOf(TripEvent(EventType.NIGHT_DRIVING, atMs, mapOf("minutes" to minutes.toString())))
        else emptyList()
    }

    private fun onPassed(m: Maneuver, atMs: Long): List<TripEvent> {
        val out = mutableListOf<TripEvent>()
        if (m.type.needsSignal) {
            val type = if (signalSeenForCurrent) EventType.TURN_SIGNAL_USED else EventType.TURN_SIGNAL_MISSED
            out += TripEvent(type, atMs, mapOf("maneuver" to m.type.name))
        }
        if (m.type == ManeuverType.MERGE) {
            out += TripEvent(EventType.MERGE_SUCCESS, atMs, mapOf("road" to m.roadName))
        }
        spotsByIndex[m.index]?.let { spot ->
            val smooth = !hardBrakeSinceCurrent && (!m.type.needsSignal || signalSeenForCurrent)
            out += TripEvent(
                EventType.TENSION_SPOT_PASSED, atMs,
                mapOf("spotType" to spot.spotType, "road" to m.roadName, "smooth" to smooth.toString()),
            )
        }
        return out
    }
}
