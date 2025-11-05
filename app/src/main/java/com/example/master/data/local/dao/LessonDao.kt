package com.example.master.data.local.dao

import androidx.room.*
import com.example.master.data.local.entity.LessonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    
    @Query("SELECT * FROM lessons ORDER BY `order`")
    fun getAllLessons(): Flow<List<LessonEntity>>
    
    @Query("SELECT * FROM lessons WHERE id = :lessonId")
    suspend fun getLessonById(lessonId: Int): LessonEntity?
    
    @Query("SELECT * FROM lessons WHERE isUnlocked = 1 ORDER BY `order`")
    fun getUnlockedLessons(): Flow<List<LessonEntity>>
    
    @Query("SELECT * FROM lessons WHERE category = :category ORDER BY `order`")
    fun getLessonsByCategory(category: String): Flow<List<LessonEntity>>
    
    @Query("SELECT * FROM lessons WHERE difficulty = :difficulty ORDER BY `order`")
    fun getLessonsByDifficulty(difficulty: String): Flow<List<LessonEntity>>
    
    @Query("SELECT COUNT(*) FROM lessons")
    suspend fun getTotalLessonsCount(): Int
    
    @Query("SELECT COUNT(*) FROM lessons WHERE isUnlocked = 1")
    suspend fun getUnlockedLessonsCount(): Int
    
    @Query("UPDATE lessons SET isUnlocked = 1 WHERE id = :lessonId")
    suspend fun unlockLesson(lessonId: Int)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonEntity>)
    
    @Update
    suspend fun updateLesson(lesson: LessonEntity)
    
    @Delete
    suspend fun deleteLesson(lesson: LessonEntity)
    
    @Query("DELETE FROM lessons")
    suspend fun deleteAllLessons()
}
