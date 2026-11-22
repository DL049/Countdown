package com.example.countdown

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class Repeat { NONE, YEARLY }

enum class Phase { UPCOMING, TODAY, PAST }

/** A named range of dates (inclusive; a single day has start == end) that can be skipped. */
data class Holiday(val id: String, val name: String, val start: LocalDate, val end: LocalDate) {
    operator fun contains(d: LocalDate) = !d.isBefore(start) && !d.isAfter(end)
    val lengthDays: Int get() = ChronoUnit.DAYS.between(start, end).toInt() + 1
}

data class CountdownEvent(
    val id: String,
    val title: String,
    val target: LocalDate,
    /** Days of the week that count (all 7 = every day, Mon–Fri = school days, or any custom set). */
    val activeDays: Set<DayOfWeek>,
    val includeToday: Boolean,
    /** Ids of the holidays that must NOT be counted for this event. */
    val holidayIds: Set<String>,
    /** Creation date; the left end of the progress bar. */
    val start: LocalDate,
    val pinned: Boolean = false,
    val colorIdx: Int = 0,
    val category: String = "",
    val repeat: Repeat = Repeat.NONE,
    val notify: Boolean = true,
    /** Widget background image: darkness of the overlay (0–80) and whether text is white. */
    val scrimPercent: Int = 40,
    val lightText: Boolean = true,
)

data class EventSummary(
    val left: Int,
    val skippedByHolidays: Int,
    val phase: Phase,
    val elapsedDays: Int,
    val progress: Float,
    /** The date actually being counted to (next occurrence for yearly events). */
    val target: LocalDate,
)

/** Pure date math, kept free of Android classes so it can be unit-tested. */
object CountdownCalc {
    val ALL_DAYS: Set<DayOfWeek> = DayOfWeek.values().toSet()
    val WEEKDAYS: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
    )

    fun isWeekday(d: LocalDate): Boolean =
        d.dayOfWeek != DayOfWeek.SATURDAY && d.dayOfWeek != DayOfWeek.SUNDAY

    private fun counts(day: LocalDate, activeDays: Set<DayOfWeek>, holidays: List<Holiday>): Boolean =
        day.dayOfWeek in activeDays && holidays.none { day in it }

    /**
     * Days remaining from [today] to [target], counting days in (today, target]
     * (plus today when [includeToday]). Only days whose weekday is in [activeDays]
     * count, and any day inside one of [holidays] is skipped.
     * Returns 0 once the target date has been reached or passed.
     */
    fun daysLeft(
        today: LocalDate,
        target: LocalDate,
        activeDays: Set<DayOfWeek>,
        includeToday: Boolean = false,
        holidays: List<Holiday> = emptyList(),
    ): Int {
        if (!target.isAfter(today)) return 0
        var n = 0
        var day = if (includeToday) today else today.plusDays(1)
        while (!day.isAfter(target)) {
            if (counts(day, activeDays, holidays)) n++
            day = day.plusDays(1)
        }
        return n
    }

    /** The date being counted to: yearly events roll forward to their next occurrence. */
    fun effectiveTarget(e: CountdownEvent, today: LocalDate): LocalDate {
        if (e.repeat != Repeat.YEARLY) return e.target
        var k = 0L
        var t = e.target
        while (t.isBefore(today)) {
            k++
            t = e.target.plusYears(k)
        }
        return t
    }

    fun summarize(
        e: CountdownEvent,
        allHolidays: List<Holiday>,
        today: LocalDate = LocalDate.now(),
    ): EventSummary {
        val t = effectiveTarget(e, today)
        val skip = allHolidays.filter { it.id in e.holidayIds }
        val left = daysLeft(today, t, e.activeDays, e.includeToday, skip)
        val without = daysLeft(today, t, e.activeDays, e.includeToday)
        val phase = when {
            t.isAfter(today) -> Phase.UPCOMING
            t == today -> Phase.TODAY
            else -> Phase.PAST
        }
        val elapsed = if (phase == Phase.PAST) ChronoUnit.DAYS.between(t, today).toInt() else 0
        val from = if (e.repeat == Repeat.YEARLY) maxOf(e.start, t.minusYears(1)) else e.start
        val progress = if (phase != Phase.UPCOMING) {
            1f
        } else {
            val total = ChronoUnit.DAYS.between(from, t).coerceAtLeast(1)
            val done = ChronoUnit.DAYS.between(from, today).coerceIn(0, total)
            done.toFloat() / total
        }
        return EventSummary(left, without - left, phase, elapsed, progress, t)
    }

    /** "Round number" moments worth a notification: 1, 2, 3, 5, 7 and every multiple of 10. */
    fun isMilestone(left: Int): Boolean = left in setOf(1, 2, 3, 5, 7) || (left >= 10 && left % 10 == 0)

    fun sortEvents(events: List<CountdownEvent>, today: LocalDate = LocalDate.now()): List<CountdownEvent> =
        events.sortedWith(
            compareBy<CountdownEvent>(
                { !it.pinned },
                { effectiveTarget(it, today).isBefore(today) },
                {
                    val t = effectiveTarget(it, today)
                    if (t.isBefore(today)) -t.toEpochDay() else t.toEpochDay()
                },
            ),
        )
}
