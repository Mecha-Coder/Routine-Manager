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

    /** A timetable block's range: `6:00am-11:30am`. A block ending at 1440 ends at `12:00am`. */
    fun blockRange(start: Int, end: Int): String = "${lowerTime(start)}-${lowerTime(end)}"

    private fun lowerTime(minuteOfDay: Int): String {
        val h = minuteOfDay / 60 % 24
        return "${hour12(h)}:${pad2(minuteOfDay % 60)}${if (h < 12) "am" else "pm"}"
    }

    private fun hour12(h: Int) = if (h % 12 == 0) 12 else h % 12

    /** The timetable's hour labels: `12 AM`, `7 AM`, `12 PM`; hour 24 is `12 AM` again. */
    fun hour(hour: Int): String = "${hour12(hour % 24)} ${if (hour % 24 < 12) "AM" else "PM"}"

    /** `7:00 AM`, no leading zero (the prototype's `lbl`); 1440 is `12:00 AM`. */
    fun time(minuteOfDay: Int): String {
        val h = minuteOfDay / 60 % 24
        return "${hour12(h)}:${pad2(minuteOfDay % 60)} ${if (h < 12) "AM" else "PM"}"
    }

    /** The create gesture's ghost label: `7:00 AM – 8:00 AM`. */
    fun ghostRange(start: Int, end: Int): String = "${time(start)} – ${time(end)}"

    /** `Mon` for day index 0. */
    fun day(dayIndex: Int): String = days[dayIndex]

    /** `19 Oct`: no leading zero, no year. */
    fun date(date: LocalDate): String = "${date.dayOfMonth} ${months[date.monthValue - 1]}"
}
