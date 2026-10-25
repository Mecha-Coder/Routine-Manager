package com.jsk.routinemanager.ui.lists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.jsk.routinemanager.data.OutlineItem
import com.jsk.routinemanager.domain.Numbering
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
        // The prototype's min-width (24, or 38 for "2.1)") includes the 8 dp gap.
        Text(
            number,
            Modifier
                .widthIn(min = if (item.depth == 0) 24.dp else 38.dp)
                .padding(end = 8.dp),
            color = c.textSecondary,
            style = RmType.body,
        )
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
