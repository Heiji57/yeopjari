package com.yeopjari.domain.model;

import java.util.List;

/** 경로 후보 하나 (빠른 길 / 편한 길). routeOption 은 NaviHelper RouteOption 이름 (RECOMMENDED, COMFORTABLE 등). */
public record RouteCandidate(String routeOption, int minutes, List<Maneuver> maneuvers) {

    public RouteCandidate {
        maneuvers = maneuvers == null ? List.of() : List.copyOf(maneuvers);
    }
}
