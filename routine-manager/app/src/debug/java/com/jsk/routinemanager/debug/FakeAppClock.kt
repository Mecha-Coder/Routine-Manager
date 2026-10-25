package com.jsk.routinemanager.debug

import com.jsk.routinemanager.domain.dayIndex
import com.jsk.routinemanager.time.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

/**
 * The debug screen's clock: the prototype's `simNow()` (SPEC §3.6). Live, it is the phone's clock.
 * Once moved, it runs on from [anchor][Sim.anchor] at [speed] × real time (0 = paused).
 *
 * @param systemNow the phone's local time, used while live.
 * @param realMillis a monotonic real-time clock in milliseconds.
 */
class FakeAppClock(
    private val systemNow: () -> LocalDateTime,
    private val realMillis: () -> Long,
) : AppClock {

    data class Sim(val anchor: LocalDateTime, val realAnchor: Long, val speed: Double)

    /** One immutable value, so the ticker's thread never sees half a change. */
    private val state = MutableStateFlow<Sim?>(null)

    /** Null while live. The debug screen shows the mode from it. */
    val sim: StateFlow<Sim?> = state.asStateFlow()

    /** Emits after every change, so the minute ticker shows the new time at once. */
    val changes: Flow<Unit> = state.drop(1).map { }

    override val speed: Double get() = state.value?.speed ?: 1.0

    override fun now(): LocalDateTime {
        val s = state.value ?: return systemNow()
        return s.anchor.plusNanos(((realMillis() - s.realAnchor) * s.speed * 1_000_000).toLong())
    }

    fun goLive() {
        state.value = null
    }

    /** Pause (0), 1×, 60× or 600× from the current time. Leaves live mode, as in the prototype. */
    fun setSpeed(speed: Double) = runFrom(now(), speed)

    /** The same time of day on [day] (Mon = 0) of the current week. */
    fun jumpToDay(day: Int) {
        val now = now()
        runFrom(now.plusDays((day - dayIndex(now.toLocalDate())).toLong()), speed)
    }

    /** [minute] of the current day, keeping the seconds as the prototype's slider does. */
    fun setMinuteOfDay(minute: Int) {
        val now = now()
        runFrom(now.toLocalDate().atStartOfDay().plusMinutes(minute.toLong()).plusSeconds(now.second.toLong()), speed)
    }

    private fun runFrom(time: LocalDateTime, speed: Double) {
        state.value = Sim(time, realMillis(), speed)
    }
}
