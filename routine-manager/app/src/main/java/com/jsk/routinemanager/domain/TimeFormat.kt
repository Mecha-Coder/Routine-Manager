package com.jsk.routinemanager.domain

import java.time.LocalDate
import kotlin.math.ceil

/** A 12-hour time split for display: [time] `07:05`, [amPm] `AM`. */
data class ClockText(val time: String, val amPm: String)

/** The README's text formats. Always 12-hour and English, whatever the phone's settings (SPEC D19). */
object TimeFormat {

    private val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    private fun pad2(n: Int) = n.toString().padStart(2, '0')

    /** `07:05` + `AM`, with a leading zero. 1440 (a block ending at midnight) is `12:00` `AM`. */
    fun clock(minuteOfDay: Int): ClockText {
        val h = minuteOfDay / 60 % 24
        val m = minuteOfDay % 60
        val h12 = if (h % 12 == 0) 12 else h % 12
        return ClockText("${pad2(h12)}:${pad2(m)}", if (h < 12) "AM" else "PM")
    }

    /** LEFT: whole minutes from now to [end], rounded up: `4hr 30min`, `2hr`, `33min`. */
    fun left(end: Int, minuteOfDay: Double): String = duration(ceil(end - minuteOfDay).toInt())

    private fun duration(minutes: Int): String {
        val m = minutes.coerceAtLeast(0)
        val h = m / 60
        val r = m % 60
        return when {
            h == 0 -> "${r}min"
            r == 0 -> "${h}hr"
            else -> "${h}hr ${r}min"
        }
    }

    /** `Mon` for day index 0. */
    fun day(dayIndex: Int): String = days[dayIndex]

    /** `19 Oct`: no leading zero, no year. */
    fun date(date: LocalDate): String = "${date.dayOfMonth} ${months[date.monthValue - 1]}"
}
