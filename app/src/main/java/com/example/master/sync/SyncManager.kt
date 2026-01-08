package com.example.master.sync

import com.example.master.auth.AuthManager
import com.example.master.data.local.PendingSyncStore
import com.example.master.data.remote.RealtimeDatabaseService
import com.example.master.data.repository.LearningRepository
import com.example.master.network.AchievementRemote
import com.example.master.network.LessonCompletedEventRemote
import com.example.master.network.ProgressRemote
import com.example.master.network.SyncEventsPayloadRemote
import com.example.master.network.UserRemote
import com.example.master.network.toEntity
import com.example.master.network.toRemote
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val authManager: AuthManager,
    private val repository: LearningRepository,
    private val realtimeDatabaseService: RealtimeDatabaseService,
    private val pendingSyncStore: PendingSyncStore
) {
    suspend fun syncNow() {
        if (authManager.isAnonymous()) return
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
        if (authManager.isAnonymous()) return
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
        val userId = authManager.getCurrentUserId() ?: return
        if (authManager.isAnonymous()) return

        val queued = pendingSyncStore.getQueue()
        if (queued.isNotEmpty()) {
            pendingSyncStore.clear()
            pushSnapshot(userId)
        }
        pullSnapshot(userId)
    }

    private suspend fun pushSnapshot(userId: String) {
        val user = repository.getUserByIdSync(userId) ?: return
        val progress = repository.getUserProgressList(userId)
        val achievements = repository.getUserAchievementsList(userId)
        realtimeDatabaseService.saveUserProfile(userId, user.toRemote())
        realtimeDatabaseService.saveUserProgress(userId, progress.map { it.toRemote() })
        realtimeDatabaseService.saveUserAchievements(userId, achievements.map { it.toRemote() })
    }

    private suspend fun pullSnapshot(userId: String) {
        val remoteUser = realtimeDatabaseService.getUserProfile(userId)
        val remoteProgress = realtimeDatabaseService.getUserProgress(userId)
        val remoteAchievements = realtimeDatabaseService.getUserAchievements(userId)

        if (remoteUser == null) {
            pushSnapshot(userId)
            return
        }

        remoteUser?.let { user ->
            val existing = repository.getUserByIdSync(user.userId)
            val merged = mergeUser(existing, user)
            repository.replaceUser(merged)
        }

        if (remoteProgress.isNotEmpty()) {
            val local = repository.getUserProgressList(userId)
            val merged = mergeProgress(local, remoteProgress)
            repository.replaceProgress(userId, merged.map { it.toEntity() })
        }

        if (remoteAchievements.isNotEmpty()) {
            val local = repository.getUserAchievementsList(userId)
            val merged = mergeAchievements(local, remoteAchievements)
            repository.replaceAchievements(userId, merged.map { it.toEntity() })
        }
    }

    private fun mergeUser(
        existing: com.example.master.data.local.entity.UserEntity?,
        remote: UserRemote
    ): com.example.master.data.local.entity.UserEntity {
        if (existing == null) return remote.toEntity(null)
        return if (remote.updatedAt > existing.updatedAt) {
            remote.toEntity(existing)
        } else {
            existing
        }
    }

    private fun mergeProgress(
        local: List<com.example.master.data.local.entity.UserProgressEntity>,
        remote: List<ProgressRemote>
    ): List<ProgressRemote> {
        val localMap = local.associateBy { it.lessonId }
        val merged = mutableListOf<ProgressRemote>()
        remote.forEach { item ->
            val localItem = localMap[item.lessonId]
            val localUpdated = localItem?.updatedAt ?: 0L
            if (item.updatedAt >= localUpdated) {
                merged.add(item)
            } else {
                if (localItem != null) {
                    merged.add(localItem.toRemote())
                } else {
                    merged.add(item)
                }
            }
        }
        local.forEach { item ->
            if (remote.none { it.lessonId == item.lessonId }) {
                merged.add(item.toRemote())
            }
        }
        return merged
    }

    private fun mergeAchievements(
        local: List<com.example.master.data.local.entity.AchievementEntity>,
        remote: List<AchievementRemote>
    ): List<AchievementRemote> {
        val localMap = local.associateBy { it.achievementType }
        val merged = mutableListOf<AchievementRemote>()
        remote.forEach { item ->
            val localItem = localMap[item.achievementType]
            if (localItem == null) {
                merged.add(item)
            } else {
                val unlocked = item.isUnlocked || localItem.isUnlocked
                val progress = maxOf(item.progress, localItem.progress)
                val unlockedAt = listOfNotNull(item.unlockedAt, localItem.unlockedAt).maxOrNull()
                merged.add(
                    item.copy(
                        isUnlocked = unlocked,
                        progress = progress,
                        unlockedAt = unlockedAt
                    )
                )
            }
        }
        local.forEach { item ->
            if (remote.none { it.achievementType == item.achievementType }) {
                merged.add(item.toRemote())
            }
        }
        return merged
    }
}
