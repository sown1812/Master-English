package com.example.master

import android.app.Application
import com.example.master.di.ApplicationScope
import com.example.master.sync.ContentSyncManager
import com.example.master.sync.SyncManager
import com.example.master.core.network.NetworkMonitor
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltAndroidApp
class MasterApplication : Application() {

    @Inject
    lateinit var contentSyncManager: ContentSyncManager

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        runCatching { FirebaseDatabase.getInstance().setPersistenceEnabled(true) }
        applicationScope.launch {
            runCatching { contentSyncManager.refreshFromServer() }
            // Khi online sẽ flush ngay các tiến độ/achievements còn hàng đợi
            runCatching { syncManager.flushQueue() }

            // Lắng nghe trạng thái mạng để tự flush khi có kết nối
            networkMonitor.isConnected.collectLatest { connected ->
                if (connected) {
                    runCatching { syncManager.flushQueue() }
                }
            }
        }
    }
}
