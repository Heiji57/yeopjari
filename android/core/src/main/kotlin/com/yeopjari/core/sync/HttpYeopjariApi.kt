package com.yeopjari.core.sync

import com.yeopjari.core.model.Maneuver
import com.yeopjari.core.model.TripEvent
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

/**
 * Spring 서버 클라이언트. java.net 만 써서 Android 와 JVM 테스트에서 똑같이 돈다.
 * 블로킹 호출이니 앱에서는 Dispatchers.IO 에서 부른다.
 *
 * @param baseUrl 에뮬레이터에서 PC 의 localhost 는 http://10.0.2.2:8080
 */
class HttpYeopjariApi(private val baseUrl: String, private val timeoutMs: Int = 5_000) : YeopjariApi {

    override fun previewRoute(
        driverId: String, fastOption: String, fastMinutes: Int, fast: List<Maneuver>,
        easyOption: String?, easyMinutes: Int?, easy: List<Maneuver>?,
    ): RoutePreviewDto {
        val body = mapOf(
            "driverId" to driverId,
            "fastRoute" to route(fastOption, fastMinutes, fast),
            "easyRoute" to if (easy != null && easyOption != null && easyMinutes != null) route(easyOption, easyMinutes, easy) else null,
        )
        val r = call("POST", "/api/route/preview", body).obj()
        val easyDto = (r["easyRoute"] as? Map<*, *>)?.let { e ->
            val m = e.obj()
            EasyRouteDto(m.str("routeOption"), m.int("extraMinutes"), m.int("spotsBefore"), m.int("spotsAfter"), m.str("label"),
                (m["spots"] as? List<*>)?.map { spot(it.obj()) } ?: emptyList())
        }
        return RoutePreviewDto(r.int("minutes"), r["spots"].list().map { spot(it.obj()) }, easyDto)
    }

    override fun startTrip(driverId: String, destinationName: String, destinationCategory: String,
                           placeId: String?, tensionSpotsPlanned: Int): String =
        call("POST", "/api/trips", mapOf(
            "driverId" to driverId, "destinationName" to destinationName,
            "destinationCategory" to destinationCategory, "placeId" to placeId,
            "tensionSpotsPlanned" to tensionSpotsPlanned,
        )).obj().str("tripId")

    override fun sendEvents(tripId: String, events: List<TripEvent>) {
        call("POST", "/api/trips/$tripId/events", mapOf(
            "events" to events.map { mapOf("type" to it.type.name, "atMs" to it.atMs, "attrs" to it.attrs) },
        ))
    }

    override fun finishTrip(tripId: String): ReportDto {
        val r = call("POST", "/api/trips/$tripId/finish", null).obj()
        return ReportDto(
            tripId = r.str("tripId"),
            title = r.str("title"),
            items = r["items"].list().map {
                val m = it.obj()
                ReportItemDto(m.str("key"), m.str("label"), m.str("value"), m.str("trend"), m.bool("positive"), m.str("note"))
            },
            milestones = r["milestones"].list().map { it as String },
            nextSuggestion = r.str("nextSuggestion"),
        )
    }

    override fun parkingAdvice(placeId: String): ParkingAdviceDto {
        val r = call("GET", "/api/parking/$placeId/difficulty", null).obj()
        val target = r["target"].obj()
        val alt = (r["alternative"] as? Map<*, *>)?.obj()
        return ParkingAdviceDto(
            targetStars = target.int("stars"), targetName = target.str("placeName"),
            alternativeName = alt?.str("placeName"), alternativeStars = alt?.int("stars"),
            walkMinutes = r.intOrNull("walkMinutes"), message = r.str("message"),
        )
    }

    override fun insights(driverId: String): List<SeniorInsightDto> =
        call("GET", "/api/drivers/$driverId/insights", null).list().map {
            val m = it.obj()
            SeniorInsightDto(m.str("type"), m.str("message"), m.str("action"), m.bool("positive"))
        }

    private fun route(option: String, minutes: Int, maneuvers: List<Maneuver>) = mapOf(
        "routeOption" to option, "minutes" to minutes,
        "maneuvers" to maneuvers.map {
            mapOf(
                "index" to it.index, "type" to it.type.name, "distanceFromPrevM" to it.distanceFromPrevM,
                "lanesToCross" to it.lanesToCross, "targetLane" to it.targetLane,
                "speedLimitBefore" to it.speedLimitBefore, "speedLimitAfter" to it.speedLimitAfter,
                "roadName" to it.roadName, "lat" to it.lat, "lng" to it.lng,
            )
        },
    )

    private fun spot(m: Map<String, Any?>) = TensionSpotDto(
        m.str("id"), m.int("maneuverIndex"), m.str("type"), m.int("severity"), m.str("title"), m.str("tip"), m.strOrNull("roadName"),
    )

    private fun call(method: String, path: String, body: Any?): Any? {
        val conn = URI(baseUrl.trimEnd('/') + path).toURL().openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.setRequestProperty("Accept", "application/json")
            if (body != null || method == "POST" || method == "PUT") {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(Json.write(body ?: emptyMap<String, Any>()).toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val msg = runCatching { Json.parse(text).obj().str("error") }.getOrDefault(text)
                throw IOException("서버 오류 $code: $msg")
            }
            return if (text.isBlank()) null else Json.parse(text)
        } finally {
            conn.disconnect()
        }
    }
}
