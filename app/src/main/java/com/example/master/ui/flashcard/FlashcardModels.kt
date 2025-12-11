package com.example.master.ui.flashcard

import androidx.compose.runtime.Immutable

@Immutable
data class Flashcard(
    val id: Int,
    val front: String,
    val back: String,
    val pronunciation: String,
    val partOfSpeech: String,
    val exampleSentence: String,
    val exampleTranslation: String,
    val audioUrl: String?
)

@Immutable
data class FlashcardUiState(
    val lessonId: Int = 0,
    val deckTitle: String = "",
    val cards: List<Flashcard> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val knownCount: Int = 0,
    val unknownCount: Int = 0,
    val isCompleted: Boolean = false,
    val isLoading: Boolean = true
) {
    val totalCards: Int get() = cards.size
    val currentCard: Flashcard? get() = cards.getOrNull(currentIndex)
}
