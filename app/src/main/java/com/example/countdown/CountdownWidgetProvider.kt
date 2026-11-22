package com.example.countdown

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle

/** Small (2×2) widget showing one event. Also receives the system time/boot broadcasts. */
class CountdownWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { WidgetUpdater.updateOne(context, mgr, it) }
        WidgetUpdater.scheduleNextMidnight(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle,
    ) {
        WidgetUpdater.updateOne(context, mgr, id) // re-render the background image for the new size
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { Repo.unbindWidget(context, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent) // handles APPWIDGET_* actions
        when (intent.action) {
            WidgetUpdater.ACTION_REFRESH,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> WidgetUpdater.updateAll(context)
        }
    }
}

/** Wide (4×1) widget showing one event. */
class CountdownWideWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { WidgetUpdater.updateOne(context, mgr, it) }
        WidgetUpdater.scheduleNextMidnight(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle,
    ) {
        WidgetUpdater.updateOne(context, mgr, id)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { Repo.unbindWidget(context, it) }
    }
}

/** List (4×2) widget showing the next few events. */
class CountdownListWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { WidgetUpdater.updateOne(context, mgr, it) }
        WidgetUpdater.scheduleNextMidnight(context)
    }
}
