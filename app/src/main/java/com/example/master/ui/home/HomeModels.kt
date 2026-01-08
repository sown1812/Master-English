package com.example.master.ui.home

import androidx.compose.runtime.Immutable

@Immutable
data class HomeUiState(
    val avatarUrl: String?,
    val userName: String,
    val coins: Int,
    val streakDays: Int,
    val streakRewardAvailable: Boolean,
    val level: Int,
    val difficulty: Difficulty,
    val progress: Float,
    val maxLevel: Int,
    val totalScore: Int,
    val nextLesson: LessonSummary?,
    val sections: List<SectionUi>,
    val badges: List<AchievementBadge>,
    val quests: List<Quest>,
    val boosters: List<BoosterItem>,
    val themes: List<ThemeOption>
) {
    companion object {
        fun sample(): HomeUiState = HomeUiState(
            avatarUrl = null,
            userName = "Alex",
            coins = 120,
            streakDays = 1,
            streakRewardAvailable = true,
            level = 1,
            difficulty = Difficulty.EASY,
            progress = 0.05f,
            maxLevel = 20,
            totalScore = 120,
            nextLesson = LessonSummary(
                id = 1,
                title = "Basics 1",
                description = "Greetings and self-intros",
                difficulty = "EASY",
                totalWords = 20,
                totalExercises = 12,
                isUnlocked = true,
                unlockCost = 0
            ),
            sections = listOf(
                SectionUi(
                    id = 1,
                    title = "A1 Foundation",
                    cefrLevel = "A1",
                    units = listOf(
                        UnitUi(
                            id = 1,
                            title = "Basics",
                            topic = "basics",
                            levels = listOf(
                                LevelUi(
                                    id = 1,
                                    order = 1,
                                    lessonIds = listOf(1),
                                    status = LevelStatus.COMPLETED,
                                    lessonTitle = "Basics 1",
                                    unlockCost = 0,
                                    isUnlocked = true
                                ),
                                LevelUi(
                                    id = 2,
                                    order = 2,
                                    lessonIds = listOf(2),
                                    status = LevelStatus.AVAILABLE,
                                    lessonTitle = "Basics 2",
                                    unlockCost = 50,
                                    isUnlocked = true
                                )
                            )
                        )
                    )
                )
            ),
            badges = listOf(
                AchievementBadge.FirstWords(unlocked = true, date = "2025-10-01"),
                AchievementBadge.VocabularyMaster(unlocked = false, date = null),
                AchievementBadge.LanguageLegend(unlocked = false, date = null),
                AchievementBadge.StreakHero(unlocked = false, date = null),
                AchievementBadge.PerfectScore(unlocked = false, date = null)
            ),
            quests = listOf(
                Quest(
                    title = "On luyen 15 tu bat ky",
                    description = "Hoan thanh 3 level o do kho Medium",
                    rewardCoins = 80,
                    progress = 0.6f,
                    stepsLabel = "3/5"
                ),
                Quest(
                    title = "Lam thu thach tu vung hom nay",
                    description = "Dat diem toi thieu 40/50",
                    rewardCoins = 120,
                    progress = 0.2f,
                    stepsLabel = "1/4"
                ),
                Quest(
                    title = "Chia se streak",
                    description = "Chia se ket qua streak len mang xa hoi",
                    rewardCoins = 40,
                    progress = 1f,
                    stepsLabel = "1/1"
                )
            ),
            boosters = listOf(
                BoosterItem(
                    title = "Hint tu vung",
                    description = "Hien tieng Viet cho 1 cau hoi",
                    costCoins = 30,
                    isOwned = true
                ),
                BoosterItem(
                    title = "Double XP",
                    description = "Nhan doi diem level ke tiep",
                    costCoins = 120,
                    isOwned = false
                ),
                BoosterItem(
                    title = "Skip cau",
                    description = "Bo qua 1 cau hoi kho",
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
                    name = "Forest",
                    primaryColor = "#2F855A",
                    secondaryColor = "#68D391",
                    isUnlocked = true,
                    isSelected = false
                ),
                ThemeOption(
                    name = "Rose",
                    primaryColor = "#E11D48",
                    secondaryColor = "#F472B6",
                    isUnlocked = true,
                    isSelected = false
                ),
                ThemeOption(
                    name = "Midnight",
                    primaryColor = "#4C6FFF",
                    secondaryColor = "#7C3AED",
                    isUnlocked = true,
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

data class LessonSummary(
    val id: Int,
    val title: String,
    val description: String,
    val difficulty: String,
    val totalWords: Int,
    val totalExercises: Int,
    val isUnlocked: Boolean,
    val unlockCost: Int = 0
)

data class SectionUi(
    val id: Int,
    val title: String,
    val cefrLevel: String,
    val units: List<UnitUi>
)

data class UnitUi(
    val id: Int,
    val title: String,
    val topic: String,
    val levels: List<LevelUi>
)

data class LevelUi(
    val id: Int,
    val order: Int,
    val lessonIds: List<Int>,
    val status: LevelStatus,
    val lessonTitle: String,
    val unlockCost: Int,
    val isUnlocked: Boolean
)

enum class LevelStatus {
    COMPLETED,
    IN_PROGRESS,
    AVAILABLE,
    LOCKED
}
