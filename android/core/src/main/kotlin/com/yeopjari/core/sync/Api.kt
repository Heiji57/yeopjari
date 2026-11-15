package com.yeopjari.core.sync

import com.yeopjari.core.model.Maneuver
import com.yeopjari.core.model.TripEvent

/** 서버(/api/...) 응답 모양. 서버 com.yeopjari.domain.model 의 record 와 필드 이름이 같다. */
data class TensionSpotDto(
    val id: String, val maneuverIndex: Int, val type: String, val severity: Int,
    val title: String, val tip: String, val roadName: String?,
)

data class EasyRouteDto(
    val routeOption: String, val extraMinutes: Int, val spotsBefore: Int, val spotsAfter: Int, val label: String,
    /** 쉬운 길의 긴장 구간. maneuverIndex 는 쉬운 길 안내 지점 기준. */
    val spots: List<TensionSpotDto> = emptyList(),
)

data class RoutePreviewDto(val minutes: Int, val spots: List<TensionSpotDto>, val easyRoute: EasyRouteDto?)

data class ReportItemDto(
    val key: String, val label: String, val value: String, val trend: String, val positive: Boolean, val note: String,
)

data class ReportDto(
    val tripId: String, val title: String, val items: List<ReportItemDto>,
    val milestones: List<String>, val nextSuggestion: String,
)

data class ParkingAdviceDto(
    val targetStars: Int, val targetName: String, val alternativeName: String?, val alternativeStars: Int?,
    val walkMinutes: Int?, val message: String,
)

data class SeniorInsightDto(val type: String, val message: String, val action: String, val positive: Boolean)

/** 앱 모듈의 HttpYeopjariApi 가 구현한다. 모두 블로킹 호출이니 IO 스레드에서 부른다. */
interface YeopjariApi {
    fun previewRoute(driverId: String, fastOption: String, fastMinutes: Int, fast: List<Maneuver>,
                     easyOption: String?, easyMinutes: Int?, easy: List<Maneuver>?): RoutePreviewDto

    fun startTrip(driverId: String, destinationName: String, destinationCategory: String,
                  placeId: String?, tensionSpotsPlanned: Int): String

    fun sendEvents(tripId: String, events: List<TripEvent>)
    fun finishTrip(tripId: String): ReportDto
    fun parkingAdvice(placeId: String): ParkingAdviceDto
    fun insights(driverId: String): List<SeniorInsightDto>
}
