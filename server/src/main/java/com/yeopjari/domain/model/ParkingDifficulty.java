package com.yeopjari.domain.model;

/**
 * 주차 난이도.
 *
 * @param stars 1(쉬움) ~ 5(어려움). 표본이 부족하면 0.
 */
public record ParkingDifficulty(String placeId, String placeName, String kind, int stars, double avgReverse,
                                double avgDurationSec, int sampleCount, String label) {

    public boolean known() {
        return stars > 0;
    }
}
