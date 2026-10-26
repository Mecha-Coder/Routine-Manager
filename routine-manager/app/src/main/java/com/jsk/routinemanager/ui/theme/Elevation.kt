package com.jsk.routinemanager.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/** README › Elevation: only the drag ghost and the light-mode dialog have a shadow. */
object RmElevation {
    /**
     * The drag ghost's `0 12 30 rgba(0,0,0,.18)`, in both palettes. A CSS blur of 30 is a radius of
     * 15 here: Chrome blurs a box shadow with sigma 0.289 × blur + 0.5, Android's blur filter (which
     * `dropShadow` uses) with sigma 0.577 × radius + 0.5.
     */
    val dragGhost = Shadow(radius = 15.dp, color = Color.Black, offset = DpOffset(0.dp, 12.dp), alpha = 0.18f)
}
