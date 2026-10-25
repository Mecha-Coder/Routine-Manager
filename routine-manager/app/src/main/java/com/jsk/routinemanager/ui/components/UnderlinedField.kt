package com.jsk.routinemanager.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.jsk.routinemanager.ui.theme.RmTheme
import com.jsk.routinemanager.ui.theme.RmType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

/**
 * A single-line text field with a 1 dp underline, `outlineSoft`, or `textPrimary` while focused
 * (README › Edit mode; the prototype's `padding: 9px 0` input).
 *
 * The field owns its text while it is shown: it starts from [initialText] and reports every change
 * through [onTextChange]; later changes to [initialText] are ignored. Typing never waits for the
 * saved data to come back, so the cursor can't jump and no key is lost.
 *
 * @param autoFocus focuses the field, and opens the keyboard, when it becomes true (a new item).
 * @param onEnter the keyboard's action key (SPEC §3.7); without it the key closes the keyboard.
 * @param onBackspaceWhenEmpty Backspace in an empty field, when the keyboard sends it as a key
 *   event (Gboard does, M6 notes); ✕ is the delete path that always works (SPEC §3.7).
 */
@Composable
fun UnderlinedField(
    initialText: String,
    onTextChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    style: TextStyle = RmType.field,
    textDecoration: TextDecoration? = null,
    autoFocus: Boolean = false,
    onEnter: (() -> Unit)? = null,
    onBackspaceWhenEmpty: (() -> Unit)? = null,
) {
    val c = RmTheme.colors
    val state = rememberTextFieldState(initialText)
    val latestOnTextChange by rememberUpdatedState(onTextChange)
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.drop(1).collect { latestOnTextChange(it) }
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(autoFocus) {
        if (autoFocus) focusRequester.requestFocus()
    }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    // The text field itself keeps only its cursor line above the keyboard, which leaves the
    // underline under it; this keeps the whole field in view while the keyboard opens.
    val bringIntoView = remember { BringIntoViewRequester() }
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    LaunchedEffect(focused) {
        if (focused) snapshotFlow { ime.getBottom(density) }.collectLatest { bringIntoView.bringIntoView() }
    }
    val line = if (focused) c.textPrimary else c.outlineSoft
    BasicTextField(
        state,
        modifier
            .focusRequester(focusRequester)
            .bringIntoViewRequester(bringIntoView)
            .onPreviewKeyEvent {
                val delete = onBackspaceWhenEmpty != null && state.text.isEmpty() &&
                    it.type == KeyEventType.KeyDown && it.key == Key.Backspace
                if (delete) onBackspaceWhenEmpty()
                delete
            }
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawRect(line, Offset(0f, size.height - stroke), Size(size.width, stroke))
            },
        textStyle = style.merge(color = c.textPrimary, textDecoration = textDecoration),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = if (onEnter != null) ImeAction.Next else ImeAction.Done,
        ),
        onKeyboardAction = onEnter?.let { enter -> KeyboardActionHandler { enter() } },
        lineLimits = TextFieldLineLimits.SingleLine,
        interactionSource = interaction,
        cursorBrush = SolidColor(c.textPrimary),
        decorator = { innerTextField ->
            // 9 dp above and below the text, then the 1 dp line.
            Box(Modifier.padding(top = 9.dp, bottom = 10.dp)) {
                if (state.text.isEmpty()) Text(placeholder, color = c.textHint, style = style)
                innerTextField()
            }
        },
    )
}
