package com.example.master.data.local.dao

import androidx.room.*
import com.example.master.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    
    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUserById(userId: String): Flow<UserEntity?>
    
    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserByIdSync(userId: String): UserEntity?
    
    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>
    
    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUserSync(): UserEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Query("UPDATE users SET totalXP = totalXP + :xp WHERE userId = :userId")
    suspend fun addXP(userId: String, xp: Int)
    
    @Query("UPDATE users SET coins = coins + :coins WHERE userId = :userId")
    suspend fun addCoins(userId: String, coins: Int)
    
    @Query("UPDATE users SET wordsLearned = wordsLearned + 1 WHERE userId = :userId")
    suspend fun incrementWordsLearned(userId: String)
    
    @Query("UPDATE users SET lessonsCompleted = lessonsCompleted + 1 WHERE userId = :userId")
    suspend fun incrementLessonsCompleted(userId: String)
    
    @Query("UPDATE users SET exercisesCompleted = exercisesCompleted + 1 WHERE userId = :userId")
    suspend fun incrementExercisesCompleted(userId: String)
    
    @Query("UPDATE users SET streakDays = :streakDays, lastStudyDate = :lastStudyDate WHERE userId = :userId")
    suspend fun updateStreak(userId: String, streakDays: Int, lastStudyDate: Long)
    
    @Delete
    suspend fun deleteUser(user: UserEntity)
    
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
