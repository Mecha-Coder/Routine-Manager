package com.jsk.routinemanager.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jsk.routinemanager.R
import com.jsk.routinemanager.ui.MenuBadges
import com.jsk.routinemanager.ui.components.CountBadge
import com.jsk.routinemanager.ui.components.RmIconButton
import com.jsk.routinemanager.ui.lists.ListKey
import com.jsk.routinemanager.ui.theme.RmShapes
import com.jsk.routinemanager.ui.theme.RmTheme
import com.jsk.routinemanager.ui.theme.RmType

/**
 * The list panel's menu (README › Screens › 1, right panel): "Lists", the Settings gear, and one
 * card of four rows with To-Do and Grocery badges.
 *
 * @param badges null until the saved data is loaded; the rows show no badge until then.
 * @param notchSides the panel edges that touch the screen edge, where the notch may be (SPEC D18).
 * @param onDebug opens the debug screen on a long press of the gear; null in release (SPEC §3.6).
 */
@Composable
fun ListMenu(
    badges: MenuBadges?,
    onOpen: (ListKey) -> Unit,
    onSettings: () -> Unit,
    onDebug: (() -> Unit)?,
    notchSides: WindowInsetsSides,
    modifier: Modifier = Modifier,
) {
    val notch = WindowInsets.displayCutout.only(notchSides)
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(notch.union(WindowInsets(left = 30.dp, top = 18.dp, right = 14.dp, bottom = 6.dp))),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Lists", style = RmType.screenTitle)
            RmIconButton(R.drawable.ic_settings, "Settings", onClick = onSettings, onLongClick = onDebug)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(
                    notch.only(WindowInsetsSides.Horizontal)
                        .union(WindowInsets(left = 24.dp, top = 8.dp, right = 22.dp, bottom = 24.dp))
                ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val lists = ListKey.entries
            lists.forEachIndexed { i, list ->
                MenuRow(
                    list,
                    count = badges?.let {
                        when (list) {
                            ListKey.TODO -> it.todo
                            ListKey.GROCERY -> it.grocery
                            ListKey.SELF, ListKey.ROOM -> null
                        }
                    },
                    shape = RmShapes.listMenuRow(first = i == 0, last = i == lists.lastIndex),
                    onClick = { onOpen(list) },
                )
            }
        }
    }
}

/** A 60 dp row: icon, label and, for To-Do and Grocery, a count badge. Pressed = `surfacePressed`. */
@Composable
private fun MenuRow(list: ListKey, count: Int?, shape: Shape, onClick: () -> Unit) {
    val c = RmTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(if (pressed) c.surfacePressed else c.surface, shape)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(start = 18.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(list.icon), null, Modifier.size(24.dp))
        Text(list.label, Modifier.weight(1f), style = RmType.menuLabel)
        if (count != null) CountBadge(count)
    }
}
