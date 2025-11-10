package com.example.server.model

import kotlinx.serialization.Serializable

@Serializable
data class LessonDto(
    val id: Int,
    val title: String,
    val description: String,
    val order: Int,
    val totalWords: Int,
    val totalExercises: Int,
    val difficulty: String,
    val category: String,
    val iconUrl: String? = null,
    val xpReward: Int,
    val coinsReward: Int,
    val isUnlocked: Boolean,
    val isPremium: Boolean
)

@Serializable
data class WordDto(
    val id: Int,
    val word: String,
    val translation: String,
    val pronunciation: String,
    val partOfSpeech: String,
    val exampleSentence: String,
    val exampleTranslation: String,
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val lessonId: Int,
    val difficulty: Int,
    val category: String
)

@Serializable
data class ExerciseDto(
    val id: Int,
    val lessonId: Int,
    val wordId: Int? = null,
    val type: String,
    val question: String,
    val correctAnswer: String,
    val optionA: String? = null,
    val optionB: String? = null,
    val optionC: String? = null,
    val optionD: String? = null,
    val matchPairs: String? = null,
    val hint: String? = null,
    val explanation: String? = null,
    val order: Int,
    val difficulty: Int
)

@Serializable
data class UserDto(
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

@Serializable
data class SaveProgressRequest(
    val userId: String,
    val lessonId: Int,
    val wordId: Int? = null,
    val isCompleted: Boolean,
    val score: Int,
    val accuracy: Double,
    val timeSpent: Long,
    val attempts: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val xpEarned: Int,
    val coinsEarned: Int,
    val reviewCount: Int,
    val easeFactor: Double
)

@Serializable
data class ProgressDto(
    val id: Int,
    val userId: String,
    val lessonId: Int,
    val wordId: Int? = null,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val score: Int,
    val accuracy: Double,
    val timeSpent: Long,
    val attempts: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val xpEarned: Int,
    val coinsEarned: Int,
    val lastReviewDate: Long?,
    val nextReviewDate: Long?,
    val reviewCount: Int,
    val easeFactor: Double,
    val createdAt: Long,
    val updatedAt: Long
)
