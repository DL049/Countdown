package com.example.countdown

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Milestone notifications: once a day (at the chosen time) each event is checked and a
 * notification is posted when its remaining count is a round number (see
 * [CountdownCalc.isMilestone]) or the date arrives. A value is only announced once.
 */
object Notifier {
    private const val CHANNEL = "milestones"
    const val ACTION = "com.example.countdown.NOTIFY"

    fun ensureChannel(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, c.getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    fun hasPermission(c: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun alarmIntent(c: Context): PendingIntent = PendingIntent.getBroadcast(
        c, 1, Intent(c, NotifyReceiver::class.java).setAction(ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** (Re)schedules the next daily check, or cancels it when notifications are off. */
    fun schedule(c: Context) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(alarmIntent(c))
        if (!Repo.notifyEnabled(c)) return
        val zone = ZoneId.systemDefault()
        val minutes = Repo.notifyMinutes(c)
        var at = LocalDate.now(zone).atStartOfDay(zone).plusMinutes(minutes.toLong())
        if (!at.isAfter(ZonedDateTime.now(zone))) at = at.plusDays(1)
        am.set(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), alarmIntent(c))
    }

    fun checkAndNotify(c: Context) {
        if (!Repo.notifyEnabled(c) || !hasPermission(c)) return
        ensureChannel(c)
        val nm = c.getSystemService(NotificationManager::class.java)
        val holidays = Repo.holidays(c)
        val today = LocalDate.now()
        for (e in Repo.events(c)) {
            if (!e.notify) continue
            val s = CountdownCalc.summarize(e, holidays, today)
            val key = when (s.phase) {
                Phase.TODAY -> -1
                Phase.UPCOMING -> if (CountdownCalc.isMilestone(s.left)) s.left else continue
                Phase.PAST -> continue
            }
            // remember which value was announced so weekends/holidays don't repeat it
            val stamp = "${s.target.toEpochDay()}:$key"
            if (Repo.lastNotified(c, e.id) == stamp) continue
            Repo.setLastNotified(c, e.id, stamp)

            val text = if (s.phase == Phase.TODAY) {
                c.getString(R.string.notify_today)
            } else {
                c.getString(R.string.notify_left, s.left, Fmt.unit(c, e, s))
            }
            val open = PendingIntent.getActivity(
                c, e.id.hashCode(),
                Intent(c, EventEditActivity::class.java).putExtra(EventEditActivity.EXTRA_EVENT_ID, e.id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            nm.notify(
                e.id.hashCode(),
                NotificationCompat.Builder(c, CHANNEL)
                    .setSmallIcon(R.drawable.ic_hourglass)
                    .setContentTitle(e.title)
                    .setContentText(text)
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .build(),
            )
        }
    }
}

class NotifyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Notifier.ACTION) Notifier.checkAndNotify(context)
        Notifier.schedule(context) // boot / app update / after a check: set the next alarm
    }
}
