package com.example.master.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

class ReminderScheduler(private val context: Context) {
    fun updateSchedule(notificationsEnabled: Boolean, reminderEnabled: Boolean, reminderTime: String) {
        if (notificationsEnabled && reminderEnabled) {
            scheduleDailyReminder(reminderTime)
        } else {
            cancelReminder()
        }
    }

    fun scheduleDailyReminder(time: String) {
        val (hour, minute) = parseTimeOrDefault(time)
        val triggerAtMillis = nextTriggerMillis(hour, minute)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = reminderPendingIntent()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelReminder() {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(reminderPendingIntent())
    }

    private fun reminderPendingIntent(): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = NotificationConstants.ACTION_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            NotificationConstants.REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun parseTimeOrDefault(time: String): Pair<Int, Int> {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return hour to minute
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (next.timeInMillis <= now.timeInMillis) {
            next.add(Calendar.DATE, 1)
        }
        return next.timeInMillis
    }
}
