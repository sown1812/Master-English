package com.example.master.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.master.data.local.entity.MistakeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MistakeDao {

    @Query("SELECT * FROM mistakes WHERE userId = :userId ORDER BY createdAt DESC")
    fun getMistakes(userId: String): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistakes WHERE userId = :userId AND lessonId = :lessonId ORDER BY createdAt DESC")
    fun getMistakesByLesson(userId: String, lessonId: Int): Flow<List<MistakeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMistake(mistake: MistakeEntity)

    @Query("DELETE FROM mistakes WHERE id = :id")
    suspend fun deleteMistake(id: Int)

    @Query("DELETE FROM mistakes WHERE userId = :userId AND lessonId = :lessonId")
    suspend fun deleteMistakesByLesson(userId: String, lessonId: Int)

    @Query("DELETE FROM mistakes WHERE userId = :userId")
    suspend fun deleteAll(userId: String)
}
