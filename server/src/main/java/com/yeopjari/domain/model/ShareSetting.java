package com.yeopjari.domain.model;

/**
 * 가족 공유 설정. 운전자 본인만 바꿀 수 있다. 실시간 위치 공유 항목은 일부러 없다.
 *
 * @param milestones    "첫 합류 성공" 같은 성장 소식
 * @param weeklySummary 주간 요약 (깜빡이 사용률, 밤 운전 추이 등)
 * @param arrivalAlert  도착 알림 (기본 꺼짐, 본인이 켤 때만)
 */
public record ShareSetting(String driverId, boolean milestones, boolean weeklySummary, boolean arrivalAlert) {

    public static ShareSetting defaults(String driverId) {
        return new ShareSetting(driverId, true, true, false);
    }
}
