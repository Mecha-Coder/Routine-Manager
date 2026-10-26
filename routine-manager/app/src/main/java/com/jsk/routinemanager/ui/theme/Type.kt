package com.jsk.routinemanager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.jsk.routinemanager.R

/** Google Sans Flex, subset variable font with only the weight axis (ARCHITECTURE §9). */
val GoogleSansFlex = FontFamily(
    listOf(400, 500, 600, 700).map { w ->
        Font(
            R.font.google_sans_flex,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
        )
    }
)

// Tabular figures on every style, so numbers never shift width as they change (README › Typography).
private val Base = TextStyle(fontFamily = GoogleSansFlex, fontFeatureSettings = "tnum")

/**
 * CSS `normal` line height for this font, the line height Chrome gives the prototype's text: hhea
 * ascender 1932 − descender −572, no line gap, at 2000 units/em.
 */
private const val NORMAL_LINE_EM = 1.252f

/**
 * Text boxes as tall as in the prototype's CSS: exactly the line height, text centred. Without it
 * the phone makes them taller, which pushed the dashboard 4–5 dp down (M3 notes):
 * - Compose always turns on Android's fallback line spacing, and on this phone that gives any text
 *   with letters a 1.466 em line instead of the font's 1.252 (digits alone keep 1.252).
 * - Compose's default mode pads a single line back up to that height when the line height is
 *   smaller (the 0.84 em clock came out 45 dp too tall).
 * Mode.Tight with Trim.Both pins the line to the given height. Known gap: a digits-only text in a
 * style taller than 1.252 em (sub-routines, list body) is trimmed to 1.252 em.
 */
private val CssLineBox = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both, LineHeightStyle.Mode.Tight)

private fun style(size: Float, weight: Int, lineHeight: Float = NORMAL_LINE_EM) = Base.copy(
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    lineHeight = lineHeight.em,
    lineHeightStyle = CssLineBox,
)

/**
 * The README type scale (README › Typography). Colours are applied by the screens.
 * Compose has no text-transform: callers uppercase [label] text themselves.
 */
object RmType {
    val clock = style(104f, 500, lineHeight = 0.84f).copy(letterSpacing = (-4).sp)
    val clockAmPm = style(20f, 500)
    val day = style(21f, 600)
    val date = style(21f, 400)

    /** "Lists", "Settings", "Weekly routine". */
    val screenTitle = style(22f, 500)
    val listTitle = style(20f, 500)
    val dialogTitle = style(20f, 500)

    val routineHeading = style(20f, 600)
    val blockTime = style(20f, 600)
    val blockTimeAmPm = style(12f, 600)

    /** CURRENT and NEXT block titles. */
    val routineValue = style(19f, 700, lineHeight = 1.15f)
    val leftValue = style(17f, 600)
    val subRoutine = style(17f, 400, lineHeight = 1.3f)
    val bedtime = style(22f, 600, lineHeight = 1.2f)

    /** The small "Routine" label above the Bedtime message (prototype: 16 / 600). */
    val bedtimeLabel = style(16f, 600)

    val menuLabel = style(17f, 500)
    val groceryCategory = style(17f, 600)
    val body = style(16f, 400, lineHeight = 1.35f)

    /** A selected grocery item. */
    val bodySelected = body.copy(fontWeight = FontWeight(700))

    /** A list item's text field in edit mode: 16 / 400 at the input's `normal` line height. */
    val field = style(16f, 400)

    /** The drag ghost's "+2 sub-tasks" (prototype: 12 / 400). */
    val dragGhostCount = style(12f, 400)

    /** "Nothing here. Tap ✎ to add." (prototype: 15 / 400). */
    val emptyState = style(15f, 400)
    val badge = style(13f, 600)

    /** List text buttons ("Add task", "Add item") and the "Add category" pill. */
    val button = style(15f, 600)

    /** Settings "Preview sound" pill and the block editor's buttons. */
    val buttonSmall = style(14f, 600)

    /** LEFT, CURRENT, NEXT. */
    val label = style(11f, 600).copy(letterSpacing = 0.08.em)
    val settingsSection = style(13f, 600)

    /** Settings row labels ("Theme", "Weekly timetable") and tone names (prototype: 16 / 500). */
    val settingsLabel = style(16f, 500)

    /** The selected tone's name. */
    val settingsLabelSelected = settingsLabel.copy(fontWeight = FontWeight(700))

    /** A tone's description under its name (prototype: 13 / 400). */
    val settingsDescription = style(13f, 400)

    /** The Theme segmented control's Light / Dark and the volume's `60%` (prototype: 14 / 500). */
    val control = style(14f, 500)

    /** The timetable's day chips, Mon…Sun (prototype: 12 / 600). */
    val timetableDay = style(12f, 600)

    /** Also the create gesture's ghost label. */
    val timetableBlockTitle = style(10.5f, 600)

    /** Drawn at 72 % opacity by the timetable. */
    val timetableBlockRange = style(10.5f, 400)
    val timetableHour = style(10f, 500)
}

private fun TextStyle.rm() = merge(Base)

/** Material's own styles in Google Sans Flex, for any stock component that uses them. */
internal val MaterialTypography = Typography().run {
    Typography(
        displayLarge = displayLarge.rm(),
        displayMedium = displayMedium.rm(),
        displaySmall = displaySmall.rm(),
        headlineLarge = headlineLarge.rm(),
        headlineMedium = headlineMedium.rm(),
        headlineSmall = headlineSmall.rm(),
        titleLarge = titleLarge.rm(),
        titleMedium = titleMedium.rm(),
        titleSmall = titleSmall.rm(),
        bodyLarge = bodyLarge.rm(),
        bodyMedium = bodyMedium.rm(),
        bodySmall = bodySmall.rm(),
        labelLarge = labelLarge.rm(),
        labelMedium = labelMedium.rm(),
        labelSmall = labelSmall.rm(),
    )
}
