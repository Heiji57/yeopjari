package com.yeopjari.domain;

import com.yeopjari.domain.model.SeniorInsight;
import com.yeopjari.domain.model.Trend;
import com.yeopjari.domain.model.WeeklySummary;
import com.yeopjari.domain.seed.DemoSeed;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeniorInsightEngineTest {

    @Test
    void suggestsRepairShopAndDaytimeHospitalVisit() {
        DemoFixture fx = new DemoFixture();
        List<SeniorInsight> insights = fx.family.insights(DemoSeed.FATHER);

        assertTrue(insights.stream().anyMatch(i ->
                i.message().equals("타이어 공기압 경고 5일째예요. 가는 길에 정비소 들를까요?")
                        && i.action().equals("ADD_WAYPOINT_REPAIR_SHOP")), insights::toString);
        assertTrue(insights.stream().anyMatch(i ->
                i.message().equals("밤 운전이 늘었어요. 병원 예약은 오전으로 해볼까요?")), insights::toString);
    }

    @Test
    void weeklySummaryIsStableWithMoreNightDriving() {
        DemoFixture fx = new DemoFixture();
        WeeklySummary s = fx.seniorEngine.weekly(fx.store.finishedTripsOf(DemoSeed.FATHER),
                DemoFixture.CLOCK.instant());

        assertTrue(s.stable());
        assertEquals(Trend.UP, s.nightTrend());
        assertEquals(45, s.nightMinutes());
        assertEquals(5, s.tpmsWarningDays());
    }
}
