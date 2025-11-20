package com.example.master.ui.dashboard

import androidx.compose.runtime.Immutable

@Immutable
data class DashboardUiState(
    val totalWordsLearned: Int,
    val totalCoins: Int,
    val streakDays: Int,
    val levelsCompleted: Int,
    val weeklyProgress: List<DailyProgress>,
    val xpProgress: XpProgress,
    val achievements: List<AchievementSummary>,
    val upcomingChallenges: List<DashboardChallenge>,
    val leaderboard: List<FriendProgress>
)

@Immutable
data class DailyProgress(
    val label: String,
    val completion: Float,
    val xpEarned: Int
)

@Immutable
data class XpProgress(
    val currentXp: Int,
    val nextLevelXp: Int,
    val levelLabel: String
) {
    val ratio: Float get() = (currentXp.toFloat() / nextLevelXp).coerceIn(0f, 1f)
}

@Immutable
data class AchievementSummary(
    val title: String,
    val progress: Float,
    val completedCount: Int,
    val totalCount: Int
)

@Immutable
data class DashboardChallenge(
    val title: String,
    val rewardCoins: Int,
    val timeRemaining: String
)

@Immutable
data class FriendProgress(
    val name: String,
    val avatarInitial: String,
    val score: Int,
    val trend: Int
)

data class DashboardScreenState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val leaderboardLoading: Boolean = false,
    val data: DashboardUiState? = null
)
