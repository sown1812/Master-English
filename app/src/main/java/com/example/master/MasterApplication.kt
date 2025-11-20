package com.example.master

import android.app.Application
import com.example.master.auth.AuthManager
import com.example.master.data.local.AppDatabase
import com.example.master.data.local.GameStateStore
import com.example.master.data.repository.LearningRepository
import com.example.master.network.NetworkModule
import com.example.master.network.ApiService

class MasterApplication : Application() {

    // Database instance
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    val gameStateStore: GameStateStore by lazy {
        GameStateStore(this)
    }

    val apiService: ApiService by lazy {
        NetworkModule.createApiService(authManager)
    }

    // Repository instance
    val repository: LearningRepository by lazy {
        LearningRepository(database)
    }

    // Auth manager instance
    val authManager: AuthManager by lazy {
        AuthManager(repository)
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize anything needed at app start
    }
}
