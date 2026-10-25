package com.jsk.routinemanager.debug

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jsk.routinemanager.R
import com.jsk.routinemanager.domain.TimeFormat
import com.jsk.routinemanager.domain.dayIndex
import com.jsk.routinemanager.ui.components.RmIconButton
import com.jsk.routinemanager.ui.theme.RmShapes
import com.jsk.routinemanager.ui.theme.RmTheme
import com.jsk.routinemanager.ui.theme.RmType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import kotlin.math.roundToInt

private val SPEEDS = listOf("Pause" to 0.0, "1×" to 1.0, "60×" to 60.0, "600×" to 600.0)

/**
 * The prototype's TEST bar as a screen (SPEC §3.6): Live, jump to a day, minute-of-day slider,
 * speed, and reset to the sample data. Debug builds only.
 */
@Composable
internal fun DebugScreen(clock: FakeAppClock, ticks: Flow<LocalDateTime>, onReset: () -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val c = RmTheme.colors
    val now by ticks.collectAsStateWithLifecycle(initialValue = clock.now())
    val sim by clock.sim.collectAsStateWithLifecycle()
    val today = dayIndex(now.toLocalDate())
    val minute = now.hour * 60 + now.minute
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .windowInsetsPadding(WindowInsets.displayCutout.union(WindowInsets(left = 32.dp, top = 12.dp, right = 32.dp, bottom = 16.dp)))
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RmIconButton(R.drawable.ic_arrow_back, "Back", onBack, Modifier.offset(x = (-12).dp))
            Text("Debug", style = RmType.screenTitle)
        }
        Text("Fake clock", color = c.textSecondary, style = RmType.settingsSection)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip("Live", selected = sim == null, onClick = clock::goLive)
            repeat(7) { day -> Chip(TimeFormat.day(day), selected = day == today) { clock.jumpToDay(day) } }
        }
        // Full width, so in landscape every minute is at least one pixel: an exact test time can be
        // set with one tap, also by `adb shell input tap`.
        Slider(
            value = minute.toFloat(),
            onValueChange = { clock.setMinuteOfDay(it.roundToInt()) },
            valueRange = 0f..1439f,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            SPEEDS.forEach { (label, speed) ->
                Chip(label, selected = sim?.speed == speed) { clock.setSpeed(speed) }
            }
            val time = TimeFormat.clock(minute)
            Text("${TimeFormat.day(today)} ${time.time} ${time.amPm}", Modifier.padding(start = 16.dp), style = RmType.body)
        }
        Text("Data", Modifier.padding(top = 8.dp), color = c.textSecondary, style = RmType.settingsSection)
        Chip("Reset data to sample", selected = false, onClick = onReset)
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = RmTheme.colors
    Box(
        Modifier
            .height(36.dp)
            .clip(RmShapes.pill)
            .background(if (selected) c.textPrimary else c.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) c.onPrimary else c.textPrimary, style = RmType.buttonSmall)
    }
}
