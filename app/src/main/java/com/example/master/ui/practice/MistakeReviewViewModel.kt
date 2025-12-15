package com.example.master.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.master.data.local.entity.MistakeEntity
import com.example.master.data.repository.LearningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MistakeReviewViewModel @Inject constructor(
    private val repository: LearningRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MistakeReviewUiState(isLoading = true))
    val uiState: StateFlow<MistakeReviewUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val user = repository.getCurrentUserSync() ?: run {
                _uiState.value = MistakeReviewUiState(isLoading = false)
                return@launch
            }
            currentUserId = user.userId
            combine(
                repository.getMistakes(user.userId),
                repository.getAllLessons()
            ) { mistakes, lessons ->
                val lessonTitles = lessons.associate { it.id to it.title }
                mistakes
                    .groupBy { it.lessonId }
                    .map { (lessonId, lessonMistakes) ->
                        MistakeGroup(
                            lessonId = lessonId,
                            lessonTitle = lessonTitles[lessonId] ?: "Bài $lessonId",
                            mistakes = lessonMistakes.map { it.toUi(lessonTitles[lessonId]) }
                        )
                    }
            }.collect { groups ->
                val total = groups.sumOf { it.mistakes.size }
                _uiState.value = MistakeReviewUiState(
                    isLoading = false,
                    totalMistakes = total,
                    groups = groups.sortedBy { it.lessonId }
                )
            }
        }
    }

    fun clearMistake(id: Int) {
        viewModelScope.launch {
            repository.deleteMistake(id)
        }
    }

    fun clearLesson(lessonId: Int) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            repository.deleteMistakesByLesson(userId, lessonId)
        }
    }
}

private fun MistakeEntity.toUi(lessonTitle: String?): MistakeItemUi =
    MistakeItemUi(
        id = id,
        lessonId = lessonId,
        lessonTitle = lessonTitle ?: "Bài $lessonId",
        question = question,
        userAnswer = userAnswer,
        correctAnswer = correctAnswer,
        reason = reason,
        createdAt = createdAt
    )

data class MistakeReviewUiState(
    val isLoading: Boolean = false,
    val totalMistakes: Int = 0,
    val groups: List<MistakeGroup> = emptyList()
)

data class MistakeGroup(
    val lessonId: Int,
    val lessonTitle: String,
    val mistakes: List<MistakeItemUi>
)

data class MistakeItemUi(
    val id: Int,
    val lessonId: Int,
    val lessonTitle: String,
    val question: String,
    val userAnswer: String,
    val correctAnswer: String,
    val reason: String?,
    val createdAt: Long
)
