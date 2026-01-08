package com.example.master.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.master.data.local.entity.LevelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelDao {
    @Query("SELECT * FROM levels ORDER BY `order` ASC")
    fun getAllLevels(): Flow<List<LevelEntity>>

    @Query("SELECT COUNT(*) FROM levels")
    suspend fun getLevelsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevels(items: List<LevelEntity>)
}
