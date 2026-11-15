package com.yeopjari.domain;

import com.yeopjari.domain.family.FamilyFeedBuilder;
import com.yeopjari.domain.model.EventType;
import com.yeopjari.domain.model.Report;
import com.yeopjari.domain.model.TensionType;
import com.yeopjari.domain.model.Trip;
import com.yeopjari.domain.model.TripEvent;
import com.yeopjari.domain.parking.ParkingDifficultyCalculator;
import com.yeopjari.domain.report.ReportGenerator;
import com.yeopjari.domain.seed.DemoSeed;
import com.yeopjari.domain.senior.SeniorInsightEngine;
import com.yeopjari.domain.service.FamilyService;
import com.yeopjari.domain.service.ParkingService;
import com.yeopjari.domain.service.TripService;
import com.yeopjari.domain.store.InMemoryStore;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** 테스트용: 시연 데이터 + "소연이의 첫 강변북로" 주행 한 번. */
public final class DemoFixture {

    public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    /** 2026-11-13 20:00 KST */
    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-13T11:00:00Z"), SEOUL);

    public final InMemoryStore store = new InMemoryStore();
    public final SeniorInsightEngine seniorEngine = new SeniorInsightEngine(SEOUL);
    public final TripService trips = new TripService(store, new ReportGenerator(), CLOCK);
    public final ParkingService parking = new ParkingService(store, new ParkingDifficultyCalculator());
    public final FamilyService family = new FamilyService(store, new FamilyFeedBuilder(seniorEngine), seniorEngine,
            CLOCK);

    public DemoFixture() {
        DemoSeed.seed(store, CLOCK);
    }

    /** 시연 시나리오(soyeon_first_gangbyeon.json)와 같은 결과를 내는 주행. */
    public Report driveFirstGangbyeon() {
        Trip trip = trips.start(DemoSeed.SOYEON, "○○몰", "MALL", DemoSeed.MALL_FLAT, 3);
        List<TripEvent> events = new ArrayList<>();
        for (int i = 0; i < 23; i++) {
            events.add(TripEvent.of(EventType.TURN_SIGNAL_USED, i, "maneuver", "TURN_RIGHT"));
        }
        events.add(TripEvent.of(EventType.TURN_SIGNAL_MISSED, 30, "maneuver", "TURN_LEFT"));
        events.add(TripEvent.of(EventType.TURN_SIGNAL_MISSED, 31, "maneuver", "TURN_LEFT"));
        events.add(TripEvent.of(EventType.TENSION_SPOT_PASSED, 40, "spotType",
                TensionType.MERGE_THEN_LANE_CHANGE.name(), "road", "강변북로", "smooth", "true"));
        events.add(TripEvent.of(EventType.MERGE_SUCCESS, 41, "road", "강변북로"));
        events.add(TripEvent.of(EventType.TENSION_SPOT_PASSED, 50, "spotType",
                TensionType.QUICK_SUCCESSIVE_TURNS.name(), "road", "양화로", "smooth", "true"));
        events.add(TripEvent.of(EventType.TENSION_SPOT_PASSED, 60, "spotType",
                TensionType.SCHOOL_ZONE.name(), "road", "망원로", "smooth", "true"));
        events.add(TripEvent.of(EventType.REVERSE, 70));
        events.add(TripEvent.of(EventType.REVERSE, 71));
        events.add(TripEvent.of(EventType.PARKING_DONE, 80, "placeId", DemoSeed.MALL_FLAT,
                "reverseCount", "2", "brakeCount", "3", "durationSec", "85"));
        trips.addEvents(trip.getId(), events);
        return trips.finish(trip.getId());
    }
}
