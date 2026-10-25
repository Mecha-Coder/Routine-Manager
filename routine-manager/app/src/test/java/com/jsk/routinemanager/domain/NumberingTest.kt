package com.jsk.routinemanager.domain

import com.jsk.routinemanager.data.OutlineItem
import com.jsk.routinemanager.data.SampleData
import org.junit.Assert.assertEquals
import org.junit.Test

/** The numbers are generated, never typed (README › numbered outline lists), so they must always follow the order. */
class NumberingTest {

    private fun item(depth: Int, done: Boolean = false) = OutlineItem(id = "id", text = "x", depth = depth, done = done)

    @Test
    fun topLevelItemsCountUpWithAParenthesis() {
        assertEquals(listOf("1)", "2)", "3)"), Numbering.labels(listOf(item(0), item(0), item(0))))
    }

    @Test
    fun childrenTakeTheirParentsNumberAndTheirCounterRestartsUnderEachParent() {
        // Without the restart the second parent's children would read 2.3), 2.4).
        val labels = Numbering.labels(listOf(item(0), item(1), item(1), item(0), item(1), item(1)))
        assertEquals(listOf("1)", "1.1)", "1.2)", "2)", "2.1)", "2.2)"), labels)
    }

    @Test
    fun aChildDoesNotAdvanceTheTopLevelCount() {
        assertEquals(listOf("1)", "1.1)", "2)"), Numbering.labels(listOf(item(0), item(1), item(0))))
    }

    @Test
    fun doneItemsKeepTheirNumberBecauseTheyStayInPlace() {
        assertEquals(listOf("1)", "2)", "2.1)"), Numbering.labels(listOf(item(0, done = true), item(0), item(1, done = true))))
    }

    @Test
    fun aChildWithNoParentAboveIsNumberedUnderOneNotZero() {
        // Never in normalised data, but a damaged file must not show "0.1)" (prototype: `a||1`).
        assertEquals(listOf("1.1)", "1)"), Numbering.labels(listOf(item(1), item(0))))
    }

    @Test
    fun sampleToDoIsNumberedAsInThePrototype() {
        assertEquals(listOf("1)", "2)", "2.1)", "2.2)", "3)"), Numbering.labels(SampleData.create().todo))
    }
}
