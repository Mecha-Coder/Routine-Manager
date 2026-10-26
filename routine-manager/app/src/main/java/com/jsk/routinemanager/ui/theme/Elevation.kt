package com.jsk.routinemanager.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * README › Shape, spacing and elevation: everything is flat except the drag ghost and the light-mode
 * dialog. A CSS blur of b is a radius of b / 2 here: Chrome blurs a box shadow with sigma
 * 0.289 × blur + 0.5, Android's blur filter (which `dropShadow` uses) with sigma 0.577 × radius + 0.5.
 */
object RmElevation {
    /** The drag ghost's `0 12 30 rgba(0,0,0,.18)`, in both palettes. */
    val dragGhost = Shadow(radius = 15.dp, color = Color.Black, offset = DpOffset(0.dp, 12.dp), alpha = 0.18f)

    private val lightDialog = Shadow(radius = 20.dp, color = Color.Black, offset = DpOffset(0.dp, 12.dp), alpha = 0.18f)

    /**
     * The light-mode dialog's shadow, `0 12 40 rgba(0,0,0,.18)`. Null in dark mode, where the
     * dialog has only its 1 dp `divider` border (README › Colour).
     */
    val dialog: Shadow?
        @Composable @ReadOnlyComposable get() = if (RmTheme.colors === DarkColors) null else lightDialog
}
