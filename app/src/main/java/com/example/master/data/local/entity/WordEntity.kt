package com.example.master.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val word: String,                    // "hello"
    val translation: String,             // "xin chào"
    val pronunciation: String,           // "həˈloʊ" (IPA)
    val partOfSpeech: String,           // "noun", "verb", "adjective", etc.
    val exampleSentence: String,        // "Hello, how are you?"
    val exampleTranslation: String,     // "Xin chào, bạn khỏe không?"
    val imageUrl: String? = null,       // URL to image (optional)
    val audioUrl: String? = null,       // URL to audio file (optional)
    
    val lessonId: Int,                  // Foreign key to lesson
    val difficulty: Int = 1,            // 1-5 (1=easy, 5=hard)
    val category: String = "",          // "greetings", "food", "animals", etc.
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
