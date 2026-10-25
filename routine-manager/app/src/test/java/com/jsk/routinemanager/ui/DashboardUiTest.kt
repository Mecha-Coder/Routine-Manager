package com.jsk.routinemanager.ui

import com.jsk.routinemanager.data.SampleData
import com.jsk.routinemanager.domain.ClockText
import com.jsk.routinemanager.domain.FREE_TIME_TITLE
import com.jsk.routinemanager.domain.NO_NEXT
import com.jsk.routinemanager.domain.RoutineState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * BUILD_PLAN › Test times: the states the owner checks on the phone, with the sample data. If the
 * maths and the sample agree here, any difference on the phone is a drawing problem.
 */
class DashboardUiTest {

    private val blocks = SampleData.create().blocks

    private fun mon(h: Int, m: Int) = dashboardUi(blocks, LocalDateTime.of(2026, 10, 19, h, m))
    private fun sat(h: Int, m: Int) = dashboardUi(blocks, LocalDateTime.of(2026, 10, 24, h, m))

    private fun DashboardUi.running() = routine as RoutineState.Running

    private fun DashboardUi.shownBar() = checkNotNull(bar) { "no progress bar" }

    @Test
    fun mon0530IsBedtimeWithNoBar() {
        val ui = mon(5, 30)
        assertEquals(RoutineState.Bedtime, ui.routine)
        assertNull(ui.bar)
    }

    @Test
    fun mon0705IsCommuteWith55MinLeftAndWorkSessionNext() {
        val ui = mon(7, 5)
        assertEquals(ClockText("07:05", "AM"), ui.clock)
        assertEquals("Mon", ui.day)
        assertEquals("19 Oct", ui.date)
        assertEquals("Commute", ui.running().title)
        assertEquals("Work Session1", ui.running().next)
        assertEquals("55min", ui.shownBar().left)
        assertEquals(ClockText("07:00", "AM"), ui.shownBar().start)
        assertEquals(ClockText("08:00", "AM"), ui.shownBar().end)
    }

    @Test
    fun mon0930IsWorkSession1WithFourSubsAnd2hr30minLeft() {
        val ui = mon(9, 30)
        assertEquals("Work Session1", ui.running().title)
        assertEquals(4, ui.running().subs.size)
        assertEquals("2hr 30min", ui.shownBar().left)
        assertEquals(1.5f / 4f, ui.shownBar().progress, 1e-6f)
    }

    @Test
    fun mon1259IsLunchWith1MinLeft() {
        val ui = mon(12, 59)
        assertEquals("Lunch", ui.running().title)
        assertEquals("1min", ui.shownBar().left)
    }

    @Test
    fun mon2245IsWindDownWith15MinLeftAndNothingNext() {
        val ui = mon(22, 45)
        assertEquals("Wind down", ui.running().title)
        assertEquals("15min", ui.shownBar().left)
        assertEquals(NO_NEXT, ui.running().next)
        assertEquals(ClockText("10:30", "PM"), ui.shownBar().start)
    }

    @Test
    fun mon2330IsBedtime() {
        assertEquals(RoutineState.Bedtime, mon(23, 30).routine)
    }

    @Test
    fun sat1230IsFreeTimeFromNoonTo2pmWithErrandsNext() {
        val ui = sat(12, 30)
        val r = ui.running()
        assertTrue(r.isFreeTime)
        assertEquals(FREE_TIME_TITLE, r.title)
        assertEquals(emptyList<String>(), r.subs)
        assertEquals("Errands", r.next)
        assertEquals("1hr 30min", ui.shownBar().left)
        assertEquals(ClockText("12:00", "PM"), ui.shownBar().start)
        assertEquals(ClockText("02:00", "PM"), ui.shownBar().end)
        assertEquals("Sat", ui.day)
    }
}
