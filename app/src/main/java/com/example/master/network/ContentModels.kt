package com.example.master.network

import com.example.master.data.local.entity.ExerciseEntity
import com.example.master.data.local.entity.LessonEntity
import com.example.master.data.local.entity.WordEntity

data class LessonRemote(
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

data class WordRemote(
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

data class ExerciseRemote(
    val id: Int,
    val lessonId: Int,
    val wordId: Int?,
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

fun LessonRemote.toEntity(): LessonEntity = LessonEntity(
    id = id,
    title = title,
    description = description,
    order = order,
    totalWords = totalWords,
    totalExercises = totalExercises,
    difficulty = difficulty,
    category = category,
    iconUrl = iconUrl,
    xpReward = xpReward,
    coinsReward = coinsReward,
    isUnlocked = isUnlocked,
    isPremium = isPremium,
    levelId = 0
)

fun WordRemote.toEntity(): WordEntity = WordEntity(
    word = word,
    translation = translation,
    pronunciation = pronunciation,
    partOfSpeech = partOfSpeech,
    exampleSentence = exampleSentence,
    exampleTranslation = exampleTranslation,
    imageUrl = imageUrl,
    audioUrl = audioUrl,
    lessonId = lessonId,
    difficulty = difficulty,
    category = category,
    id = id
)

fun ExerciseRemote.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id,
    lessonId = lessonId,
    wordId = wordId ?: 0,
    type = type,
    question = question,
    correctAnswer = correctAnswer,
    optionA = optionA,
    optionB = optionB,
    optionC = optionC,
    optionD = optionD,
    matchPairs = matchPairs,
    hint = hint,
    explanation = explanation,
    order = order,
    difficulty = difficulty
)
