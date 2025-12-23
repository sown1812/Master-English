package com.example.master.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WordleStateStore(private val context: Context) {

    private val Context.dataStore by preferencesDataStore(name = "wordle_state")

    private val lastPlayedDayKey = intPreferencesKey("wordle_last_played_day")
    private val lastWinDayKey = intPreferencesKey("wordle_last_win_day")
    private val streakKey = intPreferencesKey("wordle_streak")

    val dailyState: Flow<WordleDailyState> = context.dataStore.data.map { prefs ->
        WordleDailyState(
            lastPlayedDay = prefs[lastPlayedDayKey] ?: -1,
            lastWinDay = prefs[lastWinDayKey] ?: -1,
            streak = prefs[streakKey] ?: 0
        )
    }

    suspend fun updateDailyResult(day: Int, isWin: Boolean) {
        context.dataStore.edit { prefs ->
            val lastWinDay = prefs[lastWinDayKey] ?: -1
            val currentStreak = prefs[streakKey] ?: 0

            val newStreak = when {
                !isWin -> 0
                lastWinDay == day - 1 -> currentStreak + 1
                else -> 1
            }

            prefs[lastPlayedDayKey] = day
            if (isWin) {
                prefs[lastWinDayKey] = day
            }
            prefs[streakKey] = newStreak
        }
    }
}

data class WordleDailyState(
    val lastPlayedDay: Int,
    val lastWinDay: Int,
    val streak: Int
)
