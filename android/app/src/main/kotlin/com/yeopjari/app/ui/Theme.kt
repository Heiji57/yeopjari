package com.yeopjari.app.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Pleos Connect 차량 화면에 맞춘 밝은 테마.
 * 흰 바탕 · 검정 제목 · 회색 카드 · 검정 주 버튼 · 주황 포인트 하나. 시니어 모드는 글씨를 1.25배로.
 */
object YeopjariColors {
    val Background = Color(0xFFF6F7F8)
    val Surface = Color(0xFFFFFFFF)
    val Card = Color(0xFFEDEEF0)
    val Line = Color(0xFFDDDFE2)
    val Ink = Color(0xFF111214)
    val InkSoft = Color(0xFF6E737B)
    val InkFaint = Color(0xFFA4A8AE)
    val Button = Color(0xFF26282C)
    val Accent = Color(0xFFF08A24)
    val AccentSoft = Color(0xFFFFF1E2)
    val Dark = Color(0xFF16181B)
}

@Composable
fun YeopjariTheme(senior: Boolean, content: @Composable () -> Unit) {
    val k = if (senior) 1.25f else 1f
    val typography = Typography(
        displayLarge = TextStyle(fontSize = (64 * k).sp, fontWeight = FontWeight.Bold, lineHeight = (70 * k).sp),
        displaySmall = TextStyle(fontSize = (38 * k).sp, fontWeight = FontWeight.Bold, lineHeight = (46 * k).sp),
        headlineMedium = TextStyle(fontSize = (30 * k).sp, fontWeight = FontWeight.Bold, lineHeight = (40 * k).sp),
        titleLarge = TextStyle(fontSize = (21 * k).sp, fontWeight = FontWeight.Bold, lineHeight = (28 * k).sp),
        titleMedium = TextStyle(fontSize = (17 * k).sp, fontWeight = FontWeight.Bold, lineHeight = (24 * k).sp),
        bodyLarge = TextStyle(fontSize = (17 * k).sp, lineHeight = (25 * k).sp),
        bodyMedium = TextStyle(fontSize = (15 * k).sp, lineHeight = (22 * k).sp),
        labelLarge = TextStyle(fontSize = (17 * k).sp, fontWeight = FontWeight.Bold),
        labelMedium = TextStyle(fontSize = (13 * k).sp, lineHeight = (18 * k).sp),
    )
    val colors = lightColorScheme(
        background = YeopjariColors.Background,
        surface = YeopjariColors.Surface,
        surfaceVariant = YeopjariColors.Card,
        onBackground = YeopjariColors.Ink,
        onSurface = YeopjariColors.Ink,
        onSurfaceVariant = YeopjariColors.InkSoft,
        outline = YeopjariColors.Line,
        primary = YeopjariColors.Button,
        onPrimary = Color.White,
        secondary = YeopjariColors.Accent,
        onSecondary = Color.White,
    )
    MaterialTheme(colorScheme = colors, typography = typography) {
        // 색을 따로 정하지 않은 글자도 테마 글자색으로
        CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
    }
}
