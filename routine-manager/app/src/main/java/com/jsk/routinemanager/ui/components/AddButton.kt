package com.jsk.routinemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jsk.routinemanager.R
import com.jsk.routinemanager.ui.theme.RmShapes
import com.jsk.routinemanager.ui.theme.RmTheme
import com.jsk.routinemanager.ui.theme.RmType

/**
 * "+ Add task", "+ Add item": a 40 dp text button with the `add` icon (README › Edit mode).
 * Pressed = `surface`, the README value of the prototype's light hover tint (as in M5).
 */
@Composable
fun AddButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier
            .height(40.dp)
            .then(if (pressed) Modifier.background(RmTheme.colors.surface, RmShapes.pill) else Modifier)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(start = 12.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_add), null, Modifier.size(20.dp))
        Text(label, style = RmType.button)
    }
}
