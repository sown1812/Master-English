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

class NotificationSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore by preferencesDataStore(name = "notification_settings")

    private val notificationsEnabledKey = booleanPreferencesKey("notifications_enabled")
    private val reminderEnabledKey = booleanPreferencesKey("reminder_enabled")
    private val reminderTimeKey = stringPreferencesKey("reminder_time")

    val settings: Flow<NotificationSettings> = context.dataStore.data.map { prefs ->
        NotificationSettings(
            notificationsEnabled = prefs[notificationsEnabledKey] ?: true,
            reminderEnabled = prefs[reminderEnabledKey] ?: false,
            reminderTime = prefs[reminderTimeKey] ?: "20:00"
        )
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[notificationsEnabledKey] = enabled }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[reminderEnabledKey] = enabled }
    }

    suspend fun setReminderTime(time: String) {
        context.dataStore.edit { prefs -> prefs[reminderTimeKey] = time }
    }
}
