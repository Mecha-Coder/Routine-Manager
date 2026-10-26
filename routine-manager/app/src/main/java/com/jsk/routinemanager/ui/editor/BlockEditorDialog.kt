package com.jsk.routinemanager.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.jsk.routinemanager.R
import com.jsk.routinemanager.data.Block
import com.jsk.routinemanager.data.newId
import com.jsk.routinemanager.domain.BlockDraft
import com.jsk.routinemanager.domain.BlockRules
import com.jsk.routinemanager.domain.TimeRange
import com.jsk.routinemanager.ui.components.RmIconButton
import com.jsk.routinemanager.ui.components.UnderlinedField
import com.jsk.routinemanager.ui.theme.RmElevation
import com.jsk.routinemanager.ui.theme.RmShapes
import com.jsk.routinemanager.ui.theme.RmTheme
import com.jsk.routinemanager.ui.theme.RmType
import kotlinx.coroutines.flow.drop

/** What the block editor opens with (README › Screens › 5): a range drawn on one day, or a tapped block. */
sealed interface EditorRequest {
    data class New(val day: Int, val range: TimeRange) : EditorRequest

    data class Edit(val blockId: String) : EditorRequest
}

/** A sub-routine field. The id keeps each field's text and focus on its own row as rows come and go. */
private data class SubField(val id: String, val text: String)

private val DAY_NAMES = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

/** The prototype's sub-routine input: 36 dp with its 1 dp underline, the 15 sp text centred. */
private val SUB_ROUTINE_TEXT_PADDING = PaddingValues(top = 8.1.dp, bottom = 9.1.dp)

/**
 * The block editor (README › Screens › 6): title, days, Start and End wheels, up to 5
 * sub-routines, the first validation error, Delete (editing only) and Save. ✕ and Back close it
 * without saving. Saving or deleting a multi-day block acts on its whole group.
 *
 * The unsaved values live here while it is open, so rotation keeps them, as the activity is not
 * recreated (SPEC §3.7). It is drawn in the activity's window, not as a Dialog, so at Bedtime the
 * first touch on it still only brightens the screen (M4 notes); its scrim takes every touch.
 *
 * @param onSave saves the draft, or returns why it can't be saved.
 */
@Composable
fun BlockEditorDialog(
    request: EditorRequest,
    blocks: List<Block>,
    onSave: (BlockDraft) -> String?,
    onDelete: (groupId: String) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val c = RmTheme.colors
    // Title, days and times. The sub-routines are edited in `subs`, which gives each field an id.
    var draft by remember {
        mutableStateOf(
            when (request) {
                is EditorRequest.New -> BlockRules.newDraft(request.day, request.range)
                is EditorRequest.Edit -> BlockRules.editDraft(blocks, request.blockId)
            }
        )
    }
    var subs by remember { mutableStateOf(draft.subs.map { SubField(newId(), it) }) }
    var error by remember { mutableStateOf<String?>(null) }
    // The sub-routine field just added, which takes the focus (the prototype's focusId).
    var focusSub by remember { mutableStateOf<String?>(null) }

    // A change to the title, days or times hides the error, as the prototype's `sh` does.
    fun change(new: BlockDraft) {
        draft = new
        error = null
    }

    fun addSub(after: String?) {
        if (subs.size >= BlockRules.MAX_SUBS) return
        val field = SubField(newId(), "")
        val at = if (after == null) subs.size else subs.indexOfFirst { it.id == after } + 1
        subs = subs.toMutableList().apply { add(at, field) }
        focusSub = field.id
    }

    fun editSub(id: String, text: String) {
        subs = subs.map { if (it.id == id) it.copy(text = text) else it }
    }

    fun deleteSub(id: String) {
        subs = subs.filter { it.id != id }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.scrim)
            // Takes every touch, so nothing under the scrim reacts. A tap on it does nothing.
            .pointerInput(Unit) { detectTapGestures {} }
            // Above the keyboard, so the card shrinks and scrolls (Save stays reachable); 16 dp from
            // the screen's edges, or the notch's depth on its side (SPEC D18).
            .windowInsetsPadding(WindowInsets.ime)
            .windowInsetsPadding(WindowInsets.displayCutout.union(WindowInsets(16.dp, 16.dp, 16.dp, 16.dp))),
        contentAlignment = Alignment.Center,
    ) {
        val shadow = RmElevation.dialog
        Column(
            Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .then(if (shadow != null) Modifier.dropShadow(RmShapes.dialog, shadow) else Modifier)
                .clip(RmShapes.dialog)
                .background(c.bg)
                .border(1.dp, c.divider, RmShapes.dialog)
                .verticalScroll(rememberScrollState())
                // 16 / 20 dp inside the 1 dp border.
                .padding(horizontal = 21.dp, vertical = 17.dp)
        ) {
            Row(Modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (draft.groupId == null) "New block" else "Edit block", Modifier.weight(1f), style = RmType.dialogTitle)
                RmIconButton(
                    R.drawable.ic_close,
                    "Close",
                    onClose,
                    Modifier.border(1.dp, c.textPrimary, RmShapes.pill),
                    size = 36.dp,
                    iconSize = 20.dp,
                )
            }
            EditorColumns(
                left = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Labelled("Title") { TitleField(draft.title, onTextChange = { change(draft.copy(title = it)) }) }
                        Labelled("Days") { DayToggles(draft.days, onToggle = { change(BlockRules.toggleDay(draft, it)) }) }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Labelled("Start", Modifier.weight(1f)) {
                                WheelPicker(draft.start, onChange = { change(BlockRules.withStart(draft, it)) }, "Start")
                            }
                            Labelled("End", Modifier.weight(1f)) {
                                WheelPicker(draft.end, onChange = { change(BlockRules.withEnd(draft, it)) }, "End")
                            }
                        }
                    }
                },
                right = {
                    // Fixed height, so the footer doesn't move as sub-routines come and go (README).
                    Column(Modifier.height(206.dp)) {
                        Text("Sub-routines", Modifier.height(20.dp), color = c.textSecondary, style = RmType.fieldLabel)
                        subs.forEach { sub ->
                            key(sub.id) {
                                SubRoutineRow(
                                    sub,
                                    autoFocus = sub.id == focusSub,
                                    onTextChange = { editSub(sub.id, it) },
                                    onEnter = { addSub(sub.id) },
                                    onDelete = { deleteSub(sub.id) },
                                )
                            }
                        }
                        // Hidden at 5, back below 5 (README).
                        if (subs.size < BlockRules.MAX_SUBS) AddSubRoutine(onClick = { addSub(null) })
                    }
                },
            )
            error?.let { message ->
                Row(
                    Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_error), null, Modifier.size(18.dp), tint = c.accent)
                    Text(message, style = RmType.error)
                }
            }
            Row(
                Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                draft.groupId?.let { groupId ->
                    DeleteButton(onClick = {
                        onDelete(groupId)
                        onClose()
                    })
                }
                Spacer(Modifier.weight(1f))
                SaveButton(onClick = {
                    val message = onSave(draft.copy(subs = subs.map { it.text }))
                    if (message == null) onClose() else error = message
                })
            }
        }
    }
}

/**
 * The prototype's `grid-template-columns: repeat(auto-fit, minmax(220px, 1fr))` with 14 / 22 dp
 * gaps: side by side when both columns get 220 dp (landscape), stacked otherwise (portrait). One
 * layout for both, so rotating keeps the fields' text, focus and wheel positions.
 */
@Composable
private fun EditorColumns(left: @Composable () -> Unit, right: @Composable () -> Unit) {
    Layout(contents = listOf(left, right)) { (leftColumn, rightColumn), constraints ->
        val columnGap = 22.dp.roundToPx()
        val width = constraints.maxWidth
        if (width >= 2 * 220.dp.roundToPx() + columnGap) {
            val columnWidth = Constraints.fixedWidth((width - columnGap) / 2)
            val l = leftColumn.single().measure(columnWidth)
            val r = rightColumn.single().measure(columnWidth)
            layout(width, maxOf(l.height, r.height)) {
                l.place(0, 0)
                r.place(width - r.width, 0)
            }
        } else {
            val rowGap = 14.dp.roundToPx()
            val l = leftColumn.single().measure(Constraints.fixedWidth(width))
            val r = rightColumn.single().measure(Constraints.fixedWidth(width))
            layout(width, l.height + rowGap + r.height) {
                l.place(0, 0)
                r.place(0, l.height + rowGap)
            }
        }
    }
}

/** A field's label above it ("Title", "Days", "Start", "End"). */
@Composable
private fun Labelled(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = RmTheme.colors.textSecondary, style = RmType.fieldLabel)
        content()
    }
}

/**
 * The title: a 42 dp box with a 1 dp `outline` border, 2 dp `textPrimary` while focused. Like
 * [UnderlinedField] it owns its text while shown and reports each change.
 */
@Composable
private fun TitleField(initialText: String, onTextChange: (String) -> Unit) {
    val c = RmTheme.colors
    val state = rememberTextFieldState(initialText)
    val latestOnTextChange by rememberUpdatedState(onTextChange)
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.drop(1).collect { latestOnTextChange(it) }
    }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    BasicTextField(
        state,
        Modifier
            .fillMaxWidth()
            .height(42.dp)
            .border(if (focused) 2.dp else 1.dp, if (focused) c.textPrimary else c.outline, RmShapes.field),
        textStyle = RmType.titleField.merge(color = c.textPrimary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        lineLimits = TextFieldLineLimits.SingleLine,
        interactionSource = interaction,
        cursorBrush = SolidColor(c.textPrimary),
        decorator = { innerTextField ->
            Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                if (state.text.isEmpty()) Text("e.g. Work Session1", color = c.textHint, style = RmType.titleField)
                innerTextField()
            }
        },
    )
}

/** M T W T F S S: 34 dp circles spread across the column; selected = `textPrimary` fill. */
@Composable
private fun DayToggles(days: List<Int>, onToggle: (Int) -> Unit) {
    val c = RmTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DAY_NAMES.forEachIndexed { day, name ->
            val on = day in days
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RmShapes.pill)
                    .then(if (on) Modifier.background(c.textPrimary) else Modifier)
                    .border(1.dp, c.outline, RmShapes.pill)
                    .toggleable(on, interactionSource = null, indication = null, role = Role.Checkbox) { onToggle(day) }
                    .semantics { contentDescription = name },
                contentAlignment = Alignment.Center,
            ) {
                Text(name.take(1), color = if (on) c.onPrimary else c.textPrimary, style = RmType.dayToggle)
            }
        }
    }
}

/** An underlined sub-routine field and its ✕. Enter adds a field below; Backspace on empty removes it. */
@Composable
private fun SubRoutineRow(
    sub: SubField,
    autoFocus: Boolean,
    onTextChange: (String) -> Unit,
    onEnter: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier.height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UnderlinedField(
            sub.text,
            onTextChange,
            placeholder = "Sub-routine",
            modifier = Modifier.weight(1f),
            style = RmType.subRoutineField,
            autoFocus = autoFocus,
            onEnter = onEnter,
            onBackspaceWhenEmpty = onDelete,
            textPadding = SUB_ROUTINE_TEXT_PADDING,
        )
        RmIconButton(R.drawable.ic_close, "Remove", onDelete, size = 34.dp, iconSize = 18.dp, tint = RmTheme.colors.textSecondary)
    }
}

/** "+ Add sub-routine": a plain text button, as in the prototype. */
@Composable
private fun AddSubRoutine(onClick: () -> Unit) {
    Row(
        Modifier
            .height(36.dp)
            .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_add), null, Modifier.size(19.dp))
        Text("Add sub-routine", style = RmType.buttonSmall)
    }
}

/** Delete: a text button with the `delete` icon; pressed = `surface`, as the lists' text buttons. */
@Composable
private fun DeleteButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        Modifier
            .height(40.dp)
            .clip(RmShapes.pill)
            .then(if (pressed) Modifier.background(RmTheme.colors.surface) else Modifier)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(start = 10.dp, end = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_delete), null, Modifier.size(19.dp))
        Text("Delete", style = RmType.buttonSmall)
    }
}

/** Save: a 40 dp `textPrimary` pill. */
@Composable
private fun SaveButton(onClick: () -> Unit) {
    val c = RmTheme.colors
    Box(
        Modifier
            .height(40.dp)
            .clip(RmShapes.pill)
            .background(c.textPrimary)
            .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Save", color = c.onPrimary, style = RmType.buttonSmall)
    }
}
