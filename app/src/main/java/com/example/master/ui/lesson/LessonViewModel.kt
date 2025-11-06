package com.example.master.ui.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.master.data.local.entity.UserProgressEntity
import com.example.master.data.repository.LearningRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val FAIL_XP_REWARD = 10
private const val FAIL_COIN_REWARD = 4

class LessonViewModel(
    private val repository: LearningRepository,
    private val lessonId: Int
) : ViewModel() {
    
    private val gson = Gson()
    private val engine = ExerciseEngine()
    
    private val _uiState = MutableStateFlow(LessonUiState(lessonId = lessonId))
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()
    
    init {
        loadLesson()
    }
    
    private fun loadLesson() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val lesson = repository.getLessonById(lessonId)
                val exercises = repository.getExercisesByLesson(lessonId).first()
                val words = repository.getWordsByLesson(lessonId).first()
                
                val exerciseList = exercises
                    .sortedBy { it.order }
                    .map { exerciseEntity ->
                        val word = words.find { it.id == exerciseEntity.wordId }
                        when (exerciseEntity.type.uppercase()) {
                            "MULTIPLE_CHOICE" -> Exercise.MultipleChoice(
                                id = exerciseEntity.id,
                                question = exerciseEntity.question,
                                correctAnswer = exerciseEntity.correctAnswer,
                                word = word,
                                explanation = exerciseEntity.explanation,
                                options = listOfNotNull(
                                    exerciseEntity.optionA,
                                    exerciseEntity.optionB,
                                    exerciseEntity.optionC,
                                    exerciseEntity.optionD,
                                    exerciseEntity.correctAnswer
                                ).distinct().shuffled()
                            )
                            
                            "FILL_BLANK" -> Exercise.FillBlank(
                                id = exerciseEntity.id,
                                question = exerciseEntity.question,
                                correctAnswer = exerciseEntity.correctAnswer,
                                word = word,
                                explanation = exerciseEntity.explanation,
                                hint = exerciseEntity.hint
                            )
                            
                            "TRANSLATION" -> Exercise.Translation(
                                id = exerciseEntity.id,
                                question = exerciseEntity.question,
                                correctAnswer = exerciseEntity.correctAnswer,
                                word = word,
                                explanation = exerciseEntity.explanation
                            )
                            
                            "MATCHING" -> Exercise.Matching(
                                id = exerciseEntity.id,
                                question = exerciseEntity.question.ifBlank { "Match the words with their translations" },
                                correctAnswer = "",
                                word = word,
                                explanation = exerciseEntity.explanation,
                                pairs = parseMatchPairs(exerciseEntity.matchPairs, words)
                            )
                            
                            "LISTENING" -> Exercise.Listening(
                                id = exerciseEntity.id,
                                question = exerciseEntity.question.ifBlank { "Listen and choose the correct answer" },
                                correctAnswer = exerciseEntity.correctAnswer,
                                word = word,
                                explanation = exerciseEntity.explanation,
                                audioUrl = word?.audioUrl,
                                options = buildListeningOptions(exerciseEntity, words)
                            )
                            
                            "SPEAKING" -> Exercise.Speaking(
                                id = exerciseEntity.id,
                                question = exerciseEntity.question.ifBlank { "Speak the highlighted phrase" },
                                correctAnswer = exerciseEntity.correctAnswer,
                                word = word,
                                explanation = exerciseEntity.explanation,
                                prompt = exerciseEntity.question.ifBlank {
                                    word?.word ?: exerciseEntity.correctAnswer
                                }
                            )
                            
                            "PICTURE_MATCH", "PICTURE" -> Exercise.PictureMatching(
                                id = exerciseEntity.id,
                                question = exerciseEntity.question.ifBlank { "Tap the picture that matches the word" },
                                correctAnswer = word?.word ?: exerciseEntity.correctAnswer,
                                word = word,
                                explanation = exerciseEntity.explanation,
                                options = buildPictureOptions(exerciseEntity, words, word)
                            )
                            
                            else -> Exercise.MultipleChoice(
                                id = exerciseEntity.id,
                                question = exerciseEntity.question,
                                correctAnswer = exerciseEntity.correctAnswer,
                                word = word,
                                explanation = exerciseEntity.explanation,
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
            is LessonEvent.PictureOptionSelected -> handlePictureOptionSelected(event.optionId)
            is LessonEvent.SpeakingAnswerCaptured -> handleSpeakingTranscript(event.transcript)
            LessonEvent.SubmitAnswer -> submitAnswer()
            LessonEvent.NextExercise -> nextExercise()
            LessonEvent.ShowHint -> showHint()
            LessonEvent.RetryLesson -> retryLesson()
            LessonEvent.ExitLesson -> Unit
        }
    }
    
    private fun handleAnswerSelected(answer: String) {
        val currentExercise = getCurrentExercise() ?: return
        val index = _uiState.value.currentExerciseIndex
        val updatedExercises = _uiState.value.exercises.toMutableList()
        
        val updatedExercise = when (currentExercise) {
            is Exercise.MultipleChoice -> currentExercise.copy(selectedAnswer = answer)
            is Exercise.Listening -> currentExercise.copy(selectedAnswer = answer)
            else -> return
        }
        
        updatedExercises[index] = updatedExercise
        _uiState.update {
            it.copy(
                exercises = updatedExercises,
                isAnswerReady = true,
                feedbackMessage = null,
                explanation = null
            )
        }
    }
    
    private fun handleFillBlankAnswered(answer: String) {
        val currentExercise = getCurrentExercise() ?: return
        val index = _uiState.value.currentExerciseIndex
        val updatedExercises = _uiState.value.exercises.toMutableList()
        
        when (currentExercise) {
            is Exercise.FillBlank -> {
                updatedExercises[index] = currentExercise.copy(userAnswer = answer)
            }
            is Exercise.Translation -> {
                updatedExercises[index] = currentExercise.copy(userAnswer = answer)
            }
            else -> return
        }
        
        _uiState.update {
            it.copy(
                exercises = updatedExercises,
                isAnswerReady = answer.isNotBlank(),
                feedbackMessage = null,
                explanation = null
            )
        }
    }
    
    private fun handlePairMatched(left: String, right: String) {
        val currentExercise = getCurrentExercise() as? Exercise.Matching ?: return
        val index = _uiState.value.currentExerciseIndex
        
        val updatedPairs = currentExercise.selectedPairs.toMutableMap()
        updatedPairs[left] = right
        
        val updatedExercise = currentExercise.copy(selectedPairs = updatedPairs)
        val updatedExercises = _uiState.value.exercises.toMutableList()
        updatedExercises[index] = updatedExercise
        
        val allPairsSelected = updatedExercise.selectedPairs.size == updatedExercise.pairs.size
        _uiState.update {
            it.copy(
                exercises = updatedExercises,
                isAnswerReady = allPairsSelected,
                feedbackMessage = null,
                explanation = null
            )
        }
    }
    
    private fun handlePictureOptionSelected(optionId: String) {
        val currentExercise = getCurrentExercise() as? Exercise.PictureMatching ?: return
        val index = _uiState.value.currentExerciseIndex
        
        val updatedExercise = currentExercise.copy(selectedOptionId = optionId)
        val updatedExercises = _uiState.value.exercises.toMutableList()
        updatedExercises[index] = updatedExercise
        
        _uiState.update {
            it.copy(
                exercises = updatedExercises,
                isAnswerReady = true,
                feedbackMessage = null,
                explanation = null
            )
        }
    }
    
    private fun handleSpeakingTranscript(transcript: String) {
        val currentExercise = getCurrentExercise() as? Exercise.Speaking ?: return
        val index = _uiState.value.currentExerciseIndex
        
        val updatedExercise = currentExercise.copy(recognizedText = transcript)
        val updatedExercises = _uiState.value.exercises.toMutableList()
        updatedExercises[index] = updatedExercise
        
        _uiState.update {
            it.copy(
                exercises = updatedExercises,
                isAnswerReady = transcript.isNotBlank(),
                feedbackMessage = null,
                explanation = null
            )
        }
    }
    
    private fun submitAnswer() {
        val currentExercise = getCurrentExercise() ?: return
        if (!uiState.value.isAnswerReady && currentExercise !is Exercise.Matching) return
        
        val evaluation = engine.evaluate(currentExercise)
        val previousCorrect = _uiState.value.correctAnswers
        val previousWrong = _uiState.value.wrongAnswers
        
        val newCorrect = if (evaluation.isCorrect) previousCorrect + 1 else previousCorrect
        val newWrong = if (!evaluation.isCorrect) previousWrong + 1 else previousWrong
        val totalAttempts = newCorrect + newWrong
        val accuracy = engine.calculateAccuracy(newCorrect, totalAttempts)
        val newHearts = (_uiState.value.hearts + evaluation.heartsDelta).coerceAtLeast(0)
        
        _uiState.update {
            it.copy(
                score = it.score + evaluation.scoreDelta,
                hearts = newHearts,
                correctAnswers = newCorrect,
                wrongAnswers = newWrong,
                accuracy = accuracy,
                showResult = true,
                lastAnswerCorrect = evaluation.isCorrect,
                feedbackMessage = evaluation.feedback,
                explanation = if (evaluation.isCorrect) null else evaluation.explanation,
                isAnswerReady = false
            )
        }
        
        if (!evaluation.isCorrect && newHearts == 0) {
            completeLesson(forceFail = true)
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
                    lastAnswerCorrect = null,
                    feedbackMessage = null,
                    explanation = null,
                    isAnswerReady = when (val nextExercise = it.exercises[nextIndex]) {
                        is Exercise.FillBlank -> nextExercise.userAnswer.isNotBlank()
                        is Exercise.Translation -> nextExercise.userAnswer.isNotBlank()
                        is Exercise.MultipleChoice -> nextExercise.selectedAnswer != null
                        is Exercise.Listening -> nextExercise.selectedAnswer != null
                        is Exercise.Matching -> nextExercise.selectedPairs.isNotEmpty()
                        is Exercise.PictureMatching -> nextExercise.selectedOptionId != null
                        is Exercise.Speaking -> nextExercise.recognizedText.isNotBlank()
                    }
                )
            }
        }
    }
    
    private fun completeLesson(forceFail: Boolean = false) {
        viewModelScope.launch {
            val state = _uiState.value
            val totalAttempts = (state.correctAnswers + state.wrongAnswers).coerceAtLeast(1)
            val accuracy = engine.calculateAccuracy(state.correctAnswers, totalAttempts)
            val isPassed = !forceFail && accuracy >= PASS_ACCURACY_THRESHOLD
            val rewards = if (isPassed) {
                engine.calculateRewards(state.score, accuracy)
            } else {
                LessonRewards(xp = FAIL_XP_REWARD, coins = FAIL_COIN_REWARD)
            }
            
            val userId = repository.getCurrentUserSync()?.userId
            if (userId != null) {
                val progress = UserProgressEntity(
                    userId = userId,
                    lessonId = lessonId,
                    isCompleted = isPassed,
                    score = state.score,
                    accuracy = accuracy,
                    attempts = totalAttempts,
                    correctAnswers = state.correctAnswers,
                    wrongAnswers = state.wrongAnswers,
                    xpEarned = rewards.xp,
                    coinsEarned = rewards.coins,
                    completedAt = if (isPassed) System.currentTimeMillis() else null
                )
                repository.saveProgress(progress)
            }
            
            _uiState.update {
                it.copy(
                    isCompleted = true,
                    isFailed = !isPassed,
                    showResult = false,
                    lastAnswerCorrect = null,
                    feedbackMessage = null,
                    explanation = null,
                    lastXpEarned = rewards.xp,
                    lastCoinsEarned = rewards.coins
                )
            }
        }
    }
    
    private fun showHint() {
        // TODO: Integrate hint system consuming coins
    }
    
    private fun retryLesson() {
        _uiState.update { state ->
            state.copy(
                currentExerciseIndex = 0,
                score = 0,
                correctAnswers = 0,
                wrongAnswers = 0,
                accuracy = 0f,
                hearts = state.totalHearts,
                lastXpEarned = 0,
                lastCoinsEarned = 0,
                isAnswerReady = false,
                isCompleted = false,
                isFailed = false,
                showResult = false,
                lastAnswerCorrect = null,
                feedbackMessage = null,
                explanation = null,
                exercises = state.exercises.map { resetExercise(it) }
            )
        }
    }
    
    private fun resetExercise(exercise: Exercise): Exercise {
        return when (exercise) {
            is Exercise.MultipleChoice -> exercise.copy(selectedAnswer = null)
            is Exercise.FillBlank -> exercise.copy(userAnswer = "")
            is Exercise.Matching -> exercise.copy(selectedPairs = emptyMap())
            is Exercise.Translation -> exercise.copy(userAnswer = "")
            is Exercise.Listening -> exercise.copy(selectedAnswer = null)
            is Exercise.Speaking -> exercise.copy(recognizedText = "")
            is Exercise.PictureMatching -> exercise.copy(selectedOptionId = null)
        }
    }
    
    private fun getCurrentExercise(): Exercise? {
        return _uiState.value.exercises.getOrNull(_uiState.value.currentExerciseIndex)
    }
    
    fun getLessonResult(): LessonResult {
        val state = _uiState.value
        val totalAttempts = (state.correctAnswers + state.wrongAnswers).coerceAtLeast(1)
        val accuracy = engine.calculateAccuracy(state.correctAnswers, totalAttempts)
        val isPassed = !state.isFailed && accuracy >= PASS_ACCURACY_THRESHOLD
        
        return LessonResult(
            lessonId = lessonId,
            totalExercises = state.totalExercises,
            correctAnswers = state.correctAnswers,
            wrongAnswers = state.wrongAnswers,
            accuracy = accuracy,
            xpEarned = state.lastXpEarned,
            coinsEarned = state.lastCoinsEarned,
            isPassed = isPassed
        )
    }
    
    private fun parseMatchPairs(
        matchPairsJson: String?,
        words: List<com.example.master.data.local.entity.WordEntity>
    ): List<MatchPair> {
        if (!matchPairsJson.isNullOrBlank()) {
            return runCatching {
                val listType = object : TypeToken<List<MatchPair>>() {}.type
                gson.fromJson<List<MatchPair>>(matchPairsJson, listType)
            }.getOrNull()?.takeIf { it.isNotEmpty() } ?: emptyList()
        }
        
        return words
            .shuffled()
            .take(4)
            .map { word -> MatchPair(word.word, word.translation) }
    }
    
    private fun buildListeningOptions(
        exerciseEntity: com.example.master.data.local.entity.ExerciseEntity,
        words: List<com.example.master.data.local.entity.WordEntity>
    ): List<String> {
        val baseOptions = listOfNotNull(
            exerciseEntity.optionA,
            exerciseEntity.optionB,
            exerciseEntity.optionC,
            exerciseEntity.optionD
        ).toMutableList()
        
        if (exerciseEntity.correctAnswer.isNotBlank() && exerciseEntity.correctAnswer !in baseOptions) {
            baseOptions.add(exerciseEntity.correctAnswer)
        }
        
        if (baseOptions.size < 4) {
            baseOptions += words.shuffled()
                .map { it.translation }
                .filter { it !in baseOptions }
                .take(4 - baseOptions.size)
        }
        
        return baseOptions.distinct().shuffled()
    }
    
    private fun buildPictureOptions(
        exerciseEntity: com.example.master.data.local.entity.ExerciseEntity,
        words: List<com.example.master.data.local.entity.WordEntity>,
        targetWord: com.example.master.data.local.entity.WordEntity?
    ): List<PictureOption> {
        val parsed = if (!exerciseEntity.matchPairs.isNullOrBlank()) {
            runCatching {
                val listType = object : TypeToken<List<PictureOption>>() {}.type
                gson.fromJson<List<PictureOption>>(exerciseEntity.matchPairs, listType)
            }.getOrNull().orEmpty()
        } else emptyList()
        
        val fallback = if (parsed.isEmpty()) {
            (listOfNotNull(targetWord) + words.filter { it.id != targetWord?.id })
                .distinct()
                .take(4)
                .map { word ->
                    PictureOption(
                        id = word.word,
                        label = word.word,
                        imageUrl = word.imageUrl
                    )
                }
        } else parsed
        
        return fallback.shuffled()
    }
}
