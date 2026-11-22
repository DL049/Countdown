package com.example.countdown

import android.content.Context
import java.time.LocalDate
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Stores events, holidays and widget→event bindings as JSON in SharedPreferences. */
object Repo {
    private const val PREFS = "countdown_data"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun newId(): String = UUID.randomUUID().toString()

    // ---- holidays ----
    fun holidays(c: Context): List<Holiday> {
        val arr = JSONArray(prefs(c).getString("holidays", "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Holiday(
                o.getString("id"), o.getString("name"),
                LocalDate.ofEpochDay(o.getLong("start")), LocalDate.ofEpochDay(o.getLong("end")),
            )
        }.sortedBy { it.start }
    }

    fun holiday(c: Context, id: String): Holiday? = holidays(c).firstOrNull { it.id == id }

    private fun saveHolidays(c: Context, list: List<Holiday>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("name", it.name)
                    .put("start", it.start.toEpochDay()).put("end", it.end.toEpochDay()),
            )
        }
        prefs(c).edit().putString("holidays", arr.toString()).apply()
    }

    fun upsertHoliday(c: Context, h: Holiday) {
        val list = holidays(c).filter { it.id != h.id } + h
        saveHolidays(c, list)
    }

    fun deleteHoliday(c: Context, id: String) {
        saveHolidays(c, holidays(c).filter { it.id != id })
        // remove the deleted holiday from every event that referenced it
        saveEvents(c, events(c).map { it.copy(holidayIds = it.holidayIds - id) })
    }

    // ---- events ----
    fun events(c: Context): List<CountdownEvent> {
        val arr = JSONArray(prefs(c).getString("events", "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            val ids = o.getJSONArray("holidays")
            CountdownEvent(
                id = o.getString("id"),
                title = o.getString("title"),
                target = LocalDate.ofEpochDay(o.getLong("target")),
                mode = runCatching { CountMode.valueOf(o.getString("mode")) }
                    .getOrDefault(CountMode.EVERYDAY),
                includeToday = o.getBoolean("today"),
                holidayIds = (0 until ids.length()).map { i -> ids.getString(i) }.toSet(),
            )
        }.sortedBy { it.target }
    }

    fun event(c: Context, id: String): CountdownEvent? = events(c).firstOrNull { it.id == id }

    private fun saveEvents(c: Context, list: List<CountdownEvent>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("title", it.title)
                    .put("target", it.target.toEpochDay()).put("mode", it.mode.name)
                    .put("today", it.includeToday)
                    .put("holidays", JSONArray(it.holidayIds.toList())),
            )
        }
        prefs(c).edit().putString("events", arr.toString()).apply()
    }

    fun upsertEvent(c: Context, e: CountdownEvent) {
        saveEvents(c, events(c).filter { it.id != e.id } + e)
    }

    fun deleteEvent(c: Context, id: String) {
        saveEvents(c, events(c).filter { it.id != id })
    }

    // ---- widget bindings ----
    fun bindWidget(c: Context, widgetId: Int, eventId: String) {
        prefs(c).edit().putString("widget_$widgetId", eventId).apply()
    }

    fun unbindWidget(c: Context, widgetId: Int) {
        prefs(c).edit().remove("widget_$widgetId").apply()
    }

    fun eventForWidget(c: Context, widgetId: Int): CountdownEvent? =
        prefs(c).getString("widget_$widgetId", null)?.let { event(c, it) }
}
