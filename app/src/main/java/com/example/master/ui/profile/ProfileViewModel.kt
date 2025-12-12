package com.example.master.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.master.auth.AuthManager
import com.example.master.core.user.UserProfile
import com.example.master.data.repository.LearningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val repository: LearningRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun refresh() {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val userId = authManager.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Chưa đăng nhập") }
                return@launch
            }

            repository.getUserProfile(userId).collect { profile ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        profile = profile,
                        errorMessage = if (profile == null) "Không tìm thấy hồ sơ" else null
                    )
                }
            }
        }
    }
}
