package com.yeopjari.domain.model;

/** 사용자들이 실제로 주차한 기록 한 건. PARKING_DONE 이벤트에서 만들어진다. */
public record ParkingSample(String placeId, int reverseCount, int brakeCount, int durationSec) {
}
