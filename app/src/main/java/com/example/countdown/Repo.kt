package com.example.countdown

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Stores events, holidays, settings and widget→event bindings as JSON in SharedPreferences. */
object Repo {
    private const val PREFS = "countdown_data"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun newId(): String = UUID.randomUUID().toString()

    private fun toMask(days: Set<DayOfWeek>) = days.fold(0) { a, d -> a or (1 shl (d.value - 1)) }
    private fun fromMask(m: Int) =
        DayOfWeek.values().filter { (m and (1 shl (it.value - 1))) != 0 }.toSet()

    // ---- holidays ----
    private fun parseHolidays(arr: JSONArray): List<Holiday> = (0 until arr.length()).map {
        val o = arr.getJSONObject(it)
        Holiday(
            o.getString("id"), o.getString("name"),
            LocalDate.ofEpochDay(o.getLong("start")), LocalDate.ofEpochDay(o.getLong("end")),
        )
    }

    fun holidays(c: Context): List<Holiday> =
        parseHolidays(JSONArray(prefs(c).getString("holidays", "[]"))).sortedBy { it.start }

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
        saveHolidays(c, holidays(c).filter { it.id != h.id } + h)
    }

    fun deleteHoliday(c: Context, id: String) {
        saveHolidays(c, holidays(c).filter { it.id != id })
        // remove the deleted holiday from every event that referenced it
        saveEvents(c, events(c).map { it.copy(holidayIds = it.holidayIds - id) })
    }

    // ---- events ----
    private fun parseEvents(arr: JSONArray): List<CountdownEvent> = (0 until arr.length()).map {
        val o = arr.getJSONObject(it)
        val ids = o.getJSONArray("holidays")
        val days = when {
            o.has("days") -> fromMask(o.getInt("days"))
            o.optString("mode") == "WEEKDAYS" -> CountdownCalc.WEEKDAYS // data from the first version
            else -> CountdownCalc.ALL_DAYS
        }
        CountdownEvent(
            id = o.getString("id"),
            title = o.getString("title"),
            target = LocalDate.ofEpochDay(o.getLong("target")),
            activeDays = days.ifEmpty { CountdownCalc.ALL_DAYS },
            includeToday = o.getBoolean("today"),
            holidayIds = (0 until ids.length()).map { i -> ids.getString(i) }.toSet(),
            start = if (o.has("start")) LocalDate.ofEpochDay(o.getLong("start")) else LocalDate.now(),
            pinned = o.optBoolean("pinned", false),
            colorIdx = o.optInt("color", 0),
            category = o.optString("category", ""),
            repeat = runCatching { Repeat.valueOf(o.optString("repeat", "NONE")) }.getOrDefault(Repeat.NONE),
            notify = o.optBoolean("notify", true),
            scrimPercent = o.optInt("scrim", 40),
            lightText = o.optBoolean("light", true),
        )
    }

    fun events(c: Context): List<CountdownEvent> {
        val arr = JSONArray(prefs(c).getString("events", "[]"))
        val list = parseEvents(arr).sortedBy { it.target }
        // one-time migration: persist the creation date of events saved by older versions
        val needsMigration = (0 until arr.length()).any { !arr.getJSONObject(it).has("start") }
        if (needsMigration) saveEvents(c, list)
        return list
    }

    fun event(c: Context, id: String): CountdownEvent? = events(c).firstOrNull { it.id == id }

    private fun saveEvents(c: Context, list: List<CountdownEvent>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("title", it.title)
                    .put("target", it.target.toEpochDay()).put("days", toMask(it.activeDays))
                    .put("today", it.includeToday)
                    .put("holidays", JSONArray(it.holidayIds.toList()))
                    .put("start", it.start.toEpochDay()).put("pinned", it.pinned)
                    .put("color", it.colorIdx).put("category", it.category)
                    .put("repeat", it.repeat.name).put("notify", it.notify)
                    .put("scrim", it.scrimPercent).put("light", it.lightText),
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

    // ---- notification settings ----
    fun notifyEnabled(c: Context) = prefs(c).getBoolean("notify_enabled", false)
    fun setNotifyEnabled(c: Context, v: Boolean) = prefs(c).edit().putBoolean("notify_enabled", v).apply()

    /** Minutes after midnight; default 08:00. */
    fun notifyMinutes(c: Context) = prefs(c).getInt("notify_minutes", 8 * 60)
    fun setNotifyMinutes(c: Context, v: Int) = prefs(c).edit().putInt("notify_minutes", v).apply()

    fun lastNotified(c: Context, eventId: String): String? = prefs(c).getString("last_$eventId", null)
    fun setLastNotified(c: Context, eventId: String, v: String) =
        prefs(c).edit().putString("last_$eventId", v).apply()

    // ---- backup / restore ----
    fun exportJson(c: Context): String {
        val o = JSONObject()
        o.put("version", 2)
        o.put("events", JSONArray(prefs(c).getString("events", "[]")))
        o.put("holidays", JSONArray(prefs(c).getString("holidays", "[]")))
        val images = JSONObject()
        events(c).forEach { e -> WidgetBg.readBase64(c, e.id)?.let { images.put(e.id, it) } }
        o.put("images", images)
        o.put("notify_enabled", notifyEnabled(c))
        o.put("notify_minutes", notifyMinutes(c))
        return o.toString()
    }

    /** Replaces all current data with a backup. Returns false (and changes nothing) if it is invalid. */
    fun importJson(c: Context, text: String): Boolean = try {
        val o = JSONObject(text)
        val events = o.getJSONArray("events")
        val holidays = o.getJSONArray("holidays")
        parseEvents(events) // validate before touching anything
        parseHolidays(holidays)
        prefs(c).edit()
            .putString("events", events.toString())
            .putString("holidays", holidays.toString())
            .putBoolean("notify_enabled", o.optBoolean("notify_enabled", false))
            .putInt("notify_minutes", o.optInt("notify_minutes", 8 * 60))
            .apply()
        o.optJSONObject("images")?.let { imgs ->
            imgs.keys().forEach { id -> WidgetBg.writeBase64(c, id, imgs.getString(id)) }
        }
        true
    } catch (e: Exception) {
        false
    }
}
