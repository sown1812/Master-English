package com.example.master.ui.dashboard

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.master.auth.AuthManager
import com.example.master.data.local.entity.AchievementEntity
import com.example.master.data.local.entity.UserProgressEntity
import com.example.master.data.repository.LearningRepository
import com.example.master.network.ApiService
import com.example.master.network.LeaderboardEntryRemote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: LearningRepository,
    private val authManager: AuthManager,
    private val api: ApiService
) : ViewModel() {

    private val _uiState = mutableStateOf(DashboardScreenState())
    val uiState: State<DashboardScreenState> = _uiState
    private var leaderboardLoaded = false

    init {
        loadDashboard()
    }

    fun refreshLeaderboard() {
        viewModelScope.launch { fetchLeaderboard() }
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            val userId = authManager.getCurrentUserId()
            if (userId.isNullOrBlank()) {
                _uiState.value = DashboardScreenState(
                    isLoading = false,
                    errorMessage = "Chua dang nh?p"
                )
                return@launch
            }

            combine(
                repository.getUserProgress(userId),
                repository.getUserAchievements(userId),
                repository.getCurrentUser()
            ) { progress, achievements, user -> Triple(progress, achievements, user) }
                .collect { (progress, achievements, user) ->
                    val stats = repository.getUserStatistics(userId)
                    val weekly = buildWeeklyProgress(progress)
                    val achievementsSummary = mapAchievements(achievements)
                    val xpProgress = XpProgress(
                        currentXp = stats.totalXP,
                        nextLevelXp = (stats.level * 100).coerceAtLeast(stats.totalXP + 100),
                        levelLabel = "Level ${stats.level}"
                    )
                    val leaderboard = buildLeaderboard(
                        currentUserName = user?.displayName ?: "You",
                        currentScore = stats.totalXP
                    )
                    if (!leaderboardLoaded) {
                        viewModelScope.launch { fetchLeaderboard() }
                    }
                    val ui = DashboardUiState(
                        totalWordsLearned = stats.wordsLearned,
                        totalCoins = stats.coins,
                        streakDays = stats.streakDays,
                        levelsCompleted = stats.lessonsCompleted,
                        weeklyProgress = weekly,
                        xpProgress = xpProgress,
                        achievements = achievementsSummary,
                        upcomingChallenges = buildUpcomingChallenges(),
                        leaderboard = leaderboard
                    )
                    _uiState.value = DashboardScreenState(
                        isLoading = false,
                        data = ui
                    )
                }
        }
    }

    private suspend fun fetchLeaderboard() {
        runCatching { api.getLeaderboard() }
            .onSuccess { entries ->
                val current = _uiState.value.data
                _uiState.value = _uiState.value.copy(
                    data = current?.copy(leaderboard = entries.map { it.toUi() }),
                    isLoading = false
                )
                leaderboardLoaded = true
            }
    }

    private fun mapAchievements(list: List<AchievementEntity>): List<AchievementSummary> {
        return list.map { achievement ->
            val ratio = (achievement.progress.toFloat() / achievement.target).coerceIn(0f, 1f)
            AchievementSummary(
                title = achievement.title,
                progress = ratio,
                completedCount = if (achievement.isUnlocked) 1 else 0,
                totalCount = 1
            )
        }
    }

    private fun buildWeeklyProgress(progress: List<UserProgressEntity>): List<DailyProgress> {
        if (progress.isEmpty()) return defaultWeeklyProgress()
        val zone = ZoneId.systemDefault()
        val now = Instant.now().atZone(zone).toLocalDate()
        val days = (0..6).map { now.minusDays((6 - it).toLong()) }
        return days.map { day ->
            val dayProgress = progress.filter { p ->
                p.completedAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == day } == true
            }
            val completion = (dayProgress.size / 5f).coerceIn(0f, 1f)
            val xp = dayProgress.sumOf { it.xpEarned }
            DailyProgress(
                label = day.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                completion = completion,
                xpEarned = xp
            )
        }
    }

    private fun buildLeaderboard(currentUserName: String, currentScore: Int): List<FriendProgress> {
        val peers = listOf(
            FriendProgress("Lan", "L", currentScore - 40, trend = +1),
            FriendProgress("Minh", "M", currentScore - 120, trend = +3),
            FriendProgress("Alex", "A", currentScore - 300, trend = -1)
        )
        val me = FriendProgress(
            name = currentUserName.ifBlank { "You" },
            avatarInitial = currentUserName.firstOrNull()?.uppercaseChar()?.toString() ?: "Y",
            score = currentScore,
            trend = 0
        )
        return (peers + me).sortedByDescending { it.score }
    }

    private fun buildUpcomingChallenges(): List<DashboardChallenge> {
        return listOf(
            DashboardChallenge(title = "Complete 2 lessons today", rewardCoins = 80, timeRemaining = "Today"),
            DashboardChallenge(title = "Maintain streak 3 days", rewardCoins = 120, timeRemaining = "3d left")
        )
    }

    private fun defaultWeeklyProgress(): List<DailyProgress> {
        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return labels.map { DailyProgress(label = it, completion = 0f, xpEarned = 0) }
    }
}

private fun LeaderboardEntryRemote.toUi(): FriendProgress {
    val safeName = displayName.ifBlank { "Learner" }
    val initial = safeName.firstOrNull()?.uppercaseChar()?.toString() ?: "L"
    return FriendProgress(
        name = safeName,
        avatarInitial = initial,
        score = totalXp,
        trend = 0
    )
}
