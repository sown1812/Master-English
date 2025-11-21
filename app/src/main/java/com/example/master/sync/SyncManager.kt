package com.example.master.sync

import com.example.master.auth.AuthManager
import com.example.master.data.repository.LearningRepository
import com.example.master.network.ApiService
import com.example.master.network.SyncPayload
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.firstOrNull

@Singleton
class SyncManager @Inject constructor(
    private val authManager: AuthManager,
    private val repository: LearningRepository,
    private val apiService: ApiService
) {
    suspend fun syncNow() {
        val userId = authManager.getCurrentUserId() ?: return
        val user = repository.getUserByIdSync(userId) ?: return
        val progress = repository.getUserProgress(userId).firstOrNull().orEmpty()
        val achievements = repository.getUserAchievements(userId).firstOrNull().orEmpty()

        val payload = SyncPayload(
            user = user,
            progress = progress,
            achievements = achievements
        )

        runCatching { apiService.sync(payload) }
            .onSuccess { response ->
                response.user?.let { repository.replaceUser(it) }
                response.progress?.let { repository.replaceProgress(userId, it) }
                response.achievements?.let { repository.replaceAchievements(userId, it) }
            }
    }
}
