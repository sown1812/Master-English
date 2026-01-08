package com.example.master.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

data class PendingShopAction(
    val userId: String,
    val type: String,
    val key: String,
    val value: Boolean
)

private val Context.shopSyncDataStore by preferencesDataStore(name = "shop_sync_queue")

class ShopSyncStore(private val context: Context, private val gson: Gson) {
    private val queueKey = stringPreferencesKey("queue_json")

    suspend fun getQueue(): List<PendingShopAction> {
        val json = context.shopSyncDataStore.data.first()[queueKey] ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<PendingShopAction>>() {}.type
            gson.fromJson<List<PendingShopAction>>(json, type)
        }.getOrDefault(emptyList())
    }

    suspend fun saveQueue(queue: List<PendingShopAction>) {
        val json = gson.toJson(queue)
        context.shopSyncDataStore.edit { prefs -> prefs[queueKey] = json }
    }

    suspend fun enqueue(action: PendingShopAction) {
        val current = getQueue().toMutableList()
        current.add(action)
        saveQueue(current)
    }

    suspend fun clear() {
        context.shopSyncDataStore.edit { prefs -> prefs.remove(queueKey) }
    }
}
