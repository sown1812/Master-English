package com.example.master.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mistakes")
data class MistakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val lessonId: Int,
    val exerciseId: Int? = null,
    val wordId: Int? = null,
    val question: String,
    val userAnswer: String,
    val correctAnswer: String,
    val reason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
