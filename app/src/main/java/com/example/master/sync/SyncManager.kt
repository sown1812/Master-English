package com.example.master.sync

import com.example.master.auth.AuthManager
import com.example.master.data.local.PendingSyncStore
import com.example.master.data.repository.LearningRepository
import com.example.master.network.ApiService
import com.example.master.network.LessonCompletedEventRemote
import com.example.master.network.SyncEventsPayloadRemote
import com.example.master.network.toEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val authManager: AuthManager,
    private val repository: LearningRepository,
    private val apiService: ApiService,
    private val pendingSyncStore: PendingSyncStore
) {
    suspend fun syncNow() {
        flushQueue()
    }

    suspend fun enqueueLessonCompleted(
        lessonId: Int,
        score: Int,
        correctAnswers: Int,
        wrongAnswers: Int,
        timeSpent: Long = 0
    ) {
        val userId = authManager.getCurrentUserId() ?: return
        val event = LessonCompletedEventRemote(
            eventId = UUID.randomUUID().toString(),
            occurredAt = System.currentTimeMillis(),
            lessonId = lessonId,
            score = score,
            correctAnswers = correctAnswers,
            wrongAnswers = wrongAnswers,
            timeSpent = timeSpent
        )
        pendingSyncStore.enqueue(
            SyncEventsPayloadRemote(
                userId = userId,
                lessonCompletions = listOf(event)
            )
        )
    }

    suspend fun flushQueue() {
        val queued = pendingSyncStore.getQueue().toMutableList()
        if (queued.isEmpty()) {
            pullSnapshot()
            return
        }

        val remaining = mutableListOf<SyncEventsPayloadRemote>()
        for (item in queued) {
            val result = runCatching { apiService.sync(item) }
            if (result.isSuccess) {
                result.getOrNull()?.let { response ->
                    applyResponse(item.userId, response)
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

    private suspend fun pullSnapshot() {
        val userId = authManager.getCurrentUserId() ?: return
        runCatching { apiService.sync(SyncEventsPayloadRemote(userId = userId)) }
            .onSuccess { response -> applyResponse(userId, response) }
    }

    private suspend fun applyResponse(userId: String, response: com.example.master.network.SyncResponseRemote) {
        response.user?.let { remoteUser ->
            val existing = repository.getUserByIdSync(remoteUser.userId)
            repository.replaceUser(remoteUser.toEntity(existing))
        }
        response.progress?.let { items ->
            repository.replaceProgress(userId, items.map { it.toEntity() })
        }
        response.achievements?.let { items ->
            repository.replaceAchievements(userId, items.map { it.toEntity() })
        }
    }
}
