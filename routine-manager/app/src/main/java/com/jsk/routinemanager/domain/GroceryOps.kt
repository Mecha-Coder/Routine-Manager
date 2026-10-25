package com.jsk.routinemanager.domain

import com.jsk.routinemanager.data.GroceryCategory
import com.jsk.routinemanager.data.GroceryItem

/** Changes to the Grocery list. Selected ([com.jsk.routinemanager.data.GroceryItem.on]) = "need to buy". */
object GroceryOps {

    /** The name a category gets when it is left without one but still has items (README › Saving edits). */
    const val UNTITLED = "Untitled"

    /** The Grocery badge on the list menu: selected items in every category. */
    fun selectedCount(categories: List<GroceryCategory>): Int = categories.sumOf(::selectedCount)

    /** The badge on a category header. */
    fun selectedCount(category: GroceryCategory): Int = category.items.count { it.on }

    fun toggle(categories: List<GroceryCategory>, categoryId: String, itemId: String): List<GroceryCategory> =
        categories.map { c ->
            if (c.id != categoryId) c
            else c.copy(items = c.items.map { if (it.id == itemId) it.copy(on = !it.on) else it })
        }

    /** The collapsed state is saved with the list (README › Grocery). */
    fun toggleCollapsed(categories: List<GroceryCategory>, categoryId: String): List<GroceryCategory> =
        categories.map { if (it.id == categoryId) it.copy(collapsed = !it.collapsed) else it }

    fun rename(categories: List<GroceryCategory>, categoryId: String, name: String): List<GroceryCategory> =
        categories.map { if (it.id == categoryId) it.copy(name = name) else it }

    /** The category's `delete` button removes it with all its items. */
    fun deleteCategory(categories: List<GroceryCategory>, categoryId: String): List<GroceryCategory> =
        categories.filter { it.id != categoryId }

    /** "+ Add category": a new category with no name and no items, at the end. */
    fun addCategory(categories: List<GroceryCategory>, newId: String): List<GroceryCategory> =
        categories + GroceryCategory(newId, "")

    fun editItem(categories: List<GroceryCategory>, categoryId: String, itemId: String, text: String) =
        mapItems(categories, categoryId) { items -> items.map { if (it.id == itemId) it.copy(text = text) else it } }

    fun deleteItem(categories: List<GroceryCategory>, categoryId: String, itemId: String) =
        mapItems(categories, categoryId) { items -> items.filter { it.id != itemId } }

    /**
     * A new empty, unselected item [newId]: after [afterItemId] (Enter in that item), or at the end
     * of the category when it is null ("+ Add item").
     */
    fun addItem(categories: List<GroceryCategory>, categoryId: String, afterItemId: String?, newId: String) =
        mapItems(categories, categoryId) { items ->
            val at = if (afterItemId == null) items.size else items.indexOfFirst { it.id == afterItemId } + 1
            items.toMutableList().apply { add(at, GroceryItem(newId, "")) }
        }

    /**
     * When leaving edit mode or the list (README › Saving edits): category names are trimmed, items
     * with no text dropped, categories with neither a name nor items dropped, and any other nameless
     * category renamed [UNTITLED]. Item text is not trimmed, as in the prototype.
     */
    fun cleanup(categories: List<GroceryCategory>): List<GroceryCategory> =
        categories
            .map { c -> c.copy(name = c.name.trim(), items = c.items.filter { it.text.isNotBlank() }) }
            .filter { it.name.isNotEmpty() || it.items.isNotEmpty() }
            .map { if (it.name.isEmpty()) it.copy(name = UNTITLED) else it }

    private fun mapItems(
        categories: List<GroceryCategory>,
        categoryId: String,
        transform: (List<GroceryItem>) -> List<GroceryItem>,
    ): List<GroceryCategory> = categories.map { if (it.id == categoryId) it.copy(items = transform(it.items)) else it }
}
