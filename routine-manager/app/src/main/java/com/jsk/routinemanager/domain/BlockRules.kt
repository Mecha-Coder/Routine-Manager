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

/**
 * The block editor's unsaved values for one group (README › Screens › 6). [groupId] is null for a
 * block that isn't saved yet. [days] are 0..6 (Mon..Sun), sorted.
 */
data class BlockDraft(
    val groupId: String?,
    val title: String,
    val days: List<Int>,
    val start: Int,
    val end: Int,
    val subs: List<String>,
)

/** A wheel picker's three drums: [hour] 0..11 (0 shows `12`), [quarter] 0..3 (`00`…`45`), [pm]. */
data class WheelTime(val hour: Int, val quarter: Int, val pm: Boolean)

/**
 * Block rules (README › Screens › 5 and 6). Ported from the prototype's `colDown`/`colMove`/`colUp`
 * (create gesture) and `openSheet`/`wIdx`/`wSet`/`saveSheet`/`deleteSheet` (editor).
 */
object BlockRules {
    /** Times snap to 15 minutes (README › Data model). */
    const val STEP = 15
    const val DAY_END = 24 * 60

    /** At most this many sub-routines per block (README › Screens › 6). */
    const val MAX_SUBS = 5

    /**
     * A long press without a drag gives a block this long, if there is room (README › Create); and
     * Start moved to or past End pushes End this far after it (README › Screens › 6).
     */
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

    /** The editor for a range drawn on [day]: no title, that day only, no sub-routines. */
    fun newDraft(day: Int, range: TimeRange) = BlockDraft(null, "", listOf(day), range.start, range.end, emptyList())

    /** The editor for a tapped block: its group's days, with the title, times and sub-routines they share. */
    fun editDraft(blocks: List<Block>, blockId: String): BlockDraft {
        val block = blocks.first { it.id == blockId }
        val days = blocks.filter { it.groupId == block.groupId }.map { it.day }.sorted()
        return BlockDraft(block.groupId, block.title, days, block.start, block.end, block.subs)
    }

    /** The wheels for [minutes]. 1440, midnight at the end of the day, shows `12:00 am`. */
    fun wheelTime(minutes: Int): WheelTime {
        val m = minutes % DAY_END
        return WheelTime(hour = m / 60 % 12, quarter = m % 60 / STEP, pm = m >= 12 * 60)
    }

    /** Minutes from midnight for a wheel position: `12:00 am` is 0 here; [withEnd] makes an end of 0 midnight. */
    fun minutes(wheel: WheelTime): Int = (wheel.hour + if (wheel.pm) 12 else 0) * 60 + wheel.quarter * STEP

    /** Moving Start to or past End pushes End to Start + 60 min, at most midnight (README › Screens › 6). */
    fun withStart(draft: BlockDraft, start: Int): BlockDraft =
        if (draft.end <= start) draft.copy(start = start, end = min(DAY_END, start + DEFAULT_LENGTH)) else draft.copy(start = start)

    /** End `12:00 am` means midnight at the end of the day, since a block can't end at its start of day. */
    fun withEnd(draft: BlockDraft, end: Int): BlockDraft = draft.copy(end = if (end == 0) DAY_END else end)

    fun toggleDay(draft: BlockDraft, day: Int): BlockDraft =
        draft.copy(days = if (day in draft.days) draft.days - day else (draft.days + day).sorted())

    /**
     * The first reason the draft can't be saved, in the README's order, or null. An overlap names the
     * first clashing block and every selected day with a clash; the group's own copies never clash.
     */
    fun validate(draft: BlockDraft, blocks: List<Block>): String? {
        if (draft.title.isBlank()) return "Give the block a title."
        if (draft.end <= draft.start) return "End time must be after the start time."
        if (draft.days.isEmpty()) return "Pick at least one day."
        val clashes = blocks.filter {
            it.groupId != draft.groupId && it.day in draft.days && it.start < draft.end && draft.start < it.end
        }
        val first = clashes.firstOrNull() ?: return null
        val days = clashes.map { it.day }.distinct().sorted().joinToString(", ", transform = TimeFormat::day)
        return "Overlaps “${first.title}” (${TimeFormat.shortTime(first.start)} – ${TimeFormat.shortTime(first.end)}) on $days."
    }

    /**
     * Writes a valid draft as one block per selected day, all sharing the group, title, times and
     * sub-routines (trimmed, empty ones dropped). A day that already had a copy keeps its block id,
     * so editing the block on now is not a block change (no tone, SPEC §3.1). A deselected day's
     * copy is removed.
     */
    fun save(blocks: List<Block>, draft: BlockDraft, newId: () -> String): List<Block> {
        val groupId = draft.groupId ?: newId()
        val oldIds = blocks.filter { it.groupId == draft.groupId }.associate { it.day to it.id }
        val title = draft.title.trim()
        val subs = draft.subs.map(String::trim).filter(String::isNotEmpty)
        return blocks.filter { it.groupId != draft.groupId } +
            draft.days.map { day -> Block(oldIds[day] ?: newId(), groupId, day, draft.start, draft.end, title, subs) }
    }

    /** Delete removes the whole group, on every day. */
    fun deleteGroup(blocks: List<Block>, groupId: String): List<Block> = blocks.filter { it.groupId != groupId }
}
