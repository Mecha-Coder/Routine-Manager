package com.jsk.routinemanager.time

import java.time.LocalDateTime

/** The app's notion of "now" (ARCHITECTURE §5). Debug builds can swap in a fake clock (M3). */
interface AppClock {
    /** Local date-time in the phone's time zone. */
    fun now(): LocalDateTime

    /** Clock time per real time: 1 for the real clock; a debug clock can run faster, or 0 = paused. */
    val speed: Double
}

object SystemAppClock : AppClock {
    override fun now(): LocalDateTime = LocalDateTime.now()
    override val speed = 1.0
}
