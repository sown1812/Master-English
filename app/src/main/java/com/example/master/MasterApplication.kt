package com.example.master

import android.app.Application
import com.example.master.di.ApplicationScope
import com.example.master.sync.ContentSyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class MasterApplication : Application() {

    @Inject
    lateinit var contentSyncManager: ContentSyncManager

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            contentSyncManager.refreshFromServer()
        }
    }
}
