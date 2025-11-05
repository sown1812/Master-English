package com.example.master.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val title: String,                  // "Greetings & Introductions"
    val description: String,            // "Learn basic greetings"
    val order: Int,                     // Display order (1, 2, 3...)
    val totalWords: Int,                // Number of words in this lesson
    val totalExercises: Int,            // Number of exercises
    
    val difficulty: String = "EASY",    // "EASY", "MEDIUM", "HARD"
    val category: String = "",          // "basics", "intermediate", "advanced"
    val iconUrl: String? = null,        // Icon for the lesson
    
    val xpReward: Int = 50,            // XP earned on completion
    val coinsReward: Int = 10,         // Coins earned on completion
    
    val isUnlocked: Boolean = false,    // Is this lesson available?
    val isPremium: Boolean = false,     // Requires premium?
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
