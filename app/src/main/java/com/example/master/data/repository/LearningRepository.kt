package com.example.master.data.repository

import com.example.master.core.user.UserProfile
import com.example.master.core.user.toUserProfile
import com.example.master.data.local.AppDatabase
import com.example.master.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

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
        progressDao.insertProgress(progress)
        
        // Update user stats
        if (progress.isCompleted) {
            userDao.incrementLessonsCompleted(progress.userId)
            addXP(progress.userId, progress.xpEarned)
            addCoins(progress.userId, progress.coinsEarned)
            updateStreak(progress.userId)
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
                xpReward = 100,
                coinsReward = 40
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "WORDS_100",
                title = "Word Master",
                description = "Learn 100 words",
                target = 100,
                xpReward = 200,
                coinsReward = 80
            ),
            AchievementEntity(
                userId = userId,
                achievementType = "PERFECT_LESSON",
                title = "Perfectionist",
                description = "Complete a lesson with 100% accuracy",
                target = 1,
                xpReward = 100,
                coinsReward = 50
            )
        )
        achievementDao.insertAchievements(achievements)
    }
    
    private suspend fun checkAchievements(userId: String) {
        val user = userDao.getUserByIdSync(userId) ?: return
        
        // Check lessons completed
        checkAndUnlockAchievement(userId, "FIRST_LESSON", user.lessonsCompleted)
        checkAndUnlockAchievement(userId, "LESSONS_5", user.lessonsCompleted)
        checkAndUnlockAchievement(userId, "LESSONS_10", user.lessonsCompleted)
        
        // Check streak
        checkAndUnlockAchievement(userId, "STREAK_3", user.streakDays)
        checkAndUnlockAchievement(userId, "STREAK_7", user.streakDays)
        
        // Check words learned
        checkAndUnlockAchievement(userId, "WORDS_50", user.wordsLearned)
        checkAndUnlockAchievement(userId, "WORDS_100", user.wordsLearned)
    }
    
    private suspend fun checkAndUnlockAchievement(userId: String, type: String, progress: Int) {
        val achievement = achievementDao.getAchievementByType(userId, type) ?: return
        
        if (!achievement.isUnlocked && progress >= achievement.target) {
            achievementDao.unlockAchievement(achievement.id, System.currentTimeMillis())
            addXP(userId, achievement.xpReward)
            addCoins(userId, achievement.coinsReward)
        } else if (!achievement.isUnlocked) {
            achievementDao.updateProgress(achievement.id, progress)
        }
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
