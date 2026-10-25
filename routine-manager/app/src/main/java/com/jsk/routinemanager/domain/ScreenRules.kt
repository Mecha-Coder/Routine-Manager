package com.jsk.routinemanager.domain

import com.jsk.routinemanager.data.Theme

/** Below this battery level, unless charging, the screen is no longer kept on (SPEC D9). */
const val LOW_BATTERY_PERCENT = 15

/**
 * The keep-screen-on and Bedtime decisions (SPEC §3.2, §3.3). ScreenPolicy only applies them to the
 * window (ARCHITECTURE §8).
 */
object ScreenRules {

    /**
     * Whether the visible app keeps the screen on. [charging] means plugged into AC, USB or a
     * wireless charger, even when the battery is full or held at a charge limit.
     */
    fun keepScreenOn(charging: Boolean, batteryPercent: Int): Boolean =
        charging || batteryPercent >= LOW_BATTERY_PERCENT

    /** Bedtime dims the screen unless a touch has brightened it. Free time between blocks is daytime. */
    fun dim(routine: RoutineState, brightened: Boolean): Boolean =
        routine is RoutineState.Bedtime && !brightened

    /** Bedtime uses the dark palette whatever the Theme setting says, also while brightened. */
    fun darkPalette(theme: Theme, routine: RoutineState): Boolean =
        theme == Theme.DARK || routine is RoutineState.Bedtime
}
