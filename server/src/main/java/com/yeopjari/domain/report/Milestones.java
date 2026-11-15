package com.yeopjari.domain.report;

import com.yeopjari.domain.model.EventType;
import com.yeopjari.domain.model.Trip;
import com.yeopjari.domain.model.TripEvent;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** "처음 해낸 것" 계산. 도착 리포트와 가족 피드가 같은 규칙을 쓴다. */
public final class Milestones {

    private Milestones() {
    }

    /** @param previous 이번 주행보다 앞선 주행들 */
    public static List<String> of(Trip trip, List<Trip> previous) {
        Set<String> roadsBefore = new HashSet<>();
        for (Trip p : previous) {
            p.eventsOf(EventType.MERGE_SUCCESS).forEach(e -> roadsBefore.add(e.attr("road")));
        }
        Set<String> result = new LinkedHashSet<>();
        for (TripEvent merge : trip.eventsOf(EventType.MERGE_SUCCESS)) {
            String road = merge.attr("road");
            if (road != null && !roadsBefore.contains(road)) {
                result.add("첫 %s 합류 성공".formatted(road));
            }
        }
        return List.copyOf(result);
    }
}
