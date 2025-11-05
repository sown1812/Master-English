package com.example.master.ui.dashboard

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class DashboardViewModel : ViewModel() {
    private val _uiState = mutableStateOf(DashboardUiState.sample())

    val uiState: State<DashboardUiState> = _uiState
}