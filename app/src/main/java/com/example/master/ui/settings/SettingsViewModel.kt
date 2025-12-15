package com.example.master.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.master.auth.AuthManager
import com.example.master.sync.OfflineManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val autoPlayAudio: Boolean = false,
    val darkMode: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "20:00",
    val offlineStatus: String = "",
    val isOfflineDownloading: Boolean = false,
    val message: String? = null,
    val isProcessing: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val offlineManager: OfflineManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun toggleNotifications(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun toggleSound(enabled: Boolean) {
        _uiState.update { it.copy(soundEnabled = enabled) }
    }

    fun toggleAutoPlay(enabled: Boolean) {
        _uiState.update { it.copy(autoPlayAudio = enabled) }
    }

    fun toggleDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(darkMode = enabled) }
    }

    fun toggleReminder(enabled: Boolean) {
        _uiState.update { it.copy(reminderEnabled = enabled) }
    }

    fun updateReminderTime(time: String) {
        _uiState.update { it.copy(reminderTime = time) }
    }

    fun prefetchOfflineContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isOfflineDownloading = true, offlineStatus = "Đang tải nội dung...") }
            runCatching { offlineManager.prefetchAllLessons() }
                .onSuccess {
                    _uiState.update { it.copy(isOfflineDownloading = false, offlineStatus = "Đã tải xong để học offline") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isOfflineDownloading = false, offlineStatus = "Lỗi khi tải: ${e.message}") }
                }
        }
    }

    fun logout(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, message = null) }
            runCatching { authManager.signOut() }
                .onSuccess {
                    _uiState.update { it.copy(isProcessing = false, message = "Đã đăng xuất") }
                    onComplete(true)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            message = e.message ?: "Không thể đăng xuất"
                        )
                    }
                    onComplete(false)
                }
        }
    }
}
