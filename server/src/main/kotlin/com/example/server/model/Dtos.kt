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
