package com.example.countdown

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownCalcTest {
    // 2026-11-23 is a Monday.
    private val mon = LocalDate.of(2026, 11, 23)

    @Test fun everydayCountsCalendarDays() {
        assertEquals(10, CountdownCalc.daysLeft(mon, mon.plusDays(10), CountMode.EVERYDAY))
        assertEquals(11, CountdownCalc.daysLeft(mon, mon.plusDays(10), CountMode.EVERYDAY, true))
    }

    @Test fun weekdaysSkipWeekends() {
        // Mon -> next Mon: Tue Wed Thu Fri Mon = 5
        assertEquals(5, CountdownCalc.daysLeft(mon, mon.plusDays(7), CountMode.WEEKDAYS))
        // Mon -> Sun: Tue..Fri = 4
        assertEquals(4, CountdownCalc.daysLeft(mon, mon.plusDays(6), CountMode.WEEKDAYS))
        // Two full weeks = 10 weekdays
        assertEquals(10, CountdownCalc.daysLeft(mon, mon.plusDays(14), CountMode.WEEKDAYS))
    }

    @Test fun includeTodayOnlyOnWeekdays() {
        assertEquals(6, CountdownCalc.daysLeft(mon, mon.plusDays(7), CountMode.WEEKDAYS, true))
        val sat = mon.plusDays(5)
        assertEquals(2, CountdownCalc.daysLeft(sat, sat.plusDays(3), CountMode.WEEKDAYS, true))
    }

    @Test fun pastOrTodayTargetIsZero() {
        assertEquals(0, CountdownCalc.daysLeft(mon, mon, CountMode.WEEKDAYS, true))
        assertEquals(0, CountdownCalc.daysLeft(mon, mon.minusDays(3), CountMode.EVERYDAY))
    }

    @Test fun holidaysAreSkippedInBothModes() {
        // Holiday Wed..Thu next week (mon+2 .. mon+3)
        val h = Holiday("h", "break", mon.plusDays(2), mon.plusDays(3))
        // everyday: 10 days minus 2 holiday days
        assertEquals(8, CountdownCalc.daysLeft(mon, mon.plusDays(10), CountMode.EVERYDAY, false, listOf(h)))
        // weekdays Mon->next Mon is 5, minus Wed+Thu = 3
        assertEquals(3, CountdownCalc.daysLeft(mon, mon.plusDays(7), CountMode.WEEKDAYS, false, listOf(h)))
    }

    @Test fun holidayOnWeekendDoesNotDoubleSubtract() {
        // Sat+Sun holiday in weekdays mode changes nothing
        val h = Holiday("h", "wk", mon.plusDays(5), mon.plusDays(6))
        assertEquals(5, CountdownCalc.daysLeft(mon, mon.plusDays(7), CountMode.WEEKDAYS, false, listOf(h)))
    }

    @Test fun summaryReportsSkippedDays() {
        val h = Holiday("h", "break", mon.plusDays(2), mon.plusDays(3))
        val e = CountdownEvent("e", "t", mon.plusDays(7), CountMode.WEEKDAYS, false, setOf("h"))
        val s = CountdownCalc.summarize(e, listOf(h), mon)
        assertEquals(3, s.left)
        assertEquals(2, s.skippedByHolidays)
    }
}
