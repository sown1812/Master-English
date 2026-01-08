package com.example.master.ui.lesson

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.master.core.network.NetworkMonitor
import com.example.master.data.local.entity.ExerciseEntity
import com.example.master.data.local.entity.UserProgressEntity
import com.example.master.data.local.entity.WordEntity
import com.example.master.data.repository.LearningRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val FAIL_XP_REWARD = 10
private const val FAIL_COIN_REWARD = 4
private const val HINT_COIN_COST = 10

@HiltViewModel
class LessonViewModel @Inject constructor(
    private val repository: LearningRepository,
    private val syncManager: com.example.master.sync.SyncManager,
    private val networkMonitor: NetworkMonitor,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val lessonId: Int = savedStateHandle["lessonId"] ?: 1
    
    private val gson = Gson()
    private val engine = ExerciseEngine()
    private var hasLoaded = false
    private var isLoadingLesson = false
    
    private val _uiState = MutableStateFlow(LessonUiState(lessonId = lessonId))
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()
    
    init {
        observeNetwork()
        loadLesson()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { connected ->
                val hasUser = repository.getCurrentUserSync() != null
                _uiState.update {
                    it.copy(networkError = if (!connected && !hasUser) "Can ket noi internet de hoc bai nay." else null)
                }
                if (connected && !hasLoaded) loadLesson()
                if (!connected && hasUser && !hasLoaded) loadLesson()
            }
        }
    }

    private fun loadLesson() {
        viewModelScope.launch {
            val connected = networkMonitor.isConnectedNow()
            val hasUser = repository.getCurrentUserSync() != null
            if (!connected && !hasUser) {
                _uiState.update {
                    it.copy(isLoading = false, networkError = "Can ket noi internet de hoc bai nay.")
                }
                return@launch
            }
            if (hasLoaded || isLoadingLesson) return@launch
            isLoadingLesson = true
            _uiState.update { it.copy(isLoading = true) }
            try {
                val lesson = repository.getLessonById(lessonId)
                val exercises = repository.getExercisesByLesson(lessonId).first()
                val lessonWords = repository.getWordsByLesson(lessonId).first()
                val reviewWords = if (lessonId > 1) {
                    val userId = repository.getCurrentUserSync()?.userId
                    val previousCompleted = if (userId != null) {
                        repository.getLessonProgress(userId, lessonId - 1)?.isCompleted == true
                    } else {
                        false
                    }
                    if (previousCompleted) repository.getWordsByLesson(lessonId - 1).first() else emptyList()
                } else {
                    emptyList()
                }
                val optionWords = (lessonWords + reviewWords).distinctBy { it.id }
                val wordById = lessonWords.associateBy { it.id }
                
                val normalizedExercises = exercises
                    .sortedWith(compareBy<ExerciseEntity> { it.difficulty }.thenBy { it.order })
                    .let { list ->
                        val seen = mutableSetOf<String>()
                        list.filter { exercise ->
                            val key = listOf(
                                exercise.type.uppercase(),
                                exercise.question.trim().lowercase(),
                                exercise.correctAnswer.trim().lowercase()
                            ).joinToString("|")
                            if (key in seen) {
                                false
                            } else {
                                seen.add(key)
                                true
                            }
                        }
                    }

                val builtExercises = normalizedExercises
                    .map { exerciseEntity ->
                        val word = wordById[exerciseEntity.wordId]
                        when (exerciseEntity.type.uppercase()) {
                            "MULTIPLE_CHOICE" -> Exercise.MultipleChoice(
                                id = exerciseEntity.id,
                                question = exerciseEntity.question,
                                correctAnswer = exerciseEntity.correctAnswer,
                                word = word,
                                explanation = exerciseEntity.explanation,
                                options = buildMultipleChoiceOptions(exerciseEntity, optionWords)
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

                            "WORD_TILES" -> Exercise.WordTiles(
                                id = exerciseEntity.id,
                                question = exerciseEntity.question,
                                correctAnswer = exerciseEntity.correctAnswer,
                                word = word,
                                explanation = exerciseEntity.explanation,
                                tiles = buildWordTiles(exerciseEntity, lessonWords)
                            )
                            
                            "MATCHING" -> Exercise.Matching(
                                id = exerciseEntity.id,
                                question = exerciseEntity.question.ifBlank { "Match the words with their translations" },
                                correctAnswer = "",
                                word = word,
                                explanation = exerciseEntity.explanation,
                                pairs = parseMatchPairs(exerciseEntity.matchPairs, lessonWords)
                            )
                            
                            "LISTENING" -> Exercise.Listening(
                                id = exerciseEntity.id,
                                question = exerciseEntity.question.ifBlank { "Listen and choose the correct answer" },
                                correctAnswer = exerciseEntity.correctAnswer,
                                word = word,
                                explanation = exerciseEntity.explanation,
                                audioUrl = word?.audioUrl,
                                options = buildListeningOptions(exerciseEntity, optionWords)
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
                                options = buildPictureOptions(exerciseEntity, lessonWords, word)
                            )

                            "FLASHCARD" -> Exercise.Flashcard(
                                id = exerciseEntity.id,
                                question = exerciseEntity.question.ifBlank { word?.word ?: "Review this word" },
                                correctAnswer = exerciseEntity.correctAnswer.ifBlank { word?.translation ?: "" },
                                word = word,
                                explanation = exerciseEntity.explanation,
                                frontText = word?.word ?: exerciseEntity.question,
                                backText = word?.translation
                                    ?: exerciseEntity.correctAnswer.ifBlank { "Check the definition" }
                            )

                            "SPEED_MATCH" -> run {
                                val pairs = parseSpeedMatchPairs(exerciseEntity.matchPairs, lessonWords)
                                Exercise.SpeedMatching(
                                    id = exerciseEntity.id,
                                    question = exerciseEntity.question.ifBlank { "Ghép từ thật nhanh!" },
                                    correctAnswer = "",
                                    word = word,
                                    explanation = exerciseEntity.explanation,
                                    pairs = pairs,
                                    wordOrder = pairs.map { it.id }.shuffled(),
                                    timeLeftSec = 45,
                                    timeLimitSec = 45
                                )
                            }
                            
                            else -> Exercise.MultipleChoice(
                                id = exerciseEntity.id,
                                question = exerciseEntity.question,
                                correctAnswer = exerciseEntity.correctAnswer,
                                word = word,
                                explanation = exerciseEntity.explanation,
                                options = buildMultipleChoiceOptions(exerciseEntity, optionWords)
                            )
                        }
                    }
                val reviewExercises = if (reviewWords.isNotEmpty()) {
                    buildGeneratedExercises(reviewWords).take(2)
                } else {
                    emptyList()
                }

                val exerciseList = (reviewExercises + builtExercises)
                    .let { built ->
                        if (built.isNotEmpty()) built else buildGeneratedExercises(lessonWords)
                    }
                    .let { built ->
                        if (built.any { it is Exercise.SpeedMatching }) {
                            built
                        } else {
                            val speedMatch = buildSpeedMatchExercise(lessonWords)
                            if (speedMatch != null) built + speedMatch else built
                        }
                    }
                    .let { built -> ensureAllExerciseTypes(built, lessonWords) }
                    .distinctBy { exerciseDedupKey(it) }
                    .take(10)

                _uiState.update {
                    it.copy(
                        lessonTitle = lesson?.title ?: "Lesson",
                        exercises = exerciseList,
                        totalExercises = exerciseList.size,
                        isLoading = false,
                        networkError = null
                    )
                }
                hasLoaded = true
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            } finally {
                isLoadingLesson = false
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
            is LessonEvent.FlashcardFlipped -> handleFlashcardFlipped(event.flipped)
            is LessonEvent.FlashcardRated -> handleFlashcardRated(event.remembered)
            is LessonEvent.SpeedMatchClueSelected -> handleSpeedMatchClueSelected(event.clueId)
            is LessonEvent.SpeedMatchWordSelected -> handleSpeedMatchWordSelected(event.wordId)
            is LessonEvent.WordTileSelected -> handleWordTileSelected(event.word)
            is LessonEvent.WordTileRemoved -> handleWordTileRemoved(event.index)
            LessonEvent.SpeedMatchTick -> handleSpeedMatchTick()
            LessonEvent.SubmitAnswer -> submitAnswer()
            LessonEvent.NextExercise -> nextExercise()
            LessonEvent.ShowHint -> showHint()
            LessonEvent.RetryLesson -> retryLesson()
            LessonEvent.ExitLesson -> Unit
        }
    }
    
    private fun handleAnswerSelected(answer: String) {
        val currentExercise = getCurrentExercise() ?: return
        
        val updatedExercise = when (currentExercise) {
            is Exercise.MultipleChoice -> currentExercise.copy(selectedAnswer = answer)
            is Exercise.Listening -> currentExercise.copy(selectedAnswer = answer)
            else -> return
        }

        updateExercise(updatedExercise, isAnswerReady = true)
    }
    
    private fun handleFillBlankAnswered(answer: String) {
        val currentExercise = getCurrentExercise() ?: return
        
        val updatedExercise = when (currentExercise) {
            is Exercise.FillBlank -> currentExercise.copy(userAnswer = answer)
            is Exercise.Translation -> currentExercise.copy(userAnswer = answer)
            else -> return
        }

        updateExercise(updatedExercise, isAnswerReady = answer.isNotBlank())
    }
    
    private fun handlePairMatched(left: String, right: String) {
        val currentExercise = getCurrentExercise() as? Exercise.Matching ?: return
        
        val updatedPairs = currentExercise.selectedPairs.toMutableMap()
        updatedPairs.entries.removeAll { it.key != left && it.value == right }
        updatedPairs[left] = right
        
        val updatedExercise = currentExercise.copy(selectedPairs = updatedPairs)
        
        val allPairsSelected = updatedExercise.selectedPairs.size == updatedExercise.pairs.size
        updateExercise(updatedExercise, isAnswerReady = allPairsSelected)
    }
    
    private fun handlePictureOptionSelected(optionId: String) {
        val currentExercise = getCurrentExercise() as? Exercise.PictureMatching ?: return
        
        val updatedExercise = currentExercise.copy(selectedOptionId = optionId)
        updateExercise(updatedExercise, isAnswerReady = true)
    }
    
    private fun handleSpeakingTranscript(transcript: String) {
        val currentExercise = getCurrentExercise() as? Exercise.Speaking ?: return
        
        val updatedExercise = currentExercise.copy(recognizedText = transcript)
        updateExercise(updatedExercise, isAnswerReady = transcript.isNotBlank())
    }

    private fun handleFlashcardFlipped(flipped: Boolean) {
        val currentExercise = getCurrentExercise() as? Exercise.Flashcard ?: return
        
        val updatedExercise = currentExercise.copy(isFlipped = flipped)
        updateExercise(updatedExercise, resetFeedback = true)
    }

    private fun handleFlashcardRated(remembered: Boolean) {
        val currentExercise = getCurrentExercise() as? Exercise.Flashcard ?: return
        
        val updatedExercise = currentExercise.copy(
            isRemembered = remembered,
            isFlipped = true
        )
        updateExercise(updatedExercise, isAnswerReady = true)
    }

    private fun handleSpeedMatchClueSelected(clueId: String) {
        if (_uiState.value.showResult) return
        val currentExercise = getCurrentExercise() as? Exercise.SpeedMatching ?: return
        if (currentExercise.isExpired) return
        if (currentExercise.matchedIds.contains(clueId)) return
        val updated = currentExercise.copy(selectedClueId = clueId)
        applySpeedMatchUpdate(updated)
    }

    private fun handleSpeedMatchWordSelected(wordId: String) {
        if (_uiState.value.showResult) return
        val currentExercise = getCurrentExercise() as? Exercise.SpeedMatching ?: return
        if (currentExercise.isExpired) return
        if (currentExercise.matchedIds.contains(wordId)) return
        val updated = currentExercise.copy(selectedWordId = wordId)
        applySpeedMatchUpdate(updated)
    }

    private fun handleSpeedMatchTick() {
        if (_uiState.value.showResult) return
        val currentExercise = getCurrentExercise() as? Exercise.SpeedMatching ?: return
        if (currentExercise.isExpired) return
        if (currentExercise.matchedIds.size == currentExercise.pairs.size) return

        val nextTime = (currentExercise.timeLeftSec - 1).coerceAtLeast(0)
        val expired = nextTime == 0
        val updated = currentExercise.copy(
            timeLeftSec = nextTime,
            isExpired = expired
        )

        val index = _uiState.value.currentExerciseIndex
        val updatedExercises = _uiState.value.exercises.toMutableList()
        updatedExercises[index] = updated
        _uiState.update {
            it.copy(
                exercises = updatedExercises,
                isAnswerReady = expired,
                feedbackMessage = if (expired) "Hết giờ!" else null,
                explanation = null
            )
        }
    }

    private fun handleWordTileSelected(word: String) {
        if (_uiState.value.showResult) return
        val currentExercise = getCurrentExercise() as? Exercise.WordTiles ?: return
        val available = buildAvailableTiles(currentExercise.tiles, currentExercise.selectedWords)
        if (word !in available) return
        if (currentExercise.selectedWords.size >= expectedTileCount(currentExercise)) return

        val updatedExercise = currentExercise.copy(
            selectedWords = currentExercise.selectedWords + word
        )
        updateExercise(
            updatedExercise,
            isAnswerReady = expectedTileCount(updatedExercise) == updatedExercise.selectedWords.size
        )
    }

    private fun handleWordTileRemoved(indexToRemove: Int) {
        if (_uiState.value.showResult) return
        val currentExercise = getCurrentExercise() as? Exercise.WordTiles ?: return
        if (indexToRemove !in currentExercise.selectedWords.indices) return
        val updatedWords = currentExercise.selectedWords.toMutableList().apply {
            removeAt(indexToRemove)
        }
        val updatedExercise = currentExercise.copy(selectedWords = updatedWords)
        updateExercise(
            updatedExercise,
            isAnswerReady = expectedTileCount(updatedExercise) == updatedWords.size
        )
    }

    private fun applySpeedMatchUpdate(exercise: Exercise.SpeedMatching) {
        val selectedClueId = exercise.selectedClueId
        val selectedWordId = exercise.selectedWordId
        var updatedExercise = exercise

        if (!selectedClueId.isNullOrBlank() && !selectedWordId.isNullOrBlank()) {
            val isCorrect = selectedClueId == selectedWordId
            val newCombo = if (isCorrect) exercise.combo + 1 else 0
            val bestCombo = maxOf(exercise.bestCombo, newCombo)
            val speedBonus = (exercise.timeLeftSec / 5).coerceAtLeast(0)
            val points = if (isCorrect) 10 + (newCombo * 2) + speedBonus else 0
            val newMatched = if (isCorrect) exercise.matchedIds + selectedClueId else exercise.matchedIds

            updatedExercise = exercise.copy(
                matchedIds = newMatched,
                selectedClueId = null,
                selectedWordId = null,
                combo = newCombo,
                bestCombo = bestCombo,
                scoreEarned = exercise.scoreEarned + points
            )
        }

        val index = _uiState.value.currentExerciseIndex
        val updatedExercises = _uiState.value.exercises.toMutableList()
        updatedExercises[index] = updatedExercise
        val done = updatedExercise.matchedIds.size == updatedExercise.pairs.size

        _uiState.update {
            it.copy(
                exercises = updatedExercises,
                isAnswerReady = done,
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

        if (!evaluation.isCorrect) {
            viewModelScope.launch {
                logMistake(currentExercise, evaluation)
            }
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
                    hintText = null,
                    isAnswerReady = when (val nextExercise = it.exercises[nextIndex]) {
                        is Exercise.FillBlank -> nextExercise.userAnswer.isNotBlank()
                        is Exercise.Translation -> nextExercise.userAnswer.isNotBlank()
                        is Exercise.WordTiles -> expectedTileCount(nextExercise) == nextExercise.selectedWords.size
                        is Exercise.MultipleChoice -> nextExercise.selectedAnswer != null
                        is Exercise.Listening -> nextExercise.selectedAnswer != null
                        is Exercise.Matching -> nextExercise.selectedPairs.isNotEmpty()
                        is Exercise.PictureMatching -> nextExercise.selectedOptionId != null
                        is Exercise.Speaking -> nextExercise.recognizedText.isNotBlank()
                        is Exercise.Flashcard -> nextExercise.isRemembered != null
                        is Exercise.SpeedMatching -> nextExercise.matchedIds.size == nextExercise.pairs.size
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
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching {
                        syncManager.enqueueLessonCompleted(
                            lessonId = lessonId,
                            score = state.score,
                            correctAnswers = state.correctAnswers,
                            wrongAnswers = state.wrongAnswers
                        )
                        syncManager.flushQueue()
                    }
                }
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
        val currentExercise = getCurrentExercise() ?: return
        if (_uiState.value.hintText != null) return

        viewModelScope.launch {
            val user = repository.getCurrentUserSync() ?: return@launch
            val userId = user.userId
            if (user.coins < HINT_COIN_COST) {
                _uiState.update { it.copy(feedbackMessage = "Không đủ coins để dùng hint.") }
                return@launch
            }
            repository.addCoins(userId, -HINT_COIN_COST)

            val hint = when (currentExercise) {
                is Exercise.FillBlank -> currentExercise.hint
                    ?: "Gợi ý: bắt đầu bằng \"${currentExercise.correctAnswer.firstOrNull() ?: '?'}\""
                is Exercise.Translation -> "Gợi ý: ${currentExercise.correctAnswer.split(" ").firstOrNull().orEmpty()}"
                is Exercise.MultipleChoice -> "Gợi ý: đáp án bắt đầu với \"${currentExercise.correctAnswer.firstOrNull() ?: '?'}\""
                is Exercise.Listening -> "Gợi ý: đáp án bắt đầu với \"${currentExercise.correctAnswer.firstOrNull() ?: '?'}\""
                is Exercise.WordTiles -> "Gợi ý: ${expectedTileCount(currentExercise)} từ"
                is Exercise.Matching -> "Gợi ý: bắt đầu ghép các từ dễ trước"
                is Exercise.PictureMatching -> "Gợi ý: đọc kỹ từ khóa trong câu hỏi"
                is Exercise.Speaking -> "Gợi ý: ${currentExercise.correctAnswer.split(" ").firstOrNull().orEmpty()}"
                is Exercise.Flashcard -> "Gợi ý: ${currentExercise.backText}"
                is Exercise.SpeedMatching -> "Gợi ý: ghép các từ ngắn trước"
            }

            _uiState.update { it.copy(hintText = hint) }
        }
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
                hintText = null,
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
            is Exercise.WordTiles -> exercise.copy(selectedWords = emptyList())
            is Exercise.Listening -> exercise.copy(selectedAnswer = null)
            is Exercise.Speaking -> exercise.copy(recognizedText = "")
            is Exercise.PictureMatching -> exercise.copy(selectedOptionId = null)
            is Exercise.Flashcard -> exercise.copy(isFlipped = false, isRemembered = null)
            is Exercise.SpeedMatching -> exercise.copy(
                matchedIds = emptySet(),
                selectedClueId = null,
                selectedWordId = null,
                combo = 0,
                bestCombo = 0,
                scoreEarned = 0,
                timeLeftSec = exercise.timeLimitSec,
                isExpired = false
            )
        }
    }
    
    private fun getCurrentExercise(): Exercise? {
        return _uiState.value.exercises.getOrNull(_uiState.value.currentExerciseIndex)
    }

    private fun updateExercise(
        updatedExercise: Exercise,
        isAnswerReady: Boolean? = null,
        resetFeedback: Boolean = true
    ) {
        val index = _uiState.value.currentExerciseIndex
        val updatedExercises = _uiState.value.exercises.toMutableList()
        updatedExercises[index] = updatedExercise
        _uiState.update { state ->
            state.copy(
                exercises = updatedExercises,
                isAnswerReady = isAnswerReady ?: state.isAnswerReady,
                feedbackMessage = if (resetFeedback) null else state.feedbackMessage,
                explanation = if (resetFeedback) null else state.explanation
            )
        }
    }

    private suspend fun logMistake(exercise: Exercise, evaluation: ExerciseEvaluation) {
        val userId = repository.getCurrentUserSync()?.userId ?: return
        val userAnswer = when (exercise) {
            is Exercise.MultipleChoice -> exercise.selectedAnswer.orEmpty()
            is Exercise.FillBlank -> exercise.userAnswer
            is Exercise.Matching -> exercise.selectedPairs.entries.joinToString { "${it.key}→${it.value}" }
            is Exercise.Translation -> exercise.userAnswer
            is Exercise.WordTiles -> exercise.selectedWords.joinToString(" ")
            is Exercise.Listening -> exercise.selectedAnswer.orEmpty()
            is Exercise.Speaking -> exercise.recognizedText
            is Exercise.PictureMatching -> exercise.selectedOptionId.orEmpty()
            is Exercise.Flashcard -> if (exercise.isRemembered == true) "remembered" else "forgot"
            is Exercise.SpeedMatching -> "matched ${exercise.matchedIds.size}/${exercise.pairs.size}"
        }

        val mistake = com.example.master.data.local.entity.MistakeEntity(
            userId = userId,
            lessonId = lessonId,
            exerciseId = exercise.id,
            wordId = exercise.word?.id,
            question = exercise.question,
            userAnswer = userAnswer,
            correctAnswer = exercise.correctAnswer,
            reason = evaluation.explanation ?: evaluation.feedback
        )
        repository.saveMistake(mistake)
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

    private fun parseSpeedMatchPairs(
        pairsJson: String?,
        words: List<com.example.master.data.local.entity.WordEntity>
    ): List<SpeedMatchPair> {
        if (!pairsJson.isNullOrBlank()) {
            return runCatching {
                val listType = object : TypeToken<List<SpeedMatchPair>>() {}.type
                gson.fromJson<List<SpeedMatchPair>>(pairsJson, listType)
            }.getOrNull()?.takeIf { it.isNotEmpty() } ?: emptyList()
        }

        return words
            .shuffled()
            .take(6)
            .map { word ->
                SpeedMatchPair(
                    id = word.word,
                    word = word.word,
                    clue = word.translation,
                    imageUrl = word.imageUrl
                )
            }
    }

    private fun buildMultipleChoiceOptions(
        exerciseEntity: ExerciseEntity,
        words: List<WordEntity>
    ): List<String> {
        val base = listOfNotNull(
            exerciseEntity.optionA,
            exerciseEntity.optionB,
            exerciseEntity.optionC,
            exerciseEntity.optionD,
            exerciseEntity.correctAnswer
        )
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val normalized = LinkedHashMap<String, String>()
        base.forEach { option ->
            val key = option.lowercase()
            normalized.putIfAbsent(key, option)
        }
        if (exerciseEntity.correctAnswer.isNotBlank()) {
            val correct = exerciseEntity.correctAnswer.trim()
            normalized.putIfAbsent(correct.lowercase(), correct)
        }

        if (normalized.size < 4) {
            val candidates = (words.map { it.word } + words.map { it.translation })
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            for (candidate in candidates.shuffled()) {
                if (normalized.size >= 4) break
                if (candidate.equals(exerciseEntity.correctAnswer, ignoreCase = true)) continue
                normalized.putIfAbsent(candidate.lowercase(), candidate)
            }
        }

        return normalized.values.toList().shuffled()
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
        
        return baseOptions
            .distinctBy { it.trim().lowercase() }
            .shuffled()
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

    private fun buildWordTiles(
        exerciseEntity: com.example.master.data.local.entity.ExerciseEntity,
        words: List<WordEntity>
    ): List<String> {
        if (!exerciseEntity.matchPairs.isNullOrBlank()) {
            return runCatching {
                val listType = object : TypeToken<List<String>>() {}.type
                gson.fromJson<List<String>>(exerciseEntity.matchPairs, listType)
            }.getOrNull()?.filter { it.isNotBlank() }.orEmpty()
        }

        val answerWords = splitAnswerWords(exerciseEntity.correctAnswer)
        val translationTokens = words.map { it.translation.lowercase() }.toSet()
        val useTranslations = exerciseEntity.correctAnswer.any { it.code > 127 } ||
            answerWords.any { it.lowercase() in translationTokens }
        val distractors = (if (useTranslations) words.map { it.translation } else words.map { it.word })
            .filter { it.isNotBlank() && it !in answerWords }
            .distinct()
            .shuffled()
            .take(3)

        return (answerWords + distractors).shuffled()
    }

    private fun buildAvailableTiles(tiles: List<String>, selected: List<String>): List<String> {
        val remaining = tiles.toMutableList()
        selected.forEach { word -> remaining.remove(word) }
        return remaining
    }

    private fun splitAnswerWords(text: String): List<String> {
        return text.split(" ")
            .map { it.replace(Regex("[^\\p{L}\\p{N}']"), "") }
            .filter { it.isNotBlank() }
    }

    private fun expectedTileCount(exercise: Exercise.WordTiles): Int {
        val tokens = exercise.correctAnswer
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}']"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        return tokens.size.coerceAtLeast(1)
    }

    private fun buildSpeedMatchExercise(words: List<WordEntity>): Exercise.SpeedMatching? {
        val pairs = words.shuffled().take(6).map { word ->
            SpeedMatchPair(
                id = word.word,
                word = word.word,
                clue = word.translation,
                imageUrl = word.imageUrl
            )
        }
        if (pairs.size < 4) return null

        return Exercise.SpeedMatching(
            id = -2000 - lessonId,
            question = "Ghép từ thật nhanh!",
            correctAnswer = "",
            word = words.firstOrNull(),
            explanation = null,
            pairs = pairs,
            wordOrder = pairs.map { it.id }.shuffled(),
            timeLeftSec = 45,
            timeLimitSec = 45
        )
    }

    private fun exerciseDedupKey(exercise: Exercise): String {
        val type = when (exercise) {
            is Exercise.MultipleChoice -> "MULTIPLE_CHOICE"
            is Exercise.FillBlank -> "FILL_BLANK"
            is Exercise.Translation -> "TRANSLATION"
            is Exercise.WordTiles -> "WORD_TILES"
            is Exercise.Matching -> "MATCHING"
            is Exercise.Listening -> "LISTENING"
            is Exercise.Speaking -> "SPEAKING"
            is Exercise.PictureMatching -> "PICTURE_MATCH"
            is Exercise.Flashcard -> "FLASHCARD"
            is Exercise.SpeedMatching -> "SPEED_MATCH"
        }
        val question = exercise.question.trim().lowercase()
        val answer = exercise.correctAnswer.trim().lowercase()
        return "$type|$question|$answer"
    }

    private fun ensureAllExerciseTypes(
        exercises: List<Exercise>,
        words: List<WordEntity>
    ): List<Exercise> {
        if (words.isEmpty()) return exercises

        val types = exercises.map { it::class }.toSet()
        val baseWord = words.first()
        val fallbackWord = words.getOrNull(1) ?: baseWord
        val extended = exercises.toMutableList()

        if (Exercise.MultipleChoice::class !in types) {
            val options = (listOf(baseWord.translation) + words.map { it.translation })
                .distinct()
                .shuffled()
                .take(4)
            extended.add(
                Exercise.MultipleChoice(
                    id = -3001,
                    question = "Choose the meaning of \"${baseWord.word}\"",
                    correctAnswer = baseWord.translation,
                    word = baseWord,
                    explanation = baseWord.exampleTranslation,
                    options = options
                )
            )
        }

        if (Exercise.FillBlank::class !in types) {
            extended.add(
                Exercise.FillBlank(
                    id = -3002,
                    question = "I ____ ${fallbackWord.word}.",
                    correctAnswer = "like",
                    word = fallbackWord,
                    explanation = "Use a common verb"
                )
            )
        }

        if (Exercise.Translation::class !in types) {
            extended.add(
                Exercise.Translation(
                    id = -3003,
                    question = "Translate: ${baseWord.translation}",
                    correctAnswer = baseWord.word,
                    word = baseWord,
                    explanation = baseWord.exampleTranslation
                )
            )
        }

        if (Exercise.Matching::class !in types) {
            val pairs = words.shuffled().take(4).map { MatchPair(it.word, it.translation) }
            extended.add(
                Exercise.Matching(
                    id = -3004,
                    question = "Match the words with their translations",
                    correctAnswer = "",
                    word = baseWord,
                    explanation = null,
                    pairs = pairs
                )
            )
        }

        if (Exercise.Listening::class !in types) {
            val options = words.map { it.word }.distinct().shuffled().take(4)
            extended.add(
                Exercise.Listening(
                    id = -3005,
                    question = "Listen and choose the word",
                    correctAnswer = baseWord.word,
                    word = baseWord,
                    explanation = null,
                    audioUrl = baseWord.audioUrl,
                    options = if (options.contains(baseWord.word)) options else (options + baseWord.word).take(4)
                )
            )
        }

        if (Exercise.Speaking::class !in types) {
            extended.add(
                Exercise.Speaking(
                    id = -3006,
                    question = "Speak the highlighted phrase",
                    correctAnswer = baseWord.word,
                    word = baseWord,
                    explanation = null,
                    prompt = baseWord.word
                )
            )
        }

        if (Exercise.PictureMatching::class !in types) {
            val options = words.shuffled().take(4).map { word ->
                PictureOption(
                    id = word.word,
                    label = word.word,
                    imageUrl = word.imageUrl
                )
            }
            extended.add(
                Exercise.PictureMatching(
                    id = -3007,
                    question = "Tap the picture that matches the word",
                    correctAnswer = baseWord.word,
                    word = baseWord,
                    explanation = null,
                    options = options
                )
            )
        }

        if (Exercise.Flashcard::class !in types) {
            extended.add(
                Exercise.Flashcard(
                    id = -3008,
                    question = baseWord.word,
                    correctAnswer = baseWord.translation,
                    word = baseWord,
                    explanation = null,
                    frontText = baseWord.word,
                    backText = baseWord.translation
                )
            )
        }

        if (Exercise.WordTiles::class !in types) {
            val translationTiles = buildTranslationTilesExercise(words)
            if (translationTiles != null) {
                extended.add(translationTiles)
            } else {
                val answer = "I am ${baseWord.word}"
                extended.add(
                    Exercise.WordTiles(
                        id = -3009,
                        question = "Translate: ${baseWord.translation}",
                        correctAnswer = answer,
                        word = baseWord,
                        explanation = null,
                        tiles = buildWordTilesFromAnswer(answer, words)
                    )
                )
            }
        }

        if (Exercise.SpeedMatching::class !in types) {
            buildSpeedMatchExercise(words)?.let { extended.add(it) }
        }

        return extended
    }

    private fun buildWordTilesFromAnswer(
        answer: String,
        words: List<WordEntity>,
        useTranslations: Boolean = false
    ): List<String> {
        val answerWords = splitAnswerWords(answer)
        val distractors = (if (useTranslations) words.map { it.translation } else words.map { it.word })
            .filter { it.isNotBlank() && it !in answerWords }
            .distinct()
            .shuffled()
            .take(3)
        return (answerWords + distractors).shuffled()
    }

    private fun buildTranslationTilesExercise(words: List<WordEntity>): Exercise.WordTiles? {
        val candidate = words.firstOrNull {
            it.exampleSentence.isNotBlank() && it.exampleTranslation.isNotBlank()
        } ?: return null

        val toEnglish = candidate.id % 2 == 0
        val question = if (toEnglish) {
            "Translate: ${candidate.exampleTranslation}"
        } else {
            "Dich: ${candidate.exampleSentence}"
        }
        val answer = if (toEnglish) candidate.exampleSentence else candidate.exampleTranslation

        return Exercise.WordTiles(
            id = -3009,
            question = question,
            correctAnswer = answer,
            word = candidate,
            explanation = null,
            tiles = buildWordTilesFromAnswer(answer, words, useTranslations = !toEnglish)
        )
    }

    private fun buildGeneratedExercises(words: List<WordEntity>): List<Exercise> {
        if (words.isEmpty()) return emptyList()

        val wordPool = words.shuffled().take(6)
        val exercises = mutableListOf<Exercise>()

        wordPool.forEachIndexed { idx, word ->
            val baseOptions = (listOf(word.translation) + words.filter { it.id != word.id }.map { it.translation })
                .distinct()
                .shuffled()
                .toMutableList()
            if (!baseOptions.contains(word.translation)) {
                baseOptions.add(0, word.translation)
            }
            val options = baseOptions
                .distinct()
                .shuffled()
                .let { list ->
                    if (list.contains(word.translation)) {
                        val trimmed = list.take(4).toMutableList()
                        if (!trimmed.contains(word.translation)) {
                            trimmed[trimmed.lastIndex] = word.translation
                        }
                        trimmed
                    } else {
                        list.take(4)
                    }
                }
                .ifEmpty { listOf(word.translation) }

            exercises.add(
                Exercise.MultipleChoice(
                    id = -(idx + 1),
                    question = "Chọn nghĩa của " + "\"${word.word}\"",
                    correctAnswer = word.translation,
                    word = word,
                    explanation = word.exampleTranslation,
                    options = options
                )
            )

            exercises.add(
                Exercise.Translation(
                    id = -(idx + 100),
                    question = "Dịch: ${word.translation}",
                    correctAnswer = word.word,
                    word = word,
                    explanation = word.exampleTranslation
                )
            )
        }

        buildSpeedMatchExercise(words)?.let { exercises.add(it) }

        return exercises
    }
}
