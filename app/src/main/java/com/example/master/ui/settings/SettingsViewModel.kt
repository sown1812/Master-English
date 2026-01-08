package com.example.master.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.master.auth.AuthManager
import com.example.master.data.local.NotificationSettingsStore
import com.example.master.notifications.ReminderScheduler
import com.example.master.ui.theme.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val autoPlayAudio: Boolean = false,
    val darkMode: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "20:00",
    val message: String? = null,
    val isProcessing: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val notificationSettingsStore: NotificationSettingsStore,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(darkMode = ThemeManager.getDarkModeOverride() ?: false) }
        viewModelScope.launch {
            notificationSettingsStore.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        notificationsEnabled = settings.notificationsEnabled,
                        reminderEnabled = settings.reminderEnabled,
                        reminderTime = settings.reminderTime
                    )
                }
                runCatching {
                    reminderScheduler.updateSchedule(
                        settings.notificationsEnabled,
                        settings.reminderEnabled,
                        settings.reminderTime
                    )
                }
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            notificationSettingsStore.setNotificationsEnabled(enabled)
        }
    }

    fun toggleSound(enabled: Boolean) {
        _uiState.update { it.copy(soundEnabled = enabled) }
    }

    fun toggleAutoPlay(enabled: Boolean) {
        _uiState.update { it.copy(autoPlayAudio = enabled) }
    }

    fun toggleDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(darkMode = enabled) }
        ThemeManager.setDarkMode(enabled)
    }

    fun toggleReminder(enabled: Boolean) {
        viewModelScope.launch {
            notificationSettingsStore.setReminderEnabled(enabled)
        }
    }

    fun updateReminderTime(time: String) {
        viewModelScope.launch {
            notificationSettingsStore.setReminderTime(time)
        }
    }

    fun logout(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, message = null) }
            runCatching { authManager.signOut() }
                .onSuccess {
                    _uiState.update { it.copy(isProcessing = false, message = "Da dang xuat") }
                    onComplete(true)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            message = e.message ?: "Khong the dang xuat"
                        )
                    }
                    onComplete(false)
                }
        }
    }
}
