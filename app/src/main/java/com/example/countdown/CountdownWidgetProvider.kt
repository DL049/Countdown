package com.example.countdown

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class CountdownWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, mgr, it) }
        scheduleNextMidnight(context)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { CountdownStore.delete(context, it) }
    }

    override fun onDisabled(context: Context) {
        alarmManager(context).cancel(refreshIntent(context))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent) // handles APPWIDGET_* actions
        when (intent.action) {
            ACTION_REFRESH,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED -> updateAll(context)
        }
    }

    companion object {
        private const val ACTION_REFRESH = "com.example.countdown.REFRESH"

        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, CountdownWidgetProvider::class.java))
            ids.forEach { updateWidget(context, mgr, it) }
            if (ids.isNotEmpty()) scheduleNextMidnight(context)
        }

        fun updateWidget(context: Context, mgr: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_countdown)
            val cfg = CountdownStore.load(context, id)

            if (cfg == null) {
                views.setTextViewText(R.id.widget_title, context.getString(R.string.default_title))
                views.setTextViewText(R.id.widget_number, "?")
                views.setTextViewText(R.id.widget_unit, "")
                views.setTextViewText(R.id.widget_date, "")
            } else {
                val today = LocalDate.now(ZoneId.systemDefault())
                val left = CountdownCalc.daysLeft(today, cfg.target, cfg.mode, cfg.includeToday)
                val unit = when {
                    left == 0 && !cfg.target.isAfter(today) -> R.string.unit_done
                    cfg.mode == CountMode.WEEKDAYS -> R.string.unit_weekdays
                    else -> R.string.unit_days
                }
                views.setTextViewText(
                    R.id.widget_title,
                    cfg.title.ifBlank { context.getString(R.string.default_title) },
                )
                views.setTextViewText(R.id.widget_number, left.toString())
                views.setTextViewText(R.id.widget_unit, context.getString(unit))
                views.setTextViewText(
                    R.id.widget_date,
                    cfg.target.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                )
            }

            // Tapping the widget reopens the configuration screen.
            val configIntent = Intent(context, ConfigActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    context, id, configIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            mgr.updateAppWidget(id, views)
        }

        private fun alarmManager(context: Context) =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        private fun refreshIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context, 0,
                Intent(context, CountdownWidgetProvider::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        /** Inexact alarm shortly after the next local midnight (no special permission needed). */
        private fun scheduleNextMidnight(context: Context) {
            val zone = ZoneId.systemDefault()
            val at = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).plusMinutes(1)
            alarmManager(context).set(
                AlarmManager.RTC,
                at.toInstant().toEpochMilli(),
                refreshIntent(context),
            )
        }
    }
}
