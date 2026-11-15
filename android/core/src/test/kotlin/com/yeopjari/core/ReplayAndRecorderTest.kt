package com.yeopjari.core

import com.yeopjari.core.model.DriveFrame
import com.yeopjari.core.model.Maneuver
import com.yeopjari.core.model.TripEvent
import com.yeopjari.core.model.EventType
import com.yeopjari.core.scenario.DemoScenarios
import com.yeopjari.core.scenario.ScenarioReplaySource
import com.yeopjari.core.scenario.Ticker
import com.yeopjari.core.sync.ParkingAdviceDto
import com.yeopjari.core.sync.ReportDto
import com.yeopjari.core.sync.RoutePreviewDto
import com.yeopjari.core.sync.SeniorInsightDto
import com.yeopjari.core.sync.TripRecorder
import com.yeopjari.core.sync.YeopjariApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayAndRecorderTest {

    @Test
    fun replayDeliversEveryFrameInOrderWithScaledDelays() {
        val s = DemoScenarios.soyeonFirstGangbyeon()
        val delays = mutableListOf<Long>()
        val queue = ArrayDeque<() -> Unit>()
        val source = ScenarioReplaySource(s, Ticker { d, a -> delays += d; queue.addLast(a) }, speed = 4.0)
        val got = mutableListOf<DriveFrame>()
        source.start { got += it }
        while (queue.isNotEmpty()) queue.removeFirst()()

        assertEquals(s.frames, got)
        assertEquals(250L, delays.first()) // 1초 프레임을 4배속
    }

    @Test
    fun stopHaltsReplay() {
        val queue = ArrayDeque<() -> Unit>()
        val source = ScenarioReplaySource(DemoScenarios.soyeonFirstGangbyeon(), Ticker { _, a -> queue.addLast(a) })
        var n = 0
        source.start { n++ }
        repeat(5) { queue.removeFirst()() }
        source.stop()
        while (queue.isNotEmpty()) queue.removeFirst()()
        assertEquals(6, n)
    }

    private class FlakyApi : YeopjariApi {
        var fail = true
        val sent = mutableListOf<TripEvent>()
        override fun sendEvents(tripId: String, events: List<TripEvent>) {
            if (fail) throw RuntimeException("터널")
            sent += events
        }
        override fun finishTrip(tripId: String) = ReportDto(tripId, "t", emptyList(), emptyList(), "")
        override fun previewRoute(driverId: String, fastOption: String, fastMinutes: Int, fast: List<Maneuver>,
                                  easyOption: String?, easyMinutes: Int?, easy: List<Maneuver>?) =
            RoutePreviewDto(0, emptyList(), null)
        override fun startTrip(driverId: String, destinationName: String, destinationCategory: String,
                               placeId: String?, tensionSpotsPlanned: Int) = "trip"
        override fun parkingAdvice(placeId: String) = ParkingAdviceDto(0, "", null, null, null, "")
        override fun insights(driverId: String) = emptyList<SeniorInsightDto>()
    }

    @Test
    fun recorderKeepsEventsWhenOfflineAndSendsThemLater() {
        val api = FlakyApi()
        val recorder = TripRecorder(api, "trip")
        recorder.record(List(3) { TripEvent(EventType.REVERSE, it.toLong()) })
        assertFalse(recorder.flush())
        api.fail = false
        recorder.record(listOf(TripEvent(EventType.HARD_BRAKE, 9)))
        assertTrue(recorder.flush())
        assertEquals(listOf(0L, 1L, 2L, 9L), api.sent.map { it.atMs })
        assertEquals("trip", recorder.finish().tripId)
    }
}
