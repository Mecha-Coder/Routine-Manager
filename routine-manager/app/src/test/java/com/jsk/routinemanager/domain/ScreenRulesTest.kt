package com.jsk.routinemanager.domain

import com.jsk.routinemanager.data.SampleData
import com.jsk.routinemanager.data.Theme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * The phone sits on a stand all day and night (SPEC D6, D7, D9). Keeping the screen on when the
 * battery is nearly empty would drain it; dimming at the wrong time would leave the owner with a
 * dark screen in the middle of the day.
 */
class ScreenRulesTest {

    private val blocks = SampleData.create().blocks

    private fun mon(h: Int, m: Int) = RoutineResolver.resolve(blocks, LocalDateTime.of(2026, 10, 19, h, m))
    private fun sat(h: Int, m: Int) = RoutineResolver.resolve(blocks, LocalDateTime.of(2026, 10, 24, h, m))

    @Test
    fun chargingKeepsTheScreenOnEvenWithANearlyEmptyBattery() {
        assertTrue(ScreenRules.keepScreenOn(charging = true, batteryPercent = 14))
        assertTrue(ScreenRules.keepScreenOn(charging = true, batteryPercent = 15))
    }

    @Test
    fun onBatteryTheScreenStaysOnAt15Percent() {
        assertTrue(ScreenRules.keepScreenOn(charging = false, batteryPercent = 15))
    }

    @Test
    fun onBatteryBelow15PercentTheScreenMayTimeOutToSaveTheLastCharge() {
        assertFalse(ScreenRules.keepScreenOn(charging = false, batteryPercent = 14))
    }

    @Test
    fun bedtimeBeforeTheFirstAndAfterTheLastBlockDims() {
        assertTrue(ScreenRules.dim(mon(5, 30), brightened = false))
        assertTrue(ScreenRules.dim(mon(23, 30), brightened = false))
    }

    @Test
    fun freeTimeBetweenTwoBlocksIsDaytimeSoItDoesNotDim() {
        val freeTime = sat(12, 30)
        assertTrue((freeTime as RoutineState.Running).isFreeTime)
        assertFalse(ScreenRules.dim(freeTime, brightened = false))
    }

    @Test
    fun aRunningBlockDoesNotDim() {
        assertFalse(ScreenRules.dim(mon(9, 30), brightened = false))
    }

    @Test
    fun aTouchKeepsTheBedtimeScreenBrightUntilItsTimerEnds() {
        assertFalse(ScreenRules.dim(mon(23, 30), brightened = true))
    }

    @Test
    fun bedtimeForcesTheDarkPaletteSoALightThemeNeverGlaresAtNight() {
        assertTrue(ScreenRules.darkPalette(Theme.LIGHT, mon(23, 30)))
        assertTrue(ScreenRules.darkPalette(Theme.DARK, mon(23, 30)))
    }

    @Test
    fun outsideBedtimeTheChosenThemeApplies() {
        assertFalse(ScreenRules.darkPalette(Theme.LIGHT, sat(12, 30)))
        assertFalse(ScreenRules.darkPalette(Theme.LIGHT, mon(9, 30)))
        assertTrue(ScreenRules.darkPalette(Theme.DARK, mon(9, 30)))
    }
}
