package com.example.master.network

import com.example.master.data.local.entity.AchievementEntity
import com.example.master.data.local.entity.UserEntity
import com.example.master.data.local.entity.UserProgressEntity

data class SyncEventsPayloadRemote(
    val userId: String,
    val lessonCompletions: List<LessonCompletedEventRemote> = emptyList()
)

data class LessonCompletedEventRemote(
    val eventId: String,
    val occurredAt: Long,
    val lessonId: Int,
    val score: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val timeSpent: Long = 0
)

data class SyncResponseRemote(
    val user: UserRemote?,
    val progress: List<ProgressRemote>?,
    val achievements: List<AchievementRemote>?
)

data class UserRemote(
    val userId: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val currentLevel: Int,
    val totalXp: Int,
    val coins: Int,
    val streakDays: Int,
    val lastStudyDate: Long,
    val longestStreak: Int,
    val wordsLearned: Int,
    val lessonsCompleted: Int,
    val exercisesCompleted: Int,
    val isPremium: Boolean,
    val premiumExpiryDate: Long? = null
)

data class ProgressRemote(
    val id: Int,
    val userId: String,
    val lessonId: Int,
    val wordId: Int? = null,
    val isCompleted: Boolean,
    val completedAt: Long? = null,
    val score: Int,
    val accuracy: Double,
    val timeSpent: Long,
    val attempts: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val xpEarned: Int,
    val coinsEarned: Int,
    val lastReviewDate: Long? = null,
    val nextReviewDate: Long? = null,
    val reviewCount: Int,
    val easeFactor: Double,
    val createdAt: Long,
    val updatedAt: Long
)

data class AchievementRemote(
    val id: Int,
    val userId: String,
    val achievementType: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val unlockedAt: Long? = null,
    val progress: Int,
    val target: Int,
    val xpReward: Int,
    val coinsReward: Int,
    val badgeUrl: String? = null,
    val createdAt: Long
)

fun UserEntity.toRemote(): UserRemote =
    UserRemote(
        userId = userId,
        email = email,
        displayName = displayName,
        avatarUrl = avatarUrl,
        currentLevel = currentLevel,
        totalXp = totalXP,
        coins = coins,
        streakDays = streakDays,
        lastStudyDate = lastStudyDate,
        longestStreak = longestStreak,
        wordsLearned = wordsLearned,
        lessonsCompleted = lessonsCompleted,
        exercisesCompleted = exercisesCompleted,
        isPremium = isPremium,
        premiumExpiryDate = premiumExpiryDate
    )

fun UserRemote.toEntity(existing: UserEntity? = null): UserEntity {
    val now = System.currentTimeMillis()
    return UserEntity(
        userId = userId,
        email = email,
        displayName = displayName,
        avatarUrl = avatarUrl,
        currentLevel = currentLevel,
        totalXP = totalXp,
        coins = coins,
        streakDays = streakDays,
        lastStudyDate = lastStudyDate,
        longestStreak = longestStreak,
        wordsLearned = wordsLearned,
        lessonsCompleted = lessonsCompleted,
        exercisesCompleted = exercisesCompleted,
        isPremium = isPremium,
        premiumExpiryDate = premiumExpiryDate,
        createdAt = existing?.createdAt ?: now,
        updatedAt = now,
        lastSyncedAt = now
    )
}

fun UserProgressEntity.toRemote(): ProgressRemote {
    val percent = if (accuracy > 1f) accuracy.toDouble() else (accuracy.toDouble() * 100.0)
    return ProgressRemote(
        id = id,
        userId = userId,
        lessonId = lessonId,
        wordId = wordId,
        isCompleted = isCompleted,
        completedAt = completedAt,
        score = score,
        accuracy = percent.coerceIn(0.0, 100.0),
        timeSpent = timeSpent,
        attempts = attempts,
        correctAnswers = correctAnswers,
        wrongAnswers = wrongAnswers,
        xpEarned = xpEarned,
        coinsEarned = coinsEarned,
        lastReviewDate = lastReviewDate,
        nextReviewDate = nextReviewDate,
        reviewCount = reviewCount,
        easeFactor = easeFactor.toDouble(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ProgressRemote.toEntity(): UserProgressEntity =
    UserProgressEntity(
        id = id,
        userId = userId,
        lessonId = lessonId,
        wordId = wordId,
        isCompleted = isCompleted,
        completedAt = completedAt,
        score = score,
        accuracy = (accuracy / 100.0).coerceIn(0.0, 1.0).toFloat(),
        timeSpent = timeSpent,
        attempts = attempts,
        correctAnswers = correctAnswers,
        wrongAnswers = wrongAnswers,
        xpEarned = xpEarned,
        coinsEarned = coinsEarned,
        lastReviewDate = lastReviewDate,
        nextReviewDate = nextReviewDate,
        reviewCount = reviewCount,
        easeFactor = easeFactor.toFloat(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun AchievementEntity.toRemote(): AchievementRemote =
    AchievementRemote(
        id = id,
        userId = userId,
        achievementType = achievementType,
        title = title,
        description = description,
        isUnlocked = isUnlocked,
        unlockedAt = unlockedAt,
        progress = progress,
        target = target,
        xpReward = xpReward,
        coinsReward = coinsReward,
        badgeUrl = badgeUrl,
        createdAt = createdAt
    )

fun AchievementRemote.toEntity(): AchievementEntity =
    AchievementEntity(
        id = id,
        userId = userId,
        achievementType = achievementType,
        title = title,
        description = description,
        isUnlocked = isUnlocked,
        unlockedAt = unlockedAt,
        progress = progress,
        target = target,
        xpReward = xpReward,
        coinsReward = coinsReward,
        badgeUrl = badgeUrl,
        createdAt = createdAt
    )
