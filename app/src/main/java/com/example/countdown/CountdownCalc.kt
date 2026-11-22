package com.example.countdown

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class CountMode { EVERYDAY, WEEKDAYS }

/** Pure date math, kept free of Android classes so it can be unit-tested. */
object CountdownCalc {

    /**
     * Days remaining from [today] to [target].
     *
     * Days are counted in the range (today, target] — i.e. today is not counted unless
     * [includeToday] is set (and, in WEEKDAYS mode, only if today is itself a weekday).
     * Returns 0 once the target date has been reached or passed.
     */
    fun daysLeft(
        today: LocalDate,
        target: LocalDate,
        mode: CountMode,
        includeToday: Boolean = false,
    ): Int {
        if (!target.isAfter(today)) return 0
        return when (mode) {
            CountMode.EVERYDAY -> {
                val d = ChronoUnit.DAYS.between(today, target).toInt()
                d + if (includeToday) 1 else 0
            }
            CountMode.WEEKDAYS -> {
                var n = 0
                var day = today.plusDays(1)
                while (!day.isAfter(target)) {
                    if (isWeekday(day)) n++
                    day = day.plusDays(1)
                }
                n + if (includeToday && isWeekday(today)) 1 else 0
            }
        }
    }

    fun isWeekday(d: LocalDate): Boolean =
        d.dayOfWeek != DayOfWeek.SATURDAY && d.dayOfWeek != DayOfWeek.SUNDAY
}
