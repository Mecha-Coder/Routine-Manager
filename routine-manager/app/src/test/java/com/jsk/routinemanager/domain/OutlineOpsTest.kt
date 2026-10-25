package com.jsk.routinemanager.domain

import com.jsk.routinemanager.data.OutlineItem
import com.jsk.routinemanager.data.SampleData
import org.junit.Assert.assertEquals
import org.junit.Test

class OutlineOpsTest {

    private val list = listOf(
        OutlineItem("a", "Sabah", depth = 0, done = true),
        OutlineItem("a1", "Complete places", depth = 1),
        OutlineItem("a2", "Itinerary", depth = 1, done = true),
        OutlineItem("b", "Clean room", depth = 0),
    )

    @Test
    fun toDoBadgeCountsNotDoneItemsAtEveryLevel() {
        // A done parent with an open child still has work left: the child counts.
        assertEquals(2, OutlineOps.openCount(list))
    }

    @Test
    fun sampleToDoBadgeShowsFourOpenItems() {
        // 5 items, "Find future room + strategy" is done.
        assertEquals(4, OutlineOps.openCount(SampleData.create().todo))
    }

    @Test
    fun tickingAnItemKeepsItInPlaceAndTickingAgainUndoesIt() {
        val ticked = OutlineOps.toggleDone(list, "b")
        assertEquals(list.map { it.id }, ticked.map { it.id })
        assertEquals(true, ticked.last().done)
        assertEquals(list, OutlineOps.toggleDone(ticked, "b"))
    }

    @Test
    fun tickingAParentLeavesItsChildrenAsTheyWere() {
        // Each to-do is ticked on its own; a parent is not a "select all".
        val unticked = OutlineOps.toggleDone(list, "a")
        assertEquals(false, unticked[0].done)
        assertEquals(list.drop(1), unticked.drop(1))
    }

    // Edit mode (M6). Short ids: p = parent, c = child.
    private fun item(id: String, depth: Int = 0, text: String = id, done: Boolean = false) = OutlineItem(id, text, depth, done)

    private val outline = listOf(item("p1"), item("c1", 1), item("c2", 1), item("p2"))

    private fun List<OutlineItem>.shape() = map { it.id to it.depth }

    @Test
    fun enterOnAParentAddsTheNewItemBelowItsChildrenSoItDoesNotSplitThem() {
        val result = OutlineOps.insertAfter(outline, "p1", "n")
        assertEquals(listOf("p1" to 0, "c1" to 1, "c2" to 1, "n" to 0, "p2" to 0), result.shape())
        assertEquals(OutlineItem("n", "", depth = 0, done = false), result[3])
    }

    @Test
    fun enterOnAChildAddsASiblingUnderTheSameParent() {
        val result = OutlineOps.insertAfter(outline, "c1", "n")
        assertEquals(listOf("p1" to 0, "c1" to 1, "n" to 1, "c2" to 1, "p2" to 0), result.shape())
    }

    @Test
    fun addTaskAppendsAnEmptyTopLevelItemEvenAfterAChild() {
        val result = OutlineOps.append(listOf(item("p1"), item("c1", 1)), "n")
        assertEquals(listOf("p1" to 0, "c1" to 1, "n" to 0), result.shape())
        assertEquals("", result.last().text)
    }

    @Test
    fun deletingAParentPromotesItsChildrenSoNoChildIsOrphaned() {
        val result = OutlineOps.delete(outline, "p1")
        assertEquals(listOf("c1" to 0, "c2" to 0, "p2" to 0), result.shape())
    }

    @Test
    fun deletingAChildLeavesItsParentAndSiblingsAsTheyWere() {
        assertEquals(listOf("p1" to 0, "c2" to 1, "p2" to 0), OutlineOps.delete(outline, "c1").shape())
    }

    @Test
    fun deletingASecondParentPromotesItsChildrenInsteadOfMovingThemUnderTheFirst() {
        val list = listOf(item("p1"), item("p2"), item("c1", 1))
        assertEquals(listOf("p1" to 0, "c1" to 0), OutlineOps.delete(list, "p2").shape())
    }

    @Test
    fun normalizeLiftsChildrenThatHaveNoParentAboveThem() {
        // A child first in the list, or two levels deeper than the item above, has no valid parent.
        val list = listOf(item("a", 1), item("b", 0), item("c", 2), item("d", 1))
        assertEquals(listOf("a" to 0, "b" to 0, "c" to 1, "d" to 1), OutlineOps.normalize(list).shape())
    }

    @Test
    fun cleanupDropsEmptyAndBlankItemsButKeepsTheRestAsTyped() {
        val list = listOf(item("a", text = " Keep me "), item("b", text = ""), item("c", text = "   "), item("d", text = "x", done = true))
        assertEquals(listOf(item("a", text = " Keep me "), item("d", text = "x", done = true)), OutlineOps.cleanup(list))
    }

    @Test
    fun cleanupOfAnEmptyFirstParentLiftsItsFirstChildSoNoChildIsOrphaned() {
        // Normalising (as in the prototype) lifts only what has no parent: c1 becomes top-level,
        // and c2 stays a child, now under c1.
        val list = listOf(item("p", text = ""), item("c1", 1), item("c2", 1))
        assertEquals(listOf("c1" to 0, "c2" to 1), OutlineOps.cleanup(list).shape())
    }

    @Test
    fun cleanupOfAnEmptyLaterParentLeavesItsChildrenUnderTheParentAbove() {
        // As in the prototype: the children still have a parent (the item above), so they stay children.
        val list = listOf(item("p1"), item("p2", text = ""), item("c", 1))
        assertEquals(listOf("p1" to 0, "c" to 1), OutlineOps.cleanup(list).shape())
    }

    @Test
    fun editingTextChangesOnlyThatItem() {
        val result = OutlineOps.editText(outline, "c2", "New text")
        assertEquals("New text", result[2].text)
        assertEquals(outline.filterIndexed { i, _ -> i != 2 }, result.filterIndexed { i, _ -> i != 2 })
    }
}
