package com.example.master.network

import com.google.gson.annotations.SerializedName

data class BoosterStateRemote(
    @SerializedName("boosterKey") val boosterKey: String,
    @SerializedName("isOwned") val isOwned: Boolean
)

data class QuestStateRemote(
    @SerializedName("questKey") val questKey: String,
    @SerializedName("isClaimed") val isClaimed: Boolean
)

data class DailyChallengeStateRemote(
    @SerializedName("status") val status: String,
    @SerializedName("progress") val progress: Int,
    @SerializedName("target") val target: Int
)

data class GameStateResponseRemote(
    @SerializedName("boosters") val boosters: List<BoosterStateRemote>,
    @SerializedName("quests") val quests: List<QuestStateRemote>,
    @SerializedName("daily") val daily: DailyChallengeStateRemote?
)

data class UpdateBoosterRequest(
    val boosterKey: String,
    val owned: Boolean
)

data class UpdateQuestRequest(
    val questKey: String,
    val claimed: Boolean
)

data class UpdateDailyRequest(
    val status: String,
    val progress: Int,
    val target: Int
)

data class LeaderboardEntryRemote(
    val userId: String,
    val displayName: String,
    val totalXp: Int,
    val coins: Int,
    val streakDays: Int
)
