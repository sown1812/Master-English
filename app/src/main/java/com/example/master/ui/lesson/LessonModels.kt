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
    val explanation: String? = null,
    val hintText: String? = null,
    val networkError: String? = null
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

    data class WordTiles(
        override val id: Int,
        override val question: String,
        override val correctAnswer: String,
        override val word: WordEntity?,
        override val explanation: String?,
        val tiles: List<String>,
        val selectedWords: List<String> = emptyList()
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

    data class Flashcard(
        override val id: Int,
        override val question: String,
        override val correctAnswer: String,
        override val word: WordEntity?,
        override val explanation: String?,
        val frontText: String,
        val backText: String,
        val isFlipped: Boolean = false,
        val isRemembered: Boolean? = null
    ) : Exercise()

    data class SpeedMatching(
        override val id: Int,
        override val question: String,
        override val correctAnswer: String,
        override val word: WordEntity?,
        override val explanation: String?,
        val pairs: List<SpeedMatchPair>,
        val wordOrder: List<String> = emptyList(),
        val matchedIds: Set<String> = emptySet(),
        val selectedClueId: String? = null,
        val selectedWordId: String? = null,
        val combo: Int = 0,
        val bestCombo: Int = 0,
        val scoreEarned: Int = 0,
        val timeLeftSec: Int = 45,
        val timeLimitSec: Int = 45,
        val isExpired: Boolean = false
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

data class SpeedMatchPair(
    val id: String,
    val word: String,
    val clue: String,
    val imageUrl: String? = null
)

sealed class LessonEvent {
    data class AnswerSelected(val answer: String) : LessonEvent()
    data class FillBlankAnswered(val answer: String) : LessonEvent()
    data class PairMatched(val left: String, val right: String) : LessonEvent()
    data class PictureOptionSelected(val optionId: String) : LessonEvent()
    data class SpeakingAnswerCaptured(val transcript: String) : LessonEvent()
    data class FlashcardFlipped(val flipped: Boolean) : LessonEvent()
    data class FlashcardRated(val remembered: Boolean) : LessonEvent()
    data class SpeedMatchClueSelected(val clueId: String) : LessonEvent()
    data class SpeedMatchWordSelected(val wordId: String) : LessonEvent()
    data class WordTileSelected(val word: String) : LessonEvent()
    data class WordTileRemoved(val index: Int) : LessonEvent()
    object SpeedMatchTick : LessonEvent()
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
