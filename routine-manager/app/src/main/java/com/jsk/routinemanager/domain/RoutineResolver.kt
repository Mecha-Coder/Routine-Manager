package com.jsk.routinemanager.domain

import com.jsk.routinemanager.data.Block
import java.time.LocalDate
import java.time.LocalDateTime

/** What the routine panel shows now. */
sealed interface RoutineState {
    /**
     * Changes exactly when the routine changes: the block id, `gap:<previous block id>` for Free
     * time, or `bed`. The block-change tone and the Bedtime dim watch it (SPEC §3.1, §3.2).
     */
    val key: String

    /** A real block, or the Free-time gap between two blocks ([isFreeTime]; no tone, no subs). */
    data class Running(
        override val key: String,
        val isFreeTime: Boolean,
        val title: String,
        /** Minutes from midnight. */
        val start: Int,
        /** Minutes from midnight; 1440 = midnight at the end of the day. */
        val end: Int,
        val subs: List<String>,
        /** The next block's title, or `-` when nothing follows today. */
        val next: String,
    ) : RoutineState

    /** Before the day's first block, after its last, or all day on a day without blocks. */
    data object Bedtime : RoutineState {
        override val key = "bed"
    }
}

const val FREE_TIME_TITLE = "Free time"
const val NO_NEXT = "-"

/** Port of the prototype's `routine()` (README › Behaviour and system › Routine resolution). */
object RoutineResolver {

    fun resolve(blocks: List<Block>, now: LocalDateTime): RoutineState =
        resolve(blocks, dayIndex(now.toLocalDate()), minuteOfDay(now))

    /** [minuteOfDay] keeps the seconds, so LEFT = ceil(end − now) is right between minute ticks. */
    fun resolve(blocks: List<Block>, day: Int, minuteOfDay: Double): RoutineState {
        val today = blocks.filter { it.day == day }.sortedBy { it.start }
        val current = today.firstOrNull { it.start <= minuteOfDay && minuteOfDay < it.end }
        if (current != null) {
            return RoutineState.Running(
                key = current.id,
                isFreeTime = false,
                title = current.title,
                start = current.start,
                end = current.end,
                subs = current.subs,
                next = today.firstOrNull { it.start >= current.end }?.title ?: NO_NEXT,
            )
        }
        val previous = today.lastOrNull { it.end <= minuteOfDay }
        val next = today.firstOrNull { it.start > minuteOfDay }
        if (previous != null && next != null) {
            return RoutineState.Running(
                key = "gap:${previous.id}",
                isFreeTime = true,
                title = FREE_TIME_TITLE,
                start = previous.end,
                end = next.start,
                subs = emptyList(),
                next = next.title,
            )
        }
        return RoutineState.Bedtime
    }
}

/** Mon = 0 … Sun = 6, the index [Block.day] uses. */
fun dayIndex(date: LocalDate): Int = date.dayOfWeek.value - 1

/** Minutes since midnight, with the seconds as a fraction. */
fun minuteOfDay(time: LocalDateTime): Double = time.toLocalTime().toNanoOfDay() / 60e9
