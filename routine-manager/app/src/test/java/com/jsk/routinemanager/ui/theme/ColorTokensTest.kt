package com.jsk.routinemanager.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToLong

/**
 * Screens may use only these tokens, so a mistyped hex value or a token missing from one palette
 * would put a wrong colour on every screen that uses it, and the light and dark themes would drift
 * apart. The expected values below are copied from the README's colour table, not from Color.kt.
 */
class ColorTokensTest {

    private fun rgba(r: Int, g: Int, b: Int, a: Double): Long =
        ((a * 255).roundToLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()

    // token name -> (light ARGB, dark ARGB), in the README's table order.
    private val readme = listOf(
        "bg" to (0xFFE6E6E6 to 0xFF000000),
        "surface" to (0xFFDBDBDB to 0xFF242424),
        "surfacePressed" to (0xFFD4D4D4 to 0xFF2F2F2F),
        "badgeIdle" to (0xFFD1D1D1 to 0xFF323232),
        "divider" to (0xFFCFCFCF to 0xFF353535),
        "outlineSoft" to (0xFFC8C8C8 to 0xFF3D3D3D),
        "outline" to (0xFFAAAAAA to 0xFF595959),
        "outlineStrong" to (0xFF808080 to 0xFF838383),
        "textPrimary" to (0xFF0F0F0F to 0xFFEDEDED),
        "textSecondary" to (0xFF565656 to 0xFFB2B2B2),
        "textDone" to (0xFF7E7E7E to 0xFF858585),
        "textHint" to (0xFF979797 to 0xFF6B6B6B),
        "onPrimary" to (0xFFE6E6E6 to 0xFF000000),
        "accent" to (0xFF2952E6 to 0xFF4DE0A6),
        "onAccent" to (0xFFE6E6E6 to 0xFF111111),
        "scrim" to (rgba(0, 0, 0, .32) to rgba(255, 255, 255, .62)),
        "todayColumn" to (0xFFE1E1E1 to 0xFF0F0F0F),
        "gridLine" to (0xFFD7D7D7 to 0xFF2A2A2A),
    )

    // Naming every field here means a token dropped from RmColors fails to compile.
    private fun RmColors.byName(): Map<String, Color> = mapOf(
        "bg" to bg, "surface" to surface, "surfacePressed" to surfacePressed,
        "badgeIdle" to badgeIdle, "divider" to divider, "outlineSoft" to outlineSoft,
        "outline" to outline, "outlineStrong" to outlineStrong, "textPrimary" to textPrimary,
        "textSecondary" to textSecondary, "textDone" to textDone, "textHint" to textHint,
        "onPrimary" to onPrimary, "accent" to accent, "onAccent" to onAccent, "scrim" to scrim,
        "todayColumn" to todayColumn, "gridLine" to gridLine,
    )

    private fun Color.argb(): Long = toArgb().toLong() and 0xFFFFFFFFL

    private fun hex(v: Long) = "#" + v.toString(16).uppercase().padStart(8, '0')

    @Test
    fun everyReadmeTokenHasTheReadmeValueInBothPalettes() {
        val light = LightColors.byName()
        val dark = DarkColors.byName()
        assertEquals(readme.map { it.first }.toSet(), light.keys)
        for ((name, values) in readme) {
            assertEquals("light $name", hex(values.first), hex(light.getValue(name).argb()))
            assertEquals("dark $name", hex(values.second), hex(dark.getValue(name).argb()))
        }
    }
}
