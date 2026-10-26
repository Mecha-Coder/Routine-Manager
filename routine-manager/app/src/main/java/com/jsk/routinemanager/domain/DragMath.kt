package com.jsk.routinemanager.domain

import com.jsk.routinemanager.data.OutlineItem
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The drag handle's maths (README › Drag handle gestures), ported from the prototype's `dDown`,
 * `dMove`, `dCalc`, `dGapTop` and `dUp`. Distances are in any one unit; the UI passes pixels.
 *
 * Row positions are the rows' tops and heights in list order, measured without any drag offset.
 * A dragged item covers the rows `from until end`: the item and its children.
 */
object DragMath {

    /** Holding the handle this long without moving lifts the item for reordering. */
    const val HOLD_MILLIS = 280L

    /** Movement, in dp, that decides between indent and scroll before the hold ends. */
    const val DECIDE_DP = 8f

    /** One indent level, in dp: the children's indent. */
    const val LEVEL_DP = 24f

    /** Past the allowed indent range the row follows the finger at this rate. */
    private const val RUBBER_BAND = 0.25f

    enum class Decision { INDENT, SCROLL }

    /**
     * Before the hold ends: more than [slop] sideways, and more sideways than vertical, is an indent;
     * more than [slop] vertically first lets the list scroll; less than that keeps waiting, so a
     * still finger becomes a reorder when the hold ends.
     */
    fun decide(dx: Float, dy: Float, slop: Float): Decision? = when {
        abs(dx) > slop && abs(dx) > abs(dy) -> Decision.INDENT
        abs(dy) > slop -> Decision.SCROLL
        else -> null
    }

    /** An item can become a child only of the item above it, and there is one level of children. */
    fun maxIndentDepth(items: List<OutlineItem>, index: Int): Int =
        if (index == 0) 0 else minOf(items[index - 1].depth + 1, OutlineOps.MAX_DEPTH)

    /**
     * Where the indenting row is drawn for a finger [dx] from where it went down: with the finger
     * between depth 0 and [maxDepth], and only a quarter of the way past either end.
     */
    fun indentOffset(dx: Float, depth: Int, maxDepth: Int, level: Float): Float {
        val min = -depth * level
        val max = (maxDepth - depth) * level
        return when {
            dx < min -> min + (dx - min) * RUBBER_BAND
            dx > max -> max + (dx - max) * RUBBER_BAND
            else -> dx
        }
    }

    /** On release the row snaps to the nearest level within the allowed range. */
    fun indentDepth(offset: Float, depth: Int, maxDepth: Int, level: Float): Int =
        (depth + (offset / level).roundToInt()).coerceIn(0, maxDepth)

    /**
     * Where the dragged item would land, as an index into the list without it: the number of other
     * rows whose middle the ghost's middle has passed. The ghost is as tall as the item's own row.
     */
    fun dropIndex(tops: List<Float>, heights: List<Float>, from: Int, end: Int, ghostTop: Float): Int {
        val middle = ghostTop + heights[from] / 2
        var index = 0
        for (i in tops.indices) {
            if (i in from until end) continue
            val rowMiddle = tops[i] + heights[i] / 2
            // A row above the item counts once the ghost reaches its middle, a row below once it passes it.
            if (if (i < from) middle >= rowMiddle else middle > rowMiddle) index++
        }
        return index
    }

    /**
     * The dropped item's depth: never deeper than it was, at most one deeper than the item above
     * it, and one level of children. At the top it becomes a parent.
     */
    fun dropDepth(items: List<OutlineItem>, from: Int, end: Int, index: Int): Int {
        val rest = items.filterIndexed { k, _ -> k !in from until end }
        val previous = rest.getOrNull(index - 1)?.depth ?: -1
        return minOf(items[from].depth, previous + 1, OutlineOps.MAX_DEPTH).coerceAtLeast(0)
    }

    /** The top of the gap at drop [index], where the ghost lands. */
    fun gapTop(tops: List<Float>, heights: List<Float>, from: Int, end: Int, index: Int): Float {
        if (index <= from) return tops[index]
        // Below: the rows that move up into the item's place, from the item's own top.
        var top = tops[from]
        for (i in end until minOf(end + index - from, tops.size)) top += heights[i]
        return top
    }

    /**
     * How far the row at [row] slides to open the gap at drop [index]: rows between the item and
     * the gap move by the dragged rows' total [height], down when the gap is above, up when below.
     */
    fun shift(row: Int, from: Int, end: Int, index: Int, height: Float): Float = when {
        row < from -> if (row >= index) height else 0f
        row >= end -> if (row - end + from < index) -height else 0f
        else -> 0f
    }
}
