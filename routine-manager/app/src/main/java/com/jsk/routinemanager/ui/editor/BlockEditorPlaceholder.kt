package com.jsk.routinemanager.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.jsk.routinemanager.R
import com.jsk.routinemanager.data.Block
import com.jsk.routinemanager.domain.TimeFormat
import com.jsk.routinemanager.domain.TimeRange
import com.jsk.routinemanager.ui.components.RmIconButton
import com.jsk.routinemanager.ui.theme.RmShapes
import com.jsk.routinemanager.ui.theme.RmTheme
import com.jsk.routinemanager.ui.theme.RmType

/** What the block editor opens with (README › Screens › 5): a range drawn on one day, or a tapped block. */
sealed interface EditorRequest {
    data class New(val day: Int, val range: TimeRange) : EditorRequest

    data class Edit(val blockId: String) : EditorRequest
}

/**
 * Placeholder until M10 builds the block editor dialog (README › Screens › 6). It shows what the
 * editor would open with; Back or ✕ closes it. It is drawn in the activity's window, not as a
 * Dialog, so at Bedtime the first touch on it still only brightens the screen (M4 notes).
 */
@Composable
fun BlockEditorPlaceholder(request: EditorRequest, blocks: List<Block>, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val c = RmTheme.colors
    val heading: String
    val detail: String
    when (request) {
        is EditorRequest.New -> {
            heading = "New block"
            detail = "${TimeFormat.day(request.day)}, ${TimeFormat.ghostRange(request.range.start, request.range.end)}"
        }
        is EditorRequest.Edit -> {
            heading = "Edit block"
            detail = blocks.firstOrNull { it.id == request.blockId }?.let { block ->
                val days = blocks.filter { it.groupId == block.groupId }.map { it.day }.sorted()
                "${block.title}: ${days.joinToString(", ", transform = TimeFormat::day)}, " +
                    TimeFormat.ghostRange(block.start, block.end)
            }.orEmpty()
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(c.scrim)
            // Takes every touch, so nothing under the scrim reacts.
            .pointerInput(Unit) { detectTapGestures {} }
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .background(c.bg, RmShapes.dialog)
                .border(1.dp, c.divider, RmShapes.dialog)
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(heading, Modifier.weight(1f), style = RmType.dialogTitle)
                RmIconButton(
                    R.drawable.ic_close,
                    "Close",
                    onClose,
                    Modifier.border(1.dp, c.textPrimary, RmShapes.pill),
                    size = 36.dp,
                    iconSize = 20.dp,
                )
            }
            Text(detail, Modifier.padding(top = 12.dp), style = RmType.body)
            Text("The block editor comes in M10.", Modifier.padding(top = 8.dp), color = c.textSecondary, style = RmType.body)
        }
    }
}
