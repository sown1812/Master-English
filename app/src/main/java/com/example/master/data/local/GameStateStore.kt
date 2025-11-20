package com.example.master.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameStateStore(private val context: Context) {

    private val Context.dataStore by preferencesDataStore(name = "game_state")

    private val boosterOwnedKey = stringSetPreferencesKey("booster_owned")
    private val questClaimedKey = stringSetPreferencesKey("quest_claimed")
    private val dailyStatusKey = stringPreferencesKey("daily_status")
    private val dailyProgressKey = intPreferencesKey("daily_progress")

    val ownedBoosters: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[boosterOwnedKey] ?: emptySet()
    }

    val claimedQuests: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[questClaimedKey] ?: emptySet()
    }

    val dailyState: Flow<DailyState> = context.dataStore.data.map { prefs ->
        DailyState(
            status = prefs[dailyStatusKey] ?: ChallengeStatus.READY.name,
            progress = prefs[dailyProgressKey] ?: 0
        )
    }

    suspend fun setBoosterOwned(title: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[boosterOwnedKey]?.toMutableSet() ?: mutableSetOf()
            current.add(title)
            prefs[boosterOwnedKey] = current
        }
    }

    suspend fun setQuestClaimed(title: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[questClaimedKey]?.toMutableSet() ?: mutableSetOf()
            current.add(title)
            prefs[questClaimedKey] = current
        }
    }

    suspend fun setDailyStatus(status: ChallengeStatus, progress: Int) {
        context.dataStore.edit { prefs ->
            prefs[dailyStatusKey] = status.name
            prefs[dailyProgressKey] = progress
        }
    }
}

data class DailyState(
    val status: String,
    val progress: Int
)

enum class ChallengeStatus { READY, IN_PROGRESS, COMPLETED, CLAIMED }
