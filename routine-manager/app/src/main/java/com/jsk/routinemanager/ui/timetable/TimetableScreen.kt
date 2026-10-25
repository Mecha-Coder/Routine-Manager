package com.jsk.routinemanager.ui.timetable

import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jsk.routinemanager.R
import com.jsk.routinemanager.data.Block
import com.jsk.routinemanager.domain.BlockRules
import com.jsk.routinemanager.domain.TimeFormat
import com.jsk.routinemanager.domain.TimeRange
import com.jsk.routinemanager.domain.dayIndex
import com.jsk.routinemanager.domain.minuteOfDay
import com.jsk.routinemanager.ui.MainViewModel
import com.jsk.routinemanager.ui.components.RmIconButton
import com.jsk.routinemanager.ui.editor.BlockEditorPlaceholder
import com.jsk.routinemanager.ui.editor.EditorRequest
import com.jsk.routinemanager.ui.theme.RmShapes
import com.jsk.routinemanager.ui.theme.RmTheme
import com.jsk.routinemanager.ui.theme.RmType
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

private val HOUR_HEIGHT = 44.dp
private val GRID_HEIGHT = HOUR_HEIGHT * 24
private val GUTTER_WIDTH = 46.dp

/** The grid's side padding (prototype: 10 left, 14 right), shared by the day chips so they line up. */
private val GRID_START = 10.dp
private val GRID_END = 14.dp

/** Each column has a 1 dp border on its left; blocks sit 3 dp inside it and 3 dp from the right. */
private val BLOCK_START = 1.dp + 3.dp
private val BLOCK_END = 3.dp

/** On open, now − 1.5 h is at the top (README › Screens › 5). */
private const val SCROLL_BACK_HOURS = 1.5

/** Create: hold this long on empty time, moving less than [CREATE_SLOP]; moving first scrolls instead. */
private const val LONG_PRESS_MS = 300L
private val CREATE_SLOP = 8.dp
private const val CREATE_HAPTIC_MS = 10L

/** The ghost's dashed outline (prototype: `1.5px dashed`, which Chrome draws as 3 dp dashes, 2 dp gaps). */
private val GHOST_STROKE = 1.5.dp
private val GHOST_DASH = 3.dp
private val GHOST_GAP = 2.dp

/**
 * The weekly routine (README › Screens › 5): Mon–Sun columns of 44 dp hours, today's column and
 * the now line, blocks with their range, the current block in the accent. A long press on empty
 * time and a drag draws a new block; a tap on a block opens it. Back returns to Settings.
 *
 * Only the now line follows the minute: it reads the time in its offset lambda, so the minute tick
 * moves it without recomposing the grid (ARCHITECTURE §3).
 */
@Composable
fun TimetableScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val c = RmTheme.colors
    val blocks by viewModel.timetableBlocks.collectAsStateWithLifecycle()
    val currentBlockId by viewModel.currentBlockId.collectAsStateWithLifecycle()
    // Not read during composition: only through `today`, the now line and the scroll on open.
    val now = viewModel.now.collectAsStateWithLifecycle()
    val today by remember { derivedStateOf { now.value?.let { dayIndex(it.toLocalDate()) } } }
    val nowMinute = remember { { now.value?.let(::minuteOfDay) ?: 0.0 } }
    // Placeholder until M10. Kept through rotation (the activity is not recreated), dropped on Back.
    var editor by remember { mutableStateOf<EditorRequest?>(null) }
    val vibrator = rememberVibrator()
    val haptic = remember(vibrator) { { vibrator.tick(CREATE_HAPTIC_MS) } }

    val scroll = rememberScrollState()
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        val hours = minuteOfDay(snapshotFlow { now.value }.filterNotNull().first()) / 60 - SCROLL_BACK_HOURS
        scroll.scrollTo((with(density) { HOUR_HEIGHT.toPx() } * hours).roundToInt().coerceAtLeast(0))
    }

    // Design padding, or the notch where it is deeper (SPEC D18): top in portrait, a side in landscape.
    val notch = WindowInsets.displayCutout
    val gridSides = notch.only(WindowInsetsSides.Horizontal).union(WindowInsets(left = GRID_START, right = GRID_END))
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(c.bg)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(notch.union(WindowInsets(left = 14.dp, top = 12.dp, right = 22.dp))),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RmIconButton(R.drawable.ic_arrow_back, "Back", onBack)
                Text("Weekly routine", style = RmType.screenTitle)
            }
            DayChips(today, gridSides)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .windowInsetsPadding(gridSides)
                    .padding(top = 10.dp, bottom = 14.dp)
                    // Full height before the blocks load, so the scroll on open isn't cut to 0.
                    .height(GRID_HEIGHT)
            ) {
                val all = blocks ?: return@Box
                WeekGrid(
                    all,
                    today,
                    currentBlockId,
                    nowMinute,
                    haptic,
                    onCreate = { day, range -> editor = EditorRequest.New(day, range) },
                    onOpen = { editor = EditorRequest.Edit(it) },
                )
            }
        }
        editor?.let { BlockEditorPlaceholder(it, blocks.orEmpty(), onClose = { editor = null }) }
    }
}

/** Mon…Sun over the columns; today is an accent pill. A 1 dp line separates them from the grid. */
@Composable
private fun DayChips(today: Int?, sides: WindowInsets) {
    val c = RmTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                val line = 1.dp.toPx()
                drawRect(c.gridLine, Offset(0f, size.height - line), Size(size.width, line))
            }
            .windowInsetsPadding(sides)
            .padding(top = 6.dp, bottom = 8.dp + 1.dp)
    ) {
        Spacer(Modifier.width(GUTTER_WIDTH))
        repeat(7) { day ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val isToday = day == today
                Text(
                    TimeFormat.day(day),
                    Modifier
                        .then(if (isToday) Modifier.background(c.accent, RmShapes.pill) else Modifier)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    color = if (isToday) c.onAccent else c.textPrimary,
                    style = RmType.timetableDay,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun WeekGrid(
    blocks: List<Block>,
    today: Int?,
    currentBlockId: String?,
    nowMinute: () -> Double,
    haptic: () -> Unit,
    onCreate: (day: Int, TimeRange) -> Unit,
    onOpen: (blockId: String) -> Unit,
) {
    // Same list instances until the blocks change, so unchanged columns skip recomposition.
    val byDay = remember(blocks) { blocks.groupBy { it.day } }
    Row(
        Modifier
            .fillMaxWidth()
            .height(GRID_HEIGHT)
    ) {
        HourGutter()
        repeat(7) { day ->
            DayColumn(
                byDay[day].orEmpty(),
                isToday = day == today,
                currentBlockId,
                nowMinute,
                haptic,
                onCreate = { onCreate(day, it) },
                onOpen,
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

/** `12 AM` … `12 AM`, each centred on its hour line, 8 dp from the columns. */
@Composable
private fun HourGutter() {
    Box(
        Modifier
            .width(GUTTER_WIDTH)
            .fillMaxHeight()
    ) {
        for (hour in 0..24) {
            Text(
                TimeFormat.hour(hour),
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp)
                    .centredAt(HOUR_HEIGHT * hour),
                color = RmTheme.colors.outlineStrong,
                style = RmType.timetableHour,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/** Places the element with its vertical centre on [y] (CSS `top: y; transform: translateY(-50%)`). */
private fun Modifier.centredAt(y: Dp) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) { placeable.place(0, y.roundToPx() - placeable.height / 2) }
}

/**
 * One day: hour lines, the left border and today's fill drawn behind; its blocks; the create
 * ghost while drawing; and the now line on today.
 */
@Composable
private fun DayColumn(
    dayBlocks: List<Block>,
    isToday: Boolean,
    currentBlockId: String?,
    nowMinute: () -> Double,
    haptic: () -> Unit,
    onCreate: (TimeRange) -> Unit,
    onOpen: (blockId: String) -> Unit,
    modifier: Modifier,
) {
    val c = RmTheme.colors
    // Changes only at a quarter-hour step of the drag, not every frame.
    var ghost by remember { mutableStateOf<TimeRange?>(null) }
    val latestBlocks by rememberUpdatedState(dayBlocks)
    val latestHaptic by rememberUpdatedState(haptic)
    val latestCreate by rememberUpdatedState(onCreate)
    Box(
        modifier
            .drawBehind {
                if (isToday) drawRect(c.todayColumn)
                val line = 1.dp.toPx()
                val hour = HOUR_HEIGHT.toPx()
                for (h in 0 until 24) drawRect(c.gridLine, Offset(0f, h * hour), Size(size.width, line))
                drawRect(c.gridLine, Offset.Zero, Size(line, size.height))
            }
            .pointerInput(Unit) {
                detectCreate(
                    dayBlocks = { latestBlocks },
                    onStart = { latestHaptic() },
                    onRange = { ghost = it },
                    onRelease = { latestCreate(it) },
                )
            }
    ) {
        dayBlocks.forEach { block ->
            key(block.id) { TimetableBlock(block, current = block.id == currentBlockId, onOpen) }
        }
        ghost?.let { CreateGhost(it) }
        if (isToday) NowLine(nowMinute)
    }
}

/**
 * README › Create: hold 300 ms on empty time, then drag; the range snaps to 15 minutes and stops at
 * the neighbouring blocks. Moving more than 8 dp before that, or lifting, does nothing, so a swipe
 * scrolls the grid. A press on a block does nothing here: the block's own tap opens it.
 */
private suspend fun PointerInputScope.detectCreate(
    dayBlocks: () -> List<Block>,
    onStart: () -> Unit,
    onRange: (TimeRange?) -> Unit,
    onRelease: (TimeRange) -> Unit,
) {
    val slop = CREATE_SLOP.toPx()
    fun minuteAt(y: Float) = y / size.height * BlockRules.DAY_END.toDouble()
    awaitEachGesture {
        val down = awaitFirstDown()
        val anchor = BlockRules.createAnchor(dayBlocks(), minuteAt(down.position.y)) ?: return@awaitEachGesture
        // Null when the 300 ms ran out with the finger still down and still: a long press.
        val ended = withTimeoutOrNull(LONG_PRESS_MS) {
            var gone = false
            while (!gone) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                // Consumed: the grid has started to scroll.
                gone = change == null || !change.pressed || change.isConsumed ||
                    (change.position - down.position).getDistance() > slop
            }
            true
        }
        if (ended != null) return@awaitEachGesture
        onStart()
        var range = BlockRules.dragRange(anchor, anchor.anchor.toDouble())
        onRange(range)
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
            if (change == null || !change.pressed) break
            // From here the finger draws the block: the grid must not scroll.
            change.consume()
            range = BlockRules.dragRange(anchor, minuteAt(change.position.y))
            onRange(range)
        }
        onRange(null)
        onRelease(BlockRules.releaseRange(anchor, range))
    }
}

private fun Dp.forMinutes(minutes: Int) = this * (minutes / 60f)

/** Title and `6:00am-11:30am`; `textPrimary` fill, or the accent while the block is on now. */
@Composable
private fun TimetableBlock(block: Block, current: Boolean, onOpen: (blockId: String) -> Unit) {
    val c = RmTheme.colors
    val ink = if (current) c.onAccent else c.onPrimary
    Column(
        Modifier
            .offset(y = HOUR_HEIGHT.forMinutes(block.start))
            .padding(start = BLOCK_START, end = BLOCK_END)
            .fillMaxWidth()
            .height(max(8.dp, HOUR_HEIGHT.forMinutes(block.end - block.start) - 2.dp))
            .clip(RmShapes.timetableBlock)
            .background(if (current) c.accent else c.textPrimary)
            .clickable(interactionSource = null, indication = null, role = Role.Button) { onOpen(block.id) }
            .padding(start = 7.dp, end = 7.dp, top = 4.dp)
    ) {
        Text(
            block.title,
            color = ink,
            style = RmType.timetableBlockTitle,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            TimeFormat.blockRange(block.start, block.end),
            color = ink.copy(alpha = 0.72f),
            style = RmType.timetableBlockRange,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The range being drawn: a dashed `textPrimary` outline with its `7:00 AM – 8:00 AM` label. */
@Composable
private fun CreateGhost(range: TimeRange) {
    val c = RmTheme.colors
    Box(
        Modifier
            .offset(y = HOUR_HEIGHT.forMinutes(range.start))
            .padding(start = BLOCK_START, end = BLOCK_END)
            .fillMaxWidth()
            .height(HOUR_HEIGHT.forMinutes(range.end - range.start))
            .clip(RmShapes.timetableBlock)
            .background(c.textPrimary.copy(alpha = 0.05f))
            .drawBehind {
                val stroke = GHOST_STROKE.toPx()
                drawRoundRect(
                    c.textPrimary,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(8.dp.toPx() - stroke / 2),
                    style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(GHOST_DASH.toPx(), GHOST_GAP.toPx()))),
                )
            }
            .padding(start = GHOST_STROKE + 7.dp, end = GHOST_STROKE + 7.dp, top = GHOST_STROKE + 4.dp)
    ) {
        Text(TimeFormat.ghostRange(range.start, range.end), color = c.textPrimary, style = RmType.timetableBlockTitle)
    }
}

/**
 * A 2 dp accent line across today's column with an 8 dp dot on its left end. The minute is read
 * only in the offset lambda, so each tick moves it without a recomposition.
 */
@Composable
private fun NowLine(nowMinute: () -> Double) {
    val accent = RmTheme.colors.accent
    Canvas(
        Modifier
            .offset { IntOffset(0, (nowMinute() / 60 * HOUR_HEIGHT.toPx() - 3.dp.toPx()).roundToInt()) }
            .fillMaxWidth()
            .height(8.dp)
    ) {
        val top = 3.dp.toPx()
        val thickness = 2.dp.toPx()
        drawRect(accent, Offset(0f, top), Size(size.width, thickness))
        drawCircle(accent, radius = 4.dp.toPx(), center = Offset(0f, top + thickness / 2))
    }
}

@Composable
private fun rememberVibrator(): Vibrator {
    val context = LocalContext.current
    return remember(context) { context.getSystemService(VibratorManager::class.java).defaultVibrator }
}

/** A light tick (README › Haptics); it follows the phone's touch-vibration setting. */
private fun Vibrator.tick(millis: Long) {
    val effect = VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
    } else {
        vibrate(effect)
    }
}
