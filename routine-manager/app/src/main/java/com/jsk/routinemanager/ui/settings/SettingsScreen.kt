package com.jsk.routinemanager.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jsk.routinemanager.R
import com.jsk.routinemanager.data.Settings
import com.jsk.routinemanager.data.Theme
import com.jsk.routinemanager.data.Tone
import com.jsk.routinemanager.ui.MainViewModel
import com.jsk.routinemanager.ui.components.RadioDot
import com.jsk.routinemanager.ui.components.RmIconButton
import com.jsk.routinemanager.ui.components.Segment
import com.jsk.routinemanager.ui.components.SegmentedControl
import com.jsk.routinemanager.ui.theme.RmShapes
import com.jsk.routinemanager.ui.theme.RmTheme
import com.jsk.routinemanager.ui.theme.RmType
import kotlin.math.roundToInt

/** Each column is at least this wide; below two of them side by side, they stack (README › Screens › 4). */
private val COLUMN_MIN_WIDTH = 260.dp
private val COLUMN_GAP = 32.dp
private val SECTION_GAP = 20.dp

private val THEMES = listOf(Theme.LIGHT to Segment(R.drawable.ic_light_mode, "Light"), Theme.DARK to Segment(R.drawable.ic_dark_mode, "Dark"))

private val Tone.label: String
    get() = when (this) {
        Tone.MARIMBA -> "Marimba"
        Tone.CHIME -> "Soft chime"
        Tone.STEEL_DRUM -> "Steel drum"
    }

private val Tone.description: String
    get() = when (this) {
        Tone.MARIMBA -> "Wooden notes, rising"
        Tone.CHIME -> "Gentle bell notes"
        Tone.STEEL_DRUM -> "Bright two-note ping"
    }

/**
 * Settings, full screen (README › Screens › 4): Appearance and Routine in one column, Block-change
 * sound in the other. The columns stack when the screen is too narrow for both (portrait).
 */
@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit, onTimetable: () -> Unit) {
    BackHandler(onBack = onBack)
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val playing by viewModel.tonePlaying.collectAsStateWithLifecycle()
    // Padding is max(design padding, notch) on every side (SPEC D18): the notch is at the top in
    // portrait and on the left or right in landscape.
    val notch = WindowInsets.displayCutout
    Column(
        Modifier
            .fillMaxSize()
            .background(RmTheme.colors.bg)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(notch.union(WindowInsets(left = 14.dp, top = 16.dp, right = 24.dp, bottom = 4.dp))),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RmIconButton(R.drawable.ic_arrow_back, "Back", onBack)
            Text("Settings", style = RmType.screenTitle)
        }
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(
                    notch.only(WindowInsetsSides.Horizontal)
                        .union(WindowInsets(left = 32.dp, top = 4.dp, right = 32.dp, bottom = 14.dp))
                ),
        ) {
            val current = settings ?: return@BoxWithConstraints
            val general = @Composable { modifier: Modifier ->
                GeneralColumn(current.theme, viewModel::setTheme, onTimetable, modifier)
            }
            val sound = @Composable { modifier: Modifier ->
                SoundColumn(current, playing, viewModel::pickTone, viewModel::setVolume, viewModel::togglePreview, modifier)
            }
            if (maxWidth >= COLUMN_MIN_WIDTH * 2 + COLUMN_GAP) {
                Row(horizontalArrangement = Arrangement.spacedBy(COLUMN_GAP)) {
                    general(Modifier.weight(1f))
                    sound(Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(SECTION_GAP)) {
                    general(Modifier.fillMaxWidth())
                    sound(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier) {
        Text(
            title,
            Modifier.padding(start = 6.dp, end = 6.dp, bottom = 8.dp),
            color = RmTheme.colors.textSecondary,
            style = RmType.settingsSection,
        )
        content()
    }
}

/** Appearance (the Theme control) and Routine (the Weekly timetable row). */
@Composable
private fun GeneralColumn(theme: Theme, onTheme: (Theme) -> Unit, onTimetable: () -> Unit, modifier: Modifier) {
    val c = RmTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SECTION_GAP)) {
        Section("Appearance") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .background(c.surface, RmShapes.settingsRow)
                    .padding(start = 18.dp, end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_contrast), null, Modifier.size(22.dp))
                    Text("Theme", style = RmType.settingsLabel)
                }
                SegmentedControl(
                    THEMES.map { it.second },
                    selected = THEMES.indexOfFirst { it.first == theme },
                    onSelect = { onTheme(THEMES[it].first) },
                )
            }
        }
        Section("Routine") {
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .background(if (pressed) c.surfacePressed else c.surface, RmShapes.settingsRow)
                    .clickable(interaction, indication = null, role = Role.Button, onClick = onTimetable)
                    .padding(start = 18.dp, end = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_calendar_view_week), null, Modifier.size(22.dp))
                Text("Weekly timetable", Modifier.weight(1f), style = RmType.settingsLabel)
                Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(22.dp), tint = c.textSecondary)
            }
        }
    }
}

/** Block-change sound: one card of three tone rows and the volume row, then Preview / Stop. */
@Composable
private fun SoundColumn(
    settings: Settings,
    playing: Boolean,
    onTone: (Tone) -> Unit,
    onVolume: (Int) -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier,
) {
    val c = RmTheme.colors
    Section("Block-change sound", modifier) {
        Column {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RmShapes.settingsRow)
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Tone.entries.forEach { tone ->
                    ToneRow(tone, selected = tone == settings.tone, onClick = { onTone(tone) })
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .background(c.surface, RmShapes.settingsCardRow)
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_volume_down), null, Modifier.size(22.dp))
                    VolumeSlider(settings.volume, onVolume, Modifier.weight(1f))
                    Text("${settings.volume}%", Modifier.width(40.dp), textAlign = TextAlign.End, style = RmType.control)
                }
            }
            Row(
                Modifier
                    .padding(top = 12.dp)
                    .height(40.dp)
                    .clip(RmShapes.pill)
                    .background(c.textPrimary)
                    .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onPreview)
                    .padding(start = 16.dp, end = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(if (playing) R.drawable.ic_stop else R.drawable.ic_play_arrow),
                    null,
                    Modifier.size(20.dp),
                    tint = c.onPrimary,
                )
                Text(if (playing) "Stop" else "Preview sound", color = c.onPrimary, style = RmType.buttonSmall)
            }
        }
    }
}

/** Radio, name and description. Tapping selects the tone and plays it (README › Screens › 4). */
@Composable
private fun ToneRow(tone: Tone, selected: Boolean, onClick: () -> Unit) {
    val c = RmTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .background(if (pressed) c.surfacePressed else c.surface, RmShapes.settingsCardRow)
            .selectable(selected, interaction, indication = null, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioDot(selected)
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(tone.label, style = if (selected) RmType.settingsLabelSelected else RmType.settingsLabel)
            Text(tone.description, color = c.textSecondary, style = RmType.settingsDescription)
        }
    }
}

/** The thumb is a 16 dp disc; its centre travels from one end of the track to the other, as in Chrome. */
private val THUMB_RADIUS = 8.dp

/**
 * 0..100 in whole steps, drawn like the prototype's range input (Chrome, `accent-color` ink): an
 * 8 dp track with a 1 dp `outline` border, filled with `textPrimary` up to a 16 dp round thumb.
 * A tap sets the value; so does a sideways drag, so a vertical swipe on it still scrolls the screen.
 *
 * Not Material's Slider: with a custom thumb and track inside a weighted row, its touch positions
 * didn't match the drawn track (the last 10 dp at each end did nothing, M8 notes).
 */
@Composable
private fun VolumeSlider(volume: Int, onVolume: (Int) -> Unit, modifier: Modifier) {
    val c = RmTheme.colors
    val current by rememberUpdatedState(volume)
    val change by rememberUpdatedState(onVolume)
    fun PointerInputScope.select(x: Float) {
        val radius = THUMB_RADIUS.toPx()
        val value = ((x - radius) / (size.width - 2 * radius) * 100).roundToInt().coerceIn(0, 100)
        if (value != current) change(value)
    }
    Canvas(
        modifier
            .height(48.dp)
            .progressSemantics(volume.toFloat(), 0f..100f)
            .pointerInput(Unit) { detectTapGestures(onTap = { select(it.x) }) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { select(it.x) },
                    onHorizontalDrag = { drag, _ -> select(drag.position.x) },
                )
            }
    ) {
        val radius = THUMB_RADIUS.toPx()
        val trackHeight = 8.dp.toPx()
        val stroke = 1.dp.toPx()
        val top = (size.height - trackHeight) / 2
        val corner = CornerRadius(trackHeight / 2)
        val thumbX = radius + (size.width - 2 * radius) * volume / 100f
        drawRoundRect(
            c.outline,
            topLeft = Offset(stroke / 2, top + stroke / 2),
            size = Size(size.width - stroke, trackHeight - stroke),
            cornerRadius = corner,
            style = Stroke(stroke),
        )
        drawRoundRect(c.textPrimary, topLeft = Offset(0f, top), size = Size(thumbX, trackHeight), cornerRadius = corner)
        drawCircle(c.textPrimary, radius, Offset(thumbX, size.height / 2))
    }
}
