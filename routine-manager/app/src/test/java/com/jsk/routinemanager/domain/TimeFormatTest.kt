package com.jsk.routinemanager.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** The README fixes these texts exactly; the dashboard shows them all day. */
class TimeFormatTest {

    @Test
    fun leftUsesHoursAndMinutesAndDropsAZeroPart() {
        assertEquals("4hr 30min", TimeFormat.left(end = 17 * 60, minuteOfDay = 12.5 * 60))
        assertEquals("2hr", TimeFormat.left(end = 14 * 60, minuteOfDay = 12.0 * 60))
        assertEquals("33min", TimeFormat.left(end = 13 * 60, minuteOfDay = 12.0 * 60 + 27))
    }

    @Test
    fun leftRoundsUpSoTheLastMinuteShowsOneMinNotZero() {
        // README: LEFT = ceil(end − now). 30 s into 12:00 with the block ending at 13:00: 59.5 → 60.
        assertEquals("1hr", TimeFormat.left(end = 13 * 60, minuteOfDay = 12 * 60 + 0.5))
        assertEquals("1min", TimeFormat.left(end = 13 * 60, minuteOfDay = 12 * 60 + 59.01))
        assertEquals("55min", TimeFormat.left(end = 8 * 60, minuteOfDay = 7 * 60 + 5.0))
    }

    @Test
    fun clockIsTwelveHourWithMidnightAndNoonAsTwelve() {
        assertEquals(ClockText("12:05", "AM"), TimeFormat.clock(5))
        assertEquals(ClockText("12:00", "PM"), TimeFormat.clock(12 * 60))
        assertEquals(ClockText("11:59", "PM"), TimeFormat.clock(24 * 60 - 1))
    }

    @Test
    fun clockKeepsTheLeadingZero() {
        assertEquals(ClockText("07:05", "AM"), TimeFormat.clock(7 * 60 + 5))
        assertEquals(ClockText("01:30", "PM"), TimeFormat.clock(13 * 60 + 30))
    }

    @Test
    fun aBlockEndingAtMidnightShowsTwelveAm() {
        assertEquals(ClockText("12:00", "AM"), TimeFormat.clock(1440))
    }

    @Test
    fun timetableBlocksShowTheirFullRangeInLowerCaseWithoutLeadingZero() {
        assertEquals("6:00am-11:30am", TimeFormat.blockRange(6 * 60, 11 * 60 + 30))
        assertEquals("12:00pm-1:15pm", TimeFormat.blockRange(12 * 60, 13 * 60 + 15))
        assertEquals("12:15am-1:00am", TimeFormat.blockRange(15, 60))
    }

    @Test
    fun aTimetableBlockEndingAtMidnightEndsAtTwelveAm() {
        assertEquals("10:30pm-12:00am", TimeFormat.blockRange(22 * 60 + 30, 1440))
    }

    @Test
    fun hourLabelsRunFromTwelveAmToTwelveAmAtTheBottomOfTheGrid() {
        assertEquals("12 AM", TimeFormat.hour(0))
        assertEquals("7 AM", TimeFormat.hour(7))
        assertEquals("12 PM", TimeFormat.hour(12))
        assertEquals("11 PM", TimeFormat.hour(23))
        assertEquals("12 AM", TimeFormat.hour(24))
    }

    @Test
    fun theCreateGhostShowsItsRangeWithEndAtMidnightAsTwelveAm() {
        assertEquals("7:00 AM – 8:15 AM", TimeFormat.ghostRange(7 * 60, 8 * 60 + 15))
        assertEquals("11:45 PM – 12:00 AM", TimeFormat.ghostRange(23 * 60 + 45, 1440))
        assertEquals("12:00 PM", TimeFormat.time(12 * 60))
    }

    @Test
    fun overlapTimesDropZeroMinutesButKeepTheRestSoTheMessageIsShortAndExact() {
        assertEquals("7 AM", TimeFormat.shortTime(7 * 60))
        assertEquals("5:30 PM", TimeFormat.shortTime(17 * 60 + 30))
        assertEquals("12 PM", TimeFormat.shortTime(12 * 60))
        assertEquals("12:15 AM", TimeFormat.shortTime(15))
        assertEquals("12 AM", TimeFormat.shortTime(1440))
    }

    @Test
    fun dayAndDateAreShortEnglishWithoutYearOrLeadingZero() {
        assertEquals("Mon", TimeFormat.day(0))
        assertEquals("Sun", TimeFormat.day(6))
        assertEquals("19 Oct", TimeFormat.date(LocalDate.of(2026, 10, 19)))
        assertEquals("5 Jan", TimeFormat.date(LocalDate.of(2027, 1, 5)))
    }
}
