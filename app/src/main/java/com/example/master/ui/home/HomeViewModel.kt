package com.example.master.ui.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class HomeViewModel : ViewModel() {
    private val _uiState = mutableStateOf(HomeUiState.sample())

    val uiState: State<HomeUiState> = _uiState

    private val navigationChannel = Channel<HomeNavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<HomeNavigationEvent> = navigationChannel.receiveAsFlow()

    fun onPlayClicked() {
        emitEvent(HomeNavigationEvent.NavigateToPlay(_uiState.value.level))
    }

    fun onDailyChallengeClicked() {
        val challenge = _uiState.value.dailyChallenge
        emitEvent(HomeNavigationEvent.NavigateToDailyChallenge(challenge.title))
    }

    fun onAchievementsClicked() {
        emitEvent(HomeNavigationEvent.NavigateToAchievements)
    }

    fun onStoreClicked() {
        emitEvent(HomeNavigationEvent.NavigateToStore)
    }

    fun onQuestSelected(quest: Quest) {
        emitEvent(HomeNavigationEvent.NavigateToQuest(quest))
    }

    fun onBoosterSelected(booster: BoosterItem) {
        val message = if (booster.isOwned) {
            "Bạn đã sở hữu ${booster.title}"
        } else {
            "Mua ${booster.title} với ${booster.costCoins} coins"
        }
        emitEvent(HomeNavigationEvent.ShowMessage(message))
        emitEvent(HomeNavigationEvent.NavigateToBooster(booster))
    }

    fun onThemeSelected(theme: ThemeOption) {
        if (!theme.isUnlocked) {
            emitEvent(HomeNavigationEvent.ShowMessage("Chưa mở khóa chủ đề ${theme.name}"))
            return
        }

        val updatedThemes = _uiState.value.themes.map {
            if (it.name == theme.name) it.copy(isSelected = true) else it.copy(isSelected = false)
        }
        _uiState.value = _uiState.value.copy(themes = updatedThemes)
        emitEvent(HomeNavigationEvent.ThemeApplied(theme.name))
    }

    private fun emitEvent(event: HomeNavigationEvent) {
        navigationChannel.trySend(event)
    }
}
