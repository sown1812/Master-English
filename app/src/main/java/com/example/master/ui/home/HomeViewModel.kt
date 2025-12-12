package com.example.master.ui.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.master.auth.AuthManager
import com.example.master.data.repository.LearningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LearningRepository,
    private val authManager: AuthManager
) : ViewModel() {
    private val _uiState = mutableStateOf(HomeUiState.sample())
    val uiState: State<HomeUiState> = _uiState

    private val navigationChannel = Channel<HomeNavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<HomeNavigationEvent> = navigationChannel.receiveAsFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            val userFlow = repository.getCurrentUser()
            val lessonsFlow = repository.getAllLessons()
            val progressFlow = authManager.currentUser

            combine(userFlow, lessonsFlow, progressFlow) { user, lessons, _ ->
                val totalLessons = lessons.size.coerceAtLeast(1)
                val completed = user?.lessonsCompleted ?: 0
                val progress = (completed.toFloat() / totalLessons.toFloat()).coerceIn(0f, 1f)
                HomeUiState(
                    avatarUrl = user?.avatarUrl,
                    userName = user?.displayName ?: "Người học",
                    coins = user?.coins ?: 0,
                    streakDays = user?.streakDays ?: 0,
                    streakRewardAvailable = (user?.streakDays ?: 0) > 0,
                    nextChallengeCountdown = "Hôm nay",
                    level = user?.currentLevel ?: 1,
                    difficulty = Difficulty.EASY,
                    progress = progress,
                    maxLevel = totalLessons,
                    totalScore = user?.totalXP ?: 0,
                    badges = emptyList(),
                    dailyChallenge = DailyChallenge(
                        title = "Hoàn thành 1 bài hôm nay",
                        rewardCoins = 50,
                        isAccepted = false
                    ),
                    quests = emptyList(),
                    boosters = emptyList(),
                    themes = _uiState.value.themes
                )
            }.collectLatest { state -> _uiState.value = state }
        }
    }

    fun onPlayClicked() {
        emitEvent(HomeNavigationEvent.NavigateToPlay(_uiState.value.level))
    }

    fun onDailyChallengeClicked() {
        val challenge = _uiState.value.dailyChallenge
        emitEvent(HomeNavigationEvent.NavigateToDailyChallenge(challenge.title))
    }

    fun onFlashcardsClicked() {
        emitEvent(HomeNavigationEvent.NavigateToFlashcards(_uiState.value.level))
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
