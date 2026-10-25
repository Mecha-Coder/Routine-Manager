package com.jsk.routinemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jsk.routinemanager.ui.theme.RmShapes
import com.jsk.routinemanager.ui.theme.RmTheme
import com.jsk.routinemanager.ui.theme.RmType

/**
 * A 26 dp count pill (README › List menu): accent with `onAccent` text when [count] > 0, so
 * something to do stands out; `badgeIdle` with secondary text at 0.
 */
@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    val c = RmTheme.colors
    val active = count > 0
    Box(
        modifier
            .height(26.dp)
            .widthIn(min = 26.dp)
            .background(if (active) c.accent else c.badgeIdle, RmShapes.pill)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(count.toString(), color = if (active) c.onAccent else c.textSecondary, style = RmType.badge)
    }
}
