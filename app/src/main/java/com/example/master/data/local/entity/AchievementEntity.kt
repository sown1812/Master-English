package com.example.master.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val userId: String,
    val achievementType: String,        // "FIRST_LESSON", "STREAK_7", "WORDS_100", etc.
    val title: String,
    val description: String,
    
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    
    val progress: Int = 0,              // Current progress
    val target: Int = 1,                // Target to unlock
    
    val xpReward: Int = 0,
    val coinsReward: Int = 0,
    val badgeUrl: String? = null,
    
    val createdAt: Long = System.currentTimeMillis()
)
