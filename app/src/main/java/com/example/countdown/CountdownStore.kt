package com.example.countdown

import android.content.Context
import java.time.LocalDate

data class CountdownConfig(
    val title: String,
    val target: LocalDate,
    val mode: CountMode,
    val includeToday: Boolean,
)

/** Persists one [CountdownConfig] per widget id in SharedPreferences. */
object CountdownStore {
    private const val PREFS = "countdowns"

    fun save(ctx: Context, id: Int, c: CountdownConfig) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("title_$id", c.title)
            .putLong("date_$id", c.target.toEpochDay())
            .putString("mode_$id", c.mode.name)
            .putBoolean("today_$id", c.includeToday)
            .apply()
    }

    fun load(ctx: Context, id: Int): CountdownConfig? {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains("date_$id")) return null
        return CountdownConfig(
            title = p.getString("title_$id", "") ?: "",
            target = LocalDate.ofEpochDay(p.getLong("date_$id", 0)),
            mode = runCatching { CountMode.valueOf(p.getString("mode_$id", "") ?: "") }
                .getOrDefault(CountMode.EVERYDAY),
            includeToday = p.getBoolean("today_$id", false),
        )
    }

    fun delete(ctx: Context, id: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove("title_$id").remove("date_$id").remove("mode_$id").remove("today_$id")
            .apply()
    }
}
