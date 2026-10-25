package com.jsk.routinemanager.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.offset

/**
 * The prototype's negative horizontal margin: the element is [start] wider on the left and [end]
 * wider on the right than the space its parent gives it, reaching into the parent's padding (for
 * example a pressed tint that extends past the list's text column while the content stays aligned
 * with it).
 */
fun Modifier.bleed(start: Dp, end: Dp = start): Modifier = layout { measurable, constraints ->
    val left = start.roundToPx()
    val extra = left + end.roundToPx()
    val placeable = measurable.measure(constraints.offset(horizontal = extra))
    layout(placeable.width - extra, placeable.height) {
        placeable.place(-left, 0)
    }
}
