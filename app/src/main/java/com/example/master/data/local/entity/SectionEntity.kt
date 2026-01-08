package com.example.master.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey
    val id: Int,
    val title: String,
    val description: String,
    val cefrLevel: String,
    val order: Int
)
