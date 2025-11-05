package com.example.master.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val lessonId: Int,                  // Foreign key to lesson
    val wordId: Int,                    // Foreign key to word
    
    val type: String,                   // "MULTIPLE_CHOICE", "FILL_BLANK", "MATCHING", "LISTENING", "TRANSLATION"
    val question: String,               // "What is 'Xin chào' in English?"
    val correctAnswer: String,          // "hello"
    
    // For multiple choice
    val optionA: String? = null,
    val optionB: String? = null,
    val optionC: String? = null,
    val optionD: String? = null,
    
    // For matching exercises
    val matchPairs: String? = null,     // JSON string of pairs
    
    val hint: String? = null,           // Optional hint
    val explanation: String? = null,    // Explanation for correct answer
    
    val order: Int,                     // Order in lesson
    val difficulty: Int = 1,            // 1-5
    
    val createdAt: Long = System.currentTimeMillis()
)
