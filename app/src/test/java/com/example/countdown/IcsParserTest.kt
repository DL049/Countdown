package com.example.countdown

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class IcsParserTest {
    private val ics = """
        BEGIN:VCALENDAR
        BEGIN:VEVENT
        DTSTART;VALUE=DATE:20261220
        DTEND;VALUE=DATE:20270104
        SUMMARY:Winter\, break
        END:VEVENT
        BEGIN:VEVENT
        DTSTART:20261225T000000Z
        DTEND:20261225T235959Z
        SUMMARY:Folded
         title
        END:VEVENT
        BEGIN:VEVENT
        DTSTART;VALUE=DATE:20270210
        SUMMARY:One day
        END:VEVENT
        END:VCALENDAR
    """.trimIndent().replace("\n", "\r\n")

    @Test fun parsesRangesNamesAndExclusiveEnd() {
        val items = IcsParser.parse(ics)
        assertEquals(3, items.size)
        // all-day DTEND is exclusive: 20270104 -> last day is Jan 3
        assertEquals(IcsItem("Winter, break", LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 3)), items[0])
        // folded line is joined, date-time end on the same day stays
        assertEquals(IcsItem("Foldedtitle", LocalDate.of(2026, 12, 25), LocalDate.of(2026, 12, 25)), items[1])
        // missing DTEND = single day
        assertEquals(IcsItem("One day", LocalDate.of(2027, 2, 10), LocalDate.of(2027, 2, 10)), items[2])
    }

    @Test fun garbageGivesNothing() {
        assertEquals(0, IcsParser.parse("hello world").size)
    }
}
