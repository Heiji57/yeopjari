package com.yeopjari.domain.model;

/**
 * 운전자.
 *
 * @param label 가족 피드에서 주어로 쓰는 호칭 (예: "소연이", "아버지")
 */
public record Driver(String id, String name, DriverMode mode, String carModel, String familyId, String label) {
}
