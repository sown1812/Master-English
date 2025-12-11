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
                        currentIndex = 0,
                        isFlipped = false,
                        knownCount = 0,
                        unknownCount = 0,
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
        moveToNext(knownDelta = 1)
    }

    fun markUnknown() {
        moveToNext(unknownDelta = 1)
    }

    fun shuffleDeck() {
        _uiState.update { state ->
            state.copy(
                cards = state.cards.shuffled(),
                currentIndex = 0,
                isFlipped = false,
                knownCount = 0,
                unknownCount = 0,
                isCompleted = state.cards.isEmpty()
            )
        }
    }

    fun restartDeck() {
        _uiState.update {
            it.copy(
                currentIndex = 0,
                isFlipped = false,
                knownCount = 0,
                unknownCount = 0,
                isCompleted = it.cards.isEmpty()
            )
        }
    }

    private fun moveToNext(knownDelta: Int = 0, unknownDelta: Int = 0) {
        _uiState.update { state ->
            val nextIndex = state.currentIndex + 1
            val completed = nextIndex >= state.cards.size

            state.copy(
                currentIndex = if (completed) state.currentIndex else nextIndex,
                knownCount = state.knownCount + knownDelta,
                unknownCount = state.unknownCount + unknownDelta,
                isFlipped = false,
                isCompleted = completed || state.cards.isEmpty()
            )
        }
    }
}
