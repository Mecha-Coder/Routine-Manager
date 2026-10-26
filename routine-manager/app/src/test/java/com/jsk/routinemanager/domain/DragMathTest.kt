package com.jsk.routinemanager.domain

import com.jsk.routinemanager.data.OutlineItem
import com.jsk.routinemanager.domain.DragMath.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DragMathTest {

    private fun item(id: String, depth: Int = 0) = OutlineItem(id, id, depth)

    // p1, its children c1 and c2, then p2.
    private val outline = listOf(item("p1"), item("c1", 1), item("c2", 1), item("p2"))

    // Gesture decision (README › Drag handle gestures). Slop 8.

    @Test
    fun sidewaysMoveBeforeTheHoldIndents() {
        assertEquals(Decision.INDENT, DragMath.decide(dx = 9f, dy = 3f, slop = 8f))
        assertEquals(Decision.INDENT, DragMath.decide(dx = -9f, dy = 3f, slop = 8f))
    }

    @Test
    fun verticalMoveFirstLetsTheListScroll() {
        // Otherwise a swipe that starts on a handle could never scroll the list.
        assertEquals(Decision.SCROLL, DragMath.decide(dx = 2f, dy = 9f, slop = 8f))
        // A diagonal that is more vertical than sideways also scrolls.
        assertEquals(Decision.SCROLL, DragMath.decide(dx = 9f, dy = -10f, slop = 8f))
    }

    @Test
    fun aSmallWobbleDecidesNothingSoHoldingStillStillReorders() {
        // A finger never stays perfectly still; under 8 dp the hold must still lift the item.
        assertNull(DragMath.decide(dx = 8f, dy = -8f, slop = 8f))
    }

    // Indent mode.

    @Test
    fun theFirstItemCannotIndentBecauseNoItemIsAboveIt() {
        assertEquals(0, DragMath.maxIndentDepth(outline, 0))
    }

    @Test
    fun maxDepthIsOneDeeperThanTheItemAboveButNeverPastOneLevel() {
        // max depth = min(prevDepth + 1, 1): under a parent it can become a child; under a child it stays a child.
        assertEquals(1, DragMath.maxIndentDepth(outline, 1))
        assertEquals(1, DragMath.maxIndentDepth(outline, 2))
        assertEquals(1, DragMath.maxIndentDepth(outline, 3))
    }

    @Test
    fun theRowFollowsTheFingerInsideTheAllowedRange() {
        assertEquals(10f, DragMath.indentOffset(10f, depth = 0, maxDepth = 1, level = 24f))
        assertEquals(-20f, DragMath.indentOffset(-20f, depth = 1, maxDepth = 1, level = 24f))
    }

    @Test
    fun pastTheRangeTheRowMovesAQuarterAsFarAsTheFinger() {
        // The rubber band shows the limit without hiding the gesture.
        assertEquals(24f + 10f, DragMath.indentOffset(64f, depth = 0, maxDepth = 1, level = 24f))
        assertEquals(-10f, DragMath.indentOffset(-40f, depth = 0, maxDepth = 1, level = 24f))
        // The first item has no room either way.
        assertEquals(5f, DragMath.indentOffset(20f, depth = 0, maxDepth = 0, level = 24f))
    }

    @Test
    fun releaseSnapsToTheNearestAllowedLevel() {
        assertEquals(0, DragMath.indentDepth(11f, depth = 0, maxDepth = 1, level = 24f))
        assertEquals(1, DragMath.indentDepth(12f, depth = 0, maxDepth = 1, level = 24f))
        assertEquals(0, DragMath.indentDepth(-13f, depth = 1, maxDepth = 1, level = 24f))
        // A rubber-banded pull never snaps past the range.
        assertEquals(1, DragMath.indentDepth(40f, depth = 0, maxDepth = 1, level = 24f))
        assertEquals(0, DragMath.indentDepth(30f, depth = 0, maxDepth = 0, level = 24f))
    }

    // Reorder mode. Rows 44 tall, the first at 0: tops 0, 44, 88, 132; middles 22, 66, 110, 154.

    private val tops = listOf(0f, 44f, 88f, 132f)
    private val heights = listOf(44f, 44f, 44f, 44f)

    @Test
    fun dropIndexCountsTheRowsWhoseMiddleTheGhostHasPassed() {
        // Dragging p2 (row 3, alone) upward: index into the list without it (p1, c1, c2).
        assertEquals(3, DragMath.dropIndex(tops, heights, from = 3, end = 4, ghostTop = 132f))
        assertEquals(2, DragMath.dropIndex(tops, heights, from = 3, end = 4, ghostTop = 87f))
        assertEquals(0, DragMath.dropIndex(tops, heights, from = 3, end = 4, ghostTop = -30f))
    }

    @Test
    fun aRowAboveCountsAtItsMiddleAndARowBelowOnlyPastIt() {
        // As in the prototype: an exact tie keeps the item where it is.
        assertEquals(1, DragMath.dropIndex(tops, heights, from = 1, end = 2, ghostTop = 0f))
        assertEquals(1, DragMath.dropIndex(tops, heights, from = 1, end = 2, ghostTop = 88f))
        assertEquals(2, DragMath.dropIndex(tops, heights, from = 1, end = 2, ghostTop = 89f))
    }

    @Test
    fun aParentsChildrenAreNotDropTargetsBecauseTheyMoveWithIt() {
        // p1 with c1, c2 (rows 0..2): only p2 is left to pass.
        assertEquals(0, DragMath.dropIndex(tops, heights, from = 0, end = 3, ghostTop = 100f))
        assertEquals(1, DragMath.dropIndex(tops, heights, from = 0, end = 3, ghostTop = 133f))
    }

    @Test
    fun dropDepthIsTheLeastOfOriginalDepthOneBelowTheItemAboveAndOne() {
        // drop depth = min(originalDepth, prevDepth + 1, 1).
        // A child dropped at the top has no parent: it becomes a parent.
        assertEquals(0, DragMath.dropDepth(outline, from = 2, end = 3, index = 0))
        // A child dropped below another child stays a child.
        assertEquals(1, DragMath.dropDepth(outline, from = 2, end = 3, index = 2))
        // A child dropped after the last parent becomes that parent's child.
        assertEquals(1, DragMath.dropDepth(outline, from = 1, end = 2, index = 3))
        // A parent dropped below a child stays a parent: a drop never makes an item deeper.
        assertEquals(0, DragMath.dropDepth(outline, from = 3, end = 4, index = 2))
    }

    @Test
    fun theGhostLandsWhereTheGapOpens() {
        // Upward: at the top of the row it lands before.
        assertEquals(44f, DragMath.gapTop(tops, heights, from = 3, end = 4, index = 1))
        // Not moved: its own top.
        assertEquals(132f, DragMath.gapTop(tops, heights, from = 3, end = 4, index = 3))
        // Downward with p1's subtree (rows 0..2) past p2: p2 moves up into its place, the gap is below p2.
        assertEquals(44f, DragMath.gapTop(tops, heights, from = 0, end = 3, index = 1))
    }

    @Test
    fun onlyTheRowsBetweenTheItemAndTheGapSlideByTheDraggedHeight() {
        // p2 dragged above c1 (index 1): c1 and c2 slide down by one row; p1 stays.
        assertEquals(listOf(0f, 44f, 44f), (0..2).map { DragMath.shift(it, from = 3, end = 4, index = 1, height = 44f) })
        // p1's subtree (132 tall) dragged below p2: p2 slides up by the whole subtree.
        assertEquals(-132f, DragMath.shift(3, from = 0, end = 3, index = 1, height = 132f))
        // Rows of the dragged item itself never slide; they are hidden under the ghost.
        assertEquals(0f, DragMath.shift(1, from = 0, end = 3, index = 1, height = 132f))
    }
}
