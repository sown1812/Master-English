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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

private data class HomeInputs(
    val user: com.example.master.data.local.entity.UserEntity?,
    val lessons: List<com.example.master.data.local.entity.LessonEntity>,
    val sections: List<com.example.master.data.local.entity.SectionEntity>,
    val units: List<com.example.master.data.local.entity.UnitEntity>,
    val levels: List<com.example.master.data.local.entity.LevelEntity>
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LearningRepository,
    private val authManager: AuthManager,
    private val syncManager: com.example.master.sync.SyncManager
) : ViewModel() {
    private val _uiState = mutableStateOf(HomeUiState.sample())
    val uiState: State<HomeUiState> = _uiState

    private val navigationChannel = Channel<HomeNavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<HomeNavigationEvent> = navigationChannel.receiveAsFlow()

    init {
        observeData()
        flushPendingSync()
    }

    private fun observeData() {
        viewModelScope.launch {
            val userFlow = repository.getCurrentUser()
            val lessonsFlow = repository.getAllLessons()
            val sectionsFlow = repository.getAllSections()
            val unitsFlow = repository.getAllUnits()
            val levelsFlow = repository.getAllLevels()
            val progressFlow = userFlow.flatMapLatest { user ->
                if (user != null) repository.getUserProgress(user.userId) else flowOf(emptyList())
            }

            combine(
                combine(userFlow, lessonsFlow, sectionsFlow, unitsFlow, levelsFlow) { user, lessons, sections, units, levels ->
                    HomeInputs(
                        user = user,
                        lessons = lessons,
                        sections = sections,
                        units = units,
                        levels = levels
                    )
                },
                progressFlow
            ) { inputs, progress ->
                val user = inputs.user
                val lessons = inputs.lessons
                val sections = inputs.sections
                val units = inputs.units
                val levels = inputs.levels
                val totalLessons = lessons.size.coerceAtLeast(1)
                val completedLessonIds = progress.filter { it.isCompleted }.map { it.lessonId }.toSet()
                val completed = completedLessonIds.size
                val progress = (completed.toFloat() / totalLessons.toFloat()).coerceIn(0f, 1f)
                val lessonsByLevel = lessons.groupBy { it.levelId }
                val unitsBySection = units.groupBy { it.sectionId }
                val levelsByUnit = levels.groupBy { it.unitId }

                val sectionUi = sections.sortedBy { it.order }.map { section ->
                    val sectionUnits = unitsBySection[section.id].orEmpty()
                        .sortedBy { it.order }
                        .map { unit ->
                            val unitLevels = levelsByUnit[unit.id].orEmpty()
                                .sortedBy { it.order }
                                .map { level ->
                                    val levelLessons = lessonsByLevel[level.id].orEmpty()
                                    val lessonIds = levelLessons.map { it.id }
                                    val primaryLesson = levelLessons.firstOrNull()
                                    val levelCompleted = lessonIds.isNotEmpty() &&
                                        lessonIds.all { it in completedLessonIds }
                                    val levelInProgress = lessonIds.any { it in completedLessonIds }
                                    val levelUnlocked = levelLessons.any { it.isUnlocked }

                                    val status = when {
                                        levelCompleted -> LevelStatus.COMPLETED
                                        levelInProgress -> LevelStatus.IN_PROGRESS
                                        levelUnlocked -> LevelStatus.AVAILABLE
                                        else -> LevelStatus.LOCKED
                                    }

                                    LevelUi(
                                        id = level.id,
                                        order = level.order,
                                        lessonIds = lessonIds,
                                        status = status,
                                        lessonTitle = primaryLesson?.title ?: "Level ${level.order}",
                                        unlockCost = primaryLesson?.let { calculateUnlockCost(it) } ?: 0,
                                        isUnlocked = levelUnlocked
                                    )
                                }

                            UnitUi(
                                id = unit.id,
                                title = unit.title,
                                topic = unit.topic,
                                levels = unitLevels
                            )
                        }

                    SectionUi(
                        id = section.id,
                        title = section.title,
                        cefrLevel = section.cefrLevel,
                        units = sectionUnits
                    )
                }

                val nextLesson = selectNextLesson(lessons, completedLessonIds)

                HomeUiState(
                    avatarUrl = user?.avatarUrl,
                    userName = user?.displayName ?: "Người học",
                    coins = user?.coins ?: 0,
                    streakDays = user?.streakDays ?: 0,
                    streakRewardAvailable = (user?.streakDays ?: 0) > 0,
                    level = user?.currentLevel ?: 1,
                    difficulty = Difficulty.EASY,
                    progress = progress,
                    maxLevel = totalLessons,
                    totalScore = user?.totalXP ?: 0,
                    nextLesson = nextLesson,
                    sections = sectionUi,
                    badges = emptyList(),
                    quests = emptyList(),
                    boosters = emptyList(),
                    themes = _uiState.value.themes
                )
            }.collectLatest { state -> _uiState.value = state }
        }
    }

    private fun flushPendingSync() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { syncManager.flushQueue() }
        }
    }

    fun onPlayClicked() {
        val lessonId = _uiState.value.nextLesson?.id ?: _uiState.value.level
        emitEvent(HomeNavigationEvent.NavigateToPlay(lessonId))
    }

    fun onLessonSelected(lessonId: Int) {
        emitEvent(HomeNavigationEvent.NavigateToPlay(lessonId))
    }

    fun unlockLesson(lesson: LessonSummary) {
        viewModelScope.launch {
            val result = repository.unlockLessonWithCoins(lesson.id, lesson.unlockCost)
            emitEvent(HomeNavigationEvent.ShowMessage(result.message))
        }
    }

    fun onFlashcardsClicked() {
        emitEvent(HomeNavigationEvent.NavigateToFlashcards(_uiState.value.level))
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

    private fun selectNextLesson(
        lessons: List<com.example.master.data.local.entity.LessonEntity>,
        completedLessonIds: Set<Int>
    ): LessonSummary? {
        val orderedLessons = lessons.sortedBy { it.order }
        val nextLesson = orderedLessons.firstOrNull { it.isUnlocked && it.id !in completedLessonIds }
            ?: orderedLessons.firstOrNull { it.isUnlocked }
            ?: orderedLessons.firstOrNull()
        return nextLesson?.let { lesson ->
            LessonSummary(
                id = lesson.id,
                title = lesson.title,
                description = lesson.description,
                difficulty = lesson.difficulty,
                totalWords = lesson.totalWords,
                totalExercises = lesson.totalExercises,
                isUnlocked = lesson.isUnlocked,
                unlockCost = if (lesson.isUnlocked) 0 else calculateUnlockCost(lesson)
            )
        }
    }

    private fun calculateUnlockCost(lesson: com.example.master.data.local.entity.LessonEntity): Int {
        val base = when (lesson.difficulty.uppercase()) {
            "HARD" -> 120
            "MEDIUM" -> 80
            else -> 50
        }
        val tier = ((lesson.order - 1) / 5) * 10
        return base + tier
    }
}
