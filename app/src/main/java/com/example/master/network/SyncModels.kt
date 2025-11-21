package com.example.master.network

import com.example.master.data.local.entity.AchievementEntity
import com.example.master.data.local.entity.UserEntity
import com.example.master.data.local.entity.UserProgressEntity

data class SyncPayload(
    val user: UserEntity,
    val progress: List<UserProgressEntity>,
    val achievements: List<AchievementEntity>
)

data class SyncResponse(
    val user: UserEntity?,
    val progress: List<UserProgressEntity>?,
    val achievements: List<AchievementEntity>?
)
