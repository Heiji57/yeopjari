package com.yeopjari.core.scenario

import com.yeopjari.core.model.ExteriorLights
import com.yeopjari.core.model.Gear
import com.yeopjari.core.model.Maneuver
import com.yeopjari.core.model.ManeuverType.EXIT
import com.yeopjari.core.model.ManeuverType.MERGE
import com.yeopjari.core.model.ManeuverType.SCHOOL_ZONE
import com.yeopjari.core.model.ManeuverType.STRAIGHT
import com.yeopjari.core.model.ManeuverType.TURN_LEFT
import com.yeopjari.core.model.ManeuverType.TURN_RIGHT

/**
 * 발표용 시나리오. 서버 DemoSeed(김씨 가족)와 짝이 맞는다.
 *
 * 소연이의 첫 강변북로: 길 예습 긴장 구간 3곳(합류 후 바로 1차로 / 우회전 후 100m 좌회전 / 스쿨존 60→30),
 * 깜빡이 13번 중 12번(92%), ○○몰 옆 평면주차장에 후진 2번 → 리포트 "5→2번", "첫 강변북로 합류 성공".
 */
object DemoScenarios {

    val gangbyeonRoute: List<Maneuver> = listOf(
        Maneuver(0, TURN_RIGHT, 300, roadName = "월드컵로", lat = 37.5563, lng = 126.9012),
        Maneuver(1, TURN_LEFT, 400, roadName = "월드컵로", lat = 37.5551, lng = 126.8990),
        Maneuver(2, MERGE, 1200, lanesToCross = 3, targetLane = 1, roadName = "강변북로", lat = 37.5530, lng = 126.8950),
        Maneuver(3, STRAIGHT, 500, speedLimitBefore = 80, speedLimitAfter = 80, roadName = "강변북로", lat = 37.5490, lng = 126.9010),
        Maneuver(4, EXIT, 4200, speedLimitBefore = 80, speedLimitAfter = 60, roadName = "강변북로", lat = 37.5420, lng = 126.9250),
        Maneuver(5, TURN_RIGHT, 900, roadName = "양화로", lat = 37.5395, lng = 126.9270),
        Maneuver(6, TURN_LEFT, 100, roadName = "양화로", lat = 37.5390, lng = 126.9275),
        Maneuver(7, SCHOOL_ZONE, 400, speedLimitBefore = 60, speedLimitAfter = 30, roadName = "망원로", lat = 37.5370, lng = 126.9280),
        Maneuver(8, TURN_RIGHT, 600, roadName = "망원로", lat = 37.5340, lng = 126.9290),
        Maneuver(9, TURN_LEFT, 500, roadName = "포은로", lat = 37.5320, lng = 126.9300),
        Maneuver(10, TURN_RIGHT, 700, roadName = "포은로", lat = 37.5300, lng = 126.9305),
        Maneuver(11, TURN_LEFT, 450, roadName = "희우정로", lat = 37.5285, lng = 126.9308),
        Maneuver(12, TURN_RIGHT, 500, roadName = "희우정로", lat = 37.5270, lng = 126.9310),
        Maneuver(13, TURN_LEFT, 350, roadName = "토정로", lat = 37.5260, lng = 126.9311),
        Maneuver(14, TURN_RIGHT, 300, roadName = "토정로", lat = 37.5252, lng = 126.9312),
    )

    /** 쉬운 길(COMFORTABLE): 강변북로를 피해 시내로. 4분 더 걸린다. */
    val easyRoute: List<Maneuver> = listOf(
        Maneuver(0, TURN_RIGHT, 300, roadName = "월드컵로", lat = 37.5563, lng = 126.9012),
        Maneuver(1, STRAIGHT, 2500, speedLimitBefore = 60, speedLimitAfter = 60, roadName = "월드컵로", lat = 37.5430, lng = 126.9170),
        Maneuver(2, TURN_RIGHT, 1800, roadName = "망원로", lat = 37.5395, lng = 126.9230),
        Maneuver(3, SCHOOL_ZONE, 600, speedLimitBefore = 60, speedLimitAfter = 30, roadName = "망원로", lat = 37.5350, lng = 126.9262),
    )

    const val FAST_MINUTES = 32
    const val EASY_MINUTES = 36

    fun soyeonFirstGangbyeon(): Scenario = scenario("소연이의 첫 강변북로", gangbyeonRoute, "mall_flat", "○○몰") {
        lights(ExteriorLights.AUTO)
        park(2)
        shift(Gear.D)
        approach(0, fromM = 300, kph = 30, signalAtM = 120)
        approach(1, fromM = 400, kph = 40, signalAtM = 100)
        // 음성 안내가 나오는 구간(nearM 안쪽)은 실제 속도에 가깝게 한 프레임(1초)에 몇 m 씩만 줄인다.
        // 6배속 재생에서도 문장 하나를 다 말한 뒤에 다음 안내 지점이 오도록 (말이 화면보다 늦지 않게).
        approach(2, fromM = 1200, kph = 60, signalAtM = 300, stepM = 100, nearM = 500, nearStepM = 17) // 500m 전 음성 안내
        approach(3, fromM = 500, kph = 70, stepM = 100)
        approach(4, fromM = 1500, kph = 78, signalAtM = 300, stepM = 150)
        approach(5, fromM = 900, kph = 45, signalAtM = 120, stepM = 100, nearM = 300, nearStepM = 12)
        approach(6, fromM = 100, kph = 20, signalAtM = 60, stepM = 20, nearM = 100, nearStepM = 6) // 80m 에서 "왼쪽 깜빡이 켜볼까요?" → 켬
        approach(7, fromM = 400, kph = 30, stepM = 50, nearM = 300, nearStepM = 8)                  // 300m 전 스쿨존 안내
        approach(8, fromM = 600, kph = 30, signalAtM = 100, stepM = 100)
        approach(9, fromM = 500, kph = 30, signalAtM = null, stepM = 100, nearM = 100, nearStepM = 8) // 한 번 놓침 (92%), 80m 전 깜빡이 제안
        approach(10, fromM = 700, kph = 35, signalAtM = 120, stepM = 100)
        approach(11, fromM = 450, kph = 30, signalAtM = 100, stepM = 50)
        approach(12, fromM = 500, kph = 30, signalAtM = 100, stepM = 100)
        approach(13, fromM = 350, kph = 25, signalAtM = 100, stepM = 50)
        approach(14, fromM = 300, kph = 20, signalAtM = 100, stepM = 50)
        arrive()
        reverse(2)
        park(1)
    }

    /**
     * 소연이가 "쉬운 길"을 골랐을 때. 강변북로를 피해 시내로 ([easyRoute]), 긴장 구간은 스쿨존 1곳.
     * 합류가 없으니 "첫 합류 성공"도 없고, 주차는 같은 평면주차장.
     */
    fun soyeonEasyRoute(): Scenario = scenario("소연이의 쉬운 길", easyRoute, "mall_flat", "○○몰") {
        lights(ExteriorLights.AUTO)
        park(2)
        shift(Gear.D)
        approach(0, fromM = 300, kph = 30, signalAtM = 120)
        approach(1, fromM = 2500, kph = 50, stepM = 125)
        approach(2, fromM = 1800, kph = 45, signalAtM = 120, stepM = 100)
        approach(3, fromM = 600, kph = 30, stepM = 50, nearM = 300, nearStepM = 8) // 300m 전 스쿨존 안내
        arrive()
        reverse(2)
        park(1)
    }

    /** 아버지(시니어): 저녁 병원 길. 전조등 켜짐 + 타이어 공기압 경고. */
    fun fatherEveningHospital(): Scenario = scenario("아버지 저녁 병원 길", easyRoute, "hospital", "한빛병원") {
        lights(ExteriorLights.LOW_BEAM)
        telltale("TPMS")
        park(2)
        shift(Gear.D)
        approach(0, fromM = 300, kph = 25, signalAtM = 100)
        approach(1, fromM = 2500, kph = 50, stepM = 125)
        approach(2, fromM = 1800, kph = 45, signalAtM = 120, stepM = 100)
        approach(3, fromM = 600, kph = 30, stepM = 50)
        arrive()
        reverse(1)
        park(1)
    }
}
