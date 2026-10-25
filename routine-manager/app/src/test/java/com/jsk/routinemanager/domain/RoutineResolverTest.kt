package com.jsk.routinemanager.domain

import com.jsk.routinemanager.data.Block
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The routine panel, the block-change tone and the Bedtime dim all follow this resolution
 * (README › Behaviour and system), so an off-by-one here shows the wrong block, plays the tone at
 * the wrong minute, or dims the screen in the middle of the day.
 */
class RoutineResolverTest {

    private fun block(id: String, start: String, end: String, title: String = id, day: Int = MON) =
        Block(id, "g-$id", day, minutes(start), minutes(end), title, listOf("$title sub"))

    private fun minutes(hhmm: String): Int = hhmm.split(':').let { (h, m) -> h.toInt() * 60 + m.toInt() }

    private fun at(blocks: List<Block>, hhmm: String, day: Int = MON) =
        RoutineResolver.resolve(blocks, day, minutes(hhmm).toDouble())

    private fun RoutineState.running() = this as RoutineState.Running

    private val morning = block("morning", "06:00", "07:00", "Morning")
    private val commute = block("commute", "07:00", "08:00", "Commute")
    private val work = block("work", "09:00", "12:00", "Work")
    private val errands = block("errands", "14:00", "17:00", "Errands")

    @Test
    fun startIsInclusiveSoABlockShowsFromItsFirstMinute() {
        assertEquals("commute", at(listOf(commute), "07:00").key)
    }

    @Test
    fun endIsExclusiveSoBackToBackBlocksHandOverExactlyAtTheBoundary() {
        val day = listOf(morning, commute)
        assertEquals("morning", RoutineResolver.resolve(day, MON, minutes("07:00") - 0.01).key)
        // At 07:00 the next block is current: no Free time and no Bedtime between them.
        assertEquals("commute", at(day, "07:00").key)
    }

    @Test
    fun aBlockEndingAtMidnightStaysCurrentUntilTheDayEnds() {
        val late = block("late", "22:30", "24:00", "Late")
        val r = RoutineResolver.resolve(listOf(late), MON, 1439.99).running()
        assertEquals("late", r.key)
        assertEquals(1440, r.end)
        assertEquals(NO_NEXT, r.next)
    }

    @Test
    fun aDayWithoutBlocksIsBedtimeAllDay() {
        val tuesdayOnly = listOf(block("tue", "08:00", "17:00", day = 1))
        for (t in listOf("00:00", "08:00", "12:00", "23:59")) {
            assertEquals(t, RoutineState.Bedtime, at(tuesdayOnly, t, MON))
        }
    }

    @Test
    fun freeTimeFillsTheGapFromThePreviousEndToTheNextStart() {
        val r = at(listOf(work, errands), "12:30").running()
        assertTrue(r.isFreeTime)
        assertEquals(FREE_TIME_TITLE, r.title)
        assertEquals(minutes("12:00"), r.start)
        assertEquals(minutes("14:00"), r.end)
        assertEquals(emptyList<String>(), r.subs)
        assertEquals("Errands", r.next)
        // Keyed by the previous block, so each gap of the day is a separate routine change.
        assertEquals("gap:work", r.key)
    }

    @Test
    fun beforeTheFirstBlockAndAfterTheLastIsBedtimeNotFreeTime() {
        val day = listOf(work, errands)
        assertEquals(RoutineState.Bedtime, at(day, "08:59"))
        assertEquals(RoutineState.Bedtime, at(day, "17:00"))
    }

    @Test
    fun nextIsDashAfterTheDaysLastBlock() {
        assertEquals(NO_NEXT, at(listOf(work, errands), "15:00").running().next)
    }

    @Test
    fun nextLooksPastAGapToTheFollowingBlock() {
        assertEquals("Errands", at(listOf(work, errands), "10:00").running().next)
    }

    @Test
    fun blocksAreSortedByStartSoTheSavedOrderDoesNotMatter() {
        // Saved order is the editor's insertion order, not time order.
        val r = at(listOf(errands, commute, work, morning), "07:30").running()
        assertEquals("commute", r.key)
        assertEquals("Work", r.next)
    }

    @Test
    fun weekdayIndexIsZeroOnMondaySoBlockDaysLineUpWithTheCalendar() {
        assertEquals(0, dayIndex(LocalDate.of(2026, 10, 19))) // a Monday
        assertEquals(5, dayIndex(LocalDate.of(2026, 10, 24))) // Saturday
        assertEquals(6, dayIndex(LocalDate.of(2026, 10, 25))) // Sunday
        val mondayBlock = block("mon", "07:00", "08:00", day = 0)
        assertEquals("mon", RoutineResolver.resolve(listOf(mondayBlock), LocalDateTime.of(2026, 10, 19, 7, 30)).key)
        assertEquals(RoutineState.Bedtime, RoutineResolver.resolve(listOf(mondayBlock), LocalDateTime.of(2026, 10, 20, 7, 30)))
    }

    @Test
    fun minuteOfDayKeepsTheSecondsSoLeftIsRightBetweenMinuteTicks() {
        assertEquals(425.5, minuteOfDay(LocalDateTime.of(2026, 10, 19, 7, 5, 30)), 1e-9)
    }

    private companion object {
        const val MON = 0
    }
}
