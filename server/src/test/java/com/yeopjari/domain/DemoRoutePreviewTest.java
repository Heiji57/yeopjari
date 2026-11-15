package com.yeopjari.domain;

import com.yeopjari.domain.model.Maneuver;
import com.yeopjari.domain.model.ManeuverType;
import com.yeopjari.domain.model.RouteCandidate;
import com.yeopjari.domain.model.RoutePreview;
import com.yeopjari.domain.model.TensionSpot;
import com.yeopjari.domain.route.RouteComparator;
import com.yeopjari.domain.route.TensionSpotDetector;
import com.yeopjari.domain.seed.DemoSeed;
import com.yeopjari.domain.service.RouteService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.yeopjari.domain.model.ManeuverType.EXIT;
import static com.yeopjari.domain.model.ManeuverType.MERGE;
import static com.yeopjari.domain.model.ManeuverType.SCHOOL_ZONE;
import static com.yeopjari.domain.model.ManeuverType.STRAIGHT;
import static com.yeopjari.domain.model.ManeuverType.TURN_LEFT;
import static com.yeopjari.domain.model.ManeuverType.TURN_RIGHT;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 발표 대본 고정: Android DemoScenarios.gangbyeonRoute 를 시연 데이터(소연이 기록)로 예습하면
 * 정확히 "합류 후 바로 1차로 / 우회전 후 100m 좌회전 / 스쿨존 60→30" 이 나와야 한다.
 * 시연 데이터(DemoSeed)나 규칙을 바꾸다 이 테스트가 깨지면 발표 대본도 같이 확인할 것.
 */
class DemoRoutePreviewTest {

    static Maneuver m(int i, ManeuverType t, int d, int lanes, int target, int before, int after, String road) {
        return new Maneuver(i, t, d, lanes, target, before, after, road, 37.5, 126.9);
    }

    static List<Maneuver> gangbyeonRoute() {
        return List.of(
                m(0, TURN_RIGHT, 300, 0, 0, 0, 0, "월드컵로"),
                m(1, TURN_LEFT, 400, 0, 0, 0, 0, "월드컵로"),
                m(2, MERGE, 1200, 3, 1, 0, 0, "강변북로"),
                m(3, STRAIGHT, 500, 0, 0, 80, 80, "강변북로"),
                m(4, EXIT, 4200, 0, 0, 80, 60, "강변북로"),
                m(5, TURN_RIGHT, 900, 0, 0, 0, 0, "양화로"),
                m(6, TURN_LEFT, 100, 0, 0, 0, 0, "양화로"),
                m(7, SCHOOL_ZONE, 400, 0, 0, 60, 30, "망원로"),
                m(8, TURN_RIGHT, 600, 0, 0, 0, 0, "망원로"),
                m(9, TURN_LEFT, 500, 0, 0, 0, 0, "포은로"),
                m(10, TURN_RIGHT, 700, 0, 0, 0, 0, "포은로"),
                m(11, TURN_LEFT, 450, 0, 0, 0, 0, "희우정로"),
                m(12, TURN_RIGHT, 500, 0, 0, 0, 0, "희우정로"),
                m(13, TURN_LEFT, 350, 0, 0, 0, 0, "토정로"),
                m(14, TURN_RIGHT, 300, 0, 0, 0, 0, "토정로"));
    }

    static List<Maneuver> easyRoute() {
        return List.of(
                m(0, TURN_RIGHT, 300, 0, 0, 0, 0, "월드컵로"),
                m(1, STRAIGHT, 2500, 0, 0, 60, 60, "월드컵로"),
                m(2, TURN_RIGHT, 1800, 0, 0, 0, 0, "망원로"),
                m(3, SCHOOL_ZONE, 600, 0, 0, 60, 30, "망원로"));
    }

    @Test
    void demoPreviewMatchesThePitchScript() {
        DemoFixture fx = new DemoFixture();
        RouteService routes = new RouteService(fx.store, new RouteComparator(new TensionSpotDetector()));
        RoutePreview preview = routes.preview(DemoSeed.SOYEON, new RouteCandidate("RECOMMENDED", 32, gangbyeonRoute()),
                new RouteCandidate("COMFORTABLE", 36, easyRoute()));

        assertEquals(List.of("합류 후 바로 1차로", "우회전 후 100m 좌회전", "스쿨존 60→30"),
                preview.spots().stream().map(TensionSpot::title).toList());
        assertEquals(List.of(2, 5, 7), preview.spots().stream().map(TensionSpot::maneuverIndex).toList());
        assertEquals("쉬운 길 +4분 · 긴장 구간 3→1", preview.easyRoute().label());
    }
}
