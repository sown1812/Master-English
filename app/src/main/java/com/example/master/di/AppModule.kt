package com.example.master.di

import android.content.Context
import com.example.master.auth.AuthManager
import com.example.master.data.local.AppDatabase
import com.example.master.data.local.GameStateStore
import com.example.master.data.repository.LearningRepository
import com.example.master.network.ApiService
import com.example.master.network.NetworkModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    @Provides
    @Singleton
    fun provideRepository(database: AppDatabase): LearningRepository =
        LearningRepository(database)

    @Provides
    @Singleton
    fun provideAuthManager(repository: LearningRepository): AuthManager =
        AuthManager(repository)

    @Provides
    @Singleton
    fun provideGameStateStore(@ApplicationContext context: Context): GameStateStore =
        GameStateStore(context)

    @Provides
    @Singleton
    fun provideApiService(authManager: AuthManager): ApiService =
        NetworkModule.createApiService(authManager)

    @Provides
    @Singleton
    fun provideSyncManager(
        authManager: AuthManager,
        repository: LearningRepository,
        apiService: ApiService
    ): com.example.master.sync.SyncManager = com.example.master.sync.SyncManager(
        authManager = authManager,
        repository = repository,
        apiService = apiService
    )
}
