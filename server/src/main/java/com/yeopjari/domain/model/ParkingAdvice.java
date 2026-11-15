package com.yeopjari.domain.model;

/** 목적지 근처 주차 안내. alternative 가 null 이면 목적지 주차장을 그대로 쓰면 된다. */
public record ParkingAdvice(ParkingDifficulty target, ParkingDifficulty alternative, Integer walkMinutes,
                            String message) {
}
