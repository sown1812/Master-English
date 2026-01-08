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

data class GameStateResponseRemote(
    @SerializedName("boosters") val boosters: List<BoosterStateRemote>,
    @SerializedName("quests") val quests: List<QuestStateRemote>
)

data class UpdateBoosterRequest(
    val boosterKey: String,
    val owned: Boolean
)

data class UpdateQuestRequest(
    val questKey: String,
    val claimed: Boolean
)

