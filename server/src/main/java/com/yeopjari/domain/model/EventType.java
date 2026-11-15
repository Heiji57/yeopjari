package com.yeopjari.domain.model;

/**
 * 차량 앱이 주행 중 보내는 이벤트 종류. Android core 모듈의 EventType 과 이름이 같아야 한다.
 *
 * <p>attrs 규칙 (모두 문자열):
 * <ul>
 *   <li>TENSION_SPOT_PASSED: spotType(TensionType), road, smooth(true/false)</li>
 *   <li>MERGE_SUCCESS: road</li>
 *   <li>TURN_SIGNAL_USED / TURN_SIGNAL_MISSED: maneuver(ManeuverType)</li>
 *   <li>HARD_BRAKE: speedKph</li>
 *   <li>REVERSE: (없음)</li>
 *   <li>PARKING_DONE: placeId, reverseCount, brakeCount, durationSec</li>
 *   <li>NIGHT_DRIVING: minutes</li>
 *   <li>TELLTALE_ON: name (예: TPMS)</li>
 * </ul>
 */
public enum EventType {
    TENSION_SPOT_PASSED,
    MERGE_SUCCESS,
    TURN_SIGNAL_USED,
    TURN_SIGNAL_MISSED,
    HARD_BRAKE,
    REVERSE,
    PARKING_DONE,
    NIGHT_DRIVING,
    TELLTALE_ON
}
