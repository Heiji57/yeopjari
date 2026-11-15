package com.yeopjari.domain.route;

import com.yeopjari.domain.model.DriverMode;
import com.yeopjari.domain.model.EasyRouteOffer;
import com.yeopjari.domain.model.RouteCandidate;
import com.yeopjari.domain.model.RoutePreview;
import com.yeopjari.domain.model.TensionSpot;

import java.util.List;

/** 빠른 길과 편한 길을 비교해 "쉬운 길 +4분" 버튼을 보여줄지 정한다. */
public final class RouteComparator {

    static final int MAX_EXTRA_MINUTES = 10;

    private final TensionSpotDetector detector;

    public RouteComparator(TensionSpotDetector detector) {
        this.detector = detector;
    }

    public RoutePreview preview(RouteCandidate fast, RouteCandidate easy, DriverMode mode, DriverHistory history) {
        if (fast == null) {
            throw new IllegalArgumentException("fastRoute is required");
        }
        List<TensionSpot> fastSpots = detector.detect(fast.maneuvers(), mode, history);
        if (easy == null || easy.maneuvers().isEmpty()) {
            return new RoutePreview(fast.minutes(), fastSpots, null);
        }
        List<TensionSpot> easySpots = detector.detect(easy.maneuvers(), mode, history);
        int extra = easy.minutes() - fast.minutes();
        boolean easier = load(easySpots) < load(fastSpots);
        if (extra < 0 || extra > MAX_EXTRA_MINUTES || !easier) {
            return new RoutePreview(fast.minutes(), fastSpots, null);
        }
        String label = "쉬운 길 +%d분 · 긴장 구간 %d→%d".formatted(extra, fastSpots.size(), easySpots.size());
        String option = easy.routeOption() == null ? "COMFORTABLE" : easy.routeOption();
        return new RoutePreview(fast.minutes(), fastSpots,
                new EasyRouteOffer(option, extra, fastSpots.size(), easySpots.size(), label, easySpots));
    }

    /** 긴장도 합. 개수가 같아도 덜 긴장되는 길이면 쉬운 길로 본다. */
    private static int load(List<TensionSpot> spots) {
        return spots.stream().mapToInt(TensionSpot::severity).sum();
    }
}
