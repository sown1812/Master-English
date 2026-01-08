package com.example.master.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey
    val id: Int,
    val sectionId: Int,
    val title: String,
    val topic: String,
    val description: String,
    val order: Int
)
