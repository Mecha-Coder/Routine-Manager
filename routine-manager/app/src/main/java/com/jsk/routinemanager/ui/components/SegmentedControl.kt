package com.jsk.routinemanager.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jsk.routinemanager.ui.theme.RmShapes
import com.jsk.routinemanager.ui.theme.RmTheme
import com.jsk.routinemanager.ui.theme.RmType

/** One option of a [SegmentedControl]: an icon and a label. */
data class Segment(@param:DrawableRes val icon: Int, val label: String)

/**
 * A 40 dp pill with a 1 dp `outlineStrong` border and dividers; the selected option is filled with
 * `textPrimary` (README › Screens › 4, the Theme control).
 */
@Composable
fun SegmentedControl(
    segments: List<Segment>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = RmTheme.colors
    Row(
        modifier
            .height(40.dp)
            .border(1.dp, c.outlineStrong, RmShapes.pill)
            .padding(1.dp)
            .clip(RmShapes.pill)
            .selectableGroup()
    ) {
        segments.forEachIndexed { i, segment ->
            if (i > 0) {
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(c.outlineStrong)
                )
            }
            val isSelected = i == selected
            val content = if (isSelected) c.onPrimary else c.textPrimary
            Row(
                Modifier
                    .fillMaxHeight()
                    .then(if (isSelected) Modifier.background(c.textPrimary) else Modifier)
                    .selectable(isSelected, interactionSource = null, indication = null, role = Role.RadioButton) { onSelect(i) }
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(segment.icon), null, Modifier.size(18.dp), tint = content)
                Text(segment.label, color = content, style = RmType.control)
            }
        }
    }
}
