package com.example.master.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.master.data.local.entity.UnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitDao {
    @Query("SELECT * FROM units ORDER BY `order` ASC")
    fun getAllUnits(): Flow<List<UnitEntity>>

    @Query("SELECT COUNT(*) FROM units")
    suspend fun getUnitsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(items: List<UnitEntity>)
}
