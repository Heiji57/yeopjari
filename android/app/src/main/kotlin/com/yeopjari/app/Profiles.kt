package com.yeopjari.app

import com.yeopjari.core.model.DriverMode
import com.yeopjari.core.model.Maneuver
import com.yeopjari.core.scenario.DemoScenarios
import com.yeopjari.core.scenario.Scenario

/** 시연용 운전자. 서버 DemoSeed 의 김씨 가족과 같은 id 를 쓴다. */
data class Profile(
    val driverId: String,
    val label: String,
    val mode: DriverMode,
    val carModel: String,
    val destinationCategory: String,
    val fastMinutes: Int,
    val easyRoute: List<Maneuver>?,
    val easyMinutes: Int?,
    val scenario: () -> Scenario,
    /** "쉬운 길"을 골랐을 때 재생할 시나리오 (쉬운 길이 없으면 null) */
    val easyScenario: (() -> Scenario)? = null,
)

object Profiles {
    val SOYEON = Profile(
        driverId = "soyeon", label = "소연", mode = DriverMode.NOVICE, carModel = "디 올 뉴 아반떼",
        destinationCategory = "MALL",
        fastMinutes = DemoScenarios.FAST_MINUTES, easyRoute = DemoScenarios.easyRoute, easyMinutes = DemoScenarios.EASY_MINUTES,
        scenario = DemoScenarios::soyeonFirstGangbyeon,
        easyScenario = DemoScenarios::soyeonEasyRoute,
    )

    val FATHER = Profile(
        driverId = "father", label = "아버지", mode = DriverMode.SENIOR, carModel = "더 뉴 그랜저",
        destinationCategory = "HOSPITAL",
        fastMinutes = 22, easyRoute = null, easyMinutes = null,
        scenario = DemoScenarios::fatherEveningHospital,
    )

    val all = listOf(SOYEON, FATHER)
}
