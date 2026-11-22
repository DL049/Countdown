package com.example.countdown

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class CountMode { EVERYDAY, WEEKDAYS }

/** A named range of dates (inclusive) that can be skipped when counting. */
data class Holiday(val id: String, val name: String, val start: LocalDate, val end: LocalDate) {
    operator fun contains(d: LocalDate) = !d.isBefore(start) && !d.isAfter(end)
    val lengthDays: Int get() = ChronoUnit.DAYS.between(start, end).toInt() + 1
}

data class CountdownEvent(
    val id: String,
    val title: String,
    val target: LocalDate,
    val mode: CountMode,
    val includeToday: Boolean,
    /** Ids of the holidays that must NOT be counted for this event. */
    val holidayIds: Set<String>,
)

data class EventSummary(val left: Int, val skippedByHolidays: Int, val reached: Boolean)

/** Pure date math, kept free of Android classes so it can be unit-tested. */
object CountdownCalc {

    fun isWeekday(d: LocalDate): Boolean =
        d.dayOfWeek != DayOfWeek.SATURDAY && d.dayOfWeek != DayOfWeek.SUNDAY

    private fun counts(day: LocalDate, mode: CountMode, holidays: List<Holiday>): Boolean =
        (mode == CountMode.EVERYDAY || isWeekday(day)) && holidays.none { day in it }

    /**
     * Days remaining from [today] to [target], counting days in (today, target]
     * (plus today when [includeToday]). In WEEKDAYS mode Saturdays/Sundays are skipped;
     * in both modes any day inside one of [holidays] is skipped.
     * Returns 0 once the target date has been reached or passed.
     */
    fun daysLeft(
        today: LocalDate,
        target: LocalDate,
        mode: CountMode,
        includeToday: Boolean = false,
        holidays: List<Holiday> = emptyList(),
    ): Int {
        if (!target.isAfter(today)) return 0
        var n = 0
        var day = if (includeToday) today else today.plusDays(1)
        while (!day.isAfter(target)) {
            if (counts(day, mode, holidays)) n++
            day = day.plusDays(1)
        }
        return n
    }

    fun summarize(
        e: CountdownEvent,
        allHolidays: List<Holiday>,
        today: LocalDate = LocalDate.now(),
    ): EventSummary {
        val skip = allHolidays.filter { it.id in e.holidayIds }
        val left = daysLeft(today, e.target, e.mode, e.includeToday, skip)
        val without = daysLeft(today, e.target, e.mode, e.includeToday)
        return EventSummary(left, without - left, !e.target.isAfter(today))
    }
}
