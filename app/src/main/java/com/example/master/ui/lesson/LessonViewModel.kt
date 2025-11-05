package com.example.master.ui.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.master.data.local.entity.UserProgressEntity
import com.example.master.data.repository.LearningRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LessonViewModel(
    private val repository: LearningRepository,
    private val lessonId: Int
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LessonUiState(lessonId = lessonId))
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()
    
    init {
        loadLesson()
    }
    
    private fun loadLesson() {
        viewModelScope.launch {
            try {
                val lesson = repository.getLessonById(lessonId)
                val exercises = repository.getExercisesByLesson(lessonId).first()
                val words = repository.getWordsByLesson(lessonId).first()
                
                val exerciseList = exercises.map { exerciseEntity ->
                    val word = words.find { it.id == exerciseEntity.wordId }
                    
                    when (exerciseEntity.type) {
                        "MULTIPLE_CHOICE" -> Exercise.MultipleChoice(
                            id = exerciseEntity.id,
                            question = exerciseEntity.question,
                            correctAnswer = exerciseEntity.correctAnswer,
                            word = word,
                            options = listOf(
                                exerciseEntity.optionA,
                                exerciseEntity.optionB,
                                exerciseEntity.optionC,
                                exerciseEntity.optionD
                            ).filterNotNull().shuffled()
                        )
                        
                        "FILL_BLANK" -> Exercise.FillBlank(
                            id = exerciseEntity.id,
                            question = exerciseEntity.question,
                            correctAnswer = exerciseEntity.correctAnswer,
                            word = word,
                            hint = exerciseEntity.hint
                        )
                        
                        "MATCHING" -> {
                            // Parse match pairs from JSON or create from words
                            val pairs = words.take(4).map { w ->
                                MatchPair(w.word, w.translation)
                            }.shuffled()
                            
                            Exercise.Matching(
                                id = exerciseEntity.id,
                                question = "Match the words with their translations",
                                correctAnswer = "",
                                word = null,
                                pairs = pairs
                            )
                        }
                        
                        "TRANSLATION" -> Exercise.Translation(
                            id = exerciseEntity.id,
                            question = exerciseEntity.question,
                            correctAnswer = exerciseEntity.correctAnswer,
                            word = word
                        )
                        
                        else -> Exercise.MultipleChoice(
                            id = exerciseEntity.id,
                            question = exerciseEntity.question,
                            correctAnswer = exerciseEntity.correctAnswer,
                            word = word,
                            options = listOf(exerciseEntity.correctAnswer)
                        )
                    }
                }
                
                _uiState.update {
                    it.copy(
                        lessonTitle = lesson?.title ?: "Lesson",
                        exercises = exerciseList,
                        totalExercises = exerciseList.size,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun onEvent(event: LessonEvent) {
        when (event) {
            is LessonEvent.AnswerSelected -> handleAnswerSelected(event.answer)
            is LessonEvent.FillBlankAnswered -> handleFillBlankAnswered(event.answer)
            is LessonEvent.PairMatched -> handlePairMatched(event.left, event.right)
            LessonEvent.SubmitAnswer -> submitAnswer()
            LessonEvent.NextExercise -> nextExercise()
            LessonEvent.PlayAudio -> playAudio()
            LessonEvent.ShowHint -> showHint()
            LessonEvent.RetryLesson -> retryLesson()
            LessonEvent.ExitLesson -> {}
        }
    }
    
    private fun handleAnswerSelected(answer: String) {
        val currentExercise = getCurrentExercise() as? Exercise.MultipleChoice ?: return
        
        val updatedExercises = _uiState.value.exercises.toMutableList()
        updatedExercises[_uiState.value.currentExerciseIndex] = 
            currentExercise.copy(selectedAnswer = answer)
        
        _uiState.update { it.copy(exercises = updatedExercises) }
    }
    
    private fun handleFillBlankAnswered(answer: String) {
        val currentExercise = getCurrentExercise()
        
        val updatedExercises = _uiState.value.exercises.toMutableList()
        when (currentExercise) {
            is Exercise.FillBlank -> {
                updatedExercises[_uiState.value.currentExerciseIndex] = 
                    currentExercise.copy(userAnswer = answer)
            }
            is Exercise.Translation -> {
                updatedExercises[_uiState.value.currentExerciseIndex] = 
                    currentExercise.copy(userAnswer = answer)
            }
            else -> return
        }
        
        _uiState.update { it.copy(exercises = updatedExercises) }
    }
    
    private fun handlePairMatched(left: String, right: String) {
        val currentExercise = getCurrentExercise() as? Exercise.Matching ?: return
        
        val updatedPairs = currentExercise.selectedPairs.toMutableMap()
        updatedPairs[left] = right
        
        val updatedExercises = _uiState.value.exercises.toMutableList()
        updatedExercises[_uiState.value.currentExerciseIndex] = 
            currentExercise.copy(selectedPairs = updatedPairs)
        
        _uiState.update { it.copy(exercises = updatedExercises) }
    }
    
    private fun submitAnswer() {
        val currentExercise = getCurrentExercise() ?: return
        val isCorrect = checkAnswer(currentExercise)
        
        _uiState.update {
            it.copy(
                showResult = true,
                lastAnswerCorrect = isCorrect,
                correctAnswers = if (isCorrect) it.correctAnswers + 1 else it.correctAnswers,
                wrongAnswers = if (!isCorrect) it.wrongAnswers + 1 else it.wrongAnswers,
                hearts = if (!isCorrect) (it.hearts - 1).coerceAtLeast(0) else it.hearts,
                score = if (isCorrect) it.score + 10 else it.score
            )
        }
    }
    
    private fun checkAnswer(exercise: Exercise): Boolean {
        return when (exercise) {
            is Exercise.MultipleChoice -> {
                exercise.selectedAnswer?.equals(exercise.correctAnswer, ignoreCase = true) == true
            }
            is Exercise.FillBlank -> {
                exercise.userAnswer.trim().equals(exercise.correctAnswer, ignoreCase = true)
            }
            is Exercise.Matching -> {
                exercise.pairs.all { pair ->
                    exercise.selectedPairs[pair.left] == pair.right
                }
            }
            is Exercise.Translation -> {
                exercise.userAnswer.trim().equals(exercise.correctAnswer, ignoreCase = true)
            }
        }
    }
    
    private fun nextExercise() {
        val nextIndex = _uiState.value.currentExerciseIndex + 1
        
        if (nextIndex >= _uiState.value.totalExercises) {
            completeLesson()
        } else {
            _uiState.update {
                it.copy(
                    currentExerciseIndex = nextIndex,
                    showResult = false,
                    lastAnswerCorrect = null
                )
            }
        }
    }
    
    private fun completeLesson() {
        viewModelScope.launch {
            val state = _uiState.value
            val accuracy = state.correctAnswers.toFloat() / state.totalExercises
            val xpEarned = calculateXP(state.score, accuracy)
            val coinsEarned = calculateCoins(state.score, accuracy)
            
            // Save progress
            val userId = repository.getCurrentUserSync()?.userId ?: return@launch
            
            val progress = UserProgressEntity(
                userId = userId,
                lessonId = lessonId,
                isCompleted = accuracy >= 0.7f,
                score = state.score,
                accuracy = accuracy,
                attempts = 1,
                correctAnswers = state.correctAnswers,
                wrongAnswers = state.wrongAnswers,
                xpEarned = xpEarned,
                coinsEarned = coinsEarned,
                completedAt = if (accuracy >= 0.7f) System.currentTimeMillis() else null
            )
            
            repository.saveProgress(progress)
            
            _uiState.update {
                it.copy(
                    isCompleted = true,
                    showResult = false
                )
            }
        }
    }
    
    private fun calculateXP(score: Int, accuracy: Float): Int {
        val baseXP = 50
        val bonusXP = (score * accuracy).toInt()
        return baseXP + bonusXP
    }
    
    private fun calculateCoins(score: Int, accuracy: Float): Int {
        return when {
            accuracy >= 0.9f -> 20
            accuracy >= 0.7f -> 10
            else -> 5
        }
    }
    
    private fun playAudio() {
        // TODO: Implement TTS
        val currentExercise = getCurrentExercise()
        currentExercise?.word?.let { word ->
            // Play audio for word
        }
    }
    
    private fun showHint() {
        // TODO: Show hint (costs coins)
    }
    
    private fun retryLesson() {
        _uiState.update {
            LessonUiState(
                lessonId = lessonId,
                lessonTitle = it.lessonTitle,
                exercises = it.exercises,
                totalExercises = it.totalExercises,
                isLoading = false
            )
        }
    }
    
    private fun getCurrentExercise(): Exercise? {
        return _uiState.value.exercises.getOrNull(_uiState.value.currentExerciseIndex)
    }
    
    fun getLessonResult(): LessonResult {
        val state = _uiState.value
        val accuracy = state.correctAnswers.toFloat() / state.totalExercises
        
        return LessonResult(
            lessonId = lessonId,
            totalExercises = state.totalExercises,
            correctAnswers = state.correctAnswers,
            wrongAnswers = state.wrongAnswers,
            accuracy = accuracy,
            xpEarned = calculateXP(state.score, accuracy),
            coinsEarned = calculateCoins(state.score, accuracy),
            isPassed = accuracy >= 0.7f
        )
    }
}
