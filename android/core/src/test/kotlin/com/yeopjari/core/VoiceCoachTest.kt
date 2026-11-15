package com.yeopjari.core

import com.yeopjari.core.coach.VoiceCoach
import com.yeopjari.core.model.DriverMode
import com.yeopjari.core.scenario.DemoScenarios
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VoiceCoachTest {

    private fun lines(mode: DriverMode): List<String> {
        val coach = VoiceCoach(mode, demoSpots)
        return DemoScenarios.soyeonFirstGangbyeon().frames.mapNotNull { coach.onFrame(it) }
    }

    @Test
    fun noviceHearsTheLinesFromTheIdeaDoc() {
        assertEquals(
            listOf(
                "500m 뒤 합류예요. 지금 오른쪽 차로로 가 있을게요",
                "예습한 구간이에요. 우회전 후 100m 좌회전, 미리 차로를 골라 둘게요",
                "왼쪽 깜빡이 켜볼까요?",
                "곧 스쿨존이에요. 30으로 천천히 갈게요",
                "왼쪽 깜빡이 켜볼까요?",
            ),
            lines(DriverMode.NOVICE),
        )
    }

    @Test
    fun neverScolds() {
        val all = lines(DriverMode.NOVICE) + lines(DriverMode.SENIOR)
        for (word in listOf("감점", "위반", "위험해요", "왜", "잘못")) {
            assertFalse(all.any { it.contains(word) })
        }
    }
}
