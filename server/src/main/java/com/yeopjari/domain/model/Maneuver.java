package com.yeopjari.domain.model;

/**
 * 경로 위의 한 지점(회전·합류 등). 내비 TBT 정보나 외부 경로 API 결과를 이 모양으로 바꿔서 쓴다.
 *
 * @param distanceFromPrevM 직전 안내 지점에서 이 지점까지 거리(m)
 * @param lanesToCross      이 지점 직후 짧은 구간 안에 바꿔야 하는 차로 수 (합류 후 1차로까지 3번 등)
 * @param targetLane        이 지점 직후 가야 하는 차로 번호 (없으면 0)
 * @param speedLimitBefore  이 지점 전 제한속도 (모르면 0)
 * @param speedLimitAfter   이 지점 후 제한속도 (모르면 0)
 */
public record Maneuver(int index, ManeuverType type, int distanceFromPrevM, int lanesToCross, int targetLane,
                       int speedLimitBefore, int speedLimitAfter, String roadName, double lat, double lng) {
}
