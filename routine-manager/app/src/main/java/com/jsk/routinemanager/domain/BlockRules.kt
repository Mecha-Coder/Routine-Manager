package com.jsk.routinemanager.domain

import com.jsk.routinemanager.data.Block
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Minutes from midnight; [end] can be 1440 (midnight at the end of the day). */
data class TimeRange(val start: Int, val end: Int)

/**
 * Where the timetable's create gesture started on one day: [anchor] is the quarter hour under the
 * finger, and the new block must stay between [earliest] (the previous block's end, or 00:00) and
 * [latest] (the next block's start, or 24:00).
 */
data class CreateAnchor(val anchor: Int, val earliest: Int, val latest: Int)

/** Block rules (README › Screens › 5 and 6). Ported from the prototype's `colDown`/`colMove`/`colUp`. */
object BlockRules {
    /** Times snap to 15 minutes (README › Data model). */
    const val STEP = 15
    const val DAY_END = 24 * 60

    /** A long press without a drag gives a block this long, if there is room (README › Create). */
    private const val DEFAULT_LENGTH = 60

    /**
     * The anchor for a long press at [minute] (with fractions) on a day with [dayBlocks], or null
     * when the press is on a block: a block is tapped to edit it, never drawn over.
     */
    fun createAnchor(dayBlocks: List<Block>, minute: Double): CreateAnchor? {
        val anchor = (floor(minute / STEP).toInt() * STEP).coerceIn(0, DAY_END - STEP)
        if (dayBlocks.any { anchor >= it.start && anchor < it.end }) return null
        return CreateAnchor(
            anchor = anchor,
            earliest = dayBlocks.filter { it.end <= anchor }.maxOfOrNull { it.end } ?: 0,
            latest = dayBlocks.filter { it.start > anchor }.minOfOrNull { it.start } ?: DAY_END,
        )
    }

    /**
     * The range while the finger is at [minute]: from the anchor to the nearest quarter hour, at
     * least one 15-minute step, never past a neighbouring block.
     */
    fun dragRange(from: CreateAnchor, minute: Double): TimeRange {
        // roundToInt rounds halves up, as the prototype's Math.round does.
        val snapped = ((minute / STEP).roundToInt() * STEP).coerceIn(0, DAY_END)
        val a = from.anchor
        return if (snapped > a) {
            TimeRange(a, min(from.latest, max(snapped, a + STEP)))
        } else {
            TimeRange(max(from.earliest, snapped), min(from.latest, a + STEP))
        }
    }

    /** On release, 15 minutes or less (a press without a drag) becomes an hour, cut at the next block. */
    fun releaseRange(from: CreateAnchor, range: TimeRange): TimeRange =
        if (range.end - range.start <= STEP) range.copy(end = min(from.latest, range.start + DEFAULT_LENGTH)) else range
}
