package com.jsk.routinemanager.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.jsk.routinemanager.R
import com.jsk.routinemanager.data.OutlineItem
import com.jsk.routinemanager.domain.Numbering
import com.jsk.routinemanager.ui.components.AddButton
import com.jsk.routinemanager.ui.components.RmIconButton
import com.jsk.routinemanager.ui.components.UnderlinedField
import com.jsk.routinemanager.ui.components.bleed
import com.jsk.routinemanager.ui.theme.RmTheme
import com.jsk.routinemanager.ui.theme.RmType

/** README › Maximum depth is 1: children are indented one level. */
private val INDENT = 24.dp

/**
 * A To-Do, Self or Room list in view mode (README › numbered outline lists): automatic numbers,
 * children indented 24 dp.
 *
 * @param onToggle ticks or unticks an item (To-Do only); null makes the rows inert (Self, Room).
 */
@Composable
fun OutlineList(items: List<OutlineItem>, onToggle: ((itemId: String) -> Unit)?, modifier: Modifier = Modifier) {
    val numbers = Numbering.labels(items)
    Column(modifier) {
        items.forEachIndexed { i, item ->
            key(item.id) { OutlineRow(item, numbers[i], onToggle) }
        }
    }
}

/**
 * Number, then text that wraps. A done item is struck through in `textDone` and stays in place.
 * No pressed tint (the prototype has none); the strike-through is the feedback.
 */
@Composable
private fun OutlineRow(item: OutlineItem, number: String, onToggle: ((String) -> Unit)?) {
    val c = RmTheme.colors
    val tap = if (onToggle == null) {
        Modifier
    } else {
        Modifier.toggleable(item.done, interactionSource = null, indication = null, role = Role.Checkbox) {
            onToggle(item.id)
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .then(tap)
            .padding(start = INDENT * item.depth + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Number(number, item.depth)
        Text(
            item.text,
            Modifier
                .weight(1f)
                .padding(vertical = 9.dp),
            color = if (item.done) c.textDone else c.textPrimary,
            style = RmType.body,
            textDecoration = if (item.done) TextDecoration.LineThrough else null,
        )
    }
}

/** The prototype's min-width (24, or 38 for "2.1)") includes the 8 dp gap. */
@Composable
private fun Number(number: String, depth: Int) {
    Text(
        number,
        Modifier
            .widthIn(min = if (depth == 0) 24.dp else 38.dp)
            .padding(end = 8.dp),
        color = RmTheme.colors.textSecondary,
        style = RmType.body,
    )
}

/**
 * A To-Do, Self or Room list in edit mode (README › Edit mode): each row has the drag handle,
 * number, a text field and ✕; "+ Add task" (To-Do) or "+ Add item" (Self, Room) appends an item.
 * The handle indents and reorders ([OutlineDragState]).
 *
 * The functions that add an item return its id, so its field can take the focus.
 *
 * @param childNoun "sub-task" or "sub-item", for the drag ghost's "+2 sub-tasks".
 * @param onIndent an indent drag released at a new depth.
 * @param onMove a reorder dropped at an index in the list without the item and its children.
 */
@Composable
internal fun OutlineEditList(
    items: List<OutlineItem>,
    placeholder: String,
    addLabel: String,
    childNoun: String,
    onTextChange: (itemId: String, text: String) -> Unit,
    onInsertAfter: (itemId: String) -> String,
    onAppend: () -> String,
    onDelete: (itemId: String) -> Unit,
    onIndent: (itemId: String, depth: Int) -> Unit,
    onMove: (itemId: String, index: Int, depth: Int) -> Unit,
    drag: OutlineDragState,
    modifier: Modifier = Modifier,
) {
    val shown = drag.shown(items)
    val numbers = Numbering.labels(shown)
    SideEffect { drag.list = DragList(items, shown, placeholder, childNoun, onIndent, onMove) }
    // The item whose field should take the focus: the one just added (the prototype's focusId).
    var focusId by remember { mutableStateOf<String?>(null) }
    Column(modifier) {
        shown.forEachIndexed { i, item ->
            key(item.id) {
                OutlineEditRow(
                    item,
                    numbers[i],
                    placeholder,
                    drag,
                    autoFocus = item.id == focusId,
                    onTextChange = { onTextChange(item.id, it) },
                    onEnter = { focusId = onInsertAfter(item.id) },
                    onDelete = { onDelete(item.id) },
                )
            }
        }
        AddButton(
            addLabel,
            onClick = { focusId = onAppend() },
            Modifier
                .padding(top = 4.dp)
                .offset(x = (-12).dp),
        )
    }
}

/**
 * 44 dp: handle (28 dp, `outlineStrong`), number, field, and a 40 dp ✕. The row reaches 30 dp
 * left and 8 dp right into the list's padding, so the number sits where it does in view mode.
 * A done to-do stays struck through while it is edited, as in the prototype.
 */
@Composable
private fun OutlineEditRow(
    item: OutlineItem,
    number: String,
    placeholder: String,
    drag: OutlineDragState,
    autoFocus: Boolean,
    onTextChange: (String) -> Unit,
    onEnter: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = RmTheme.colors
    Row(
        Modifier
            .bleed(start = 30.dp, end = 8.dp)
            .outlineDragRow(drag, item.id)
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(start = INDENT * item.depth + 2.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DragHandle(drag, item.id)
        Number(number, item.depth)
        UnderlinedField(
            item.text,
            onTextChange,
            placeholder,
            Modifier.weight(1f),
            textDecoration = if (item.done) TextDecoration.LineThrough else null,
            autoFocus = autoFocus,
            onEnter = onEnter,
            onBackspaceWhenEmpty = onDelete,
        )
        RmIconButton(R.drawable.ic_close, "Delete", onDelete, size = 40.dp, iconSize = 20.dp, tint = c.textSecondary)
    }
}
