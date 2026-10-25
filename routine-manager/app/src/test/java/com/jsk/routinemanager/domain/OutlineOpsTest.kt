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
}
