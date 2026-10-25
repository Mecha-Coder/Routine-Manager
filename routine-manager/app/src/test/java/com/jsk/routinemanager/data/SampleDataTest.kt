package com.jsk.routinemanager.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The first run and the debug reset load this (SPEC D8), and the BUILD_PLAN test times are written
 * against it, so it must match the prototype's `sample()`.
 */
class SampleDataTest {

    private val data = SampleData.create()

    @Test
    fun weekdaysHaveNineBlocksAndWeekendDaysFive() {
        val perDay = (0..6).map { d -> data.blocks.count { it.day == d } }
        assertEquals(listOf(9, 9, 9, 9, 9, 5, 5), perDay)
    }

    @Test
    fun copiesOfOneRowShareAGroupSoEditingOneEditsAllDays() {
        // README › Multi-day groups: a block saved for several days is one group.
        val weekdayRows = data.blocks.filter { it.day < 5 }.groupBy { it.start to it.title }
        assertEquals(9, weekdayRows.size)
        for ((row, copies) in weekdayRows) {
            assertEquals("$row days", listOf(0, 1, 2, 3, 4), copies.map { it.day })
            assertEquals("$row group", 1, copies.map { it.groupId }.distinct().size)
        }
        val weekendRows = data.blocks.filter { it.day >= 5 }.groupBy { it.groupId }
        assertEquals(5, weekendRows.size)
        assertTrue(weekendRows.values.all { copies -> copies.map { it.day } == listOf(5, 6) })
        // Weekday and weekend rows with the same title (Morning, Wind down) are separate groups.
        assertEquals(14, data.blocks.map { it.groupId }.distinct().size)
    }

    @Test
    fun everyIdIsUniqueSoEachDaysCopyCanBeEditedOnItsOwn() {
        val ids = data.blocks.map { it.id } + (data.todo + data.self + data.room).map { it.id } +
            data.grocery.flatMap { c -> listOf(c.id) + c.items.map { it.id } }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun timesMatchThePrototype() {
        val monday = data.blocks.filter { it.day == 0 }.sortedBy { it.start }
        assertEquals("Morning", monday.first().title)
        assertEquals(6 * 60 to 7 * 60, monday.first().start to monday.first().end)
        assertEquals("Wind down", monday.last().title)
        assertEquals(22 * 60 + 30 to 23 * 60, monday.last().start to monday.last().end)
        val saturday = data.blocks.filter { it.day == 5 }.sortedBy { it.start }
        assertEquals(listOf("Morning", "Work Session1", "Errands", "Personal", "Wind down"), saturday.map { it.title })
        assertEquals(listOf("Grocery", "Clean room"), saturday[2].subs)
    }

    @Test
    fun listsMatchThePrototype() {
        assertEquals(
            listOf("Clean room" to 0, "Sabah" to 0, "Complete places" to 1, "Itinerary" to 1, "Find future room + strategy" to 0),
            data.todo.map { it.text to it.depth },
        )
        assertEquals(listOf(false, false, false, false, true), data.todo.map { it.done })
        assertEquals(listOf("Toiletries", "Food"), data.grocery.map { it.name })
        assertEquals(listOf("Chemical"), data.grocery.flatMap { it.items }.filter { it.on }.map { it.text })
        assertEquals(listOf("Cut Hair", "Cut Nail", "Floss teeth"), data.self.map { it.text })
        assertEquals(listOf("Change bedsheet", "Vacuum floor", "Water plants"), data.room.map { it.text })
        assertEquals(Settings(Theme.LIGHT, Tone.BOWL, 60), data.settings)
    }
}
