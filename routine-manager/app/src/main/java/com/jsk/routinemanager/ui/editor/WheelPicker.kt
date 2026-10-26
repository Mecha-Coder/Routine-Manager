package com.jsk.routinemanager.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.jsk.routinemanager.domain.BlockRules
import com.jsk.routinemanager.ui.theme.RmShapes
import com.jsk.routinemanager.ui.theme.RmTheme
import com.jsk.routinemanager.ui.theme.RmType

/** One value's row; three are visible, the middle one on the selection band (README › Screens › 6). */
private val ROW = 24.dp
private val DRUM_WIDTH = 36.dp

/** The picker's border box: the three rows inside a 1 dp border. */
private val PICKER_HEIGHT = ROW * 3 + 2.dp

/** The band sits 6 dp in from the border on both sides, behind the middle row. */
private val BAND_INSET = 1.dp + 6.dp

private val HOURS = listOf("12") + (1..11).map(Int::toString)
private val QUARTERS = listOf("00", "15", "30", "45")
private val HALVES = listOf("am", "pm")

/**
 * The top and bottom fade (the prototype's mask: clear at the edges, solid from 32 % to 68 %). Only
 * its alpha is used, as a mask over the drum.
 */
private val FADE = Brush.verticalGradient(0f to Color.Transparent, 0.32f to Color.Black, 0.68f to Color.Black, 1f to Color.Transparent)

/**
 * A drum keeps its scroll to itself (the prototype's `overscroll-behavior: contain`): what it can't
 * use at either end doesn't scroll the dialog and doesn't stretch.
 */
private val KeepScrollInside = object : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) = available

    override suspend fun onPostFling(consumed: Velocity, available: Velocity) = available
}

/**
 * A time wheel (README › Screens › 6): hour `12, 1…11`, minute `00 15 30 45` and `am/pm` drums
 * with a `:`, in a bordered box with a `surface` band behind the middle row. [minutes] is minutes
 * from midnight; 1440 shows `12:00 am`. Scrolling or tapping a value reports the new time at once,
 * as the prototype's `wScroll`/`wSet` do; [onChange] gets minutes from midnight (0 for `12:00 am`).
 */
@Composable
fun WheelPicker(minutes: Int, onChange: (Int) -> Unit, label: String, modifier: Modifier = Modifier) {
    val c = RmTheme.colors
    val time = BlockRules.wheelTime(minutes)
    Box(
        modifier
            .height(PICKER_HEIGHT)
            .border(1.dp, c.outline, RmShapes.field),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(start = BAND_INSET, end = BAND_INSET, top = 1.dp + ROW, bottom = 1.dp + ROW)
                .background(c.surface, RmShapes.wheelBand)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            Drum(HOURS, time.hour, { onChange(BlockRules.minutes(time.copy(hour = it))) }, "$label hour")
            // The prototype lifts it 1 dp (margin-top: -2px in a centred row).
            Text(":", Modifier.offset(y = (-1).dp), color = c.textPrimary, style = RmType.wheelColon)
            Drum(QUARTERS, time.quarter, { onChange(BlockRules.minutes(time.copy(quarter = it))) }, "$label minute")
            Drum(HALVES, if (time.pm) 1 else 0, { onChange(BlockRules.minutes(time.copy(pm = it == 1))) }, "$label am or pm")
        }
    }
}

/**
 * One drum: a snapping list of [values] with [selected] on the band. The scroll position is read
 * only through a derived index that changes when the band passes half a row, so scrolling
 * recomposes the drum once per value, not every frame (ARCHITECTURE §3).
 */
@Composable
private fun Drum(values: List<String>, selected: Int, onSelect: (Int) -> Unit, description: String) {
    val c = RmTheme.colors
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selected)
    val rowPx = with(LocalDensity.current) { ROW.toPx() }
    // The rows are all one height, so the value on the band is the scroll position in rows, rounded.
    val onBand = remember(state, rowPx, values.size) {
        derivedStateOf {
            val next = if (state.firstVisibleItemScrollOffset * 2 >= rowPx) 1 else 0
            (state.firstVisibleItemIndex + next).coerceAtMost(values.lastIndex)
        }
    }
    val latestSelected by rememberUpdatedState(selected)
    val latestOnSelect by rememberUpdatedState(onSelect)
    LaunchedEffect(state) {
        // The value passing the band is picked while scrolling, as in the prototype.
        snapshotFlow { onBand.value }.collect { if (it != latestSelected) latestOnSelect(it) }
    }
    LaunchedEffect(selected) {
        // A value set from outside (a tap, or Start pushing End) moves the drum there.
        if (onBand.value != selected && !state.isScrollInProgress) state.scrollToItem(selected)
    }
    LazyColumn(
        Modifier
            .width(DRUM_WIDTH)
            .height(ROW * 3)
            .semantics { contentDescription = description }
            .nestedScroll(KeepScrollInside)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                drawRect(FADE, blendMode = BlendMode.DstIn)
            },
        state = state,
        // A row above the first value and below the last, so each can sit on the band.
        contentPadding = PaddingValues(vertical = ROW),
        flingBehavior = rememberSnapFlingBehavior(state, SnapPosition.Center),
    ) {
        itemsIndexed(values) { i, value ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(ROW)
                    .clickable(interactionSource = null, indication = null) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    value,
                    color = if (i == selected) c.textPrimary else c.textHint,
                    style = if (i == selected) RmType.wheelSelected else RmType.wheel,
                )
            }
        }
    }
}
