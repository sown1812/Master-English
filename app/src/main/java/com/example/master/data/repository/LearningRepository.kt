package com.example.master.data.repository

import com.example.master.core.user.UserProfile
import com.example.master.core.user.toUserProfile
import com.example.master.data.local.AppDatabase
import com.example.master.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class LearningRepository @Inject constructor(
    private val database: AppDatabase
) {
    
    private val wordDao = database.wordDao()
    private val lessonDao = database.lessonDao()
    private val exerciseDao = database.exerciseDao()
    private val userDao = database.userDao()
    private val progressDao = database.userProgressDao()
    private val achievementDao = database.achievementDao()
    private val mistakeDao = database.mistakeDao()
    
    private val oneDayInMillis = 24 * 60 * 60 * 1000L
    
    // ==================== Lessons ====================
    
    fun getAllLessons(): Flow<List<LessonEntity>> = lessonDao.getAllLessons()
    
    fun getUnlockedLessons(): Flow<List<LessonEntity>> = lessonDao.getUnlockedLessons()
    
    suspend fun getLessonById(lessonId: Int): LessonEntity? = lessonDao.getLessonById(lessonId)
    
    suspend fun unlockLesson(lessonId: Int) = lessonDao.unlockLesson(lessonId)
    
    suspend fun getTotalLessonsCount(): Int = lessonDao.getTotalLessonsCount()
    
    // ==================== Words ====================
    
    fun getAllWords(): Flow<List<WordEntity>> = wordDao.getAllWords()
    
    fun getWordsByLesson(lessonId: Int): Flow<List<WordEntity>> = wordDao.getWordsByLesson(lessonId)
    
    suspend fun getWordById(wordId: Int): WordEntity? = wordDao.getWordById(wordId)
    
    fun searchWords(query: String): Flow<List<WordEntity>> = wordDao.searchWords(query)
    
    suspend fun getTotalWordsCount(): Int = wordDao.getTotalWordsCount()

    // ==================== Mistakes ====================

    fun getMistakes(userId: String): Flow<List<MistakeEntity>> =
        mistakeDao.getMistakes(userId)

    fun getMistakesByLesson(userId: String, lessonId: Int): Flow<List<MistakeEntity>> =
        mistakeDao.getMistakesByLesson(userId, lessonId)

    suspend fun saveMistake(mistake: MistakeEntity) = mistakeDao.insertMistake(mistake)

    suspend fun deleteMistake(id: Int) = mistakeDao.deleteMistake(id)

    suspend fun deleteMistakesByLesson(userId: String, lessonId: Int) =
        mistakeDao.deleteMistakesByLesson(userId, lessonId)

    suspend fun deleteAllMistakes(userId: String) = mistakeDao.deleteAll(userId)
    
    // ==================== Exercises ====================
    
    fun getExercisesByLesson(lessonId: Int): Flow<List<ExerciseEntity>> = 
        exerciseDao.getExercisesByLesson(lessonId)
    
    suspend fun getExerciseById(exerciseId: Int): ExerciseEntity? = 
        exerciseDao.getExerciseById(exerciseId)
    
    // ==================== User ====================
    
    fun getCurrentUser(): Flow<UserEntity?> = userDao.getCurrentUser()
    
    suspend fun getCurrentUserSync(): UserEntity? = userDao.getCurrentUserSync()
    
    fun getUserById(userId: String): Flow<UserEntity?> = userDao.getUserById(userId)
    
    suspend fun getUserByIdSync(userId: String): UserEntity? = userDao.getUserByIdSync(userId)
    
    fun getUserProfile(userId: String): Flow<UserProfile?> =
        userDao.getUserById(userId).map { it?.toUserProfile() }
    
    suspend fun getUserProfileSync(userId: String): UserProfile? =
        userDao.getUserByIdSync(userId)?.toUserProfile()
    
    suspend fun insertUser(user: UserEntity) = userDao.insertUser(user)
    
    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)
    
    suspend fun addXP(userId: String, xp: Int) {
        userDao.addXP(userId, xp)
        checkLevelUp(userId)
    }
    
    suspend fun addCoins(userId: String, coins: Int) = userDao.addCoins(userId, coins)
    
    private suspend fun checkLevelUp(userId: String) {
        val user = userDao.getUserByIdSync(userId) ?: return
        val newLevel = calculateLevel(user.totalXP)
        if (newLevel > user.currentLevel) {
            userDao.updateUser(user.copy(currentLevel = newLevel))
            // Unlock next lesson
            val nextLessonId = newLevel
            if (nextLessonId <= getTotalLessonsCount()) {
                unlockLesson(nextLessonId)
            }
        }
    }
    
    private fun calculateLevel(totalXP: Int): Int {
        // Simple formula: Level = XP / 100
        // Level 1: 0-99 XP
        // Level 2: 100-199 XP
        // etc.
        return (totalXP / 100) + 1
    }
    
    suspend fun updateStreak(userId: String) {
        val user = userDao.getUserByIdSync(userId) ?: return
        val currentDate = System.currentTimeMillis()
        val lastStudyDate = user.lastStudyDate
        val oneDayInMillis = 24 * 60 * 60 * 1000
        
        val daysDifference = ((currentDate - lastStudyDate) / oneDayInMillis).toInt()
        
        val newStreakDays = when {
            daysDifference == 0 -> user.streakDays // Same day, no change
            daysDifference == 1 -> user.streakDays + 1 // Next day, increment
            else -> 1 // Streak broken, reset to 1
        }
        
        userDao.updateStreak(userId, newStreakDays, currentDate)
    }
    
    // ==================== Progress ====================
    
    fun getUserProgress(userId: String): Flow<List<UserProgressEntity>> = 
        progressDao.getUserProgress(userId)
    
    suspend fun getLessonProgress(userId: String, lessonId: Int): UserProgressEntity? = 
        progressDao.getLessonProgress(userId, lessonId)
    
    suspend fun saveProgress(progress: UserProgressEntity) {
        val existing = progressDao.getLessonProgress(progress.userId, progress.lessonId)
        val wasCompleted = existing?.isCompleted == true
        val progressToSave = if (existing != null) {
            progress.copy(id = existing.id, createdAt = existing.createdAt)
        } else progress
        
        progressDao.insertProgress(progressToSave)
        
        if (progress.isCompleted) {
            addXP(progress.userId, progress.xpEarned)
            addCoins(progress.userId, progress.coinsEarned)
            updateStreak(progress.userId)
            
            if (!wasCompleted) {
                userDao.incrementLessonsCompleted(progress.userId)
            }
            checkAchievements(progress.userId)
        }
    }
    
    suspend fun updateProgress(progress: UserProgressEntity) = progressDao.updateProgress(progress)
    
    suspend fun getCompletedCount(userId: String): Int = progressDao.getCompletedCount(userId)
    
    suspend fun getAverageAccuracy(userId: String): Float = 
        progressDao.getAverageAccuracy(userId) ?: 0f
    
    // ==================== Achievements ====================
    
    fun getUserAchievements(userId: String): Flow<List<AchievementEntity>> = 
        achievementDao.getUserAchievements(userId)
    
    suspend fun initializeAchievements(userId: String) {
        val totalLessons = getTotalLessonsCount().coerceAtLeast(1)
        val achievements = listOf(
            AchievementEntity(
                userId = userId,
                achievementType = "FIRST_LESSON",
                title = "First Steps",
                description = "Complete your first lesson",
                target = 1,
                xpReward = 50,
                coinsReward = 20
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "LESSONS_5",
                title = "Quick Learner",
                description = "Complete 5 lessons",
                target = 5,
                xpReward = 100,
                coinsReward = 50
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "LESSONS_10",
                title = "Dedicated Student",
                description = "Complete all 10 lessons",
                target = 10,
                xpReward = 200,
                coinsReward = 100
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "STREAK_3",
                title = "Consistent",
                description = "Maintain a 3-day streak",
                target = 3,
                xpReward = 75,
                coinsReward = 30
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "STREAK_7",
                title = "Streak Master",
                description = "Maintain a 7-day streak",
                target = 7,
                xpReward = 150,
                coinsReward = 60
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "WORDS_50",
                title = "Vocabulary Builder",
                description = "Learn 50 words",
                target = 50,
                xpReward = 80,
                coinsReward = 40
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "WORDS_100",
                title = "Word Master",
                description = "Learn 100 words",
                target = 100,
                xpReward = 100,
                coinsReward = 50
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "VOCAB_100",
                title = "Vocabulary Novice",
                description = "Learn 100 words",
                target = 100,
                xpReward = 120,
                coinsReward = 50
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "VOCAB_500",
                title = "Vocabulary Expert",
                description = "Learn 500 words",
                target = 500,
                xpReward = 250,
                coinsReward = 120
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "VOCAB_1000",
                title = "Vocabulary Master",
                description = "Learn 1000 words",
                target = 1000,
                xpReward = 500,
                coinsReward = 200
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "GRAMMAR_A1_A2",
                title = "Grammar Foundation",
                description = "Complete grammar foundations (A1-A2)",
                target = 5,
                xpReward = 150,
                coinsReward = 60
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "GRAMMAR_GURU",
                title = "Grammar Guru",
                description = "Complete all grammar quests",
                target = 15,
                xpReward = 250,
                coinsReward = 120
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "LISTENING_100",
                title = "Listening Pro",
                description = "Finish 100 listening exercises",
                target = 100,
                xpReward = 200,
                coinsReward = 80
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "SPEAKING_100",
                title = "Speaking Champion",
                description = "Complete 100 speaking practices",
                target = 100,
                xpReward = 220,
                coinsReward = 90
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "WRITING_50",
                title = "Writing Wizard",
                description = "Submit 50 writing/translation tasks",
                target = 50,
                xpReward = 180,
                coinsReward = 80
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "QUEST_STARTER",
                title = "Quest Starter",
                description = "Complete your first unit",
                target = 1,
                xpReward = 80,
                coinsReward = 30
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "QUEST_WARRIOR",
                title = "Quest Warrior",
                description = "Complete 10 units",
                target = 10,
                xpReward = 180,
                coinsReward = 90
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "QUEST_MASTER",
                title = "Quest Master",
                description = "Finish a full learning path",
                target = totalLessons,
                xpReward = 400,
                coinsReward = 180
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "MULTI_QUEST_HERO",
                title = "Multi-Quest Hero",
                description = "Learn from 2+ categories",
                target = 2,
                xpReward = 150,
                coinsReward = 70
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "CAREER_EXPERT",
                title = "Career Expert",
                description = "Finish a specialization/career lesson",
                target = 1,
                xpReward = 200,
                coinsReward = 90
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "PERFECT_WEEK",
                title = "Perfect Week",
                description = "7 days liên tiếp đạt mục tiêu",
                target = 7,
                xpReward = 160,
                coinsReward = 70
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "SPEED_RUNNER",
                title = "Speed Runner",
                description = "Hoàn thành 10 bài trong 1 ngày",
                target = 10,
                xpReward = 220,
                coinsReward = 100
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "NIGHT_OWL",
                title = "Night Owl",
                description = "Study after 10pm",
                target = 1,
                xpReward = 120,
                coinsReward = 50
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "EARLY_BIRD",
                title = "Early Bird",
                description = "Study before 7am",
                target = 1,
                xpReward = 120,
                coinsReward = 50
            )
        )
        achievementDao.insertAchievements(achievements)
    }
    
    private suspend fun checkAchievements(userId: String) {
        val user = userDao.getUserByIdSync(userId) ?: return
        val snapshot = buildAchievementSnapshot(userId)
        
        syncUserProgressCounters(user, snapshot)
        
        val totalLessons = getTotalLessonsCount()
        val lastStudyHour = getHourOfDay(user.lastStudyDate)
        
        // Lessons & quests
        checkAndUnlockAchievement(userId, "FIRST_LESSON", snapshot.lessonsCompleted)
        checkAndUnlockAchievement(userId, "LESSONS_5", snapshot.lessonsCompleted)
        checkAndUnlockAchievement(userId, "LESSONS_10", snapshot.lessonsCompleted)
        checkAndUnlockAchievement(userId, "QUEST_STARTER", snapshot.lessonsCompleted)
        checkAndUnlockAchievement(userId, "QUEST_WARRIOR", snapshot.lessonsCompleted)
        checkAndUnlockAchievement(userId, "QUEST_MASTER", snapshot.lessonsCompleted.coerceAtMost(totalLessons))
        checkAndUnlockAchievement(userId, "GRAMMAR_A1_A2", snapshot.lessonsCompleted)
        checkAndUnlockAchievement(userId, "GRAMMAR_GURU", snapshot.lessonsCompleted)
        checkAndUnlockAchievement(userId, "MULTI_QUEST_HERO", snapshot.categories.size)
        
        val hasCareerLesson = snapshot.categories.any { category ->
            category.contains("work", ignoreCase = true) ||
                category.contains("career", ignoreCase = true) ||
                category.contains("technology", ignoreCase = true)
        }
        checkAndUnlockAchievement(userId, "CAREER_EXPERT", if (hasCareerLesson) 1 else 0)
        
        // Vocabulary / skills
        checkAndUnlockAchievement(userId, "WORDS_50", snapshot.wordsLearned)
        checkAndUnlockAchievement(userId, "VOCAB_100", snapshot.wordsLearned)
        checkAndUnlockAchievement(userId, "VOCAB_500", snapshot.wordsLearned)
        checkAndUnlockAchievement(userId, "VOCAB_1000", snapshot.wordsLearned)
        checkAndUnlockAchievement(userId, "LISTENING_100", snapshot.listeningCompleted)
        checkAndUnlockAchievement(userId, "SPEAKING_100", snapshot.speakingCompleted)
        checkAndUnlockAchievement(userId, "WRITING_50", snapshot.writingCompleted)
        
        // Streak / time-based
        checkAndUnlockAchievement(userId, "STREAK_3", user.streakDays)
        checkAndUnlockAchievement(userId, "STREAK_7", user.streakDays)
        checkAndUnlockAchievement(userId, "PERFECT_WEEK", user.streakDays)
        checkAndUnlockAchievement(userId, "SPEED_RUNNER", snapshot.completedLast24h)
        
        if (lastStudyHour >= 22) {
            checkAndUnlockAchievement(userId, "NIGHT_OWL", 1)
        } else if (lastStudyHour in 4..6) {
            checkAndUnlockAchievement(userId, "EARLY_BIRD", 1)
        }
    }
    
    private suspend fun checkAndUnlockAchievement(userId: String, type: String, progress: Int) {
        val achievement = achievementDao.getAchievementByType(userId, type) ?: return
        
        if (!achievement.isUnlocked && progress >= achievement.target) {
            achievementDao.updateProgress(achievement.id, achievement.target)
            achievementDao.unlockAchievement(achievement.id, System.currentTimeMillis())
            addXP(userId, achievement.xpReward)
            addCoins(userId, achievement.coinsReward)
        } else if (!achievement.isUnlocked) {
            achievementDao.updateProgress(achievement.id, progress)
        }
    }
    
    private suspend fun buildAchievementSnapshot(userId: String): AchievementSnapshot {
        val completedLessonIds = progressDao.getCompletedLessonIds(userId)
        val wordsLearned = completedLessonIds.sumOf { lessonId ->
            wordDao.getWordsCountByLesson(lessonId)
        }
        val totalExercises = completedLessonIds.sumOf { lessonId ->
            exerciseDao.getExercisesCountByLesson(lessonId)
        }
        val listeningCompleted = completedLessonIds.sumOf { lessonId ->
            exerciseDao.getExercisesCountByLessonAndType(lessonId, "LISTENING")
        }
        val speakingCompleted = completedLessonIds.sumOf { lessonId ->
            exerciseDao.getExercisesCountByLessonAndType(lessonId, "SPEAKING")
        }
        val writingCompleted = completedLessonIds.sumOf { lessonId ->
            exerciseDao.getExercisesCountByLessonAndType(lessonId, "TRANSLATION")
        }
        val categories = if (completedLessonIds.isNotEmpty()) {
            lessonDao.getLessonsByIds(completedLessonIds).map { it.category }.toSet()
        } else emptySet()
        val completedLast24h = progressDao.getCompletedCountSince(userId, System.currentTimeMillis() - oneDayInMillis)
        
        return AchievementSnapshot(
            lessonsCompleted = completedLessonIds.size,
            wordsLearned = wordsLearned,
            totalExercises = totalExercises,
            listeningCompleted = listeningCompleted,
            speakingCompleted = speakingCompleted,
            writingCompleted = writingCompleted,
            categories = categories,
            completedLast24h = completedLast24h
        )
    }
    
    private suspend fun syncUserProgressCounters(user: UserEntity, snapshot: AchievementSnapshot) {
        val updatedUser = user.copy(
            lessonsCompleted = max(user.lessonsCompleted, snapshot.lessonsCompleted),
            wordsLearned = max(user.wordsLearned, snapshot.wordsLearned),
            exercisesCompleted = max(user.exercisesCompleted, snapshot.totalExercises),
            updatedAt = System.currentTimeMillis()
        )
        if (updatedUser != user) {
            userDao.updateUser(updatedUser)
        }
    }

    private fun getHourOfDay(timestamp: Long): Int {
        if (timestamp <= 0) return -1
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.HOUR_OF_DAY)
    }
    
    // ==================== Statistics ====================
    
    suspend fun getUserStatistics(userId: String): UserStatistics {
        val user = userDao.getUserByIdSync(userId)
        val completedCount = progressDao.getCompletedCount(userId)
        val averageAccuracy = progressDao.getAverageAccuracy(userId) ?: 0f
        val totalXPEarned = progressDao.getTotalXPEarned(userId) ?: 0
        val unlockedAchievements = achievementDao.getUnlockedCount(userId)
        
        return UserStatistics(
            level = user?.currentLevel ?: 1,
            totalXP = user?.totalXP ?: 0,
            coins = user?.coins ?: 0,
            streakDays = user?.streakDays ?: 0,
            wordsLearned = user?.wordsLearned ?: 0,
            lessonsCompleted = user?.lessonsCompleted ?: 0,
            averageAccuracy = averageAccuracy,
            achievementsUnlocked = unlockedAchievements
        )
    }

    // ==================== Sync helpers ====================

    suspend fun replaceLessons(lessons: List<LessonEntity>) {
        lessonDao.deleteAllLessons()
        if (lessons.isNotEmpty()) {
            lessonDao.insertLessons(lessons)
        }
    }

    suspend fun replaceWordsForLesson(lessonId: Int, words: List<WordEntity>) {
        wordDao.deleteWordsByLesson(lessonId)
        if (words.isNotEmpty()) {
            wordDao.insertWords(words)
        }
    }

    suspend fun replaceExercisesForLesson(lessonId: Int, exercises: List<ExerciseEntity>) {
        exerciseDao.deleteExercisesByLesson(lessonId)
        if (exercises.isNotEmpty()) {
            exerciseDao.insertExercises(exercises)
        }
    }

    suspend fun clearContent() {
        wordDao.deleteAllWords()
        exerciseDao.deleteAllExercises()
        lessonDao.deleteAllLessons()
    }

    suspend fun replaceUser(user: UserEntity) = userDao.insertUser(user)
    
    suspend fun replaceProgress(userId: String, items: List<UserProgressEntity>) {
        progressDao.deleteUserProgress(userId)
        if (items.isNotEmpty()) {
            progressDao.insertProgressList(items)
        }
    }
    
    suspend fun replaceAchievements(userId: String, items: List<AchievementEntity>) {
        achievementDao.deleteAchievementsByUser(userId)
        if (items.isNotEmpty()) {
            achievementDao.insertAchievements(items)
        }
    }
}

data class UserStatistics(
    val level: Int,
    val totalXP: Int,
    val coins: Int,
    val streakDays: Int,
    val wordsLearned: Int,
    val lessonsCompleted: Int,
    val averageAccuracy: Float,
    val achievementsUnlocked: Int
)

data class AchievementSnapshot(
    val lessonsCompleted: Int,
    val wordsLearned: Int,
    val totalExercises: Int,
    val listeningCompleted: Int,
    val speakingCompleted: Int,
    val writingCompleted: Int,
    val categories: Set<String>,
    val completedLast24h: Int
)
