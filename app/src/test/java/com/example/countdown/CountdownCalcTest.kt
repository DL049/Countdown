package com.example.countdown

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CountdownCalcTest {
    // 2026-11-23 is a Monday.
    private val mon = LocalDate.of(2026, 11, 23)
    private val all = CountdownCalc.ALL_DAYS
    private val wk = CountdownCalc.WEEKDAYS

    private fun event(
        target: LocalDate,
        days: Set<DayOfWeek> = all,
        holidays: Set<String> = emptySet(),
        repeat: Repeat = Repeat.NONE,
        start: LocalDate = mon,
        includeToday: Boolean = false,
    ) = CountdownEvent("e", "t", target, days, includeToday, holidays, start, repeat = repeat)

    @Test fun everydayCountsCalendarDays() {
        assertEquals(10, CountdownCalc.daysLeft(mon, mon.plusDays(10), all))
        assertEquals(11, CountdownCalc.daysLeft(mon, mon.plusDays(10), all, true))
    }

    @Test fun weekdaysSkipWeekends() {
        assertEquals(5, CountdownCalc.daysLeft(mon, mon.plusDays(7), wk)) // Tue..Fri + Mon
        assertEquals(4, CountdownCalc.daysLeft(mon, mon.plusDays(6), wk))
        assertEquals(10, CountdownCalc.daysLeft(mon, mon.plusDays(14), wk))
    }

    @Test fun customDaysSkipTheUntickedWeekday() {
        val noFriday = wk - DayOfWeek.FRIDAY
        // Mon -> next Mon: Tue Wed Thu + Mon = 4
        assertEquals(4, CountdownCalc.daysLeft(mon, mon.plusDays(7), noFriday))
        val monTueThu = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
        assertEquals(6, CountdownCalc.daysLeft(mon, mon.plusDays(14), monTueThu))
    }

    @Test fun includeTodayOnlyWhenTodayCounts() {
        assertEquals(6, CountdownCalc.daysLeft(mon, mon.plusDays(7), wk, true))
        val sat = mon.plusDays(5)
        assertEquals(2, CountdownCalc.daysLeft(sat, sat.plusDays(3), wk, true))
    }

    @Test fun pastOrTodayTargetIsZero() {
        assertEquals(0, CountdownCalc.daysLeft(mon, mon, wk, true))
        assertEquals(0, CountdownCalc.daysLeft(mon, mon.minusDays(3), all))
    }

    @Test fun holidaysAreSkipped() {
        val h = Holiday("h", "break", mon.plusDays(2), mon.plusDays(3)) // Wed..Thu
        assertEquals(8, CountdownCalc.daysLeft(mon, mon.plusDays(10), all, false, listOf(h)))
        assertEquals(3, CountdownCalc.daysLeft(mon, mon.plusDays(7), wk, false, listOf(h)))
    }

    @Test fun holidayOnWeekendDoesNotDoubleSubtract() {
        val h = Holiday("h", "wk", mon.plusDays(5), mon.plusDays(6))
        assertEquals(5, CountdownCalc.daysLeft(mon, mon.plusDays(7), wk, false, listOf(h)))
    }

    @Test fun singleDayHoliday() {
        val h = Holiday("h", "typhoon", mon.plusDays(2), mon.plusDays(2))
        assertEquals(1, h.lengthDays)
        assertEquals(4, CountdownCalc.daysLeft(mon, mon.plusDays(7), wk, false, listOf(h)))
    }

    @Test fun summaryReportsSkippedDays() {
        val h = Holiday("h", "break", mon.plusDays(2), mon.plusDays(3))
        val s = CountdownCalc.summarize(event(mon.plusDays(7), wk, setOf("h")), listOf(h), mon)
        assertEquals(3, s.left)
        assertEquals(2, s.skippedByHolidays)
        assertEquals(Phase.UPCOMING, s.phase)
    }

    @Test fun pastDatesReportDaysSince() {
        val s = CountdownCalc.summarize(event(mon.minusDays(12)), emptyList(), mon)
        assertEquals(Phase.PAST, s.phase)
        assertEquals(12, s.elapsedDays)
        assertEquals(0, s.left)
    }

    @Test fun targetTodayIsTodayPhase() {
        val s = CountdownCalc.summarize(event(mon), emptyList(), mon)
        assertEquals(Phase.TODAY, s.phase)
    }

    @Test fun yearlyEventsRollToNextOccurrence() {
        val birthday = LocalDate.of(2020, 12, 25)
        val e = event(birthday, repeat = Repeat.YEARLY, start = birthday)
        assertEquals(LocalDate.of(2026, 12, 25), CountdownCalc.effectiveTarget(e, mon))
        val s = CountdownCalc.summarize(e, emptyList(), mon)
        assertEquals(Phase.UPCOMING, s.phase)
        assertEquals(32, s.left)
        // on the day itself it stays today, the day after it rolls to next year
        assertEquals(Phase.TODAY, CountdownCalc.summarize(e, emptyList(), LocalDate.of(2026, 12, 25)).phase)
        assertEquals(
            LocalDate.of(2027, 12, 25),
            CountdownCalc.effectiveTarget(e, LocalDate.of(2026, 12, 26)),
        )
    }

    @Test fun progressRunsFromStartToTarget() {
        val s = CountdownCalc.summarize(event(mon.plusDays(10), start = mon.minusDays(10)), emptyList(), mon)
        assertEquals(0.5f, s.progress, 0.001f)
        assertEquals(1f, CountdownCalc.summarize(event(mon.minusDays(1)), emptyList(), mon).progress, 0f)
    }

    @Test fun milestones() {
        listOf(1, 2, 3, 5, 7, 10, 20, 30, 100).forEach { assertTrue("$it", CountdownCalc.isMilestone(it)) }
        listOf(0, 4, 6, 8, 9, 11, 25, 99).forEach { assertFalse("$it", CountdownCalc.isMilestone(it)) }
    }

    @Test fun sortingPinsFirstThenUpcomingThenPast() {
        val past = event(mon.minusDays(5)).copy(id = "past")
        val soon = event(mon.plusDays(3)).copy(id = "soon")
        val later = event(mon.plusDays(30)).copy(id = "later")
        val pinned = event(mon.plusDays(90)).copy(id = "pinned", pinned = true)
        val order = CountdownCalc.sortEvents(listOf(past, later, pinned, soon), mon).map { it.id }
        assertEquals(listOf("pinned", "soon", "later", "past"), order)
    }
}
