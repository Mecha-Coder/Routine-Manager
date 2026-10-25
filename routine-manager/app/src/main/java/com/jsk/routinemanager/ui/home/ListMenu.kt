package com.jsk.routinemanager.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jsk.routinemanager.R
import com.jsk.routinemanager.ui.components.RmIconButton
import com.jsk.routinemanager.ui.theme.RmType

/**
 * The list panel (README › Screens › 1, right panel). For now only its header: "Lists" and the
 * Settings gear; the four list rows come in M5.
 *
 * @param notchSides the panel edges that touch the screen edge, where the notch may be (SPEC D18).
 * @param onDebug opens the debug screen on a long press of the gear; null in release (SPEC §3.6).
 */
@Composable
fun ListMenu(
    onSettings: () -> Unit,
    onDebug: (() -> Unit)?,
    notchSides: WindowInsetsSides,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.displayCutout.only(notchSides)
                        .union(WindowInsets(left = 30.dp, top = 18.dp, right = 14.dp, bottom = 6.dp))
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Lists", style = RmType.screenTitle)
            RmIconButton(R.drawable.ic_settings, "Settings", onClick = onSettings, onLongClick = onDebug)
        }
    }
}
