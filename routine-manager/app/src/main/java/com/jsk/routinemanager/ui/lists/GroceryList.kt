package com.jsk.routinemanager.ui.lists

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jsk.routinemanager.R
import com.jsk.routinemanager.data.GroceryCategory
import com.jsk.routinemanager.data.GroceryItem
import com.jsk.routinemanager.domain.GroceryOps
import com.jsk.routinemanager.ui.components.CountBadge
import com.jsk.routinemanager.ui.components.RadioDot
import com.jsk.routinemanager.ui.components.bleed
import com.jsk.routinemanager.ui.theme.RmShapes
import com.jsk.routinemanager.ui.theme.RmTheme
import com.jsk.routinemanager.ui.theme.RmType

/** CSS `ease`, the prototype's chevron transition. */
private val CssEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

/**
 * The Grocery list in view mode (README › Grocery): categories 14 dp apart, each a header that
 * collapses it and its items, which toggle "need to buy".
 */
@Composable
fun GroceryList(
    categories: List<GroceryCategory>,
    onToggleItem: (categoryId: String, itemId: String) -> Unit,
    onToggleCollapsed: (categoryId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        categories.forEach { category ->
            key(category.id) {
                Column {
                    CategoryHeader(category, onClick = { onToggleCollapsed(category.id) })
                    if (!category.collapsed) {
                        category.items.forEach { item ->
                            key(item.id) { ItemRow(item, onClick = { onToggleItem(category.id, item.id) }) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 48 dp: chevron (turned −90° when collapsed, 180 ms), name, and the category's selected count.
 * The pressed tint reaches 10 dp past the text on both sides, as in the prototype; it is
 * `surface`, the README value of the prototype's light hover tint.
 */
@Composable
private fun CategoryHeader(category: GroceryCategory, onClick: () -> Unit) {
    val c = RmTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val turn by animateFloatAsState(
        if (category.collapsed) -90f else 0f,
        tween(durationMillis = 180, easing = CssEase),
        label = "chevron",
    )
    Row(
        Modifier
            .bleed(10.dp)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (pressed) Modifier.background(c.surface, RmShapes.categoryHeader) else Modifier)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(R.drawable.ic_expand_more),
            if (category.collapsed) "Expand" else "Collapse",
            Modifier
                .size(22.dp)
                // Read in the layer, so the turn redraws the icon without recomposing the row.
                .graphicsLayer { rotationZ = turn },
        )
        Text(category.name, Modifier.weight(1f), style = RmType.groceryCategory)
        CountBadge(GroceryOps.selectedCount(category))
    }
}

/** Indented 30 dp: radio and text; selected = filled dot and bold text. */
@Composable
private fun ItemRow(item: GroceryItem, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .toggleable(item.on, interactionSource = null, indication = null, role = Role.Checkbox) { onClick() }
            .padding(start = 30.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioDot(item.on)
        Text(item.text, style = if (item.on) RmType.bodySelected else RmType.body)
    }
}
