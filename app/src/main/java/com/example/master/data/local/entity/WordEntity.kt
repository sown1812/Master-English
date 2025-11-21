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
    val lessonId: Int,
    val difficulty: Int = 1,
    val category: String = "",
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
