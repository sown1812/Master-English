package com.example.master.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.master.data.repository.LearningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class PracticeLessonItem(
    val id: Int,
    val title: String,
    val category: String
)

data class PracticeUiState(
    val isLoading: Boolean = true,
    val dailyTitle: String = "Thử thách hằng ngày",
    val recommendedLessons: List<PracticeLessonItem> = emptyList()
)

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val repository: LearningRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getUnlockedLessons().collectLatest { lessons ->
                val rec = lessons
                    .sortedBy { it.order }
                    .take(3)
                    .map { PracticeLessonItem(id = it.id, title = it.title, category = it.category) }

                _uiState.value = PracticeUiState(
                    isLoading = false,
                    dailyTitle = "Thử thách hằng ngày",
                    recommendedLessons = rec
                )
            }
        }
    }
}
