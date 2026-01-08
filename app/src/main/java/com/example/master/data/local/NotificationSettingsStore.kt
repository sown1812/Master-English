package com.example.master.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class NotificationSettings(
    val notificationsEnabled: Boolean,
    val reminderEnabled: Boolean,
    val reminderTime: String
)

private val Context.notificationSettingsDataStore by preferencesDataStore(name = "notification_settings")

class NotificationSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationsEnabledKey = booleanPreferencesKey("notifications_enabled")
    private val reminderEnabledKey = booleanPreferencesKey("reminder_enabled")
    private val reminderTimeKey = stringPreferencesKey("reminder_time")

    val settings: Flow<NotificationSettings> = context.notificationSettingsDataStore.data.map { prefs ->
        NotificationSettings(
            notificationsEnabled = prefs[notificationsEnabledKey] ?: true,
            reminderEnabled = prefs[reminderEnabledKey] ?: false,
            reminderTime = prefs[reminderTimeKey] ?: "20:00"
        )
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.notificationSettingsDataStore.edit { prefs -> prefs[notificationsEnabledKey] = enabled }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.notificationSettingsDataStore.edit { prefs -> prefs[reminderEnabledKey] = enabled }
    }

    suspend fun setReminderTime(time: String) {
        context.notificationSettingsDataStore.edit { prefs -> prefs[reminderTimeKey] = time }
    }
}
