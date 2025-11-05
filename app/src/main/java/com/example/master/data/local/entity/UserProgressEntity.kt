package com.example.master.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val userId: String,
    val lessonId: Int,
    val wordId: Int? = null,
    
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    
    val score: Int = 0,                 // Score for this lesson/word
    val accuracy: Float = 0f,           // 0.0 - 1.0
    val timeSpent: Long = 0,            // Milliseconds
    
    val attempts: Int = 0,              // Number of attempts
    val correctAnswers: Int = 0,
    val wrongAnswers: Int = 0,
    
    val xpEarned: Int = 0,
    val coinsEarned: Int = 0,
    
    val lastReviewDate: Long? = null,   // For spaced repetition
    val nextReviewDate: Long? = null,   // When to review again
    val reviewCount: Int = 0,           // How many times reviewed
    val easeFactor: Float = 2.5f,       // For SRS algorithm
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
