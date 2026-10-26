package com.jsk.routinemanager.ui.lists

import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jsk.routinemanager.R
import com.jsk.routinemanager.data.OutlineItem
import com.jsk.routinemanager.domain.DragMath
import com.jsk.routinemanager.domain.Numbering
import com.jsk.routinemanager.domain.OutlineOps
import com.jsk.routinemanager.ui.theme.RmElevation
import com.jsk.routinemanager.ui.theme.RmShapes
import com.jsk.routinemanager.ui.theme.RmTheme
import com.jsk.routinemanager.ui.theme.RmType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// README › Drag handle gestures, SPEC §3.7.
private const val INDENT_SNAP_MS = 140
private const val SLIDE_MS = 200
private const val DROP_MS = 170
private const val PICK_UP_HAPTIC_MS = 12L

/** The lifted ghost is drawn 3 % larger (prototype). */
private const val LIFT_SCALE = 0.03f

/** CSS `ease`, the prototype's timing for the indent snap and the drop. */
private val CssEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

/** README: the rows slide apart with `cubic-bezier(.2,.7,.3,1)`. */
private val SlideEasing = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)

/** The outline list as the edit list shows it, and where a finished drag is saved. */
internal class DragList(
    /** The saved list the edit list was given. */
    val source: List<OutlineItem>,
    /** What it shows: [source], or a drag's result until [source] has it. */
    val items: List<OutlineItem>,
    /** The ghost's text for an item with no text yet. */
    val placeholder: String,
    /** "sub-task" or "sub-item", for the ghost's "+2 sub-tasks". */
    val childNoun: String,
    val onIndent: (itemId: String, depth: Int) -> Unit,
    val onMove: (itemId: String, index: Int, depth: Int) -> Unit,
)

/** A drag's result, shown in place of [source] until the saved data comes back with it. */
private class Committed(val source: List<OutlineItem>, val result: List<OutlineItem>)

/** Indent mode: the row follows the finger sideways. */
internal class IndentDrag(val id: String) {
    /** The row's sideways offset, in px. */
    val offset = mutableFloatStateOf(0f)
}

/** Reorder mode: the item and its children are lifted as a ghost; the other rows slide apart. */
internal class ReorderDrag(
    val list: DragList,
    /** The dragged rows are `from until end`: the item and its children. */
    val from: Int,
    val end: Int,
    /** Every row's position, in list order. */
    val positions: List<LayoutCoordinates>,
    /** How far below the row's top the finger went down. */
    val grip: Float,
    /** The row's left edge in the pane, where the ghost starts. */
    val left: Float,
    top: Float,
    /** One indent level, px. */
    val level: Float,
) {
    val number = Numbering.labels(list.items)[from]
    val hidden = list.items.subList(from, end).mapTo(HashSet()) { it.id }

    /** How far each other row has slid, px. */
    val shifts = list.items.filter { it.id !in hidden }.associate { it.id to Animatable(0f) }

    /** The ghost's position in the pane, px. */
    val x = mutableFloatStateOf(left)
    val y = mutableFloatStateOf(top)

    /** 1 while lifted (3 % larger, with the drag shadow), 0 once it has landed. */
    val lift = mutableFloatStateOf(1f)

    /** Where it would land: an index into the list without the dragged rows. */
    var index = from
}

/**
 * The drag-handle gestures of an outline list in edit mode (README › Drag handle gestures). It
 * lives with the list panel (ListDetail), which draws the ghost over the whole panel as the
 * prototype does, and every position is measured in the panel.
 *
 * Positions that change every frame (the indent offset, the slides, the ghost) are read only in
 * layer, layout and draw lambdas, so a drag never recomposes the rows. Only the start and end of a
 * reorder recompose (the ghost appears and goes), and a commit recomposes the list once.
 */
@Stable
internal class OutlineDragState(
    private val scope: CoroutineScope,
    private val focusManager: FocusManager,
    private val vibrator: Vibrator?,
) {
    /** The list panel. */
    var pane: LayoutCoordinates? = null

    /** Each row's position, without the drag's offsets (those are drawn in a layer inside it). */
    private val rows = HashMap<String, LayoutCoordinates>()

    /** Published by the edit list after each composition. */
    var list: DragList? = null

    var indent by mutableStateOf<IndentDrag?>(null)
        private set
    var reorder by mutableStateOf<ReorderDrag?>(null)
        private set
    private var committed by mutableStateOf<Committed?>(null)

    /** A gesture, or its snap or drop, is running; the other handles wait for it. */
    private var busy = false

    /**
     * What the edit list shows for the saved [items]: a drag's result until the saved data has it.
     * The rows' offsets are cleared in the same frame as the result shows, so no frame shows the
     * old order without its offsets.
     */
    fun shown(items: List<OutlineItem>): List<OutlineItem> = committed?.takeIf { it.source === items }?.result ?: items

    fun placed(id: String, row: LayoutCoordinates) {
        rows[id] = row
    }

    /** The gestures of the handle of item [id]; [handle] is the handle's own position. */
    suspend fun PointerInputScope.handleGestures(id: String, handle: () -> LayoutCoordinates?) = awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val list = list ?: return@awaitEachGesture
        val index = list.items.indexOfFirst { it.id == id }
        if (busy || index < 0) return@awaitEachGesture
        // As in the prototype, touching a handle ends typing, so the keyboard closes.
        focusManager.clearFocus()
        val toPane = { position: Offset -> inPane(handle(), position) }
        val start = toPane(down.position) ?: return@awaitEachGesture
        val slop = DragMath.DECIDE_DP.dp.toPx()
        var decidedBy = down
        // true: indent; false: the list scrolls, or it was a tap; null: held still, so reorder.
        val indents = withTimeoutOrNull(DragMath.HOLD_MILLIS) {
            var decision: DragMath.Decision? = null
            while (decision == null) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                // Lifted before the hold: a tap on the handle does nothing.
                if (change == null || !change.pressed) return@withTimeoutOrNull false
                val at = toPane(change.position) ?: return@withTimeoutOrNull false
                decision = DragMath.decide(at.x - start.x, at.y - start.y, slop)
                decidedBy = change
            }
            // A scroll is not consumed here, so the list's own scrolling takes the swipe.
            decision == DragMath.Decision.INDENT
        }
        val level = DragMath.LEVEL_DP.dp.toPx()
        when (indents) {
            true -> indentGesture(list, index, down.id, decidedBy, start.x, level, toPane)
            null -> reorderGesture(list, index, down.id, start, level, toPane)
            false -> Unit
        }
    }

    private fun inPane(handle: LayoutCoordinates?, position: Offset): Offset? {
        val pane = pane
        if (pane == null || handle == null || !pane.isAttached || !handle.isAttached) return null
        return pane.localPositionOf(handle, position)
    }

    /** README › 1: the row follows the finger sideways, rubber-banded; on release it snaps to a level. */
    private suspend fun AwaitPointerEventScope.indentGesture(
        list: DragList,
        index: Int,
        pointer: PointerId,
        first: PointerInputChange,
        startX: Float,
        level: Float,
        toPane: (Offset) -> Offset?,
    ) {
        val item = list.items[index]
        val maxDepth = DragMath.maxIndentDepth(list.items, index)
        val drag = IndentDrag(item.id)
        busy = true
        indent = drag
        try {
            var change: PointerInputChange? = first
            while (change != null) {
                change.consume()
                toPane(change.position)?.let {
                    drag.offset.floatValue = DragMath.indentOffset(it.x - startX, item.depth, maxDepth, level)
                }
                if (!change.pressed) break
                change = awaitPointerEvent().changes.firstOrNull { it.id == pointer }
            }
        } finally {
            // Also when the gesture is cancelled, as in the prototype (pointercancel = release).
            val depth = DragMath.indentDepth(drag.offset.floatValue, item.depth, maxDepth, level)
            scope.launch {
                val spec = tween<Float>(INDENT_SNAP_MS, easing = CssEase)
                animate(drag.offset.floatValue, (depth - item.depth) * level, animationSpec = spec) { value, _ ->
                    drag.offset.floatValue = value
                }
                if (depth != item.depth) {
                    committed = Committed(list.source, OutlineOps.setDepth(list.items, item.id, depth))
                    list.onIndent(item.id, depth)
                }
                indent = null
                busy = false
            }
        }
    }

    /** README › 2: the item lifts as a ghost, the rows slide apart, and on release it drops into the gap. */
    private suspend fun AwaitPointerEventScope.reorderGesture(
        list: DragList,
        index: Int,
        pointer: PointerId,
        start: Offset,
        level: Float,
        toPane: (Offset) -> Offset?,
    ) {
        val pane = pane ?: return
        val coordinates = list.items.map { item -> rows[item.id]?.takeIf { it.isAttached } ?: return }
        rows.keys.retainAll(list.items.mapTo(HashSet()) { it.id })
        val row = pane.localPositionOf(coordinates[index], Offset.Zero)
        val drag = ReorderDrag(
            list,
            from = index,
            end = OutlineOps.subtreeEnd(list.items, index),
            positions = coordinates,
            grip = start.y - row.y,
            left = row.x,
            top = row.y,
            level = level,
        )
        busy = true
        reorder = drag
        vibrator?.vibrate(VibrationEffect.createOneShot(PICK_UP_HAPTIC_MS, VibrationEffect.DEFAULT_AMPLITUDE))
        try {
            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == pointer } ?: break
                change.consume()
                if (!change.pressed) break
                toPane(change.position)?.let { move(drag, it.y) }
            }
        } finally {
            drop(drag)
        }
    }

    /** The rows' tops and heights in the pane, as laid out (without the slides). */
    private fun ReorderDrag.measure(): Pair<List<Float>, List<Float>>? {
        val pane = pane?.takeIf { it.isAttached } ?: return null
        if (positions.any { !it.isAttached }) return null
        return positions.map { pane.localPositionOf(it, Offset.Zero).y } to positions.map { it.size.height.toFloat() }
    }

    private fun move(drag: ReorderDrag, fingerY: Float) {
        val top = fingerY - drag.grip
        drag.y.floatValue = top
        val (tops, heights) = drag.measure() ?: return
        val index = DragMath.dropIndex(tops, heights, drag.from, drag.end, top)
        if (index == drag.index) return
        drag.index = index
        val height = heights.subList(drag.from, drag.end).sum()
        drag.list.items.forEachIndexed { k, item ->
            val shift = drag.shifts[item.id] ?: return@forEachIndexed
            val target = DragMath.shift(k, drag.from, drag.end, index, height)
            if (shift.targetValue != target) scope.launch { shift.animateTo(target, tween(SLIDE_MS, easing = SlideEasing)) }
        }
    }

    private fun drop(drag: ReorderDrag) {
        val items = drag.list.items
        val index = drag.index
        val depth = DragMath.dropDepth(items, drag.from, drag.end, index)
        val base = items[drag.from].depth
        val top = drag.measure()?.let { (tops, heights) -> DragMath.gapTop(tops, heights, drag.from, drag.end, index) }
            ?: drag.y.floatValue
        val left = drag.left + (depth - base) * drag.level
        scope.launch {
            coroutineScope {
                val spec = tween<Float>(DROP_MS, easing = CssEase)
                launch { animate(drag.y.floatValue, top, animationSpec = spec) { value, _ -> drag.y.floatValue = value } }
                launch { animate(drag.x.floatValue, left, animationSpec = spec) { value, _ -> drag.x.floatValue = value } }
                launch { animate(drag.lift.floatValue, 0f, animationSpec = spec) { value, _ -> drag.lift.floatValue = value } }
            }
            if (index != drag.from || depth != base) {
                val id = items[drag.from].id
                committed = Committed(drag.list.source, OutlineOps.moveSubtree(items, id, index, depth))
                drag.list.onMove(id, index, depth)
            }
            reorder = null
            busy = false
        }
    }
}

@Composable
internal fun rememberOutlineDragState(): OutlineDragState {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    return remember {
        OutlineDragState(scope, focusManager, context.getSystemService(VibratorManager::class.java)?.defaultVibrator)
    }
}

/**
 * For each row of the edit list: records where the row is, and draws the drag's offsets and
 * hiding in a layer, without recomposing. The position is taken outside the layer, so it never
 * includes the drag's own offsets.
 */
internal fun Modifier.outlineDragRow(drag: OutlineDragState, id: String): Modifier = this
    .onPlaced { drag.placed(id, it) }
    .graphicsLayer {
        val lifted = drag.reorder
        translationX = drag.indent?.takeIf { it.id == id }?.offset?.floatValue ?: 0f
        translationY = lifted?.shifts?.get(id)?.value ?: 0f
        alpha = if (lifted != null && id in lifted.hidden) 0f else 1f
    }

private class HandlePosition {
    var coordinates: LayoutCoordinates? = null
}

/** The 28 × 44 dp handle with `drag_indicator` in `outlineStrong` (README › Edit mode). */
@Composable
internal fun DragHandle(drag: OutlineDragState, itemId: String) {
    val position = remember { HandlePosition() }
    Box(
        Modifier
            .size(width = 28.dp, height = 44.dp)
            .onPlaced { position.coordinates = it }
            .pointerInput(drag, itemId) { with(drag) { handleGestures(itemId) { position.coordinates } } },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_drag_indicator),
            "Drag to move or indent",
            Modifier.size(20.dp),
            tint = RmTheme.colors.outlineStrong,
        )
    }
}

/**
 * The lifted item while reordering (README): a 42 dp `bg` card with a 1 dp `divider` border,
 * radius 14 and the drag shadow, 3 % larger, showing the handle, number, text and "+N sub-tasks".
 * Its position, width, scale and shadow are read only in the offset, layout, layer and shadow
 * lambdas.
 */
@Composable
internal fun OutlineDragGhost(drag: OutlineDragState) {
    val lifted = drag.reorder ?: return
    val c = RmTheme.colors
    val item = lifted.list.items[lifted.from]
    val children = lifted.end - lifted.from - 1
    Row(
        Modifier
            .offset { IntOffset(lifted.x.floatValue.roundToInt(), lifted.y.floatValue.roundToInt()) }
            .graphicsLayer {
                val scale = 1f + LIFT_SCALE * lifted.lift.floatValue
                scaleX = scale
                scaleY = scale
            }
            .dropShadow(RmShapes.dragGhost) {
                val shadow = RmElevation.dragGhost
                radius = shadow.radius.toPx()
                offset = Offset(shadow.offset.x.toPx(), shadow.offset.y.toPx())
                color = shadow.color
                alpha = shadow.alpha * lifted.lift.floatValue
            }
            .layout { measurable, _ ->
                // As wide as the row is now, so a rotation during the drag resizes it too. The list
                // is measured before the ghost, so the row already has its new width.
                val width = lifted.positions[lifted.from].size.width
                val height = 42.dp.roundToPx()
                val placeable = measurable.measure(Constraints.fixed(width, height))
                layout(width, height) { placeable.place(0, 0) }
            }
            .background(c.bg, RmShapes.dragGhost)
            .border(1.dp, c.divider, RmShapes.dragGhost)
            .padding(start = 10.dp, end = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_drag_indicator), null, Modifier.size(20.dp), tint = c.textPrimary)
        Text(lifted.number, color = c.textSecondary, style = RmType.body)
        Text(
            item.text.ifEmpty { lifted.list.placeholder },
            Modifier.weight(1f),
            color = c.textPrimary,
            style = RmType.body,
            textDecoration = if (item.done) TextDecoration.LineThrough else null,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        if (children > 0) {
            val noun = lifted.list.childNoun + if (children > 1) "s" else ""
            Text("+$children $noun", color = c.textSecondary, style = RmType.dragGhostCount, maxLines = 1)
        }
    }
}
