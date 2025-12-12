package com.example.master.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.master.sync.ContentSyncManager
import com.example.master.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val contentSyncManager: ContentSyncManager,
    private val syncManager: SyncManager
) : ViewModel() {

    @Volatile
    private var hasSynced = false

    fun syncAll() {
        if (hasSynced) return
        hasSynced = true
        viewModelScope.launch {
            runCatching { contentSyncManager.refreshFromServer() }
            runCatching { syncManager.syncNow() }
        }
    }
}
