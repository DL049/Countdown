package com.example.countdown

import java.time.LocalDate

data class IcsItem(val name: String, val start: LocalDate, val end: LocalDate)

/** Minimal iCalendar (.ics) reader: turns VEVENTs into named date ranges. */
object IcsParser {

    fun parse(text: String): List<IcsItem> {
        // RFC 5545 line unfolding: a line starting with space/tab continues the previous one
        val lines = ArrayList<String>()
        for (raw in text.split("\r\n", "\n", "\r")) {
            if ((raw.startsWith(" ") || raw.startsWith("\t")) && lines.isNotEmpty()) {
                lines[lines.size - 1] = lines.last() + raw.substring(1)
            } else {
                lines.add(raw)
            }
        }

        val out = ArrayList<IcsItem>()
        var inEvent = false
        var name = ""
        var start: LocalDate? = null
        var end: LocalDate? = null
        var endIsExclusiveDate = false

        for (line in lines) {
            when {
                line.equals("BEGIN:VEVENT", ignoreCase = true) -> {
                    inEvent = true
                    name = ""
                    start = null
                    end = null
                    endIsExclusiveDate = false
                }
                line.equals("END:VEVENT", ignoreCase = true) -> {
                    val s = start
                    if (inEvent && s != null) {
                        var e = end ?: s
                        if (end != null && endIsExclusiveDate) e = e.minusDays(1)
                        if (e.isBefore(s)) e = s
                        out.add(IcsItem(name.ifBlank { "Holiday" }, s, e))
                    }
                    inEvent = false
                }
                inEvent -> {
                    val colon = line.indexOf(':')
                    if (colon > 0) {
                        val key = line.substring(0, colon).substringBefore(';').uppercase()
                        val value = line.substring(colon + 1)
                        when (key) {
                            "SUMMARY" -> name = unescape(value)
                            "DTSTART" -> start = parseDate(value)
                            "DTEND" -> {
                                end = parseDate(value)
                                // date-only DTEND is exclusive; a midnight date-time is too
                                endIsExclusiveDate = !value.contains('T') || value.substringAfter('T').startsWith("000000")
                            }
                        }
                    }
                }
            }
        }
        return out
    }

    private fun parseDate(v: String): LocalDate? {
        if (v.length < 8) return null
        return try {
            LocalDate.of(v.substring(0, 4).toInt(), v.substring(4, 6).toInt(), v.substring(6, 8).toInt())
        } catch (e: Exception) {
            null
        }
    }

    private fun unescape(s: String) =
        s.replace("\\n", " ").replace("\\N", " ").replace("\\,", ",").replace("\;", ";").replace("\\\\", "\\")
}
