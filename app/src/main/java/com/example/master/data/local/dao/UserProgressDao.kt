package com.example.master.data.local.dao

import androidx.room.*
import com.example.master.data.local.entity.UserProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {
    
    @Query("SELECT * FROM user_progress WHERE userId = :userId")
    fun getUserProgress(userId: String): Flow<List<UserProgressEntity>>
    
    @Query("SELECT * FROM user_progress WHERE userId = :userId AND lessonId = :lessonId")
    suspend fun getLessonProgress(userId: String, lessonId: Int): UserProgressEntity?
    
    @Query("SELECT * FROM user_progress WHERE userId = :userId AND wordId = :wordId")
    suspend fun getWordProgress(userId: String, wordId: Int): UserProgressEntity?
    
    @Query("SELECT * FROM user_progress WHERE userId = :userId AND isCompleted = 1")
    fun getCompletedProgress(userId: String): Flow<List<UserProgressEntity>>
    
    @Query("SELECT COUNT(*) FROM user_progress WHERE userId = :userId AND isCompleted = 1")
    suspend fun getCompletedCount(userId: String): Int
    
    @Query("SELECT * FROM user_progress WHERE userId = :userId AND nextReviewDate <= :currentDate")
    suspend fun getDueReviews(userId: String, currentDate: Long): List<UserProgressEntity>
    
    @Query("SELECT AVG(accuracy) FROM user_progress WHERE userId = :userId AND isCompleted = 1")
    suspend fun getAverageAccuracy(userId: String): Float?
    
    @Query("SELECT SUM(xpEarned) FROM user_progress WHERE userId = :userId")
    suspend fun getTotalXPEarned(userId: String): Int?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: UserProgressEntity): Long
    
    @Update
    suspend fun updateProgress(progress: UserProgressEntity)
    
    @Delete
    suspend fun deleteProgress(progress: UserProgressEntity)
    
    @Query("DELETE FROM user_progress WHERE userId = :userId")
    suspend fun deleteUserProgress(userId: String)
}
