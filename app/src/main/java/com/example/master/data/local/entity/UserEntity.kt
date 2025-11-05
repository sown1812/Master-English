package com.example.master.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,                 // Firebase UID or local ID
    
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
    
    val currentLevel: Int = 1,
    val totalXP: Int = 0,
    val coins: Int = 0,
    
    val streakDays: Int = 0,
    val lastStudyDate: Long = 0,
    val longestStreak: Int = 0,
    
    val wordsLearned: Int = 0,
    val lessonsCompleted: Int = 0,
    val exercisesCompleted: Int = 0,
    
    val isPremium: Boolean = false,
    val premiumExpiryDate: Long? = null,
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = 0
)
