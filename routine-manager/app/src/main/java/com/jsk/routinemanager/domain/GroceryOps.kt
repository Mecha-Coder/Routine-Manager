package com.jsk.routinemanager.domain

import com.jsk.routinemanager.data.GroceryCategory

/** Changes to the Grocery list. Selected ([com.jsk.routinemanager.data.GroceryItem.on]) = "need to buy". */
object GroceryOps {

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
}
