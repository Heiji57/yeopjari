package com.yeopjari.domain;

import com.yeopjari.domain.model.DriverMode;
import com.yeopjari.domain.model.EventType;
import com.yeopjari.domain.model.Maneuver;
import com.yeopjari.domain.model.ManeuverType;
import com.yeopjari.domain.model.RouteCandidate;
import com.yeopjari.domain.model.RoutePreview;
import com.yeopjari.domain.model.TensionSpot;
import com.yeopjari.domain.model.TensionType;
import com.yeopjari.domain.model.Trip;
import com.yeopjari.domain.model.TripEvent;
import com.yeopjari.domain.route.DriverHistory;
import com.yeopjari.domain.route.RouteComparator;
import com.yeopjari.domain.route.TensionSpotDetector;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TensionSpotDetectorTest {

    private final TensionSpotDetector detector = new TensionSpotDetector();

    static Maneuver m(int index, ManeuverType type, int dist, int lanes, int target, int before, int after,
                      String road) {
        return new Maneuver(index, type, dist, lanes, target, before, after, road, 37.5 + index / 1000.0, 126.9);
    }

    /** 시연 경로: 합류 후 바로 1차로 / 우회전 후 100m 좌회전 / 스쿨존 60→30 + 가벼운 감속 구간. */
    static List<Maneuver> fastRoute() {
        return List.of(
                m(0, ManeuverType.TURN_RIGHT, 300, 0, 0, 0, 0, "월드컵로"),
                m(1, ManeuverType.MERGE, 1200, 3, 1, 0, 0, "강변북로"),
                m(2, ManeuverType.STRAIGHT, 500, 0, 0, 80, 80, "강변북로"),
                m(3, ManeuverType.EXIT, 4200, 0, 0, 80, 60, "강변북로"),
                m(4, ManeuverType.STRAIGHT, 600, 0, 0, 80, 60, "양화로"),
                m(5, ManeuverType.TURN_RIGHT, 900, 0, 0, 0, 0, "양화로"),
                m(6, ManeuverType.TURN_LEFT, 100, 0, 0, 0, 0, "양화로"),
                m(7, ManeuverType.SCHOOL_ZONE, 400, 0, 0, 60, 30, "망원로"));
    }

    static List<Maneuver> easyRoute() {
        return List.of(
                m(0, ManeuverType.TURN_RIGHT, 300, 0, 0, 0, 0, "월드컵로"),
                m(1, ManeuverType.STRAIGHT, 2500, 0, 0, 60, 60, "월드컵로"),
                m(2, ManeuverType.TURN_RIGHT, 1800, 0, 0, 0, 0, "망원로"),
                m(3, ManeuverType.SCHOOL_ZONE, 600, 0, 0, 60, 30, "망원로"));
    }

    @Test
    void findsThreeSpotsInRouteOrder() {
        List<TensionSpot> spots = detector.detect(fastRoute(), DriverMode.NOVICE, DriverHistory.empty());

        assertEquals(3, spots.size());
        assertEquals("합류 후 바로 1차로", spots.get(0).title());
        assertEquals("우회전 후 100m 좌회전", spots.get(1).title());
        assertEquals("스쿨존 60→30", spots.get(2).title());
        assertEquals(TensionType.MERGE_THEN_LANE_CHANGE, spots.get(0).type());
        assertTrue(spots.get(0).tip().startsWith("처음 가는 길이에요"), "처음 가는 도로는 알려준다");
    }

    @Test
    void familiarSpotsGetLighterAndStruggledSpotsHeavier() {
        List<Trip> past = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Trip t = new Trip("t" + i, "d", Instant.EPOCH, "x", "ETC", null, 1);
            t.addEvents(List.of(TripEvent.of(EventType.TENSION_SPOT_PASSED, 0, "spotType",
                    TensionType.QUICK_SUCCESSIVE_TURNS.name(), "smooth", "true")));
            past.add(t);
        }
        Trip struggled = new Trip("s", "d", Instant.EPOCH, "x", "ETC", null, 1);
        struggled.addEvents(List.of(TripEvent.of(EventType.TENSION_SPOT_PASSED, 0, "spotType",
                TensionType.SCHOOL_ZONE.name(), "smooth", "false")));
        past.add(struggled);

        List<TensionSpot> spots = detector.detect(fastRoute(), DriverMode.NOVICE, DriverHistory.from(past));

        TensionSpot turns = spots.stream().filter(s -> s.type() == TensionType.QUICK_SUCCESSIVE_TURNS)
                .findFirst().orElseThrow();
        TensionSpot school = spots.stream().filter(s -> s.type() == TensionType.SCHOOL_ZONE)
                .findFirst().orElseThrow();
        assertEquals(2, turns.severity(), "세 번 부드럽게 지난 구간은 한 단계 가볍게");
        assertEquals(3, school.severity(), "어려워했던 구간은 한 단계 무겁게");
    }

    @Test
    void offersEasyRouteWhenItIsCalmerAndNotTooLong() {
        RouteComparator comparator = new RouteComparator(detector);
        RoutePreview preview = comparator.preview(new RouteCandidate("RECOMMENDED", 32, fastRoute()),
                new RouteCandidate("COMFORTABLE", 36, easyRoute()), DriverMode.NOVICE, DriverHistory.empty());

        assertNotNull(preview.easyRoute());
        assertEquals("쉬운 길 +4분 · 긴장 구간 3→1", preview.easyRoute().label());
        assertEquals("COMFORTABLE", preview.easyRoute().routeOption());
    }

    @Test
    void hidesEasyRouteWhenItTakesTooLong() {
        RouteComparator comparator = new RouteComparator(detector);
        RoutePreview preview = comparator.preview(new RouteCandidate("RECOMMENDED", 32, fastRoute()),
                new RouteCandidate("COMFORTABLE", 50, easyRoute()), DriverMode.NOVICE, DriverHistory.empty());

        assertNull(preview.easyRoute());
    }
}
