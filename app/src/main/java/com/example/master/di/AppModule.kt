package com.example.master.di

import android.content.Context
import com.example.master.auth.AuthManager
import com.example.master.auth.di.AuthProvider
import com.example.master.auth.di.FirebaseAuthProvider
import com.example.master.core.network.NetworkMonitor
import com.example.master.data.local.AppDatabase
import com.example.master.data.local.GameStateStore
import com.example.master.data.local.NotificationSettingsStore
import com.example.master.data.local.PendingSyncStore
import com.example.master.data.local.WordleStateStore
import com.example.master.data.repository.LearningRepository
import com.example.master.di.ApplicationScope
import com.example.master.network.ApiService
import com.example.master.network.NetworkModule
import com.example.master.notifications.ReminderScheduler
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    fun provideAuthProvider(): AuthProvider = FirebaseAuthProvider()

    @Provides
    @Singleton
    fun provideAuthManager(
        repository: LearningRepository,
        authProvider: AuthProvider,
        @ApplicationScope appScope: CoroutineScope
    ): AuthManager = AuthManager(repository, authProvider, appScope)

    @Provides
    @Singleton
    fun provideGameStateStore(@ApplicationContext context: Context): GameStateStore =
        GameStateStore(context)

    @Provides
    @Singleton
    fun provideWordleStateStore(@ApplicationContext context: Context): WordleStateStore =
        WordleStateStore(context)

    @Provides
    @Singleton
    fun providePendingSyncStore(
        @ApplicationContext context: Context,
        gson: Gson
    ): PendingSyncStore = PendingSyncStore(context, gson)

    @Provides
    @Singleton
    fun provideNotificationSettingsStore(
        @ApplicationContext context: Context
    ): NotificationSettingsStore = NotificationSettingsStore(context)

    @Provides
    @Singleton
    fun provideReminderScheduler(
        @ApplicationContext context: Context
    ): ReminderScheduler = ReminderScheduler(context)

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor =
        NetworkMonitor(context)

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideApiService(authManager: AuthManager): ApiService =
        NetworkModule.createApiService(authManager)

    @Provides
    @Singleton
    fun provideSyncManager(
        authManager: AuthManager,
        repository: LearningRepository,
        apiService: ApiService,
        pendingSyncStore: PendingSyncStore
    ): com.example.master.sync.SyncManager = com.example.master.sync.SyncManager(
        authManager = authManager,
        repository = repository,
        apiService = apiService,
        pendingSyncStore = pendingSyncStore
    )

    @Provides
    @Singleton
    fun provideOfflineManager(
        @ApplicationContext context: Context,
        apiService: ApiService,
        repository: LearningRepository
    ): com.example.master.sync.OfflineManager = com.example.master.sync.OfflineManager(
        apiService = apiService,
        repository = repository,
        appContext = context
    )
}
