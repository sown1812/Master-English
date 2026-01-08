package com.example.master.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

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

    @POST("/sync")
    suspend fun sync(@Body payload: SyncEventsPayloadRemote): SyncResponseRemote

    @GET("/lessons")
    suspend fun getLessons(): List<LessonRemote>

    @GET("/lessons/{id}/words")
    suspend fun getWordsByLesson(@Path("id") lessonId: Int): List<WordRemote>

    @GET("/lessons/{id}/exercises")
    suspend fun getExercisesByLesson(@Path("id") lessonId: Int): List<ExerciseRemote>
}
