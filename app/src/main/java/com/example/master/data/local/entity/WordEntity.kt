package com.example.master.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordEntity(
    val word: String,
    val translation: String,
    val pronunciation: String,
    val partOfSpeech: String,
    val exampleSentence: String = "",
    val exampleTranslation: String = "",
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    
    // Enhanced fields for better learning
    val synonyms: String? = null,           // JSON: ["big", "large", "huge"]
    val antonyms: String? = null,           // JSON: ["small", "tiny", "little"]
    val collocations: String? = null,       // JSON: ["make a mistake", "make a decision"]
    val frequencyRank: Int? = null,         // 1-5000 (how common the word is)
    val cefrLevel: String = "A1",           // "A1", "A2", "B1", "B2", "C1", "C2"
    val usageNotes: String? = null,         // Tips on how to use this word
    
    val lessonId: Int,
    val difficulty: Int = 1,
    val category: String = "",
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

