package com.jsk.routinemanager.ui.lists

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.jsk.routinemanager.ui.components.AddButton
import com.jsk.routinemanager.ui.components.CountBadge
import com.jsk.routinemanager.ui.components.RadioDot
import com.jsk.routinemanager.ui.components.RmIconButton
import com.jsk.routinemanager.ui.components.UnderlinedField
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

/**
 * The Grocery list in edit mode (README › Grocery › Edit mode): every category open, 20 dp apart,
 * each with its name field and `delete`, its items (radio, field, ✕) and "+ Add item"; then the
 * "+ Add category" pill. The radios show the selection but can't be changed here, as in the
 * prototype.
 *
 * The functions that add return the new id, so its field can take the focus.
 */
@Composable
fun GroceryEditList(
    categories: List<GroceryCategory>,
    onRename: (categoryId: String, name: String) -> Unit,
    onDeleteCategory: (categoryId: String) -> Unit,
    onAddCategory: () -> String,
    onItemTextChange: (categoryId: String, itemId: String, text: String) -> Unit,
    onDeleteItem: (categoryId: String, itemId: String) -> Unit,
    onAddItem: (categoryId: String, afterItemId: String?) -> String,
    modifier: Modifier = Modifier,
) {
    // The category name or item field that should take the focus: the one just added.
    var focusId by remember { mutableStateOf<String?>(null) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        categories.forEach { category ->
            key(category.id) {
                Column {
                    CategoryNameRow(
                        category.name,
                        autoFocus = category.id == focusId,
                        onRename = { onRename(category.id, it) },
                        onDelete = { onDeleteCategory(category.id) },
                    )
                    category.items.forEach { item ->
                        key(item.id) {
                            ItemEditRow(
                                item,
                                autoFocus = item.id == focusId,
                                onTextChange = { onItemTextChange(category.id, item.id, it) },
                                onEnter = { focusId = onAddItem(category.id, item.id) },
                                onDelete = { onDeleteItem(category.id, item.id) },
                            )
                        }
                    }
                    AddButton(
                        "Add item",
                        onClick = { focusId = onAddItem(category.id, null) },
                        Modifier
                            .padding(start = 30.dp, top = 4.dp)
                            .offset(x = (-12).dp),
                    )
                }
            }
        }
        AddCategoryPill(onClick = { focusId = onAddCategory() })
    }
}

/** Name field (17 / 600, like the header it edits) and a 40 dp `delete` for the whole category. */
@Composable
private fun CategoryNameRow(name: String, autoFocus: Boolean, onRename: (String) -> Unit, onDelete: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        UnderlinedField(
            name,
            onRename,
            placeholder = "Category",
            Modifier.weight(1f),
            style = RmType.groceryCategory,
            autoFocus = autoFocus,
        )
        RmIconButton(R.drawable.ic_delete, "Delete", onDelete, size = 40.dp, iconSize = 20.dp, tint = RmTheme.colors.textSecondary)
    }
}

/** 44 dp, indented 30: radio, field and ✕. */
@Composable
private fun ItemEditRow(
    item: GroceryItem,
    autoFocus: Boolean,
    onTextChange: (String) -> Unit,
    onEnter: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(start = 30.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioDot(item.on)
        UnderlinedField(
            item.text,
            onTextChange,
            placeholder = "New item",
            Modifier.weight(1f),
            autoFocus = autoFocus,
            onEnter = onEnter,
            onBackspaceWhenEmpty = onDelete,
        )
        RmIconButton(R.drawable.ic_close, "Delete", onDelete, size = 40.dp, iconSize = 20.dp, tint = RmTheme.colors.textSecondary)
    }
}

/** "+ Add category": a 44 dp full-width pill with a 1 dp `outlineStrong` border; pressed = `surface`. */
@Composable
private fun AddCategoryPill(onClick: () -> Unit) {
    val c = RmTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .then(if (pressed) Modifier.background(c.surface, RmShapes.pill) else Modifier)
            .border(1.dp, c.outlineStrong, RmShapes.pill)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_add), null, Modifier.size(20.dp))
        Text("Add category", style = RmType.button)
    }
}

