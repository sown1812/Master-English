package com.example.master.ui.lesson

import com.example.master.data.local.entity.ExerciseEntity
import com.example.master.data.local.entity.WordEntity

data class LessonUiState(
    val lessonId: Int = 0,
    val lessonTitle: String = "",
    val currentExerciseIndex: Int = 0,
    val totalExercises: Int = 0,
    val exercises: List<Exercise> = emptyList(),
    val score: Int = 0,
    val correctAnswers: Int = 0,
    val wrongAnswers: Int = 0,
    val isCompleted: Boolean = false,
    val isLoading: Boolean = true,
    val hearts: Int = 5,
    val showResult: Boolean = false,
    val lastAnswerCorrect: Boolean? = null
)

sealed class Exercise {
    abstract val id: Int
    abstract val question: String
    abstract val correctAnswer: String
    abstract val word: WordEntity?
    
    data class MultipleChoice(
        override val id: Int,
        override val question: String,
        override val correctAnswer: String,
        override val word: WordEntity?,
        val options: List<String>,
        val selectedAnswer: String? = null
    ) : Exercise()
    
    data class FillBlank(
        override val id: Int,
        override val question: String,
        override val correctAnswer: String,
        override val word: WordEntity?,
        val hint: String? = null,
        val userAnswer: String = ""
    ) : Exercise()
    
    data class Matching(
        override val id: Int,
        override val question: String,
        override val correctAnswer: String,
        override val word: WordEntity?,
        val pairs: List<MatchPair>,
        val selectedPairs: Map<String, String> = emptyMap()
    ) : Exercise()
    
    data class Translation(
        override val id: Int,
        override val question: String,
        override val correctAnswer: String,
        override val word: WordEntity?,
        val userAnswer: String = ""
    ) : Exercise()
}

data class MatchPair(
    val left: String,
    val right: String,
    val isMatched: Boolean = false
)

sealed class LessonEvent {
    data class AnswerSelected(val answer: String) : LessonEvent()
    data class FillBlankAnswered(val answer: String) : LessonEvent()
    data class PairMatched(val left: String, val right: String) : LessonEvent()
    object SubmitAnswer : LessonEvent()
    object NextExercise : LessonEvent()
    object PlayAudio : LessonEvent()
    object ShowHint : LessonEvent()
    object RetryLesson : LessonEvent()
    object ExitLesson : LessonEvent()
}

data class LessonResult(
    val lessonId: Int,
    val totalExercises: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val accuracy: Float,
    val xpEarned: Int,
    val coinsEarned: Int,
    val isPassed: Boolean
)
