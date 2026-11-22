package com.example.countdown

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.ZoneId

/** Renders every kind of widget (small, wide, list) and schedules the midnight refresh. */
object WidgetUpdater {
    const val ACTION_REFRESH = "com.example.countdown.REFRESH"

    private val providers = listOf(
        CountdownWidgetProvider::class.java,
        CountdownWideWidgetProvider::class.java,
        CountdownListWidgetProvider::class.java,
    )

    fun updateAll(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        var any = false
        for (cls in providers) {
            val ids = mgr.getAppWidgetIds(ComponentName(context, cls))
            if (ids.isNotEmpty()) any = true
            ids.forEach { updateOne(context, mgr, it) }
        }
        if (any) scheduleNextMidnight(context)
    }

    fun updateOne(context: Context, mgr: AppWidgetManager, id: Int) {
        when (mgr.getAppWidgetInfo(id)?.provider?.className) {
            CountdownListWidgetProvider::class.java.name -> updateList(context, mgr, id)
            CountdownWideWidgetProvider::class.java.name ->
                updateBound(context, mgr, id, R.layout.widget_wide)
            else -> updateBound(context, mgr, id, R.layout.widget_countdown)
        }
    }

    /** Small / wide widgets: both show one event and share the same view ids. */
    private fun updateBound(context: Context, mgr: AppWidgetManager, id: Int, layout: Int) {
        val views = RemoteViews(context.packageName, layout)
        val event = Repo.eventForWidget(context, id)

        val tap: Intent
        var bgImage: Bitmap? = null
        var onImageLight = true
        if (event == null) {
            views.setTextViewText(R.id.widget_title, context.getString(R.string.app_name))
            views.setTextViewText(R.id.widget_number, "?")
            views.setTextViewText(R.id.widget_unit, context.getString(R.string.widget_pick))
            views.setTextViewText(R.id.widget_date, "")
            views.setViewVisibility(R.id.widget_progress, View.GONE)
            views.setTextColor(R.id.widget_number, context.getColor(R.color.soft_blue))
            tap = Intent(context, WidgetConfigActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        } else {
            val s = CountdownCalc.summarize(event, Repo.holidays(context))
            views.setTextViewText(R.id.widget_title, event.title)
            views.setTextViewText(R.id.widget_number, Fmt.number(s).toString())
            views.setTextViewText(R.id.widget_unit, Fmt.unit(context, event, s))
            views.setTextViewText(R.id.widget_date, Fmt.dateMedium(s.target))
            views.setViewVisibility(
                R.id.widget_progress, if (s.phase == Phase.UPCOMING) View.VISIBLE else View.GONE,
            )
            views.setProgressBar(R.id.widget_progress, 100, (s.progress * 100).toInt(), false)
            tap = Intent(context, EventEditActivity::class.java)
                .putExtra(EventEditActivity.EXTRA_EVENT_ID, event.id)

            val o = mgr.getAppWidgetOptions(id)
            bgImage = WidgetBg.render(
                context, event.id,
                o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110).coerceAtLeast(60),
                o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 40).coerceAtLeast(40),
                event.scrimPercent, event.lightText,
            )
            onImageLight = event.lightText
            // numbers keep the soft theme colours unless there is a photo behind them
            if (bgImage == null) {
                views.setTextColor(R.id.widget_number, Fmt.numberColor(context, event, s))
            }
        }

        val hasImage = bgImage != null
        if (bgImage != null) views.setImageViewBitmap(R.id.widget_bg_image, bgImage)
        views.setViewVisibility(R.id.widget_bg_image, if (hasImage) View.VISIBLE else View.GONE)
        val textColor = when {
            !hasImage -> null
            onImageLight -> context.getColor(R.color.on_image_light)
            else -> context.getColor(R.color.on_image_dark)
        }
        views.setTextColor(
            R.id.widget_title, textColor ?: context.getColor(R.color.soft_text),
        )
        if (textColor != null) views.setTextColor(R.id.widget_number, textColor)
        val sub = textColor ?: context.getColor(R.color.soft_muted)
        views.setTextColor(R.id.widget_unit, sub)
        views.setTextColor(R.id.widget_date, sub)

        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(
                context, id, tap, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        mgr.updateAppWidget(id, views)
    }

    private val rowTitles = intArrayOf(R.id.row1_title, R.id.row2_title, R.id.row3_title, R.id.row4_title)
    private val rowValues = intArrayOf(R.id.row1_value, R.id.row2_value, R.id.row3_value, R.id.row4_value)

    /** List widget: the next few events at a glance (pinned first, past ones last). */
    private fun updateList(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_list)
        val holidays = Repo.holidays(context)
        val events = CountdownCalc.sortEvents(Repo.events(context)).take(rowTitles.size)
        views.setViewVisibility(R.id.list_empty, if (events.isEmpty()) View.VISIBLE else View.GONE)
        for (i in rowTitles.indices) {
            val e = events.getOrNull(i)
            if (e == null) {
                views.setViewVisibility(rowTitles[i], View.GONE)
                views.setViewVisibility(rowValues[i], View.GONE)
            } else {
                val s = CountdownCalc.summarize(e, holidays)
                views.setViewVisibility(rowTitles[i], View.VISIBLE)
                views.setViewVisibility(rowValues[i], View.VISIBLE)
                views.setTextViewText(rowTitles[i], e.title)
                views.setTextViewText(rowValues[i], Fmt.compact(context, e, s))
                views.setTextColor(rowValues[i], Fmt.numberColor(context, e, s))
            }
        }
        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(
                context, id, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        mgr.updateAppWidget(id, views)
    }

    private fun refreshIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, CountdownWidgetProvider::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Inexact alarm shortly after the next local midnight (no special permission needed). */
    fun scheduleNextMidnight(context: Context) {
        val zone = ZoneId.systemDefault()
        val at = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).plusMinutes(1)
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).set(
            AlarmManager.RTC, at.toInstant().toEpochMilli(), refreshIntent(context),
        )
    }
}
