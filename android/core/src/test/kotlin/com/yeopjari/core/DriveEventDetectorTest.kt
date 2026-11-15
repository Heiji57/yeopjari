package com.yeopjari.core

import com.yeopjari.core.detect.DriveEventDetector
import com.yeopjari.core.model.EventType
import com.yeopjari.core.model.PlannedSpot
import com.yeopjari.core.model.TripEvent
import com.yeopjari.core.scenario.DemoScenarios
import com.yeopjari.core.scenario.Scenario
import com.yeopjari.core.scenario.scenario
import com.yeopjari.core.model.Gear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 서버 길 예습이 이 경로에서 돌려주는 긴장 구간과 같은 위치. */
val demoSpots = listOf(
    PlannedSpot(2, "MERGE_THEN_LANE_CHANGE", "합류 후 바로 1차로", "합류 500m 전부터 오른쪽 차로에서 기다려요"),
    PlannedSpot(5, "QUICK_SUCCESSIVE_TURNS", "우회전 후 100m 좌회전", "첫 회전 전에 다음 차로를 정해요"),
    PlannedSpot(7, "SCHOOL_ZONE", "스쿨존 60→30", "미리 속도를 줄여요"),
)

fun runDetector(s: Scenario, spots: List<PlannedSpot> = demoSpots): Pair<List<TripEvent>, DriveEventDetector> {
    val detector = DriveEventDetector(spots, s.parkingPlaceId)
    val events = s.frames.flatMap { detector.onFrame(it) } + detector.finish(s.frames.last().vehicle.atMs)
    return events to detector
}

class DriveEventDetectorTest {

    @Test
    fun demoScenarioProducesTheEventsTheReportNeeds() {
        val (events, detector) = runDetector(DemoScenarios.soyeonFirstGangbyeon())
        fun count(t: EventType) = events.count { it.type == t }

        assertEquals(12, count(EventType.TURN_SIGNAL_USED))
        assertEquals(1, count(EventType.TURN_SIGNAL_MISSED))
        assertEquals(listOf("강변북로"), events.filter { it.type == EventType.MERGE_SUCCESS }.map { it.attrs["road"] })

        val spots = events.filter { it.type == EventType.TENSION_SPOT_PASSED }
        assertEquals(listOf("MERGE_THEN_LANE_CHANGE", "QUICK_SUCCESSIVE_TURNS", "SCHOOL_ZONE"), spots.map { it.attrs["spotType"] })
        assertTrue(spots.all { it.attrs["smooth"] == "true" })

        assertEquals(2, count(EventType.REVERSE))
        val parking = events.single { it.type == EventType.PARKING_DONE }
        assertEquals("mall_flat", parking.attrs["placeId"])
        assertEquals("2", parking.attrs["reverseCount"])
        assertEquals(0, count(EventType.HARD_BRAKE))
        assertTrue(detector.finishedParking)
    }

    @Test
    fun hardBrakeMakesTheSpotNotSmooth() {
        val s = scenario("급제동", DemoScenarios.gangbyeonRoute.take(3), null, "x") {
            park(1); shift(Gear.D)
            approach(0, fromM = 300, kph = 30, signalAtM = 120)
            approach(1, fromM = 400, kph = 40, signalAtM = 100)
            approach(2, fromM = 800, kph = 60, signalAtM = 300, stepM = 100, hardBrakeAtM = 200)
            arrive()
        }
        val (events, _) = runDetector(s)

        assertEquals(1, events.count { it.type == EventType.HARD_BRAKE })
        val merge = events.single { it.type == EventType.TENSION_SPOT_PASSED }
        assertEquals("false", merge.attrs["smooth"])
    }

    @Test
    fun seniorEveningTripRecordsNightDrivingAndTpms() {
        val (events, _) = runDetector(DemoScenarios.fatherEveningHospital(), spots = emptyList())

        val night = events.single { it.type == EventType.NIGHT_DRIVING }
        assertTrue(night.attrs.getValue("minutes").toInt() >= 1)
        assertEquals("TPMS", events.single { it.type == EventType.TELLTALE_ON }.attrs["name"])
        assertEquals("hospital", events.single { it.type == EventType.PARKING_DONE }.attrs["placeId"])
    }
}
