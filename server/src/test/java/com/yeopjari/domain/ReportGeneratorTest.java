package com.yeopjari.domain;

import com.yeopjari.domain.model.EventType;
import com.yeopjari.domain.model.Report;
import com.yeopjari.domain.model.ReportItem;
import com.yeopjari.domain.model.Trend;
import com.yeopjari.domain.model.Trip;
import com.yeopjari.domain.model.TripEvent;
import com.yeopjari.domain.report.ReportGenerator;
import com.yeopjari.domain.seed.DemoSeed;
import com.yeopjari.domain.service.TripService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportGeneratorTest {

    @Test
    void firstGangbyeonReportMatchesTheIdeaDoc() {
        Report report = new DemoFixture().driveFirstGangbyeon();

        // 긴장 구간 3/3 · 깜빡이 92% ↑ · 주차 후진 5→2번 ↓ · 첫 강변북로 합류 성공
        assertEquals("3/3", item(report, "tension").value());
        assertEquals("92%", item(report, "signal").value());
        assertEquals(Trend.UP, item(report, "signal").trend());
        assertEquals("5→2번", item(report, "parking").value());
        assertEquals(Trend.DOWN, item(report, "parking").trend());
        assertTrue(item(report, "parking").positive());
        assertEquals(List.of("첫 강변북로 합류 성공"), report.milestones());
        assertEquals("오늘 첫 강변북로 합류 성공!", report.title());
    }

    @Test
    void parkingNoteSaysComparedToUsualWhenTheLotIsDifferent() {
        // 지난 기록은 지하주차장, 오늘은 평면주차장 → "지난번보다" 가 아니라 "평소보다"
        Report report = new DemoFixture().driveFirstGangbyeon();
        assertEquals("평소보다 줄었어요", item(report, "parking").note());
    }

    @Test
    void sameReverseCountIsNotCalledFewer() {
        DemoFixture fx = new DemoFixture();
        fx.driveFirstGangbyeon();
        // 한 시간 뒤 같은 평면주차장에 또 후진 2번 (리허설을 초기화 없이 한 번 더 한 것과 같다)
        TripService later = new TripService(fx.store, new ReportGenerator(),
                Clock.offset(DemoFixture.CLOCK, Duration.ofHours(1)));
        Trip trip = later.start(DemoSeed.SOYEON, "○○몰", "MALL", DemoSeed.MALL_FLAT, 0);
        later.addEvents(trip.getId(), List.of(TripEvent.of(EventType.PARKING_DONE, 1, "placeId", DemoSeed.MALL_FLAT,
                "reverseCount", "2", "brakeCount", "3", "durationSec", "80")));
        Report again = later.finish(trip.getId());

        assertEquals("2→2번", item(again, "parking").value());
        assertEquals(Trend.SAME, item(again, "parking").trend());
        assertEquals("지난번과 비슷해요", item(again, "parking").note());
    }

    @Test
    void reportNeverScoresOrDeductsPoints() {
        Report report = new DemoFixture().driveFirstGangbyeon();
        String all = report.toString();

        assertFalse(all.contains("감점"));
        assertFalse(all.contains("점수"));
        assertFalse(all.contains("위반"));
    }

    @Test
    void finishingTwiceReturnsSameReport() {
        DemoFixture fx = new DemoFixture();
        Report first = fx.driveFirstGangbyeon();
        Report again = fx.trips.finish(first.tripId());

        assertEquals(first, again);
    }

    private static ReportItem item(Report report, String key) {
        return report.items().stream().filter(i -> i.key().equals(key)).findFirst()
                .orElseThrow(() -> new AssertionError("no item " + key + " in " + report.items()));
    }
}
