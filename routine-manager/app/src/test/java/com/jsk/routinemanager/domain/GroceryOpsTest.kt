package com.jsk.routinemanager.domain

import com.jsk.routinemanager.data.GroceryCategory
import com.jsk.routinemanager.data.GroceryItem
import com.jsk.routinemanager.data.SampleData
import org.junit.Assert.assertEquals
import org.junit.Test

class GroceryOpsTest {

    private val categories = listOf(
        GroceryCategory("t", "Toiletries", items = listOf(GroceryItem("t1", "Soap", on = true), GroceryItem("t2", "Shampoo", on = true))),
        GroceryCategory("f", "Food", collapsed = true, items = listOf(GroceryItem("f1", "Egg"), GroceryItem("f2", "Milk", on = true))),
        GroceryCategory("e", "Empty"),
    )

    @Test
    fun groceryBadgeCountsSelectedItemsInAllCategories() {
        // Selected = "need to buy", so the badge is the whole shopping list, collapsed categories included.
        assertEquals(3, GroceryOps.selectedCount(categories))
    }

    @Test
    fun eachCategoryHeaderCountsOnlyItsOwnSelectedItems() {
        assertEquals(listOf(2, 1, 0), categories.map(GroceryOps::selectedCount))
    }

    @Test
    fun sampleGroceryBadgeShowsOne() {
        // Only "Chemical" is selected in the sample.
        assertEquals(1, GroceryOps.selectedCount(SampleData.create().grocery))
    }

    @Test
    fun selectingAnItemChangesOnlyThatItemAndUpdatesTheBadge() {
        val selected = GroceryOps.toggle(categories, "f", "f1")
        assertEquals(true, selected[1].items[0].on)
        assertEquals(4, GroceryOps.selectedCount(selected))
        assertEquals(categories[0], selected[0])
        assertEquals(categories[1].items[1], selected[1].items[1])
        assertEquals(categories, GroceryOps.toggle(selected, "f", "f1"))
    }

    @Test
    fun collapsingAffectsOneCategoryAndKeepsItsSelections() {
        // Collapsing only hides items; it must not lose what is on the shopping list.
        val toggled = GroceryOps.toggleCollapsed(categories, "t")
        assertEquals(true, toggled[0].collapsed)
        assertEquals(categories[0].items, toggled[0].items)
        assertEquals(categories.drop(1), toggled.drop(1))
        assertEquals(categories, GroceryOps.toggleCollapsed(toggled, "t"))
    }
}
