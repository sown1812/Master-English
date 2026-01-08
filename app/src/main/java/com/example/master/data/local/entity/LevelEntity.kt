package com.example.master.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "levels")
data class LevelEntity(
    @PrimaryKey
    val id: Int,
    val unitId: Int,
    val title: String,
    val order: Int
)
