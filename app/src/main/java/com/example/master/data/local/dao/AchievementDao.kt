package com.example.master.data.local.dao

import androidx.room.*
import com.example.master.data.local.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    
    @Query("SELECT * FROM achievements WHERE userId = :userId ORDER BY isUnlocked DESC, id")
    fun getUserAchievements(userId: String): Flow<List<AchievementEntity>>
    
    @Query("SELECT * FROM achievements WHERE userId = :userId AND isUnlocked = 1")
    fun getUnlockedAchievements(userId: String): Flow<List<AchievementEntity>>
    
    @Query("SELECT * FROM achievements WHERE userId = :userId AND achievementType = :type")
    suspend fun getAchievementByType(userId: String, type: String): AchievementEntity?
    
    @Query("SELECT COUNT(*) FROM achievements WHERE userId = :userId AND isUnlocked = 1")
    suspend fun getUnlockedCount(userId: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)
    
    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)
    
    @Query("UPDATE achievements SET isUnlocked = 1, unlockedAt = :unlockedAt WHERE id = :achievementId")
    suspend fun unlockAchievement(achievementId: Int, unlockedAt: Long)
    
    @Query("UPDATE achievements SET progress = :progress WHERE id = :achievementId")
    suspend fun updateProgress(achievementId: Int, progress: Int)
    
    @Delete
    suspend fun deleteAchievement(achievement: AchievementEntity)
}
