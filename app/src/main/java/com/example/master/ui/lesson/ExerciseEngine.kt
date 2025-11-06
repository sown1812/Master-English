package com.example.master.ui.lesson

import kotlin.math.roundToInt

private val POSITIVE_FEEDBACK = listOf(
    "Excellent work!",
    "Great job! Keep it up!",
    "Perfect! You're on fire!",
    "Nice! That was spot on.",
    "Awesome! You're learning fast."
)

private val ENCOURAGEMENT_FEEDBACK = listOf(
    "Almost there, try again!",
    "Don't worry, keep practicing!",
    "You can do this, give it another shot.",
    "Every mistake is a step forward.",
    "Take a moment and try once more."
)

class ExerciseEngine {
    
    fun evaluate(exercise: Exercise): ExerciseEvaluation {
        val isCorrect = when (exercise) {
            is Exercise.MultipleChoice -> exercise.selectedAnswer.equals(
                exercise.correctAnswer,
                ignoreCase = true
            )
            is Exercise.FillBlank -> exercise.userAnswer.trim().equals(
                exercise.correctAnswer, ignoreCase = true
            )
            is Exercise.Matching -> exercise.pairs.all { pair ->
                exercise.selectedPairs[pair.left] == pair.right
            }
            is Exercise.Translation -> exercise.userAnswer.trim().equals(
                exercise.correctAnswer, ignoreCase = true
            )
            is Exercise.Listening -> exercise.selectedAnswer.equals(
                exercise.correctAnswer, ignoreCase = true
            )
            is Exercise.Speaking -> exercise.recognizedText.trim().equals(
                exercise.correctAnswer, ignoreCase = true
            )
            is Exercise.PictureMatching -> exercise.selectedOptionId.equals(
                exercise.correctAnswer,
                ignoreCase = true
            )
        }
        
        val feedback = if (isCorrect) {
            POSITIVE_FEEDBACK.random()
        } else {
            ENCOURAGEMENT_FEEDBACK.random()
        }
        
        val word = exercise.word
        val explanation = when {
            exercise.explanation != null -> exercise.explanation
            word?.exampleSentence != null && !isCorrect -> {
                "Example: ${word.exampleSentence}"
            }
            else -> null
        }
        
        val scoreDelta = if (isCorrect) 15 else 0
        val heartsDelta = if (isCorrect) 0 else -1
        
        return ExerciseEvaluation(
            isCorrect = isCorrect,
            scoreDelta = scoreDelta,
            heartsDelta = heartsDelta,
            feedback = feedback,
            explanation = explanation
        )
    }
    
    fun calculateAccuracy(correctAnswers: Int, totalAttempts: Int): Float {
        if (totalAttempts <= 0) return 0f
        return correctAnswers.toFloat() / totalAttempts.toFloat()
    }
    
    fun calculateRewards(score: Int, accuracy: Float): LessonRewards {
        val baseXp = 40
        val accuracyBonus = (accuracy * 60f).roundToInt()
        val performanceBonus = (score * 0.4f).roundToInt()
        val xp = (baseXp + accuracyBonus + performanceBonus).coerceAtLeast(10)
        
        val coins = when {
            accuracy >= 0.9f -> 25
            accuracy >= 0.75f -> 18
            accuracy >= 0.6f -> 12
            else -> 6
        }
        
        return LessonRewards(
            xp = xp,
            coins = coins
        )
    }
}

data class ExerciseEvaluation(
    val isCorrect: Boolean,
    val scoreDelta: Int,
    val heartsDelta: Int,
    val feedback: String,
    val explanation: String?
)

data class LessonRewards(
    val xp: Int,
    val coins: Int
)
