package com.jsk.routinemanager.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.offset

/**
 * The prototype's negative horizontal margin: the element is [horizontal] wider on each side than
 * the space its parent gives it, reaching into the parent's padding (for example a pressed tint
 * that extends past the list's text column while the content stays aligned with it).
 */
fun Modifier.bleed(horizontal: Dp): Modifier = layout { measurable, constraints ->
    val extra = (horizontal * 2).roundToPx()
    val placeable = measurable.measure(constraints.offset(horizontal = extra))
    layout(placeable.width - extra, placeable.height) {
        placeable.place(-extra / 2, 0)
    }
}
