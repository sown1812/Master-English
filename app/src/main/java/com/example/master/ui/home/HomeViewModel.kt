package com.example.master.ui.home

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

class HomeViewModel : ViewModel() {
    private val _uiState = mutableStateOf(HomeUiState.sample())

    val uiState: State<HomeUiState> = _uiState
}

data class HomeUiState(
    val avatarUrl: String?,
    val userName: String,
    val coins: Int,
    val streakDays: Int,
    val streakRewardAvailable: Boolean,
    val nextChallengeCountdown: String,
    val level: Int,
    val difficulty: Difficulty,
    val progress: Float,
    val maxLevel: Int,
    val totalScore: Int,
    val badges: List<AchievementBadge>,
    val dailyChallenge: DailyChallenge
) {
    companion object {
        fun sample(): HomeUiState = HomeUiState(
            avatarUrl = null,
            userName = "Alex",
            coins = 350,
            streakDays = 5,
            streakRewardAvailable = true,
            nextChallengeCountdown = "12:45:10",
            level = 39,
            difficulty = Difficulty.SUPER_HARD,
            progress = 0.39f,
            maxLevel = 100,
            totalScore = 4250,
            badges = listOf(
                AchievementBadge.FirstWords(unlocked = true, date = "2025-10-01"),
                AchievementBadge.VocabularyMaster(unlocked = false, date = null),
                AchievementBadge.LanguageLegend(unlocked = false, date = null),
                AchievementBadge.StreakHero(unlocked = false, date = null),
                AchievementBadge.PerfectScore(unlocked = false, date = null)
            ),
            dailyChallenge = DailyChallenge(
                title = "Quiz 10 từ vựng mới",
                rewardCoins = 100,
                isAccepted = false
            )
        )
    }
}

enum class Difficulty(val label: String, val colorHex: String, val reward: Int) {
    EASY("Easy", "#5CB85C", 30),
    MEDIUM("Medium", "#FFA500", 50),
    SUPER_HARD("Super Hard", "#FF4D4F", 75)
}

sealed class AchievementBadge {
    abstract val unlocked: Boolean
    abstract val date: String?
    abstract val title: String

    data class FirstWords(override val unlocked: Boolean, override val date: String?) : AchievementBadge() {
        override val title: String = "First Words"
    }

    data class VocabularyMaster(override val unlocked: Boolean, override val date: String?) : AchievementBadge() {
        override val title: String = "Vocabulary Master"
    }

    data class LanguageLegend(override val unlocked: Boolean, override val date: String?) : AchievementBadge() {
        override val title: String = "Language Legend"
    }

    data class StreakHero(override val unlocked: Boolean, override val date: String?) : AchievementBadge() {
        override val title: String = "Streak Hero"
    }

    data class PerfectScore(override val unlocked: Boolean, override val date: String?) : AchievementBadge() {
        override val title: String = "Perfect Score"
    }
}

data class DailyChallenge(
    val title: String,
    val rewardCoins: Int,
    val isAccepted: Boolean
)