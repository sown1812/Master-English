package com.example.master

import android.app.Application
import com.example.master.auth.AuthManager
import com.example.master.data.local.AppDatabase
import com.example.master.data.repository.LearningRepository

class MasterApplication : Application() {
    
    // Database instance
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
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
