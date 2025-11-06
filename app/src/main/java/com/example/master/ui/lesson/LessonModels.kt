package com.example.master.ui.lesson

import com.example.master.data.local.entity.WordEntity

const val DEFAULT_LESSON_HEARTS = 5
const val PASS_ACCURACY_THRESHOLD = 0.7f

data class LessonUiState(
    val lessonId: Int = 0,
    val lessonTitle: String = "",
    val currentExerciseIndex: Int = 0,
    val totalExercises: Int = 0,
    val exercises: List<Exercise> = emptyList(),
    val score: Int = 0,
    val correctAnswers: Int = 0,
    val wrongAnswers: Int = 0,
    val accuracy: Float = 0f,
    val hearts: Int = DEFAULT_LESSON_HEARTS,
    val totalHearts: Int = DEFAULT_LESSON_HEARTS,
    val lastXpEarned: Int = 0,
    val lastCoinsEarned: Int = 0,
    val isAnswerReady: Boolean = false,
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false,
    val isLoading: Boolean = true,
    val showResult: Boolean = false,
    val lastAnswerCorrect: Boolean? = null,
    val feedbackMessage: String? = null,
    val explanation: String? = null
)

sealed class Exercise {
    abstract val id: Int
    abstract val question: String
    abstract val correctAnswer: String
    abstract val word: WordEntity?
    abstract val explanation: String?

    data class MultipleChoice(
        override val id: Int,
        override val question: String,
        override val correctAnswer: String,
        override val word: WordEntity?,
        override val explanation: String?,
        val options: List<String>,
        val selectedAnswer: String? = null
    ) : Exercise()

    data class FillBlank(
        override val id: Int,
        override val question: String,
        override val correctAnswer: String,
        override val word: WordEntity?,
        override val explanation: String?,
        val hint: String? = null,
        val userAnswer: String = ""
    ) : Exercise()

    data class Matching(
        override val id: Int,
        override val question: String,
        override val correctAnswer: String,
        override val word: WordEntity?,
        override val explanation: String?,
        val pairs: List<MatchPair>,
        val selectedPairs: Map<String, String> = emptyMap()
    ) : Exercise()

    data class Translation(
        override val id: Int,
        override val question: String,
        override val correctAnswer: String,
        override val word: WordEntity?,
        override val explanation: String?,
        val userAnswer: String = ""
    ) : Exercise()

    data class Listening(
        override val id: Int,
        override val question: String,
        override val correctAnswer: String,
        override val word: WordEntity?,
        override val explanation: String?,
        val audioUrl: String?,
        val options: List<String>,
        val selectedAnswer: String? = null
    ) : Exercise()

    data class Speaking(
        override val id: Int,
        override val question: String,
        override val correctAnswer: String,
        override val word: WordEntity?,
        override val explanation: String?,
        val prompt: String,
        val recognizedText: String = "",
        val confidence: Float? = null
    ) : Exercise()

    data class PictureMatching(
        override val id: Int,
        override val question: String,
        override val correctAnswer: String,
        override val word: WordEntity?,
        override val explanation: String?,
        val options: List<PictureOption>,
        val selectedOptionId: String? = null
    ) : Exercise()
}

data class MatchPair(
    val left: String,
    val right: String
)

data class PictureOption(
    val id: String,
    val label: String,
    val imageUrl: String?
)

sealed class LessonEvent {
    data class AnswerSelected(val answer: String) : LessonEvent()
    data class FillBlankAnswered(val answer: String) : LessonEvent()
    data class PairMatched(val left: String, val right: String) : LessonEvent()
    data class PictureOptionSelected(val optionId: String) : LessonEvent()
    data class SpeakingAnswerCaptured(val transcript: String) : LessonEvent()
    object SubmitAnswer : LessonEvent()
    object NextExercise : LessonEvent()
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
