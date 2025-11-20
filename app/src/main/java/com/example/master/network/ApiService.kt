package com.example.master.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("/gamestate/{userId}")
    suspend fun getGameState(@Path("userId") userId: String): GameStateResponseRemote

    @POST("/gamestate/{userId}/booster")
    suspend fun updateBooster(
        @Path("userId") userId: String,
        @Body body: UpdateBoosterRequest
    )

    @POST("/gamestate/{userId}/quest")
    suspend fun updateQuest(
        @Path("userId") userId: String,
        @Body body: UpdateQuestRequest
    )

    @POST("/gamestate/{userId}/daily")
    suspend fun updateDaily(
        @Path("userId") userId: String,
        @Body body: UpdateDailyRequest
    )

    @GET("/leaderboard")
    suspend fun getLeaderboard(@Query("limit") limit: Int = 20): List<LeaderboardEntryRemote>
}
