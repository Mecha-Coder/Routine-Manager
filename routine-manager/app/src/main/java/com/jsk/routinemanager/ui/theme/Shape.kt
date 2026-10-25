package com.jsk.routinemanager.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** The README radii (README › Shape, spacing and elevation). */
object RmShapes {
    private val groupOuter = 24.dp
    private val groupInner = 6.dp

    /**
     * A row of the list-menu card: rows have 6 dp corners, but the card's outer corners are 24,
     * so the rows with 2 dp gaps read as one card.
     */
    fun listMenuRow(first: Boolean, last: Boolean): Shape = RoundedCornerShape(
        topStart = if (first) groupOuter else groupInner,
        topEnd = if (first) groupOuter else groupInner,
        bottomEnd = if (last) groupOuter else groupInner,
        bottomStart = if (last) groupOuter else groupInner,
    )

    val settingsRow = RoundedCornerShape(24.dp)
    val dialog = RoundedCornerShape(28.dp)

    /** Text fields and wheel pickers. */
    val field = RoundedCornerShape(12.dp)
    val dragGhost = RoundedCornerShape(14.dp)
    val timetableBlock = RoundedCornerShape(8.dp)

    /** Pills and badges: fully rounded. */
    val pill = CircleShape
}
