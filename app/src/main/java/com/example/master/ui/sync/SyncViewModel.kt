package com.example.master.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.master.auth.AuthManager
import com.example.master.sync.ContentSyncManager
import com.example.master.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val contentSyncManager: ContentSyncManager,
    private val syncManager: SyncManager
) : ViewModel() {

    @Volatile
    private var hasSynced = false
    @Volatile
    private var lastSyncedUserId: String? = null

    fun syncAll() {
        val userId = authManager.getCurrentUserId()
        if (hasSynced && lastSyncedUserId == userId) return
        hasSynced = true
        lastSyncedUserId = userId
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { contentSyncManager.refreshFromServer() }
            if (!authManager.isAnonymous()) {
                runCatching { syncManager.syncNow() }
            }
        }
    }
}
