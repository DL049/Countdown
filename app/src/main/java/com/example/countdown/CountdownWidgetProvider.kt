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

class CountdownWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, mgr, it) }
        scheduleNextMidnight(context)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { Repo.unbindWidget(context, it) }
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
            val event = Repo.eventForWidget(context, id)

            val tap: Intent
            if (event == null) {
                views.setTextViewText(R.id.widget_title, context.getString(R.string.app_name))
                views.setTextViewText(R.id.widget_number, "?")
                views.setTextViewText(R.id.widget_unit, context.getString(R.string.widget_pick))
                views.setTextViewText(R.id.widget_date, "")
                tap = Intent(context, WidgetConfigActivity::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            } else {
                val s = CountdownCalc.summarize(event, Repo.holidays(context))
                views.setTextViewText(R.id.widget_title, event.title)
                views.setTextViewText(R.id.widget_number, s.left.toString())
                views.setTextViewText(R.id.widget_unit, Fmt.unit(context, event, s))
                views.setTextViewText(R.id.widget_date, Fmt.dateMedium(event.target))
                tap = Intent(context, EventEditActivity::class.java)
                    .putExtra(EventEditActivity.EXTRA_EVENT_ID, event.id)
            }

            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    context, id, tap,
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
