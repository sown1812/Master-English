package com.example.master.ui.dashboard

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.master.auth.AuthManager
import com.example.master.data.local.entity.AchievementEntity
import com.example.master.data.local.entity.UserEntity
import com.example.master.data.local.entity.UserProgressEntity
import com.example.master.data.repository.LearningRepository
import com.example.master.network.ApiService
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

    private fun loadDashboard() {
        viewModelScope.launch {
            val userId = authManager.getCurrentUserId()
            if (userId.isNullOrBlank()) {
                _uiState.value = DashboardScreenState(
                    isLoading = false,
                    errorMessage = "Chưa đăng nhập"
                )
                return@launch
            }

            combine(
                repository.getUserProgress(userId),
                repository.getUserAchievements(userId),
                repository.getCurrentUser()
            ) { progress, achievements, user ->
                Triple(progress, achievements, user)
            }.collect { (progress, achievements, user) ->
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
                    viewModelScope.launch {
                        fetchLeaderboard()
                    }
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
                    errorMessage = null,
                    data = ui
                )
            }
        }
    }

    private suspend fun fetchLeaderboard() {
        val userId = authManager.getCurrentUserId() ?: return
        _uiState.value = _uiState.value.copy(leaderboardLoading = true)
        runCatching {
            api.getLeaderboard(20)
        }.onSuccess { remote ->
            val mapped = remote.map {
                FriendProgress(
                    name = it.displayName.ifBlank { "User" },
                    avatarInitial = it.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                    score = it.totalXp,
                    trend = 0
                )
            }
            _uiState.value = _uiState.value.copy(
                data = _uiState.value.data?.copy(leaderboard = mapped),
                leaderboardLoading = false
            )
            leaderboardLoaded = true
        }.onFailure {
            _uiState.value = _uiState.value.copy(
                leaderboardLoading = false,
                errorMessage = _uiState.value.errorMessage ?: "Không tải được leaderboard"
            )
        }
    }

    fun refreshLeaderboard() {
        viewModelScope.launch {
            fetchLeaderboard()
        }
    }

    private fun buildWeeklyProgress(progress: List<UserProgressEntity>): List<DailyProgress> {
        if (progress.isEmpty()) {
            return defaultWeeklyProgress()
        }

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val byDate = progress.groupBy {
            Instant.ofEpochMilli(it.updatedAt).atZone(zone).toLocalDate()
        }

        return (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val items = byDate[date].orEmpty()
            val xp = items.sumOf { it.xpEarned }
            val completion = (xp / 100f).coerceIn(0f, 1f)
            DailyProgress(
                label = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() },
                completion = completion,
                xpEarned = xp
            )
        }
    }

    private fun mapAchievements(data: List<AchievementEntity>): List<AchievementSummary> {
        if (data.isEmpty()) return emptyList()
        return data
            .sortedBy { it.isUnlocked.not() } // ưu tiên đã mở khóa
            .take(4)
            .map {
                val progressRatio = if (it.isUnlocked) 1f else (it.progress.toFloat() / it.target).coerceIn(0f, 1f)
                AchievementSummary(
                    title = it.title,
                    progress = progressRatio,
                    completedCount = if (it.isUnlocked) it.target else it.progress,
                    totalCount = it.target
                )
            }
    }

    private fun buildLeaderboard(
        currentUserName: String,
        currentScore: Int
    ): List<FriendProgress> {
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

class DashboardViewModelFactory(
    private val repository: LearningRepository,
    private val authManager: AuthManager,
    private val api: ApiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository, authManager, api) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

