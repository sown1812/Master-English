package com.example.master.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.master.network.SyncPayload
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

class PendingSyncStore(private val context: Context, private val gson: Gson) {
    private val Context.dataStore by preferencesDataStore(name = "pending_sync_queue")
    private val queueKey = stringPreferencesKey("queue_json")

    suspend fun getQueue(): List<SyncPayload> {
        val json = context.dataStore.data.first()[queueKey] ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<SyncPayload>>() {}.type
            gson.fromJson<List<SyncPayload>>(json, type)
        }.getOrDefault(emptyList())
    }

    suspend fun saveQueue(queue: List<SyncPayload>) {
        val json = gson.toJson(queue)
        context.dataStore.edit { prefs -> prefs[queueKey] = json }
    }

    suspend fun enqueue(payload: SyncPayload) {
        val current = getQueue().toMutableList()
        current.add(payload)
        saveQueue(current)
    }

    suspend fun clear() {
        context.dataStore.edit { prefs -> prefs.remove(queueKey) }
    }
}
