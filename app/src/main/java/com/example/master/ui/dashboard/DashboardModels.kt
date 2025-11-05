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
) {
    companion object {
        fun sample(): DashboardUiState = DashboardUiState(
            totalWordsLearned = 1280,
            totalCoins = 2450,
            streakDays = 18,
            levelsCompleted = 42,
            weeklyProgress = listOf(
                DailyProgress("Mon", 0.9f, 45),
                DailyProgress("Tue", 0.6f, 30),
                DailyProgress("Wed", 1f, 50),
                DailyProgress("Thu", 0.4f, 22),
                DailyProgress("Fri", 0.7f, 35),
                DailyProgress("Sat", 0.5f, 25),
                DailyProgress("Sun", 0.8f, 40)
            ),
            xpProgress = XpProgress(currentXp = 3400, nextLevelXp = 4000, levelLabel = "Level 40"),
            achievements = listOf(
                AchievementSummary(title = "Perfect Run", progress = 0.8f, completedCount = 4, totalCount = 5),
                AchievementSummary(title = "Grammar Guru", progress = 0.45f, completedCount = 9, totalCount = 20)
            ),
            upcomingChallenges = listOf(
                DashboardChallenge(title = "Weekend Marathon", rewardCoins = 150, timeRemaining = "18:23:10"),
                DashboardChallenge(title = "Streak Hero", rewardCoins = 200, timeRemaining = "2d 5h")
            ),
            leaderboard = listOf(
                FriendProgress(name = "Tuấn", avatarInitial = "T", score = 5120, trend = +2),
                FriendProgress(name = "Lan", avatarInitial = "L", score = 5015, trend = -1),
                FriendProgress(name = "Minh", avatarInitial = "M", score = 4880, trend = +4)
            )
        )
    }
}

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
