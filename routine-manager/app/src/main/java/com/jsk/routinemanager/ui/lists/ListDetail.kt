package com.jsk.routinemanager.ui.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import com.jsk.routinemanager.ui.MainViewModel
import com.jsk.routinemanager.ui.components.RmIconButton
import com.jsk.routinemanager.ui.theme.RmShapes
import com.jsk.routinemanager.ui.theme.RmTheme
import com.jsk.routinemanager.ui.theme.RmType

/**
 * One list in the list panel (README › Screens › 3): header with back, the list's name and Edit
 * (or Done in edit mode), then the list, scrolling. The scrolling part ends above the keyboard,
 * so the focused field can stay in view.
 *
 * @param data null until the saved data is loaded; only the header shows until then.
 * @param notchSides the panel edges that touch the screen edge, where the notch may be (SPEC D18).
 */
@Composable
fun ListDetail(
    list: ListKey,
    data: AppData?,
    editing: Boolean,
    viewModel: MainViewModel,
    notchSides: WindowInsetsSides,
    modifier: Modifier = Modifier,
) {
    val notch = WindowInsets.displayCutout.only(notchSides)
    Column(modifier) {
        Header(
            list.label,
            editing,
            onBack = viewModel::showMenu,
            onEdit = viewModel::startEditing,
            onDone = viewModel::finishEditing,
            Modifier.windowInsetsPadding(notch.union(WindowInsets(left = 14.dp, top = 16.dp, right = 14.dp, bottom = 6.dp))),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(
                    notch.only(WindowInsetsSides.Horizontal)
                        .union(WindowInsets(left = 30.dp, top = 4.dp, right = 26.dp, bottom = 24.dp))
                ),
        ) {
            if (data == null) return@Column
            when (list) {
                ListKey.GROCERY -> if (editing) {
                    GroceryEditList(
                        data.grocery,
                        onRename = viewModel::renameCategory,
                        onDeleteCategory = viewModel::deleteCategory,
                        onAddCategory = viewModel::addCategory,
                        onItemTextChange = viewModel::editGroceryItem,
                        onDeleteItem = viewModel::deleteGroceryItem,
                        onAddItem = viewModel::addGroceryItem,
                    )
                } else {
                    GroceryList(data.grocery, viewModel::toggleGroceryItem, viewModel::toggleCollapsed)
                    // As in the prototype: categories without items still show, above this line.
                    if (data.grocery.all { it.items.isEmpty() }) EmptyState()
                }
                ListKey.TODO, ListKey.SELF, ListKey.ROOM -> {
                    val items = when (list) {
                        ListKey.TODO -> data.todo
                        ListKey.SELF -> data.self
                        else -> data.room
                    }
                    if (editing) {
                        val todo = list == ListKey.TODO
                        OutlineEditList(
                            items,
                            placeholder = if (todo) "New task" else "New item",
                            addLabel = if (todo) "Add task" else "Add item",
                            onTextChange = { id, text -> viewModel.editOutlineText(list, id, text) },
                            onInsertAfter = { id -> viewModel.insertOutlineItemAfter(list, id) },
                            onAppend = { viewModel.appendOutlineItem(list) },
                            onDelete = { id -> viewModel.deleteOutlineItem(list, id) },
                        )
                    } else {
                        // Only To-Do items can be ticked; tapping a Self or Room item does nothing.
                        OutlineList(items, viewModel::toggleDone.takeIf { list == ListKey.TODO })
                        if (items.isEmpty()) EmptyState()
                    }
                }
            }
        }
    }
}

/**
 * Back on the left, the name centred, and Edit, or in edit mode Done (a 40 dp `textPrimary` circle
 * with a `check`), on the right (the prototype's `1fr auto 1fr` grid).
 */
@Composable
private fun Header(
    title: String,
    editing: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier,
) {
    val c = RmTheme.colors
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
            if (editing) {
                RmIconButton(
                    R.drawable.ic_check,
                    "Done editing",
                    onDone,
                    Modifier
                        .padding(4.dp)
                        .background(c.textPrimary, RmShapes.pill),
                    size = 40.dp,
                    iconSize = 22.dp,
                    tint = c.onPrimary,
                )
            } else {
                RmIconButton(R.drawable.ic_edit, "Edit list", onEdit, iconSize = 22.dp)
            }
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
