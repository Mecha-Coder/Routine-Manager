package com.jsk.routinemanager.debug

import com.jsk.routinemanager.time.minuteTicks
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

/**
 * The debug clock is how every time-based state is checked on the phone (SPEC §3.6, BUILD_PLAN ›
 * Test times). If it drifts, jumps to the wrong day or doesn't tell the ticker, the screenshots
 * show the wrong state and the check proves nothing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeAppClockTest {

    private var real = 0L
    private var system = LocalDateTime.of(2026, 10, 21, 7, 5, 30) // a Wednesday
    private val clock = FakeAppClock(systemNow = { system }, realMillis = { real })

    private fun mon(h: Int, m: Int, s: Int = 0) = LocalDateTime.of(2026, 10, 19, h, m, s)

    @Test
    fun liveIsThePhoneClockSoAnUntouchedDebugBuildBehavesLikeRelease() {
        assertEquals(system, clock.now())
        system = system.plusHours(1)
        assertEquals(system, clock.now())
        assertEquals(1.0, clock.speed, 0.0)
        assertNull(clock.sim.value)
    }

    @Test
    fun jumpingToADayKeepsTheTimeOfDayWithinTheSameMonToSunWeek() {
        clock.jumpToDay(0)
        assertEquals(mon(7, 5, 30), clock.now())
        clock.jumpToDay(6)
        assertEquals(LocalDateTime.of(2026, 10, 25, 7, 5, 30), clock.now())
    }

    @Test
    fun theSliderSetsTheMinuteButKeepsTheDayAndSeconds() {
        clock.jumpToDay(0)
        clock.setMinuteOfDay(12 * 60 + 59)
        assertEquals(mon(12, 59, 30), clock.now())
    }

    @Test
    fun aMovedClockRunsOnAtItsSpeed() {
        clock.jumpToDay(0) // leaves live at 1×, as the prototype does
        real += 1_000
        assertEquals(mon(7, 5, 31), clock.now())

        clock.setSpeed(600.0)
        real += 6_000 // 6 s real = 1 hour at 600×
        assertEquals(mon(8, 5, 31), clock.now())
        assertEquals(600.0, clock.speed, 0.0)
    }

    @Test
    fun pauseHoldsTheTimeSoAStateCanBeScreenshotAtAnExactMinute() {
        clock.jumpToDay(0)
        clock.setSpeed(0.0)
        real += 3_600_000
        assertEquals(mon(7, 5, 30), clock.now())
        // The ticker reads speed 0 as "wait for a change".
        assertEquals(0.0, clock.speed, 0.0)
    }

    @Test
    fun goingLiveReturnsToThePhoneClock() {
        clock.setSpeed(600.0)
        real += 60_000
        clock.goLive()
        assertEquals(system, clock.now())
        assertNull(clock.sim.value)
    }

    @Test
    fun theDashboardTickerShowsAJumpAtOnceNotAtTheNextMinute() = runTest {
        val seen = mutableListOf<LocalDateTime>()
        backgroundScope.launch { minuteTicks(clock, clock.changes).collect { seen += it } }
        runCurrent()

        clock.jumpToDay(0)
        clock.setMinuteOfDay(22 * 60 + 45)
        runCurrent()

        assertEquals(mon(22, 45, 30), seen.last())
    }
}
