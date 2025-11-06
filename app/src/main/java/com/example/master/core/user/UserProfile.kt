package com.example.master.core.user

import com.example.master.data.local.entity.UserEntity
import java.util.Date

data class UserProfile(
    val userId: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val level: Int,
    val xp: Int,
    val coins: Int,
    val streakDays: Int,
    val createdAt: Date,
    val lastActive: Date
)

fun UserEntity.toUserProfile(): UserProfile = UserProfile(
    userId = userId,
    email = email,
    displayName = displayName,
    avatarUrl = avatarUrl,
    level = currentLevel,
    xp = totalXP,
    coins = coins,
    streakDays = streakDays,
    createdAt = Date(createdAt),
    lastActive = Date(updatedAt)
)
