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
}
