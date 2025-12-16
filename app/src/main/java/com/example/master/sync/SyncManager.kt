package com.example.master.sync

import com.example.master.auth.AuthManager
import com.example.master.data.local.PendingSyncStore
import com.example.master.data.repository.LearningRepository
import com.example.master.network.ApiService
import com.example.master.network.SyncPayloadRemote
import com.example.master.network.toEntity
import com.example.master.network.toRemote
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.firstOrNull

@Singleton
class SyncManager @Inject constructor(
    private val authManager: AuthManager,
    private val repository: LearningRepository,
    private val apiService: ApiService,
    private val pendingSyncStore: PendingSyncStore
) {
    suspend fun syncNow() {
        enqueueLatestState()
        flushQueue()
    }

    /**
     * Lưu snapshot hiện tại (user + progress + achievements) vào hàng đợi để đồng bộ khi có mạng.
     */
    suspend fun enqueueLatestState() {
        val userId = authManager.getCurrentUserId() ?: return
        val user = repository.getUserByIdSync(userId) ?: return
        val progress = repository.getUserProgress(userId).firstOrNull().orEmpty()
        val achievements = repository.getUserAchievements(userId).firstOrNull().orEmpty()

        val payload = SyncPayloadRemote(
            user = user.toRemote(),
            progress = progress.map { it.toRemote() },
            achievements = achievements.map { it.toRemote() }
        )

        pendingSyncStore.enqueue(payload)
    }

    /**
     * Đẩy toàn bộ queue lên server, giữ lại các mục lỗi để thử lại lần sau.
     */
    suspend fun flushQueue() {
        val queued = pendingSyncStore.getQueue().toMutableList()
        if (queued.isEmpty()) return

        val remaining = mutableListOf<SyncPayloadRemote>()
        for (item in queued) {
            val result = runCatching { apiService.sync(item) }
            if (result.isSuccess) {
                result.getOrNull()?.let { response ->
                    response.user?.let { remoteUser ->
                        val existing = repository.getUserByIdSync(remoteUser.userId)
                        repository.replaceUser(remoteUser.toEntity(existing))
                    }
                    response.progress?.let { items ->
                        repository.replaceProgress(item.user.userId, items.map { it.toEntity() })
                    }
                    response.achievements?.let { items ->
                        repository.replaceAchievements(item.user.userId, items.map { it.toEntity() })
                    }
                }
            } else {
                remaining.add(item)
            }
        }

        if (remaining.isEmpty()) {
            pendingSyncStore.clear()
        } else {
            pendingSyncStore.saveQueue(remaining)
        }
    }
}
