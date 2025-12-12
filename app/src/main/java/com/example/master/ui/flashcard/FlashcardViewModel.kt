package com.example.master.ui.flashcard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.master.data.repository.LearningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlashcardViewModel @Inject constructor(
    private val repository: LearningRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val lessonId: Int = savedStateHandle["lessonId"] ?: 1

    private val _uiState = MutableStateFlow(
        FlashcardUiState(
            lessonId = lessonId,
            deckTitle = "Flashcards bài $lessonId"
        )
    )
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    init {
        loadDeck()
    }

    private fun loadDeck() {
        viewModelScope.launch {
            repository.getWordsByLesson(lessonId).collect { words ->
                val cards = words
                    .map { word ->
                        Flashcard(
                            id = word.id,
                            front = word.word,
                            back = word.translation,
                            pronunciation = word.pronunciation,
                            partOfSpeech = word.partOfSpeech,
                            exampleSentence = word.exampleSentence,
                            exampleTranslation = word.exampleTranslation,
                            audioUrl = word.audioUrl
                        )
                    }
                    .shuffled()

                _uiState.update {
                    it.copy(
                        cards = cards,
                        queue = cards,
                        currentIndex = 0,
                        isFlipped = false,
                        knownCount = 0,
                        unknownCount = 0,
                        unknownPool = emptyList(),
                        repetitionCounts = emptyMap(),
                        reviewingUnknown = false,
                        isCompleted = cards.isEmpty(),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun flipCard() {
        _uiState.update { it.copy(isFlipped = !it.isFlipped) }
    }

    fun markKnown() {
        val current = _uiState.value.currentCard
        if (current != null) {
            _uiState.update { state ->
                state.copy(
                    unknownPool = state.unknownPool.filterNot { it.id == current.id }
                )
            }
        }
        moveToNext(knownDelta = 1)
    }

    fun markUnknown() {
        val current = _uiState.value.currentCard ?: return
        val currentCounts = _uiState.value.repetitionCounts
        val repeats = currentCounts[current.id] ?: 0
        val canRequeue = repeats < 2

        _uiState.update { state ->
            val newQueue = if (canRequeue) {
                state.queue + current
            } else state.queue

            val newUnknown = if (state.unknownPool.any { it.id == current.id }) {
                state.unknownPool
            } else {
                state.unknownPool + current
            }

            state.copy(
                queue = newQueue,
                unknownPool = newUnknown,
                repetitionCounts = state.repetitionCounts + (current.id to (repeats + if (canRequeue) 1 else 0))
            )
        }

        moveToNext(unknownDelta = 1)
    }

    fun shuffleDeck() {
        _uiState.update { state ->
            state.copy(
                queue = state.queue.shuffled(),
                currentIndex = 0,
                isFlipped = false,
                knownCount = 0,
                unknownCount = 0,
                isCompleted = state.queue.isEmpty(),
                reviewingUnknown = false,
                unknownPool = emptyList(),
                repetitionCounts = emptyMap()
            )
        }
    }

    fun restartDeck() {
        _uiState.update {
            it.copy(
                queue = it.cards,
                currentIndex = 0,
                isFlipped = false,
                knownCount = 0,
                unknownCount = 0,
                unknownPool = emptyList(),
                repetitionCounts = emptyMap(),
                reviewingUnknown = false,
                isCompleted = it.cards.isEmpty()
            )
        }
    }

    fun reviewUnknownOnly() {
        _uiState.update { state ->
            val pool = if (state.unknownPool.isNotEmpty()) state.unknownPool else state.cards
            state.copy(
                queue = pool,
                currentIndex = 0,
                isFlipped = false,
                knownCount = 0,
                unknownCount = 0,
                repetitionCounts = emptyMap(),
                reviewingUnknown = true,
                isCompleted = pool.isEmpty()
            )
        }
    }

    private fun moveToNext(knownDelta: Int = 0, unknownDelta: Int = 0) {
        _uiState.update { state ->
            val nextIndex = state.currentIndex + 1
            val completed = nextIndex >= state.queue.size

            state.copy(
                currentIndex = if (completed) state.currentIndex else nextIndex,
                knownCount = state.knownCount + knownDelta,
                unknownCount = state.unknownCount + unknownDelta,
                isFlipped = false,
                isCompleted = completed || state.queue.isEmpty()
            )
        }
    }
}
