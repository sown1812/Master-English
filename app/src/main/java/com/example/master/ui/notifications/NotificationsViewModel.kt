package com.example.master.ui.notifications

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class NotificationsViewModel : ViewModel() {
    private val _uiState = mutableStateOf(NotificationUiState.sample())

    val uiState: State<NotificationUiState> = _uiState
}