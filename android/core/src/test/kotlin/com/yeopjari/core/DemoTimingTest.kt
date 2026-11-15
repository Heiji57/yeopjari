package com.yeopjari.core

import com.yeopjari.core.coach.VoiceCoach
import com.yeopjari.core.model.DriverMode
import com.yeopjari.core.model.EventType
import com.yeopjari.core.model.PlannedSpot
import com.yeopjari.core.scenario.DemoScenarios
import com.yeopjari.core.scenario.Scenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 시연(6배속 재생)에서 보이고 들리는 것. */
class DemoTimingTest {

    companion object {
        const val REPLAY_SPEED = 6.0 // AppViewModel 의 ScenarioReplaySource 배속

        /** 한국어 TTS 대략 초당 6.5음절(속도 0.95) + 앞뒤 여유. 어림값이라 넉넉하게 잡는다. */
        fun speechMs(line: String): Long =
            ((line.count { it in '가'..'힣' || it.isDigit() } / 6.5 + 0.5) * 1000).toLong()
    }

    private fun voiceLines(s: Scenario, spots: List<PlannedSpot>, mode: DriverMode = DriverMode.NOVICE): List<Pair<Long, String>> {
        val coach = VoiceCoach(mode, spots)
        return s.frames.mapNotNull { f -> coach.onFrame(f)?.let { f.vehicle.atMs to it } }
    }

    private fun assertNoOverlap(lines: List<Pair<Long, String>>) {
        for ((a, b) in lines.zipWithNext()) {
            val gapRealMs = ((b.first - a.first) / REPLAY_SPEED).toLong()
            assertTrue(
                "\"${a.second}\" 를 다 말하기 전에(${speechMs(a.second)}ms) 다음 말이 와요: \"${b.second}\" (${gapRealMs}ms 뒤)",
                gapRealMs >= speechMs(a.second),
            )
        }
    }

    @Test
    fun voiceLinesDoNotPileUpAt6x() {
        assertNoOverlap(voiceLines(DemoScenarios.soyeonFirstGangbyeon(), demoSpots))
    }

    @Test
    fun demoDriveTakesAboutAMinuteAt6x() {
        val s = DemoScenarios.soyeonFirstGangbyeon()
        val realSec = (s.frames.last().vehicle.atMs - s.frames.first().vehicle.atMs) / 1000.0 / REPLAY_SPEED
        assertTrue("재생 ${realSec}초", realSec in 35.0..75.0)
    }

    @Test
    fun signalNudgeComesBeforeTheTurnNotAtIt() {
        // 일부러 깜빡이를 놓치는 포은로 좌회전에서도, 제안은 회전 전에 나와야 한다
        val coach = VoiceCoach(DriverMode.NOVICE, demoSpots)
        val nudges = DemoScenarios.soyeonFirstGangbyeon().frames.mapNotNull { f ->
            coach.onFrame(f)?.takeIf { it.contains("깜빡이") }?.let { f.guidance!!.distanceToNextM }
        }
        assertEquals(2, nudges.size)
        assertTrue("제안 거리 $nudges", nudges.all { it in 1..80 })
    }

    @Test
    fun easyRouteHasOneSpotAndNoMerge() {
        val easySpots = listOf(PlannedSpot(3, "SCHOOL_ZONE", "스쿨존 60→30", "미리 속도를 줄여요"))
        val (events, detector) = runDetector(DemoScenarios.soyeonEasyRoute(), easySpots)

        assertEquals(0, events.count { it.type == EventType.MERGE_SUCCESS })
        assertEquals(listOf("SCHOOL_ZONE"), events.filter { it.type == EventType.TENSION_SPOT_PASSED }.map { it.attrs["spotType"] })
        assertEquals("mall_flat", events.single { it.type == EventType.PARKING_DONE }.attrs["placeId"])
        assertTrue(detector.finishedParking)

        val lines = voiceLines(DemoScenarios.soyeonEasyRoute(), easySpots)
        assertTrue(lines.none { it.second.contains("합류") })
        assertTrue(lines.any { it.second.contains("스쿨존") })
        assertNoOverlap(lines)
    }

    @Test
    fun fatherVoiceLinesDoNotPileUp() {
        val spots = listOf(PlannedSpot(3, "SCHOOL_ZONE", "스쿨존 60→30", "미리 속도를 줄여요"))
        assertNoOverlap(voiceLines(DemoScenarios.fatherEveningHospital(), spots, DriverMode.SENIOR))
    }
}
