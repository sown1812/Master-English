package com.example.master.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.master.data.local.entity.SectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {
    @Query("SELECT * FROM sections ORDER BY `order` ASC")
    fun getAllSections(): Flow<List<SectionEntity>>

    @Query("SELECT COUNT(*) FROM sections")
    suspend fun getSectionsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSections(items: List<SectionEntity>)
}
