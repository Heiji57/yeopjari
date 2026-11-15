package com.yeopjari.domain.model;

/**
 * 시니어 모드 차 안 제안.
 *
 * @param action 앱이 버튼으로 실행할 동작 (ADD_WAYPOINT_REPAIR_SHOP, PLAN_DAYTIME, NONE)
 */
public record SeniorInsight(String type, String message, String action, boolean positive) {
}
