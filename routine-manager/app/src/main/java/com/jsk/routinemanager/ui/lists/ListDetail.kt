package com.jsk.routinemanager.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jsk.routinemanager.R
import com.jsk.routinemanager.data.AppData
import com.jsk.routinemanager.ui.components.RmIconButton
import com.jsk.routinemanager.ui.theme.RmTheme
import com.jsk.routinemanager.ui.theme.RmType

/**
 * One list in the list panel (README › Screens › 3), in view mode: header with back, the list's
 * name and Edit, then the list, scrolling.
 *
 * @param data null until the saved data is loaded; only the header shows until then.
 * @param notchSides the panel edges that touch the screen edge, where the notch may be (SPEC D18).
 */
@Composable
fun ListDetail(
    list: ListKey,
    data: AppData?,
    onBack: () -> Unit,
    onToggleDone: (itemId: String) -> Unit,
    onToggleGroceryItem: (categoryId: String, itemId: String) -> Unit,
    onToggleCollapsed: (categoryId: String) -> Unit,
    notchSides: WindowInsetsSides,
    modifier: Modifier = Modifier,
) {
    val notch = WindowInsets.displayCutout.only(notchSides)
    Column(modifier) {
        Header(
            list.label,
            onBack,
            Modifier.windowInsetsPadding(notch.union(WindowInsets(left = 14.dp, top = 16.dp, right = 14.dp, bottom = 6.dp))),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(
                    notch.only(WindowInsetsSides.Horizontal)
                        .union(WindowInsets(left = 30.dp, top = 4.dp, right = 26.dp, bottom = 24.dp))
                ),
        ) {
            if (data == null) return@Column
            when (list) {
                ListKey.GROCERY -> {
                    GroceryList(data.grocery, onToggleGroceryItem, onToggleCollapsed)
                    // As in the prototype: categories without items still show, above this line.
                    if (data.grocery.all { it.items.isEmpty() }) EmptyState()
                }
                ListKey.TODO, ListKey.SELF, ListKey.ROOM -> {
                    val items = when (list) {
                        ListKey.TODO -> data.todo
                        ListKey.SELF -> data.self
                        else -> data.room
                    }
                    // Only To-Do items can be ticked; tapping a Self or Room item does nothing.
                    OutlineList(items, onToggleDone.takeIf { list == ListKey.TODO })
                    if (items.isEmpty()) EmptyState()
                }
            }
        }
    }
}

/** Back on the left, the name centred, Edit on the right (the prototype's `1fr auto 1fr` grid). */
@Composable
private fun Header(title: String, onBack: () -> Unit, modifier: Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            RmIconButton(R.drawable.ic_arrow_back, "Back", onBack)
        }
        Text(title, style = RmType.listTitle, maxLines = 1, softWrap = false)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            // Edit mode comes in M6.
            RmIconButton(R.drawable.ic_edit, "Edit list", onClick = {}, iconSize = 22.dp)
        }
    }
}

/** "Nothing here. Tap ✎ to add.", where ✎ is the `edit` icon. */
@Composable
private fun EmptyState() {
    val c = RmTheme.colors
    Row(
        Modifier.padding(top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Nothing here. Tap", color = c.textSecondary, style = RmType.emptyState)
        Icon(painterResource(R.drawable.ic_edit), "edit", Modifier.size(18.dp), tint = c.textSecondary)
        Text("to add.", color = c.textSecondary, style = RmType.emptyState)
    }
}
