package com.yeopjari.domain.model;

/**
 * 출발 전 길 예습에 보여줄 긴장 구간.
 *
 * @param severity 1(가벼움) ~ 3(가장 긴장)
 */
public record TensionSpot(String id, int maneuverIndex, TensionType type, int severity, String title, String tip,
                          String roadName, double lat, double lng) {
}
