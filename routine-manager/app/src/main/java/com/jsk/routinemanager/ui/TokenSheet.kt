package com.jsk.routinemanager.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.jsk.routinemanager.R
import com.jsk.routinemanager.ui.theme.RmColors
import com.jsk.routinemanager.ui.theme.RmShapes
import com.jsk.routinemanager.ui.theme.RmTheme
import com.jsk.routinemanager.ui.theme.RmType
import com.jsk.routinemanager.ui.theme.RoutineManagerTheme

// Temporary M1 screen for checking the theme on the phone: both palettes, the type scale and
// every icon. M3 replaces it with the dashboard.

private val icons: List<Pair<String, Int>> = listOf(
    "schedule" to R.drawable.ic_schedule,
    "bedtime" to R.drawable.ic_bedtime,
    "settings" to R.drawable.ic_settings,
    "arrow_back" to R.drawable.ic_arrow_back,
    "edit" to R.drawable.ic_edit,
    "check" to R.drawable.ic_check,
    "close" to R.drawable.ic_close,
    "delete" to R.drawable.ic_delete,
    "add" to R.drawable.ic_add,
    "drag_indicator" to R.drawable.ic_drag_indicator,
    "expand_more" to R.drawable.ic_expand_more,
    "format_list_bulleted" to R.drawable.ic_format_list_bulleted,
    "shopping_cart" to R.drawable.ic_shopping_cart,
    "person" to R.drawable.ic_person,
    "home" to R.drawable.ic_home,
    "contrast" to R.drawable.ic_contrast,
    "light_mode" to R.drawable.ic_light_mode,
    "dark_mode" to R.drawable.ic_dark_mode,
    "calendar_view_week" to R.drawable.ic_calendar_view_week,
    "chevron_right" to R.drawable.ic_chevron_right,
    "volume_down" to R.drawable.ic_volume_down,
    "play_arrow" to R.drawable.ic_play_arrow,
    "stop" to R.drawable.ic_stop,
    "error" to R.drawable.ic_error,
)

private val typeScale: List<Triple<String, TextStyle, String>> = listOf(
    Triple("clock 104/500", RmType.clock, "07:05"),
    Triple("clockAmPm 20/500", RmType.clockAmPm, "AM"),
    Triple("day 21/600", RmType.day, "Mon"),
    Triple("date 21/400", RmType.date, "19 Oct"),
    Triple("screenTitle 22/500", RmType.screenTitle, "Lists"),
    Triple("listTitle 20/500", RmType.listTitle, "To-Do"),
    Triple("dialogTitle 20/500", RmType.dialogTitle, "Edit block"),
    Triple("routineHeading 20/600", RmType.routineHeading, "Routine"),
    Triple("blockTime 20/600", RmType.blockTime, "07:00"),
    Triple("blockTimeAmPm 12/600", RmType.blockTimeAmPm, "AM"),
    Triple("routineValue 19/700", RmType.routineValue, "Work Session1"),
    Triple("leftValue 17/600", RmType.leftValue, "4hr 30min"),
    Triple("subRoutine 17/400", RmType.subRoutine, "Check email"),
    Triple("bedtime 22/600", RmType.bedtime, "Bedtime, see you tomorrow"),
    Triple("menuLabel 17/500", RmType.menuLabel, "Grocery"),
    Triple("groceryCategory 17/600", RmType.groceryCategory, "Produce"),
    Triple("body 16/400", RmType.body, "Overlaps “Commute” (7 AM – 8 AM) on Mon, Tue."),
    Triple("bodySelected 16/700", RmType.bodySelected, "Milk"),
    Triple("badge 13/600", RmType.badge, "12"),
    Triple("button 15/600", RmType.button, "Add task"),
    Triple("buttonSmall 14/600", RmType.buttonSmall, "Preview sound"),
    Triple("label 11/600", RmType.label, "LEFT  CURRENT  NEXT"),
    Triple("settingsSection 13/600", RmType.settingsSection, "Block-change sound"),
    Triple("timetableBlockTitle 10.5/600", RmType.timetableBlockTitle, "Work Session1"),
    Triple("timetableBlockRange 10.5/400", RmType.timetableBlockRange, "6:00am-11:30am"),
    Triple("timetableHour 10/500", RmType.timetableHour, "12 AM"),
)

private fun RmColors.named(): List<Pair<String, Color>> = listOf(
    "bg" to bg, "surface" to surface, "surfacePressed" to surfacePressed,
    "badgeIdle" to badgeIdle, "divider" to divider, "outlineSoft" to outlineSoft,
    "outline" to outline, "outlineStrong" to outlineStrong, "textPrimary" to textPrimary,
    "textSecondary" to textSecondary, "textDone" to textDone, "textHint" to textHint,
    "onPrimary" to onPrimary, "accent" to accent, "onAccent" to onAccent, "scrim" to scrim,
    "todayColumn" to todayColumn, "gridLine" to gridLine,
)

private fun Color.hex(): String {
    val argb = toArgb().toLong() and 0xFFFFFFFFL
    val digits = if (alpha == 1f) 6 else 8
    val mask = (1L shl (digits * 4)) - 1
    return "#" + (argb and mask).toString(16).uppercase().padStart(digits, '0')
}

@Composable
fun TokenSheet() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PaletteSection(dark = false)
        PaletteSection(dark = true)
    }
}

@Composable
private fun PaletteSection(dark: Boolean) {
    RoutineManagerTheme(dark = dark) {
        val c = RmTheme.colors
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.bg)
                // Design padding or the notch, whichever is larger (SPEC D18).
                .windowInsetsPadding(
                    WindowInsets.displayCutout.union(WindowInsets(24.dp, 20.dp, 24.dp, 20.dp))
                ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(if (dark) "Dark palette" else "Light palette", style = RmType.screenTitle)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                c.named().forEach { (name, color) -> Swatch(name, color) }
            }
            TabularFigures()
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                icons.forEach { (name, id) -> IconChip(name, id) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                typeScale.forEach { (name, style, sample) ->
                    Text(name, style = RmType.label, color = c.textSecondary)
                    Text(sample, style = style)
                }
            }
        }
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    val c = RmTheme.colors
    Row(
        Modifier.width(184.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(color, RmShapes.field)
                .border(1.dp, c.outline, RmShapes.field)
        )
        Column {
            Text(name, style = RmType.body)
            Text(color.hex(), style = RmType.label, color = c.textSecondary)
        }
    }
}

/** Right-aligned columns: with "tnum" the digits line up; proportional figures don't. */
@Composable
private fun TabularFigures() {
    val samples = listOf("11:11", "07:05", "00:00", "12:59")
    Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
        listOf("tnum" to RmType.blockTime, "pnum" to RmType.blockTime.copy(fontFeatureSettings = "pnum"))
            .forEach { (name, style) ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(name, style = RmType.label, color = RmTheme.colors.textSecondary)
                    samples.forEach { Text(it, style = style) }
                }
            }
    }
}

@Composable
private fun IconChip(name: String, @DrawableRes id: Int) {
    Row(
        Modifier.width(184.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(id), contentDescription = null, tint = RmTheme.colors.textPrimary)
        Text(name, style = RmType.badge)
    }
}
