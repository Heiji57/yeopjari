package com.yeopjari.app.platform

import android.util.Log

/** 내비에 시키는 일. 실차는 Pleos NaviHelper, 시연은 기록만 한다. */
interface RouteNavigator {
    /** NaviHelper RouteOption 이름 (RECOMMENDED, COMFORTABLE, SCHOOL_ZONE …) */
    fun applyRouteOption(option: String)

    /** 시니어 제안 "가는 길에 정비소 들를까요?" → 경유지 추가 */
    fun addRepairShopWaypoint()
}

class LoggingNavigator : RouteNavigator {
    override fun applyRouteOption(option: String) {
        Log.i("Yeopjari", "[시연] 경로 옵션 변경: $option")
    }

    override fun addRepairShopWaypoint() {
        Log.i("Yeopjari", "[시연] 경유지 추가: 근처 정비소")
    }
}
