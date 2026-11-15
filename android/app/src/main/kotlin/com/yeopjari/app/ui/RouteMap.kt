package com.yeopjari.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.yeopjari.core.model.Maneuver
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * 내비 지도처럼 보이는 경로 지도. 실제 지도 타일 대신, 안내 지점 좌표로 길·강·공원을 그린다.
 *
 * - follow = false: 경로 전체가 보이게 북쪽 위 (예습·리포트)
 * - follow = true: 차를 화면 아래쪽에 두고 진행 방향이 위로 (주행 중)
 */
private object MapColors {
    val Ground = Color(0xFFEDEFF1)
    val Block = Color(0xFFE4E7EA)
    val Road = Color(0xFFFFFFFF)
    val RoadEdge = Color(0xFFD6DADF)
    val Major = Color(0xFFFFFFFF)
    val Water = Color(0xFFC9DAEA)
    val Park = Color(0xFFDCE8D8)
    val Route = Color(0xFF3B6FF5)
    val RouteEdge = Color(0xFF2550C8)
    val Passed = Color(0xFFAEB4BD)
}

/** 미터 좌표 (x 동쪽, y 남쪽) */
private data class P(val x: Float, val y: Float)

private class World(route: List<Maneuver>) {
    val pts: List<P>
    val streets: List<Pair<P, P>>
    val parks: List<Pair<P, Size>>
    val river: List<P>
    val riverRoad: Pair<P, P>?
    val minX: Float
    val maxX: Float
    val minY: Float
    val maxY: Float

    init {
        val lat0 = route.firstOrNull()?.lat ?: 0.0
        val lng0 = route.firstOrNull()?.lng ?: 0.0
        val k = cos(lat0 * PI / 180)
        val raw = route.map { P(((it.lng - lng0) * 111_320 * k).toFloat(), (-(it.lat - lat0) * 110_540).toFloat()) }
        // 첫 안내 지점 앞의 출발점: 첫 구간 반대 방향으로 첫 거리만큼
        val start = if (raw.size >= 2) {
            val dx = raw[0].x - raw[1].x
            val dy = raw[0].y - raw[1].y
            val len = max(1f, hypot(dx, dy))
            val d = route[0].distanceFromPrevM.toFloat()
            P(raw[0].x + dx / len * d, raw[0].y + dy / len * d)
        } else P(0f, 0f)
        pts = listOf(start) + raw
        minX = pts.minOf { it.x }; maxX = pts.maxOf { it.x }
        minY = pts.minOf { it.y }; maxY = pts.maxOf { it.y }

        // 주변 길: 약간 기울어진 격자 + 군데군데 끊김 (늘 같은 모양이 되게 seed 고정)
        val rnd = Random(7)
        val pad = 2500f
        val list = mutableListOf<Pair<P, P>>()
        val ang = 0.21f
        val ca = cos(ang); val sa = sin(ang)
        fun rot(x: Float, y: Float) = P(x * ca - y * sa + (minX + maxX) / 2, x * sa + y * ca + (minY + maxY) / 2)
        val span = max(maxX - minX, maxY - minY) / 2 + pad
        var g = -span
        while (g <= span) {
            var s = -span
            while (s < span) {
                val len = 180f + rnd.nextFloat() * 520f
                if (rnd.nextFloat() > 0.18f) {
                    list += rot(g + rnd.nextFloat() * 30f, s) to rot(g + rnd.nextFloat() * 30f, s + len)
                    list += rot(s, g + rnd.nextFloat() * 30f) to rot(s + len, g + rnd.nextFloat() * 30f)
                }
                s += len + 20f
            }
            g += 170f + rnd.nextFloat() * 110f
        }
        streets = list
        parks = List(14) {
            P((minX - pad / 2) + rnd.nextFloat() * (maxX - minX + pad), (minY - pad / 2) + rnd.nextFloat() * (maxY - minY + pad)) to
                Size(160f + rnd.nextFloat() * 260f, 120f + rnd.nextFloat() * 200f)
        }

        // 강변북로가 있으면 그 남쪽으로 한강
        val gangIdx = route.indexOfFirst { it.roadName == "강변북로" }
        if (gangIdx >= 0 && gangIdx + 2 < route.size) {
            val a = pts[gangIdx + 1]
            val b = pts[gangIdx + 3]
            val dx = b.x - a.x; val dy = b.y - a.y
            val len = max(1f, hypot(dx, dy))
            val ux = dx / len; val uy = dy / len
            val nx = -uy; val ny = ux // 왼쪽 법선. 남쪽(y+) 을 향하게
            val sign = if (ny > 0) 1f else -1f
            val ext = 6000f
            val off = 120f
            val wid = 520f
            fun pt(t: Float, o: Float) = P(a.x + ux * t + nx * o * sign, a.y + uy * t + ny * o * sign)
            river = listOf(pt(-ext, off), pt(len + ext, off), pt(len + ext, off + wid), pt(-ext, off + wid))
            riverRoad = pt(-ext, 40f) to pt(len + ext, 40f)
        } else {
            river = emptyList()
            riverRoad = null
        }
    }

    /** 다음 안내 지점 [nextIndex] 까지 [distanceM] 남았을 때 차 위치와 진행 각도(라디안, 0 = 북쪽) */
    fun carAt(route: List<Maneuver>, nextIndex: Int?, distanceM: Int, arrived: Boolean): Pair<P, Float> {
        if (pts.size < 2) return P(0f, 0f) to 0f
        if (nextIndex == null || arrived || nextIndex >= route.size) {
            val a = pts[pts.size - 2]; val b = pts.last()
            return b to heading(a, b)
        }
        val a = pts[nextIndex]
        val b = pts[nextIndex + 1]
        val total = max(1, route[nextIndex].distanceFromPrevM)
        val f = (1f - distanceM.toFloat() / total).coerceIn(0f, 1f)
        return P(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f) to heading(a, b)
    }

    private fun heading(a: P, b: P): Float = atan2(b.x - a.x, -(b.y - a.y))
}

/**
 * @param spots 긴장 구간 안내 지점 index → 화면에 1,2,3 번호
 * @param passedUpTo 이 안내 지점 index 전까지는 지나온 길 (회색). null 이면 전부 앞으로 갈 길
 */
@Composable
fun RouteMap(
    route: List<Maneuver>,
    modifier: Modifier = Modifier,
    spots: List<Int> = emptyList(),
    follow: Boolean = false,
    nextIndex: Int? = 0,
    distanceM: Int = 0,
    arrived: Boolean = false,
    showCar: Boolean = true,
    allPassed: Boolean = false,
) {
    val world = remember(route) { World(route) }
    val (carP, carHeading) = world.carAt(route, nextIndex, distanceM, arrived)
    val anim = tween<Float>(durationMillis = 260, easing = LinearEasing)
    val cx by animateFloatAsState(carP.x, anim, label = "cx")
    val cy by animateFloatAsState(carP.y, anim, label = "cy")
    val hd by animateFloatAsState(carHeading * 180f / PI.toFloat(), tween(500), label = "hd")
    val measurer = rememberTextMeasurer()

    // 지도는 자기 영역 밖으로 그리지 않게
    Canvas(modifier.clipToBounds()) {
        drawRect(MapColors.Ground)
        val w = size.width
        val h = size.height
        val scale: Float
        if (follow) {
            scale = h / 900f // 화면 높이에 약 900m
        } else {
            val bw = max(300f, world.maxX - world.minX)
            val bh = max(300f, world.maxY - world.minY)
            scale = min(w / bw, h / bh) * 0.72f
        }
        val centerX = if (follow) cx else (world.minX + world.maxX) / 2
        val centerY = if (follow) cy else (world.minY + world.maxY) / 2
        val screenX = w / 2
        val screenY = if (follow) h * 0.66f else h / 2
        val rotation = if (follow) -hd else 0f

        withTransform({
            translate(screenX, screenY)
            rotate(rotation, Offset.Zero)
            scale(scale, scale, Offset.Zero)
            translate(-centerX, -centerY)
        }) {
            val minPx = 1f / scale // 화면에서 1px 이 되는 미터
            parksAndWater(world)
            // 일반 길
            world.streets.forEach { (a, b) ->
                drawLine(MapColors.RoadEdge, Offset(a.x, a.y), Offset(b.x, b.y), max(16f, 3f * minPx), StrokeCap.Round)
            }
            world.streets.forEach { (a, b) ->
                drawLine(MapColors.Road, Offset(a.x, a.y), Offset(b.x, b.y), max(11f, 2f * minPx), StrokeCap.Round)
            }
            // 강변 큰길
            world.riverRoad?.let { (a, b) ->
                drawLine(MapColors.RoadEdge, Offset(a.x, a.y), Offset(b.x, b.y), max(34f, 6f * minPx), StrokeCap.Round)
                drawLine(MapColors.Major, Offset(a.x, a.y), Offset(b.x, b.y), max(28f, 5f * minPx), StrokeCap.Round)
            }
            // 경로가 지나는 길
            val path = Path().apply {
                moveTo(world.pts[0].x, world.pts[0].y)
                world.pts.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, MapColors.RoadEdge, style = Stroke(max(30f, 7f * minPx), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, MapColors.Major, style = Stroke(max(24f, 6f * minPx), cap = StrokeCap.Round, join = StrokeJoin.Round))

            // 경로선: 지나온 길 회색, 갈 길 파랑
            val routeW = max(16f, 9f * minPx)
            val passedPath = Path()
            val aheadPath = Path()
            if (allPassed) {
                passedPath.addPolyline(world.pts)
            } else if (follow) {
                val idx = (nextIndex ?: route.size).coerceIn(0, route.size)
                val done = world.pts.take(idx + 1) + P(cx, cy)
                passedPath.addPolyline(done)
                aheadPath.addPolyline(listOf(P(cx, cy)) + world.pts.drop(idx + 1))
            } else {
                aheadPath.addPolyline(world.pts)
            }
            drawPath(passedPath, MapColors.Passed, style = Stroke(routeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(aheadPath, MapColors.RouteEdge, style = Stroke(routeW * 1.35f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(aheadPath, MapColors.Route, style = Stroke(routeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // 화면 좌표로 바꾸기 (표식·차는 회전 없이 똑바로)
        fun toScreen(p: P): Offset {
            val dx = (p.x - centerX) * scale
            val dy = (p.y - centerY) * scale
            val r = rotation * PI.toFloat() / 180f
            return Offset(screenX + dx * cos(r) - dy * sin(r), screenY + dx * sin(r) + dy * cos(r))
        }

        // 출발 · 도착
        val unit = h / 1100f
        val startP = toScreen(world.pts.first())
        drawCircle(Color.White, 13f * unit, startP)
        drawCircle(Color(0xFF26282C), 8f * unit, startP)
        val endP = toScreen(world.pts.last())
        drawCircle(Color(0x33000000), 26f * unit, endP + Offset(0f, 3f * unit))
        drawCircle(Color(0xFF26282C), 24f * unit, endP)
        drawCircle(Color.White, 9f * unit, endP)

        // 긴장 구간 번호
        spots.sorted().forEachIndexed { n, idx ->
            if (idx + 1 >= world.pts.size) return@forEachIndexed
            val o = toScreen(world.pts[idx + 1])
            val passed = allPassed || (follow && nextIndex != null && idx < nextIndex) || (follow && nextIndex == null)
            val r = 22f * unit
            drawCircle(Color(0x33000000), r + 2f * unit, o + Offset(0f, 3f * unit))
            drawCircle(Color.White, r + 3f * unit, o)
            drawCircle(if (passed) MapColors.Passed else YeopjariColors.Accent, r, o)
            val label = if (passed) "✓" else "${n + 1}"
            val layout = measurer.measure(label, TextStyle(color = Color.White, fontSize = (r / density * 1.1f).sp, fontWeight = FontWeight.Bold))
            drawText(layout, topLeft = o - Offset(layout.size.width / 2f, layout.size.height / 2f))
        }

        if (showCar) {
            val carPos = if (follow) Offset(screenX, screenY) else toScreen(P(cx, cy))
            val carRot = if (follow) 0f else hd
            val carLen = if (follow) h * 0.16f else 70f * unit
            drawCar(carPos, carRot, carLen)
        }
    }
}

private fun Path.addPolyline(points: List<P>) {
    if (points.isEmpty()) return
    moveTo(points[0].x, points[0].y)
    points.drop(1).forEach { lineTo(it.x, it.y) }
}

private fun DrawScope.parksAndWater(world: World) {
    world.parks.forEach { (p, s) ->
        drawRoundRect(MapColors.Park, Offset(p.x, p.y), s, CornerRadius(40f, 40f))
    }
    if (world.river.isNotEmpty()) {
        val r = Path().apply {
            moveTo(world.river[0].x, world.river[0].y)
            world.river.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        drawPath(r, MapColors.Water)
    }
}

/**
 * 위에서 내려다본 입체 차 (Pleos 차량 패널의 차처럼 은색 차체 + 검정 유리 지붕).
 * [center] 가 차 가운데, [deg] 만큼 돌린다 (0 = 위쪽).
 */
private fun DrawScope.drawCar(center: Offset, deg: Float, length: Float) {
    val l = length
    val wdt = l * 0.46f
    rotate(deg, center) {
        translate(center.x - wdt / 2, center.y - l / 2) {
            // 바닥 위치 표시 (파란 원) + 그림자
            drawCircle(Color(0x223B6FF5), l * 0.62f, Offset(wdt / 2, l / 2))
            drawCircle(Color(0x443B6FF5), l * 0.4f, Offset(wdt / 2, l / 2), style = Stroke(l * 0.015f))
            drawRoundRect(Color(0x33000000), Offset(-wdt * 0.04f, l * 0.03f), Size(wdt * 1.08f, l * 1.0f), CornerRadius(wdt * 0.4f))
            // 앞쪽 방향 화살표
            val arrow = Path().apply {
                moveTo(wdt / 2, -l * 0.3f)
                lineTo(wdt / 2 + wdt * 0.32f, -l * 0.12f)
                lineTo(wdt / 2, -l * 0.17f)
                lineTo(wdt / 2 - wdt * 0.32f, -l * 0.12f)
                close()
            }
            drawPath(arrow, Color(0xFF3B6FF5))
            // 바퀴
            val tire = Color(0xFF1C1F23)
            listOf(0.17f, 0.74f).forEach { y ->
                drawRoundRect(tire, Offset(-wdt * 0.05f, l * y), Size(wdt * 0.14f, l * 0.13f), CornerRadius(wdt * 0.05f))
                drawRoundRect(tire, Offset(wdt * 0.91f, l * y), Size(wdt * 0.14f, l * 0.13f), CornerRadius(wdt * 0.05f))
            }
            // 차체: 양옆이 어둡고 가운데가 밝게 → 둥근 입체감
            drawRoundRect(
                Brush.horizontalGradient(
                    0f to Color(0xFF9EA4AC), 0.18f to Color(0xFFDDE0E4), 0.5f to Color(0xFFF8F9FA),
                    0.82f to Color(0xFFDDE0E4), 1f to Color(0xFF9EA4AC),
                    startX = 0f, endX = wdt,
                ),
                Offset(0f, 0f), Size(wdt, l), CornerRadius(wdt * 0.42f, wdt * 0.5f),
            )
            // 보닛·트렁크 그늘
            drawRoundRect(
                Brush.verticalGradient(0f to Color(0x22000000), 0.12f to Color(0x00000000), 0.88f to Color(0x00000000), 1f to Color(0x33000000), startY = 0f, endY = l),
                Offset(0f, 0f), Size(wdt, l), CornerRadius(wdt * 0.42f, wdt * 0.5f),
            )
            // 앞 유리
            val ws = Path().apply {
                moveTo(wdt * 0.13f, l * 0.36f); lineTo(wdt * 0.87f, l * 0.36f)
                lineTo(wdt * 0.78f, l * 0.25f); lineTo(wdt * 0.22f, l * 0.25f); close()
            }
            drawPath(ws, Brush.verticalGradient(listOf(Color(0xFF5A636D), Color(0xFF23282E)), startY = l * 0.25f, endY = l * 0.36f))
            // 유리 지붕
            drawRoundRect(
                Brush.verticalGradient(listOf(Color(0xFF2A3036), Color(0xFF3E464F), Color(0xFF2A3036)), startY = l * 0.36f, endY = l * 0.78f),
                Offset(wdt * 0.15f, l * 0.36f), Size(wdt * 0.7f, l * 0.42f), CornerRadius(wdt * 0.12f),
            )
            drawLine(Color(0x55FFFFFF), Offset(wdt * 0.3f, l * 0.4f), Offset(wdt * 0.3f, l * 0.72f), l * 0.012f)
            // 뒷 유리
            val rw = Path().apply {
                moveTo(wdt * 0.17f, l * 0.78f); lineTo(wdt * 0.83f, l * 0.78f)
                lineTo(wdt * 0.76f, l * 0.86f); lineTo(wdt * 0.24f, l * 0.86f); close()
            }
            drawPath(rw, Color(0xFF2E343B))
            // 사이드 미러
            drawOval(Color(0xFFB9BEC5), Offset(-wdt * 0.12f, l * 0.31f), Size(wdt * 0.16f, l * 0.05f))
            drawOval(Color(0xFFB9BEC5), Offset(wdt * 0.96f, l * 0.31f), Size(wdt * 0.16f, l * 0.05f))
            // 전조등 · 후미등
            drawLine(Color(0xFFFFFFFF), Offset(wdt * 0.12f, l * 0.035f), Offset(wdt * 0.32f, l * 0.02f), l * 0.018f, StrokeCap.Round)
            drawLine(Color(0xFFFFFFFF), Offset(wdt * 0.88f, l * 0.035f), Offset(wdt * 0.68f, l * 0.02f), l * 0.018f, StrokeCap.Round)
            drawLine(Color(0xFFE0453A), Offset(wdt * 0.12f, l * 0.965f), Offset(wdt * 0.88f, l * 0.965f), l * 0.018f, StrokeCap.Round)
        }
    }
}
