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

    // Edit mode (M6).
    private fun cat(id: String, name: String, vararg items: GroceryItem) = GroceryCategory(id, name, items = items.toList())

    @Test
    fun cleanupTrimsNamesDropsBlankItemsAndEmptyNamelessCategoriesAndNamesTheRestUntitled() {
        val edited = listOf(
            cat("a", "  Food  ", GroceryItem("a1", "Egg"), GroceryItem("a2", "  ")),
            cat("b", " ", GroceryItem("b1", "")),               // nothing left: dropped
            cat("c", "", GroceryItem("c1", "Soap", on = true)), // items but no name: kept as Untitled
            cat("d", "Snacks"),                                  // a name but no items: kept
        )
        assertEquals(
            listOf(
                cat("a", "Food", GroceryItem("a1", "Egg")),
                cat("c", GroceryOps.UNTITLED, GroceryItem("c1", "Soap", on = true)),
                cat("d", "Snacks"),
            ),
            GroceryOps.cleanup(edited),
        )
    }

    @Test
    fun cleanupKeepsTheCollapsedStateBecauseItIsSavedWithTheList() {
        val collapsed = listOf(GroceryCategory("f", " Food ", collapsed = true, items = listOf(GroceryItem("f1", "Egg"))))
        assertEquals(true, GroceryOps.cleanup(collapsed).single().collapsed)
    }

    @Test
    fun enterInAnItemAddsAnEmptyUnselectedItemRightAfterIt() {
        val result = GroceryOps.addItem(categories, "f", afterItemId = "f1", newId = "n")
        assertEquals(listOf("f1", "n", "f2"), result[1].items.map { it.id })
        assertEquals(GroceryItem("n", "", on = false), result[1].items[1])
        assertEquals(categories[0], result[0])
    }

    @Test
    fun addItemAppendsToTheEndOfItsCategory() {
        val result = GroceryOps.addItem(categories, "e", afterItemId = null, newId = "n")
        assertEquals(listOf("n"), result[2].items.map { it.id })
    }

    @Test
    fun addCategoryAppendsANamelessEmptyCategory() {
        val result = GroceryOps.addCategory(categories, "n")
        assertEquals(GroceryCategory("n", ""), result.last())
        assertEquals(categories, result.dropLast(1))
    }

    @Test
    fun deletingACategoryRemovesItsSelectedItemsFromTheBadge() {
        val result = GroceryOps.deleteCategory(categories, "t")
        assertEquals(listOf("f", "e"), result.map { it.id })
        assertEquals(1, GroceryOps.selectedCount(result))
    }

    @Test
    fun deletingAnItemRemovesOnlyThatItem() {
        val result = GroceryOps.deleteItem(categories, "f", "f2")
        assertEquals(listOf("f1"), result[1].items.map { it.id })
        assertEquals(categories[0], result[0])
    }

    @Test
    fun renamingAndEditingChangeOnlyTheirTarget() {
        val renamed = GroceryOps.rename(categories, "e", "Snacks")
        assertEquals(listOf("Toiletries", "Food", "Snacks"), renamed.map { it.name })
        val edited = GroceryOps.editItem(categories, "t", "t2", "Conditioner")
        assertEquals(listOf("Soap", "Conditioner"), edited[0].items.map { it.text })
        assertEquals(true, edited[0].items[1].on)
    }
}
