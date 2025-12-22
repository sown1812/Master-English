package com.example.master.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.master.data.local.NotificationSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationConstants.ACTION_REMINDER) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val store = NotificationSettingsStore(context.applicationContext)
            val settings = store.settings.first()
            if (settings.notificationsEnabled && settings.reminderEnabled) {
                NotificationHelper.showPracticeReminder(context)
                ReminderScheduler(context.applicationContext)
                    .scheduleDailyReminder(settings.reminderTime)
            }
            pendingResult.finish()
        }
    }
}
