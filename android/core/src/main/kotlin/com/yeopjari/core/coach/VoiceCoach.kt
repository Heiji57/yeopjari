package com.yeopjari.core.coach

import com.yeopjari.core.model.DriveFrame
import com.yeopjari.core.model.DriverMode
import com.yeopjari.core.model.Gear
import com.yeopjari.core.model.ManeuverType
import com.yeopjari.core.model.PlannedSpot
import com.yeopjari.core.model.TurnSignal

/**
 * 옆자리에 앉은 사람처럼 말해 주는 규칙. 지적하지 않고 "같이 하자"는 말투만 쓴다.
 *
 * - 합류 500m 전: "500m 뒤 합류예요. 지금 오른쪽 차로로 가 있을게요"
 * - 긴장 구간 300m 전: 예습 때 본 내용 한 줄
 * - 회전 80m 전인데 깜빡이가 꺼져 있으면: "오른쪽 깜빡이 켜볼까요?"
 *
 * 같은 지점에서는 한 번만 말하고, 말 사이에는 최소 4초를 둔다.
 */
class VoiceCoach(
    private val mode: DriverMode,
    planned: List<PlannedSpot>,
) {
    companion object {
        const val MERGE_NOTICE_M = 500
        const val SPOT_NOTICE_M = 300
        const val SIGNAL_NUDGE_M = 80
        const val MIN_GAP_MS = 4_000L
    }

    private val spots = planned.associateBy { it.maneuverIndex }
    private val said = mutableSetOf<String>()
    private var lastSpokeAt = Long.MIN_VALUE / 2

    /** 이번 프레임에 말할 문장. 없으면 null. */
    fun onFrame(frame: DriveFrame): String? {
        val v = frame.vehicle
        val g = frame.guidance ?: return null
        val next = g.next ?: return null
        if (v.gear != Gear.D || g.arrived) return null
        val d = g.distanceToNextM

        val line: Pair<String, String>? = when {
            next.type == ManeuverType.MERGE && d <= MERGE_NOTICE_M ->
                "merge-${next.index}" to mergeLine(roundDown100(d))

            next.type.needsSignal && d <= SIGNAL_NUDGE_M && v.turnSignal != next.type.expectedSignal && v.speedKph > 0 ->
                "signal-${next.index}" to signalLine(next.type.expectedSignal)

            spots.containsKey(next.index) && d <= SPOT_NOTICE_M && next.type != ManeuverType.MERGE ->
                "spot-${next.index}" to spotLine(spots.getValue(next.index), next.speedLimitAfter)

            else -> null
        }
        if (line == null || line.first in said) return null
        // 깜빡이 제안은 놓치면 의미가 없으니 간격 제한을 받지 않는다
        if (!line.first.startsWith("signal") && v.atMs - lastSpokeAt < MIN_GAP_MS) return null
        said += line.first
        lastSpokeAt = v.atMs
        return line.second
    }

    private fun mergeLine(meters: Int): String = when (mode) {
        DriverMode.NOVICE -> "${meters}m 뒤 합류예요. 지금 오른쪽 차로로 가 있을게요"
        DriverMode.SENIOR -> "${meters}미터 뒤에 합류해요. 오른쪽 차로로 천천히 옮겨 주세요"
    }

    private fun signalLine(signal: TurnSignal): String {
        val side = if (signal == TurnSignal.LEFT) "왼쪽" else "오른쪽"
        return when (mode) {
            DriverMode.NOVICE -> "$side 깜빡이 켜볼까요?"
            DriverMode.SENIOR -> "$side 깜빡이를 켜 주세요"
        }
    }

    private fun spotLine(spot: PlannedSpot, limitAfter: Int): String = when (spot.spotType) {
        "SCHOOL_ZONE" -> if (limitAfter > 0) "곧 스쿨존이에요. ${limitAfter}으로 천천히 갈게요" else "곧 스쿨존이에요. 천천히 갈게요"
        "QUICK_SUCCESSIVE_TURNS" -> "예습한 구간이에요. ${spot.title}, 미리 차로를 골라 둘게요"
        else -> "예습한 구간이에요. ${spot.tip}"
    }

    private fun roundDown100(m: Int): Int = maxOf(100, m / 100 * 100)
}
