package com.example.master.data.local.dao

import androidx.room.*
import com.example.master.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    
    @Query("SELECT * FROM exercises WHERE lessonId = :lessonId ORDER BY `order`")
    fun getExercisesByLesson(lessonId: Int): Flow<List<ExerciseEntity>>
    
    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun getExerciseById(exerciseId: Int): ExerciseEntity?
    
    @Query("SELECT * FROM exercises WHERE type = :type")
    fun getExercisesByType(type: String): Flow<List<ExerciseEntity>>
    
    @Query("SELECT COUNT(*) FROM exercises WHERE lessonId = :lessonId")
    suspend fun getExercisesCountByLesson(lessonId: Int): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)
    
    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)
    
    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)
    
    @Query("DELETE FROM exercises WHERE lessonId = :lessonId")
    suspend fun deleteExercisesByLesson(lessonId: Int)
}
