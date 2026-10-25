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
}
