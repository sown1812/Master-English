package com.example.master.ui.home

import androidx.compose.runtime.Immutable

@Immutable
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
    val dailyChallenge: DailyChallenge,
    val quests: List<Quest>,
    val boosters: List<BoosterItem>,
    val themes: List<ThemeOption>
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
            ),
            quests = listOf(
                Quest(
                    title = "Ôn lại 15 động từ bất quy tắc",
                    description = "Hoàn thành 3 level ở độ khó Medium",
                    rewardCoins = 80,
                    progress = 0.6f,
                    stepsLabel = "3/5"
                ),
                Quest(
                    title = "Làm thử thách từ vựng hôm nay",
                    description = "Đạt điểm tối thiểu 40/50",
                    rewardCoins = 120,
                    progress = 0.2f,
                    stepsLabel = "1/4"
                ),
                Quest(
                    title = "Chia sẻ streak",
                    description = "Chia sẻ kết quả streak lên mạng xã hội",
                    rewardCoins = 40,
                    progress = 1f,
                    stepsLabel = "1/1"
                )
            ),
            boosters = listOf(
                BoosterItem(
                    title = "Hint từ vựng",
                    description = "Hiển thị nghĩa tiếng Việt cho 1 câu hỏi",
                    costCoins = 30,
                    isOwned = true
                ),
                BoosterItem(
                    title = "Double XP",
                    description = "Nhân đôi điểm level kế tiếp",
                    costCoins = 120,
                    isOwned = false
                ),
                BoosterItem(
                    title = "Skip câu",
                    description = "Bỏ qua 1 câu hỏi khó",
                    costCoins = 60,
                    isOwned = false
                )
            ),
            themes = listOf(
                ThemeOption(
                    name = "Sunrise",
                    primaryColor = "#FFB347",
                    secondaryColor = "#FFD166",
                    isUnlocked = true,
                    isSelected = true
                ),
                ThemeOption(
                    name = "Ocean",
                    primaryColor = "#118AB2",
                    secondaryColor = "#06D6A0",
                    isUnlocked = true,
                    isSelected = false
                ),
                ThemeOption(
                    name = "Galaxy",
                    primaryColor = "#4C1D95",
                    secondaryColor = "#9333EA",
                    isUnlocked = false,
                    isSelected = false
                )
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

data class Quest(
    val title: String,
    val description: String,
    val rewardCoins: Int,
    val progress: Float,
    val stepsLabel: String
)

data class BoosterItem(
    val title: String,
    val description: String,
    val costCoins: Int,
    val isOwned: Boolean
)

data class ThemeOption(
    val name: String,
    val primaryColor: String,
    val secondaryColor: String,
    val isUnlocked: Boolean,
    val isSelected: Boolean
)
