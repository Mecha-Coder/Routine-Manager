package com.jsk.routinemanager.domain

import com.jsk.routinemanager.data.Block
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The timetable's create gesture (README › Screens › 5 › Create), as the prototype does it. */
class BlockRulesTest {

    private fun block(start: Int, end: Int) = Block("b$start", "g$start", day = 0, start = start, end = end, title = "B")

    private fun hm(h: Int, m: Int = 0) = h * 60 + m

    // Monday morning in the sample: Morning 06:00–07:00, Commute 07:00–08:00, then 09:00–10:00.
    private val day = listOf(block(hm(9), hm(10)), block(hm(6), hm(7)), block(hm(7), hm(8)))

    @Test
    fun pressingOnABlockStartsNoNewBlockSoTheTapCanOpenIt() {
        assertNull(BlockRules.createAnchor(day, hm(6).toDouble()))
        assertNull(BlockRules.createAnchor(day, hm(7, 59) + 0.9))
    }

    @Test
    fun theAnchorIsTheQuarterHourUnderTheFingerSoTheBlockStartsWhereThePressWas() {
        // 08:29.9 is in the 08:15 slot; the free time runs from Commute's end to the 09:00 block.
        assertEquals(CreateAnchor(hm(8, 15), hm(8), hm(9)), BlockRules.createAnchor(day, hm(8, 29) + 0.9))
        // A block's end is free: blocks end exclusive.
        assertEquals(CreateAnchor(hm(8), hm(8), hm(9)), BlockRules.createAnchor(day, hm(8).toDouble()))
    }

    @Test
    fun anEmptyDayIsBoundedOnlyByMidnightAtBothEnds() {
        assertEquals(CreateAnchor(hm(13), 0, BlockRules.DAY_END), BlockRules.createAnchor(emptyList(), hm(13, 5).toDouble()))
    }

    @Test
    fun thePressInTheLastQuarterHourStillLeavesRoomForA15MinuteBlock() {
        // A block can't start at 24:00.
        assertEquals(hm(23, 45), BlockRules.createAnchor(emptyList(), hm(23, 59) + 0.99)?.anchor)
    }

    @Test
    fun draggingDownSnapsTheEndToTheNearestQuarterHour() {
        val a = BlockRules.createAnchor(emptyList(), hm(13).toDouble())!!
        assertEquals(TimeRange(hm(13), hm(14, 15)), BlockRules.dragRange(a, hm(14, 7) + 0.5))
        assertEquals(TimeRange(hm(13), hm(14)), BlockRules.dragRange(a, hm(14, 7).toDouble()))
    }

    @Test
    fun draggingDownStopsAtTheNextBlockSoBlocksNeverOverlap() {
        val a = BlockRules.createAnchor(day, hm(8, 15).toDouble())!!
        assertEquals(TimeRange(hm(8, 15), hm(9)), BlockRules.dragRange(a, hm(11).toDouble()))
    }

    @Test
    fun draggingUpMovesTheStartAndStopsAtThePreviousBlocksEnd() {
        val a = BlockRules.createAnchor(day, hm(8, 30).toDouble())!!
        assertEquals(TimeRange(hm(8, 15), hm(8, 45)), BlockRules.dragRange(a, hm(8, 14).toDouble()))
        assertEquals(TimeRange(hm(8), hm(8, 45)), BlockRules.dragRange(a, hm(5).toDouble()))
    }

    @Test
    fun theRangeIsNeverShorterThanOneStep() {
        // Finger back on the anchor, or just above it: the anchor's own quarter hour stays selected.
        val a = BlockRules.createAnchor(emptyList(), hm(13).toDouble())!!
        assertEquals(TimeRange(hm(13), hm(13, 15)), BlockRules.dragRange(a, hm(13).toDouble()))
        assertEquals(TimeRange(hm(13), hm(13, 15)), BlockRules.dragRange(a, hm(13, 6).toDouble()))
    }

    @Test
    fun theRangeCanReachMidnightAtTheEndOfTheDay() {
        val a = BlockRules.createAnchor(emptyList(), hm(23).toDouble())!!
        assertEquals(TimeRange(hm(23), BlockRules.DAY_END), BlockRules.dragRange(a, 1500.0))
    }

    @Test
    fun aPressWithoutADragBecomesAnHourLongBlock() {
        val a = BlockRules.createAnchor(emptyList(), hm(13).toDouble())!!
        assertEquals(TimeRange(hm(13), hm(14)), BlockRules.releaseRange(a, TimeRange(hm(13), hm(13, 15))))
    }

    @Test
    fun theDefaultHourIsCutAtTheNextBlock() {
        val a = BlockRules.createAnchor(day, hm(8, 30).toDouble())!!
        assertEquals(TimeRange(hm(8, 30), hm(9)), BlockRules.releaseRange(a, TimeRange(hm(8, 30), hm(8, 45))))
    }

    @Test
    fun aDraggedRangeLongerThan15MinutesIsKeptAsDrawn() {
        val a = BlockRules.createAnchor(emptyList(), hm(13).toDouble())!!
        assertEquals(TimeRange(hm(13), hm(13, 30)), BlockRules.releaseRange(a, TimeRange(hm(13), hm(13, 30))))
    }

    // The block editor (README › Screens › 6).

    /** Morning 06:00–07:00 and Commute 07:00–08:00 on Mon–Fri, Errands 17:30–18:30 on Sat, as in the sample. */
    private val week: List<Block> =
        (0..4).flatMap { d ->
            listOf(
                Block("morning$d", "morning", d, hm(6), hm(7), "Morning", listOf("Wake up")),
                Block("commute$d", "commute", d, hm(7), hm(8), "Commute", listOf("Podcast")),
            )
        } + Block("errands5", "errands", 5, hm(17, 30), hm(18, 30), "Errands")

    private fun draft(
        title: String = "Gym",
        days: List<Int> = listOf(0),
        start: Int = hm(9),
        end: Int = hm(10),
        groupId: String? = null,
        subs: List<String> = emptyList(),
    ) = BlockDraft(groupId, title, days, start, end, subs)

    @Test
    fun aMissingTitleIsReportedFirstEvenWhenEverythingElseIsWrongToo() {
        // The user fixes one message at a time, in the README's order.
        val everythingWrong = draft(title = "   ", days = emptyList(), start = hm(7), end = hm(7))
        assertEquals("Give the block a title.", BlockRules.validate(everythingWrong, week))
        assertEquals("End time must be after the start time.", BlockRules.validate(everythingWrong.copy(title = "Gym"), week))
        assertEquals(
            "Pick at least one day.",
            BlockRules.validate(everythingWrong.copy(title = "Gym", end = hm(7, 30)), week),
        )
    }

    @Test
    fun anEndBeforeTheStartIsAnErrorNotASilentSwap() {
        assertEquals("End time must be after the start time.", BlockRules.validate(draft(start = hm(10), end = hm(9)), week))
    }

    @Test
    fun anOverlapNamesTheClashingBlockWithItsHoursAndEveryDayItClashesOn() {
        // Sat is selected too but is free then, so only the days that clash are listed.
        val gym = draft(days = listOf(0, 1, 5), start = hm(7, 15), end = hm(7, 45))
        assertEquals("Overlaps “Commute” (7 AM – 8 AM) on Mon, Tue.", BlockRules.validate(gym, week))
    }

    @Test
    fun anOverlapOffTheHourShowsTheMinutesSoTheTimesAreNotMisread() {
        val shopping = draft(days = listOf(5), start = hm(18), end = hm(19))
        assertEquals("Overlaps “Errands” (5:30 PM – 6:30 PM) on Sat.", BlockRules.validate(shopping, week))
    }

    @Test
    fun blocksThatOnlyTouchDoNotOverlapBecauseBlocksEndExclusive() {
        assertNull(BlockRules.validate(draft(start = hm(8), end = hm(9)), week))
        assertNull(BlockRules.validate(draft(start = hm(5), end = hm(6)), week))
    }

    @Test
    fun editingAGroupNeverClashesWithItsOwnCopies() {
        // Moving Commute to 07:00–07:45 on its own days would otherwise clash with itself.
        val commute = BlockRules.editDraft(week, "commute2").copy(end = hm(7, 45))
        assertNull(BlockRules.validate(commute, week))
    }

    @Test
    fun editingOneCopyOpensTheWholeGroupWithItsSharedValues() {
        assertEquals(
            BlockDraft("commute", "Commute", listOf(0, 1, 2, 3, 4), hm(7), hm(8), listOf("Podcast")),
            BlockRules.editDraft(week, "commute3"),
        )
    }

    @Test
    fun savingAGroupKeepsEachDaysBlockIdSoEditingTheCurrentBlockIsNotABlockChange() {
        // Rename Commute, drop Fri, add Sat: Mon–Thu keep their ids, Fri's copy goes, Sat gets a new one.
        val edited = BlockRules.editDraft(week, "commute0").copy(title = "Train", days = listOf(0, 1, 2, 3, 5))
        val saved = BlockRules.save(week, edited) { "new" }
        val commute = saved.filter { it.groupId == "commute" }.sortedBy { it.day }
        assertEquals(listOf("commute0", "commute1", "commute2", "commute3", "new"), commute.map { it.id })
        assertEquals(listOf(0, 1, 2, 3, 5), commute.map { it.day })
        assertEquals(setOf("Train"), commute.map { it.title }.toSet())
        // Other groups are untouched.
        assertEquals(week.filter { it.groupId != "commute" }, saved.filter { it.groupId != "commute" })
    }

    @Test
    fun aNewBlockForSeveralDaysIsOneGroupSoLaterEditsChangeEveryCopy() {
        val ids = ArrayDeque(listOf("g", "a", "b"))
        val saved = BlockRules.save(week, draft(days = listOf(5, 6))) { ids.removeFirst() }
        val gym = saved.filter { it.title == "Gym" }
        assertEquals(listOf(Block("a", "g", 5, hm(9), hm(10), "Gym"), Block("b", "g", 6, hm(9), hm(10), "Gym")), gym)
    }

    @Test
    fun savingTrimsTheTitleAndDropsEmptySubRoutines() {
        val saved = BlockRules.save(emptyList(), draft(title = "  Gym ", subs = listOf(" Warm up ", "", "  ", "Run"))) { "x" }
        assertEquals("Gym", saved.single().title)
        assertEquals(listOf("Warm up", "Run"), saved.single().subs)
    }

    @Test
    fun deleteRemovesTheWholeGroupOnEveryDay() {
        val left = BlockRules.deleteGroup(week, "morning")
        assertEquals(week.filter { it.title != "Morning" }, left)
    }

    @Test
    fun endTwelveAmOnTheWheelIsMidnightAtTheEndOfTheDay() {
        val twelveAm = BlockRules.minutes(WheelTime(hour = 0, quarter = 0, pm = false))
        assertEquals(BlockRules.DAY_END, BlockRules.withEnd(draft(start = hm(22)), twelveAm).end)
        // ...and a block ending at midnight shows 12:00 am on the wheels, not a blank.
        assertEquals(WheelTime(0, 0, false), BlockRules.wheelTime(BlockRules.DAY_END))
        // Start 12:00 am is the start of the day.
        assertEquals(0, BlockRules.withStart(draft(), twelveAm).start)
    }

    @Test
    fun aBlockCanEndAtMidnightAndStillBeSaved() {
        assertNull(BlockRules.validate(draft(start = hm(23), end = BlockRules.DAY_END), week))
    }

    @Test
    fun theWheelsShowTwelveForNoonAndMidnightAsA12HourClockDoes() {
        assertEquals(WheelTime(0, 0, true), BlockRules.wheelTime(hm(12)))
        assertEquals(WheelTime(0, 0, false), BlockRules.wheelTime(0))
        assertEquals(WheelTime(5, 3, true), BlockRules.wheelTime(hm(17, 45)))
        assertEquals(hm(12, 30), BlockRules.minutes(WheelTime(0, 2, true)))
        assertEquals(hm(23, 15), BlockRules.minutes(WheelTime(11, 1, true)))
    }

    @Test
    fun movingStartToOrPastEndPushesEndAnHourLaterSoTheRangeStaysValid() {
        val nineToTen = draft(start = hm(9), end = hm(10))
        assertEquals(draft(start = hm(10), end = hm(11)), BlockRules.withStart(nineToTen, hm(10)))
        assertEquals(draft(start = hm(10, 30), end = hm(11, 30)), BlockRules.withStart(nineToTen, hm(10, 30)))
        // Before End, End stays where the user put it.
        assertEquals(draft(start = hm(8), end = hm(10)), BlockRules.withStart(nineToTen, hm(8)))
    }

    @Test
    fun startPushingEndStopsAtMidnightBecauseBlocksNeverCrossIt() {
        assertEquals(BlockRules.DAY_END, BlockRules.withStart(draft(start = hm(22), end = hm(23)), hm(23, 30)).end)
    }

    @Test
    fun movingEndBeforeStartIsLeftForValidationToReport() {
        // README: only Start pushes End; an early End is reported on Save instead of moving Start.
        assertEquals(draft(start = hm(9), end = hm(8)), BlockRules.withEnd(draft(start = hm(9), end = hm(10)), hm(8)))
    }

    @Test
    fun dayTogglesStaySortedSoTheOverlapMessageListsDaysInWeekOrder() {
        val d = BlockRules.toggleDay(BlockRules.toggleDay(draft(days = listOf(3)), 0), 6)
        assertEquals(listOf(0, 3, 6), d.days)
        assertEquals(listOf(0, 6), BlockRules.toggleDay(d, 3).days)
    }
}
