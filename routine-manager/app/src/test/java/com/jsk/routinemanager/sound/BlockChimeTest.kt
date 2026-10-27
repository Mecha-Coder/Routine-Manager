package com.jsk.routinemanager.sound

import com.jsk.routinemanager.domain.RoutineState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SPEC §3.1: the tone marks the start of a real block the user is watching, 350 ms after it starts,
 * and the start of Bedtime, so the day's last block ends with a sound. It must never sound for Free
 * time, for the state the app finds when it becomes visible, or for a block that was current only
 * for an instant.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BlockChimeTest {

    private fun block(id: String) = RoutineState.Running(
        key = id, isFreeTime = false, title = id, start = 0, end = 60, subs = emptyList(), next = "-",
    )

    private fun freeTime(afterId: String) = RoutineState.Running(
        key = "gap:$afterId", isFreeTime = true, title = "Free time", start = 0, end = 60, subs = emptyList(), next = "-",
    )

    /** A chime whose plays are recorded as virtual times (ms). */
    private fun TestScope.chime(plays: MutableList<Long>) = BlockChime(this) { plays += currentTime }

    @Test
    fun theFirstRoutineIsTheBaselineSoOpeningTheAppInsideABlockIsSilent() = runTest {
        val plays = mutableListOf<Long>()
        val chime = chime(plays)
        chime.onRoutine(block("work"))
        advanceUntilIdle()
        assertEquals(emptyList<Long>(), plays)
    }

    @Test
    fun aChangeToARealBlockPlaysAfter350ms() = runTest {
        val plays = mutableListOf<Long>()
        val chime = chime(plays)
        chime.onRoutine(block("commute"))
        chime.onRoutine(block("work"))
        advanceTimeBy(CHIME_DELAY_MS - 1)
        runCurrent()
        assertEquals("not before 350 ms", emptyList<Long>(), plays)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(CHIME_DELAY_MS), plays)
    }

    @Test
    fun minuteTicksInsideTheSameBlockNeverPlay() = runTest {
        val plays = mutableListOf<Long>()
        val chime = chime(plays)
        repeat(5) { chime.onRoutine(block("work")) }
        advanceUntilIdle()
        assertEquals(emptyList<Long>(), plays)
    }

    @Test
    fun changesToFreeTimeAreSilent() = runTest {
        // Free time is a pause inside the day, not a new block: a tone there would be noise.
        val plays = mutableListOf<Long>()
        val chime = chime(plays)
        chime.onRoutine(block("lunch"))
        chime.onRoutine(freeTime("lunch"))
        advanceUntilIdle()
        assertEquals(emptyList<Long>(), plays)
    }

    @Test
    fun theEndOfTheLastBlockPlaysWhenBedtimeStarts() = runTest {
        // Owner decision (27 Oct 2026): the day's last block ends with a sound, like every other
        // block. With Bedtime silent, the evening's last block just ran out without a cue.
        val plays = mutableListOf<Long>()
        val chime = chime(plays)
        chime.onRoutine(block("reading"))
        chime.onRoutine(RoutineState.Bedtime)
        advanceUntilIdle()
        assertEquals(listOf(CHIME_DELAY_MS), plays)
    }

    @Test
    fun openingTheAppAtBedtimeIsSilent() = runTest {
        // Becoming visible is not a change, also at night: the baseline rule covers Bedtime too.
        val plays = mutableListOf<Long>()
        val chime = chime(plays)
        chime.onRoutine(RoutineState.Bedtime)
        repeat(3) { chime.onRoutine(RoutineState.Bedtime) } // minute ticks through the night
        advanceUntilIdle()
        assertEquals(emptyList<Long>(), plays)
    }

    @Test
    fun aSecondChangeWithin350msPlaysOnlyForTheLastBlock() = runTest {
        val plays = mutableListOf<Long>()
        val chime = chime(plays)
        chime.onRoutine(block("a"))
        chime.onRoutine(block("b")) // at 0
        advanceTimeBy(200)
        chime.onRoutine(block("c")) // at 200: b's tone is cancelled
        advanceUntilIdle()
        assertEquals(listOf(200 + CHIME_DELAY_MS), plays)
    }

    @Test
    fun aBlockThatTurnsIntoFreeTimeWithin350msIsSilent() = runTest {
        val plays = mutableListOf<Long>()
        val chime = chime(plays)
        chime.onRoutine(block("a"))
        chime.onRoutine(block("b"))
        advanceTimeBy(100)
        chime.onRoutine(freeTime("b"))
        advanceUntilIdle()
        assertEquals(emptyList<Long>(), plays)
    }

    @Test
    fun afterRebaselineTheNextRoutineIsSilentSoMissedStartsAndResetsDontPlay() = runTest {
        val plays = mutableListOf<Long>()
        val chime = chime(plays)
        chime.onRoutine(block("a"))
        chime.onRoutine(block("b"))
        chime.rebaseline() // hidden before b's tone: it must not play later
        advanceUntilIdle()
        chime.onRoutine(block("c")) // back on screen in another block, or new ids after a reset
        advanceUntilIdle()
        assertEquals(emptyList<Long>(), plays)
        chime.onRoutine(block("d")) // a real change after the new baseline plays again
        advanceUntilIdle()
        assertEquals(1, plays.size)
    }
}
