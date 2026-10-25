package com.jsk.routinemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jsk.routinemanager.ui.theme.RmTheme

/** A 20 dp ring with a 2 dp stroke and, when [selected], a 10 dp inner dot (README › Grocery). */
@Composable
fun RadioDot(selected: Boolean, modifier: Modifier = Modifier) {
    val ink = RmTheme.colors.textPrimary
    Box(
        modifier
            .size(20.dp)
            .border(2.dp, ink, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(ink, CircleShape)
            )
        }
    }
}
