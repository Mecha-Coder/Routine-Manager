package com.jsk.routinemanager.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The README's semantic colour tokens (design/README.md › Design tokens › Colour).
 * Screens take every colour from here, through [RmTheme.colors].
 */
@Immutable
data class RmColors(
    val bg: Color,
    val surface: Color,
    val surfacePressed: Color,
    val badgeIdle: Color,
    val divider: Color,
    val outlineSoft: Color,
    val outline: Color,
    val outlineStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDone: Color,
    val textHint: Color,
    val onPrimary: Color,
    /** Highlights only (README lists where); never body text or buttons. */
    val accent: Color,
    val onAccent: Color,
    val scrim: Color,
    val todayColumn: Color,
    val gridLine: Color,
)

val LightColors = RmColors(
    bg = Color(0xFFE6E6E6),
    surface = Color(0xFFDBDBDB),
    surfacePressed = Color(0xFFD4D4D4),
    badgeIdle = Color(0xFFD1D1D1),
    divider = Color(0xFFCFCFCF),
    outlineSoft = Color(0xFFC8C8C8),
    outline = Color(0xFFAAAAAA),
    outlineStrong = Color(0xFF808080),
    textPrimary = Color(0xFF0F0F0F),
    textSecondary = Color(0xFF565656),
    textDone = Color(0xFF7E7E7E),
    textHint = Color(0xFF979797),
    onPrimary = Color(0xFFE6E6E6),
    accent = Color(0xFF2952E6),
    onAccent = Color(0xFFE6E6E6),
    scrim = Color(red = 0f, green = 0f, blue = 0f, alpha = 0.32f),
    todayColumn = Color(0xFFE1E1E1),
    gridLine = Color(0xFFD7D7D7),
)

val DarkColors = RmColors(
    bg = Color(0xFF000000),
    surface = Color(0xFF242424),
    surfacePressed = Color(0xFF2F2F2F),
    badgeIdle = Color(0xFF323232),
    divider = Color(0xFF353535),
    outlineSoft = Color(0xFF3D3D3D),
    outline = Color(0xFF595959),
    outlineStrong = Color(0xFF838383),
    textPrimary = Color(0xFFEDEDED),
    textSecondary = Color(0xFFB2B2B2),
    textDone = Color(0xFF858585),
    textHint = Color(0xFF6B6B6B),
    onPrimary = Color(0xFF000000),
    accent = Color(0xFF4DE0A6),
    onAccent = Color(0xFF111111),
    // README value. Its footnote allows rgba(0,0,0,.6) if the white veil looks wrong on the phone.
    scrim = Color(red = 1f, green = 1f, blue = 1f, alpha = 0.62f),
    todayColumn = Color(0xFF0F0F0F),
    gridLine = Color(0xFF2A2A2A),
)
