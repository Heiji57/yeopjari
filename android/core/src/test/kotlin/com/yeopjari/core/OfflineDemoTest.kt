package com.yeopjari.core

import com.yeopjari.core.detect.DriveEventDetector
import com.yeopjari.core.model.Maneuver
import com.yeopjari.core.model.PlannedSpot
import com.yeopjari.core.model.TripEvent
import com.yeopjari.core.scenario.DemoScenarios
import com.yeopjari.core.sync.FallbackYeopjariApi
import com.yeopjari.core.sync.ParkingAdviceDto
import com.yeopjari.core.sync.ReportDto
import com.yeopjari.core.sync.RoutePreviewDto
import com.yeopjari.core.sync.SeniorInsightDto
import com.yeopjari.core.sync.TripRecorder
import com.yeopjari.core.sync.YeopjariApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** 서버 없이 APK 만 설치해도 시연 대본 숫자가 그대로 나오는지. */
class OfflineDemoTest {

    /** 꺼진 서버 */
    private object DeadServer : YeopjariApi {
        private fun down(): Nothing = throw IOException("connection refused")
        override fun previewRoute(driverId: String, fastOption: String, fastMinutes: Int, fast: List<Maneuver>,
                                  easyOption: String?, easyMinutes: Int?, easy: List<Maneuver>?): RoutePreviewDto = down()
        override fun startTrip(driverId: String, destinationName: String, destinationCategory: String,
                               placeId: String?, tensionSpotsPlanned: Int): String = down()
        override fun sendEvents(tripId: String, events: List<TripEvent>) = down()
        override fun finishTrip(tripId: String): ReportDto = down()
        override fun parkingAdvice(placeId: String): ParkingAdviceDto = down()
        override fun insights(driverId: String): List<SeniorInsightDto> = down()
    }

    private fun drive(api: YeopjariApi, driverId: String, preview: RoutePreviewDto, scenario: com.yeopjari.core.scenario.Scenario): ReportDto {
        val planned = preview.spots.map { PlannedSpot(it.maneuverIndex, it.type, it.title, it.tip) }
        val tripId = api.startTrip(driverId, scenario.destinationName, "MALL", scenario.parkingPlaceId, planned.size)
        val recorder = TripRecorder(api, tripId)
        val detector = DriveEventDetector(planned, scenario.parkingPlaceId)
        scenario.frames.forEach { recorder.record(detector.onFrame(it)) }
        recorder.record(detector.finish(scenario.frames.last().vehicle.atMs))
        return recorder.finish()
    }

    @Test
    fun soyeonDemoWorksWithoutServer() {
        val api = FallbackYeopjariApi(DeadServer)
        val preview = api.previewRoute("soyeon", "RECOMMENDED", DemoScenarios.FAST_MINUTES, DemoScenarios.gangbyeonRoute,
            "COMFORTABLE", DemoScenarios.EASY_MINUTES, DemoScenarios.easyRoute)
        assertTrue(api.isOffline)
        assertEquals(listOf("합류 후 바로 1차로", "우회전 후 100m 좌회전", "스쿨존 60→30"), preview.spots.map { it.title })
        assertEquals("쉬운 길 +4분 · 긴장 구간 3→1", preview.easyRoute?.label)
        assertEquals(listOf(3), preview.easyRoute?.spots?.map { it.maneuverIndex })
        assertTrue(api.parkingAdvice("mall_b2").message.contains("공영 평면주차장 ★ 추천"))

        val report = drive(api, "soyeon", preview, DemoScenarios.soyeonFirstGangbyeon())
        assertEquals("오늘 첫 강변북로 합류 성공!", report.title)
        val byKey = report.items.associateBy { it.key }
        assertEquals("3/3", byKey.getValue("tension").value)
        assertEquals("92%", byKey.getValue("signal").value)
        assertEquals("UP", byKey.getValue("signal").trend)
        assertEquals("5→2번", byKey.getValue("parking").value)
        assertEquals("DOWN", byKey.getValue("parking").trend)
        assertEquals(listOf("첫 강변북로 합류 성공"), report.milestones)
    }

    @Test
    fun fatherDemoWorksWithoutServer() {
        val api = FallbackYeopjariApi(DeadServer)
        val preview = api.previewRoute("father", "RECOMMENDED", 22, DemoScenarios.easyRoute, null, null, null)
        assertEquals(listOf("스쿨존 60→30"), preview.spots.map { it.title })
        assertTrue(api.insights("father").first().message.contains("타이어 공기압 경고 5일째"))
        val report = drive(api, "father", preview, DemoScenarios.fatherEveningHospital())
        assertEquals("오늘 운전, 차분했어요", report.title)
        assertEquals("1/1", report.items.first { it.key == "tension" }.value)
    }
}
