package com.yeopjari.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeopjari.app.AppViewModel
import com.yeopjari.app.Profiles
import com.yeopjari.app.Screen
import com.yeopjari.core.model.DriverMode
import com.yeopjari.core.model.Gear
import com.yeopjari.core.model.ManeuverType
import com.yeopjari.core.sync.ParkingAdviceDto
import com.yeopjari.core.sync.ReportItemDto
import com.yeopjari.core.sync.SeniorInsightDto
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Pleos Connect 기본 앱(내비 · 차량 설정)처럼:
 * 왼쪽 흰 목록 패널 + 오른쪽 지도, 카드 대신 구분선 있는 목록, 검정 버튼, 작은 회색 라벨.
 */

private val C = YeopjariColors
private val R8 = RoundedCornerShape(8.dp)
private val R12 = RoundedCornerShape(12.dp)

/** 화면 폭에 맞춰 전체를 키운다 (Pleos Connect 앱 영역은 1700dp 정도로 넓어서 기본 크기면 글씨가 작다). */
private const val DESIGN_WIDTH_DP = 1150f

@Composable
fun YeopjariApp(vm: AppViewModel) {
    val base = LocalDensity.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val scale = (widthDp / DESIGN_WIDTH_DP).coerceIn(1f, 2f)
    CompositionLocalProvider(LocalDensity provides Density(base.density * scale, base.fontScale)) {
        AppContent(vm)
    }
}

@Composable
private fun AppContent(vm: AppViewModel) {
    val s = vm.screen
    Column(Modifier.fillMaxSize().background(C.Background)) {
        Header(vm)
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (s) {
                is Screen.Loading -> LoadingView(s.message)
                is Screen.Preview -> PreviewScreen(s, vm)
                is Screen.Driving -> DrivingScreen(s, vm)
                is Screen.Report -> ReportScreen(s, vm)
                is Screen.Error -> ErrorView(s.message, vm)
            }
        }
    }
}

// ───────────── 공통 ─────────────

@Composable
private fun Header(vm: AppViewModel) {
    Row(
        Modifier.fillMaxWidth().background(C.Surface).padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(C.Accent), contentAlignment = Alignment.Center) {
            Text("옆", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Text("옆자리", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(12.dp))
        val mode = if (vm.profile.mode == DriverMode.SENIOR) "시니어 모드" else "초보 모드"
        Text("${vm.profile.label} · $mode · ${vm.profile.carModel}", fontSize = 13.sp, color = C.InkSoft)
        Spacer(Modifier.weight(1f))
        if (vm.screen !is Screen.Driving) {
            // 시연용 운전자 전환. 실제 제품에서는 차량 프로필(로그인)로 정해진다.
            Row(Modifier.clip(R8).background(C.Card).padding(3.dp)) {
                Profiles.all.forEach { p ->
                    val on = p.driverId == vm.profile.driverId
                    Text(
                        p.label,
                        fontSize = 13.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                        color = if (on) C.Ink else C.InkSoft,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (on) C.Surface else Color.Transparent)
                            .clickable { vm.switchProfile(p) }
                            .padding(horizontal = 18.dp, vertical = 5.dp),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
        }
        Text("시연 모드", fontSize = 12.sp, color = C.InkFaint)
    }
}

@Composable
private fun VoiceLine(text: String?) {
    Row(Modifier.fillMaxWidth().height(26.dp), verticalAlignment = Alignment.CenterVertically) {
        SpeakerIcon(Modifier.size(15.dp), if (text != null) C.Accent else C.InkFaint)
        Spacer(Modifier.width(8.dp))
        Text(
            text ?: "음성 안내가 여기에 자막으로 나와요",
            fontSize = 13.sp,
            color = if (text != null) C.InkSoft else C.InkFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text, fontSize = 13.sp, color = C.InkSoft)
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
}

@Composable
private fun BlackButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 56.dp) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = R8,
        colors = ButtonDefaults.buttonColors(containerColor = C.Button, contentColor = Color.White),
    ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

/** 왼쪽 흰 목록 패널 + 오른쪽 지도 */
@Composable
private fun SplitLayout(panel: @Composable ColumnScope.() -> Unit, map: @Composable BoxScope.() -> Unit) {
    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier.width(430.dp).fillMaxHeight().background(C.Surface).padding(horizontal = 24.dp, vertical = 18.dp),
            content = panel,
        )
        Box(Modifier.width(1.dp).fillMaxHeight().background(C.Line))
        Box(Modifier.weight(1f).fillMaxHeight(), content = map)
    }
}

@Composable
private fun NumberDot(n: Int, color: Color = C.Accent, size: Dp = 24.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text("$n", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ───────────── ① 출발 전 (P) 길 예습: 내비 경로 선택 화면처럼 ─────────────

@Composable
private fun PreviewScreen(s: Screen.Preview, vm: AppViewModel) {
    val spots = s.activeSpots
    SplitLayout(
        panel = {
            Label("길 예습 · 출발 전")
            Spacer(Modifier.height(2.dp))
            Text(vm.destinationName, style = MaterialTheme.typography.headlineMedium)
            Text(
                "${s.activeMinutes}분 · 긴장 구간 ${spots.size}곳",
                fontSize = 15.sp, color = C.InkSoft,
            )
            Spacer(Modifier.height(14.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                val easy = s.preview.easyRoute
                if (easy != null) {
                    Divider()
                    RouteOption(1, "빠른 길", "${s.preview.minutes}분", "긴장 구간 ${s.preview.spots.size}곳 · 강변북로 합류", !s.chosenEasy) { vm.chooseFastRoute() }
                    Divider()
                    RouteOption(2, "쉬운 길", "${s.preview.minutes + easy.extraMinutes}분", "긴장 구간 ${easy.spots.size}곳 · 시내 길", s.chosenEasy) { vm.chooseEasyRoute() }
                    Divider()
                    Spacer(Modifier.height(16.dp))
                }
                s.insights.filter { !it.positive }.forEach { InsightRow(it, it.type in s.handledInsights) { vm.onInsightAction(it) } }
                if (spots.isNotEmpty()) {
                    Label("오늘 긴장 구간 · 지나갈 때 먼저 말해 줄게요")
                    Spacer(Modifier.height(6.dp))
                    spots.forEachIndexed { i, spot ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                            NumberDot(i + 1, if (spot.severity >= 3) C.Accent else C.Button)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(spot.title, style = MaterialTheme.typography.titleMedium)
                                Text(spot.tip, fontSize = 13.sp, color = C.InkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                s.parking?.let { ParkingRows(it) }
            }
            Spacer(Modifier.height(12.dp))
            BlackButton("안내 시작 · ${s.activeMinutes}분", vm::startDriving, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            VoiceLine(vm.toast)
        },
        map = {
            RouteMap(
                route = vm.route,
                modifier = Modifier.fillMaxSize(),
                spots = spots.map { it.maneuverIndex },
                nextIndex = 0,
                distanceM = vm.route.firstOrNull()?.distanceFromPrevM ?: 0,
            )
        },
    )
}

@Composable
private fun RouteOption(n: Int, name: String, minutes: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(if (selected) C.Background else C.Surface).clickable(onClick = onClick),
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(if (selected) C.Button else Color.Transparent))
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumberDot(n, if (selected) C.Button else C.InkFaint, 18.dp)
                Spacer(Modifier.width(6.dp))
                Text(name, fontSize = 13.sp, color = C.InkSoft)
            }
            Text(minutes, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (selected) C.Ink else C.InkSoft)
            Text(detail, fontSize = 13.sp, color = C.InkSoft)
        }
    }
}

@Composable
private fun ParkingRows(p: ParkingAdviceDto) {
    Label("도착하면 주차 · 난이도")
    Spacer(Modifier.height(4.dp))
    if (p.targetStars <= 0) {
        Text(p.message, fontSize = 15.sp)
        return
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(p.targetName, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Stars(p.targetStars)
    }
    val altName = p.alternativeName
    val altStars = p.alternativeStars
    if (altName != null && altStars != null) {
        Divider()
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("추천", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = C.Accent)
            Spacer(Modifier.width(8.dp))
            Text("$altName · 도보 ${p.walkMinutes ?: 0}분", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Stars(altStars)
        }
    }
}

@Composable
private fun Stars(count: Int) {
    val c = count.coerceIn(0, 5)
    Row {
        Text("★".repeat(c), fontSize = 14.sp, color = C.Accent)
        Text("★".repeat(5 - c), fontSize = 14.sp, color = C.Line)
    }
}

@Composable
private fun InsightRow(insight: SeniorInsightDto, handled: Boolean, onAction: () -> Unit) {
    val label = when (insight.action) {
        "ADD_WAYPOINT_REPAIR_SHOP" -> "들를게요"
        "PLAN_DAYTIME" -> "좋아요"
        else -> null
    }
    val done = when (insight.action) {
        "ADD_WAYPOINT_REPAIR_SHOP" -> "✓ 정비소를 경유지로 넣었어요"
        "PLAN_DAYTIME" -> "✓ 다음엔 낮 시간으로 해 볼게요"
        else -> null
    }
    Row(
        Modifier.fillMaxWidth().clip(R8).background(if (handled) C.Background else C.AccentSoft).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(insight.message, style = MaterialTheme.typography.titleMedium)
            if (handled && done != null) Text(done, fontSize = 13.sp, color = C.Accent)
        }
        if (!handled && label != null) {
            Spacer(Modifier.width(10.dp))
            BlackButton(label, onAction, height = 44.dp)
        }
    }
    Spacer(Modifier.height(10.dp))
}

// ───────────── ② 주행 중 (D): 지도 위 3D 차 + 내비 카드 ─────────────

@Composable
private fun DrivingScreen(s: Screen.Driving, vm: AppViewModel) {
    Box(Modifier.fillMaxSize()) {
        RouteMap(
            route = vm.route,
            modifier = Modifier.fillMaxSize(),
            spots = vm.plannedIndexes.toList(),
            follow = true,
            nextIndex = s.nextIndex,
            distanceM = s.distanceM,
            arrived = s.arrived,
        )
        // 왼쪽 위: 다음 안내 (내비 TBT 카드)
        Column(Modifier.align(Alignment.TopStart).padding(20.dp).width(360.dp)) {
            Row(
                Modifier.fillMaxWidth().shadow(6.dp, R12).clip(R12).background(C.Dark).padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ManeuverIcon(if (s.arrived) null else s.nextType, s.arrived, Modifier.size(58.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    if (!s.arrived && s.distanceM > 0) {
                        Text("${s.distanceM}m", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(s.nextText, color = Color(0xFFC3C7CD), fontSize = 15.sp)
                }
            }
            val title = s.nextIndex?.let { vm.plannedTitles[it] }
            if (s.spotAhead && !s.arrived && title != null) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)).background(C.Accent)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Text("예습한 구간 · $title", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        // 오른쪽 위: 옆자리가 방금 한 말
        val voice = s.lastVoice ?: vm.toast
        if (voice != null) {
            Row(
                Modifier.align(Alignment.TopEnd).padding(20.dp).width(520.dp).shadow(6.dp, R12).clip(R12).background(C.Surface)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SpeakerIcon(Modifier.size(22.dp), C.Accent)
                Spacer(Modifier.width(12.dp))
                Text(voice, style = MaterialTheme.typography.titleLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        // 왼쪽 아래: 목적지 · 예습한 곳 진행 · 속도 (내비 도착 정보 카드처럼)
        Column(
            Modifier.align(Alignment.BottomStart).padding(20.dp).width(360.dp).shadow(6.dp, R12).clip(R12).background(C.Surface)
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(vm.destinationName, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                GearIndicator(s.gear)
            }
            Spacer(Modifier.height(12.dp))
            if (s.spotsPlanned > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("예습한 곳", fontSize = 13.sp, color = C.InkSoft)
                    Spacer(Modifier.width(10.dp))
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(s.spotsPlanned) { i ->
                            Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(if (i < s.spotsPassed) C.Accent else C.Card))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("${s.spotsPassed}/${s.spotsPlanned}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${s.speedKph}", fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Text("km/h", fontSize = 14.sp, color = C.InkSoft, modifier = Modifier.padding(bottom = 6.dp))
            }
        }
    }
}

@Composable
private fun GearIndicator(gear: Gear) {
    Row {
        listOf(Gear.P, Gear.R, Gear.N, Gear.D).forEach { g ->
            val on = g == gear
            Text(
                g.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (on) Color.White else C.InkFaint,
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(if (on) C.Button else Color.Transparent)
                    .padding(horizontal = 7.dp, vertical = 1.dp),
            )
        }
    }
}

// ───────────── ③ 도착 후 (P) 리포트: 감점 없음 ─────────────

@Composable
private fun ReportScreen(s: Screen.Report, vm: AppViewModel) {
    val r = s.report
    SplitLayout(
        panel = {
            Label("도착 · 점수 없이, 오늘 운전 정리")
            Spacer(Modifier.height(2.dp))
            Text(r.title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(14.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                r.milestones.firstOrNull()?.let { m ->
                    Column(Modifier.fillMaxWidth().clip(R8).background(C.AccentSoft).padding(14.dp)) {
                        Text("처음 해냈어요", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = C.Accent)
                        Text(m, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                r.items.forEach { ReportRow(it); Divider() }
                Spacer(Modifier.height(14.dp))
                Label("다음엔 한 가지만")
                Spacer(Modifier.height(2.dp))
                Text(r.nextSuggestion, fontSize = 15.sp)
            }
            Spacer(Modifier.height(12.dp))
            BlackButton("다음 길 예습하기", vm::loadPreview, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            VoiceLine(vm.toast)
        },
        map = {
            RouteMap(
                route = vm.route,
                modifier = Modifier.fillMaxSize(),
                spots = vm.plannedIndexes.toList(),
                nextIndex = null,
                arrived = true,
                allPassed = true,
            )
        },
    )
}

@Composable
private fun ReportRow(item: ReportItemDto) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(item.label, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(item.note, fontSize = 13.sp, color = C.InkSoft)
        }
        Text(item.value, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        val arrow = when (item.trend) {
            "UP" -> "↑"
            "DOWN" -> "↓"
            else -> null
        }
        if (arrow != null) {
            Spacer(Modifier.width(4.dp))
            // 감점 없음: 아쉬운 쪽도 빨강 대신 회색
            Text(arrow, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (item.positive) C.Accent else C.InkFaint)
        }
    }
}

@Composable
private fun LoadingView(message: String) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = C.Accent, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(14.dp))
        Text(message, fontSize = 15.sp, color = C.InkSoft)
    }
}

@Composable
private fun ErrorView(message: String, vm: AppViewModel) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
        Label("연결을 확인해 주세요")
        Spacer(Modifier.height(6.dp))
        Text(message, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        BlackButton("다시 시도", vm::loadPreview, Modifier.width(240.dp))
    }
}

// ───────────── 아이콘 (Canvas) ─────────────

@Composable
private fun SpeakerIcon(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val body = Path().apply {
            moveTo(w * 0.08f, h * 0.36f); lineTo(w * 0.3f, h * 0.36f); lineTo(w * 0.55f, h * 0.12f)
            lineTo(w * 0.55f, h * 0.88f); lineTo(w * 0.3f, h * 0.64f); lineTo(w * 0.08f, h * 0.64f); close()
        }
        drawPath(body, color)
        drawArc(color, -45f, 90f, false, Offset(w * 0.45f, h * 0.25f), Size(w * 0.4f, h * 0.5f), style = Stroke(w * 0.09f, cap = StrokeCap.Round))
        drawArc(color, -50f, 100f, false, Offset(w * 0.4f, h * 0.06f), Size(w * 0.6f, h * 0.88f), style = Stroke(w * 0.09f, cap = StrokeCap.Round))
    }
}

/** 내비 화살표. 흰색 선으로 그린다. */
@Composable
private fun ManeuverIcon(type: ManeuverType?, arrived: Boolean, modifier: Modifier, size: Dp = 84.dp) {
    Canvas(modifier) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.1f
        val white = Color.White
        val st = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun line(vararg xy: Float) {
            val p = Path().apply { moveTo(xy[0], xy[1]); var i = 2; while (i < xy.size) { lineTo(xy[i], xy[i + 1]); i += 2 } }
            drawPath(p, white, style = st)
        }
        when {
            arrived -> {
                // 깃발
                line(w * 0.3f, h * 0.9f, w * 0.3f, h * 0.1f)
                val flag = Path().apply {
                    moveTo(w * 0.3f, h * 0.12f); lineTo(w * 0.8f, h * 0.27f); lineTo(w * 0.3f, h * 0.45f); close()
                }
                drawPath(flag, C.Accent)
            }
            type == ManeuverType.TURN_RIGHT -> {
                line(w * 0.35f, h * 0.9f, w * 0.35f, h * 0.4f, w * 0.8f, h * 0.4f)
                head(Offset(w * 0.8f, h * 0.4f), 0f, w, st)
            }
            type == ManeuverType.TURN_LEFT -> {
                line(w * 0.65f, h * 0.9f, w * 0.65f, h * 0.4f, w * 0.2f, h * 0.4f)
                head(Offset(w * 0.2f, h * 0.4f), 180f, w, st)
            }
            type == ManeuverType.UTURN -> {
                line(w * 0.68f, h * 0.9f, w * 0.68f, h * 0.35f)
                drawArc(white, 180f, 180f, false, Offset(w * 0.32f, h * 0.17f), Size(w * 0.36f, h * 0.36f), style = st)
                line(w * 0.32f, h * 0.35f, w * 0.32f, h * 0.7f)
                head(Offset(w * 0.32f, h * 0.72f), 90f, w, st)
            }
            type == ManeuverType.MERGE -> {
                drawLine(Color(0x66FFFFFF), Offset(w * 0.68f, h * 0.92f), Offset(w * 0.68f, h * 0.1f), sw * 0.6f, cap = StrokeCap.Round)
                line(w * 0.22f, h * 0.9f, w * 0.22f, h * 0.7f, w * 0.55f, h * 0.38f, w * 0.55f, h * 0.12f)
                head(Offset(w * 0.55f, h * 0.12f), -90f, w, st)
            }
            type == ManeuverType.EXIT -> {
                line(w * 0.4f, h * 0.9f, w * 0.4f, h * 0.55f, w * 0.75f, h * 0.2f)
                head(Offset(w * 0.75f, h * 0.2f), -45f, w, st)
            }
            type == ManeuverType.ROUNDABOUT -> {
                drawCircle(white, w * 0.2f, Offset(w * 0.5f, h * 0.45f), style = st)
                line(w * 0.5f, h * 0.92f, w * 0.5f, h * 0.65f)
            }
            type == ManeuverType.SCHOOL_ZONE -> {
                val tri = Path().apply {
                    moveTo(w * 0.5f, h * 0.12f); lineTo(w * 0.9f, h * 0.85f); lineTo(w * 0.1f, h * 0.85f); close()
                }
                drawPath(tri, C.Accent, style = st)
                line(w * 0.5f, h * 0.4f, w * 0.5f, h * 0.6f)
                drawCircle(white, sw * 0.6f, Offset(w * 0.5f, h * 0.73f))
            }
            else -> {
                line(w * 0.5f, h * 0.9f, w * 0.5f, h * 0.12f)
                head(Offset(w * 0.5f, h * 0.12f), -90f, w, st)
            }
        }
    }
}

/** 화살촉. angleDeg: 화살이 가리키는 방향 (0 = 오른쪽, 90 = 아래) */
private fun DrawScope.head(tip: Offset, angleDeg: Float, w: Float, st: Stroke) {
    val len = w * 0.2f
    val p = Path()
    for (d in listOf(150f, -150f)) {
        val a = (angleDeg + d) * PI.toFloat() / 180f
        p.moveTo(tip.x, tip.y)
        p.lineTo(tip.x + len * cos(a), tip.y + len * sin(a))
    }
    drawPath(p, Color.White, style = st)
}
