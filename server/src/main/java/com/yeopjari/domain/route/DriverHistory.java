package com.yeopjari.domain.route;

import com.yeopjari.domain.model.EventType;
import com.yeopjari.domain.model.TensionType;
import com.yeopjari.domain.model.Trip;
import com.yeopjari.domain.model.TripEvent;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 지난 주행에서 뽑은 "이 사람에게 익숙한 것 / 아직 어려운 것". 긴장 구간 개인화에 쓴다. */
public final class DriverHistory {

    private final Map<TensionType, Integer> smoothPasses = new EnumMap<>(TensionType.class);
    private final Map<TensionType, Integer> struggles = new EnumMap<>(TensionType.class);
    private final Set<String> roadsDriven = new HashSet<>();

    public static DriverHistory empty() {
        return new DriverHistory();
    }

    public static DriverHistory from(List<Trip> trips) {
        DriverHistory history = new DriverHistory();
        for (Trip trip : trips) {
            for (TripEvent event : trip.getEvents()) {
                if (event.type() == EventType.MERGE_SUCCESS && event.attr("road") != null) {
                    history.roadsDriven.add(event.attr("road"));
                }
                if (event.type() == EventType.TENSION_SPOT_PASSED) {
                    if (event.attr("road") != null) {
                        history.roadsDriven.add(event.attr("road"));
                    }
                    TensionType type = parse(event.attr("spotType"));
                    if (type == null) {
                        continue;
                    }
                    boolean smooth = !"false".equalsIgnoreCase(event.attr("smooth"));
                    (smooth ? history.smoothPasses : history.struggles).merge(type, 1, Integer::sum);
                }
            }
        }
        return history;
    }

    private static TensionType parse(String value) {
        if (value == null) {
            return null;
        }
        try {
            return TensionType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public int smoothPasses(TensionType type) {
        return smoothPasses.getOrDefault(type, 0);
    }

    public int struggles(TensionType type) {
        return struggles.getOrDefault(type, 0);
    }

    public boolean hasDriven(String road) {
        return road != null && roadsDriven.contains(road);
    }
}
