package com.jsk.routinemanager.time

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

/**
 * The dashboard redraws only when this emits (README › Always-on display: once per minute, on the
 * minute), so a late tick shows the wrong time and an extra tick wastes a frame.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MinuteTickerTest {

    /** A clock that runs on the test's virtual time, like the debug fake clock. */
    private class TestClock(private var base: LocalDateTime, private val realMs: () -> Long) : AppClock {
        private var baseReal = realMs()
        override var speed = 1.0
            set(value) {
                base = now()
                baseReal = realMs()
                field = value
            }

        override fun now(): LocalDateTime = base.plusNanos(((realMs() - baseReal) * speed * 1_000_000).toLong())

        fun jumpTo(time: LocalDateTime) {
            base = time
            baseReal = realMs()
        }
    }

    private class Run(val clock: TestClock, val changes: MutableSharedFlow<Unit>, val seen: MutableList<Pair<Long, LocalDateTime>>)

    private fun TestScope.start(at: LocalDateTime, speed: Double = 1.0): Run {
        val clock = TestClock(at) { testScheduler.currentTime }.also { it.speed = speed }
        val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val seen = mutableListOf<Pair<Long, LocalDateTime>>()
        backgroundScope.launch { minuteTicks(clock, changes).collect { seen += testScheduler.currentTime to it } }
        runCurrent()
        return Run(clock, changes, seen)
    }

    private fun TestScope.advanceTo(ms: Long) {
        advanceTimeBy(ms - testScheduler.currentTime)
        runCurrent()
    }

    private fun t(h: Int, m: Int, s: Int = 0) = LocalDateTime.of(2026, 10, 19, h, m, s)

    @Test
    fun emitsNowThenExactlyAtEachMinuteBoundary() = runTest {
        val run = start(t(7, 5, 30))
        advanceTo(90_000)

        assertEquals(
            listOf(0L to t(7, 5, 30), 30_000L to t(7, 6), 90_000L to t(7, 7)),
            run.seen,
        )
    }

    @Test
    fun aTimeChangeShowsAtOnceAndTheNextTickFollowsTheNewTime() = runTest {
        val run = start(t(7, 5, 30))
        advanceTo(10_000)
        // Time zone change: the clock jumps an hour while the old wait still has 20 s to go.
        run.clock.jumpTo(t(9, 5, 50))
        run.changes.tryEmit(Unit)
        runCurrent()
        advanceTo(30_000)

        assertEquals(
            listOf(0L to t(7, 5, 30), 10_000L to t(9, 5, 50), 20_000L to t(9, 6)),
            run.seen,
        )
    }

    @Test
    fun aChangeWithinTheSameMinuteIsDroppedSoItCausesNoExtraRedraw() = runTest {
        val run = start(t(7, 5, 30))
        advanceTo(10_000)
        run.changes.tryEmit(Unit) // e.g. a network time sync that moves the clock by a few ms
        advanceTo(30_000)

        assertEquals(listOf(0L, 30_000L), run.seen.map { it.first })
    }

    @Test
    fun aFastDebugClockTicksOncePerClockMinute() = runTest {
        val run = start(t(7, 5), speed = 60.0)
        advanceTo(3_000)

        assertEquals(
            listOf(0L to t(7, 5), 1_000L to t(7, 6), 2_000L to t(7, 7), 3_000L to t(7, 8)),
            run.seen,
        )
    }

    @Test
    fun aPausedDebugClockEmitsOnlyWhenChanged() = runTest {
        val run = start(t(7, 5), speed = 0.0)
        advanceTo(600_000)
        assertEquals(1, run.seen.size)

        run.clock.jumpTo(t(12, 59))
        run.changes.tryEmit(Unit)
        runCurrent()
        assertEquals(t(12, 59), run.seen.last().second)
    }

    @Test
    fun theWaitIsNeverZeroSoTheLoopCannotSpin() {
        assertEquals(1L, realMillisToNextMinute(LocalDateTime.of(2026, 10, 19, 7, 5, 59, 999_999_999), 1.0))
        assertEquals(60_000L, realMillisToNextMinute(t(7, 5), 1.0))
        assertEquals(100L, realMillisToNextMinute(t(7, 5), 600.0))
    }
}
