package com.example.master.network

import com.example.master.data.local.entity.UserEntity
import com.example.master.data.local.entity.UserProgressEntity
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncContractTest {

    private val gson = Gson()

    @Test
    fun `sync payload uses server field names and excludes local-only user fields`() {
        val user = UserEntity(
            userId = "u1",
            email = "u1@example.com",
            displayName = "User 1",
            totalXP = 123,
            coins = 42,
            streakDays = 3,
            lastStudyDate = 0L,
            longestStreak = 5,
            wordsLearned = 10,
            lessonsCompleted = 2,
            exercisesCompleted = 20,
            createdAt = 111L,
            updatedAt = 222L,
            lastSyncedAt = 333L
        )

        val payload = SyncPayloadRemote(
            user = user.toRemote(),
            progress = emptyList(),
            achievements = emptyList()
        )

        val json = gson.toJson(payload)
        assertTrue(json.contains("\"totalXp\":123"))
        assertFalse(json.contains("\"totalXP\""))
        assertFalse(json.contains("\"createdAt\""))
        assertFalse(json.contains("\"updatedAt\""))
        assertFalse(json.contains("\"lastSyncedAt\""))
    }

    @Test
    fun `progress accuracy scales to percent on wire and back to fraction locally`() {
        val progress = UserProgressEntity(
            id = 1,
            userId = "u1",
            lessonId = 1,
            isCompleted = true,
            completedAt = 123L,
            score = 10,
            accuracy = 0.85f,
            timeSpent = 1_000L,
            attempts = 10,
            correctAnswers = 8,
            wrongAnswers = 2,
            xpEarned = 20,
            coinsEarned = 5,
            lastReviewDate = null,
            nextReviewDate = null,
            reviewCount = 0,
            easeFactor = 2.5f,
            createdAt = 123L,
            updatedAt = 124L
        )

        val remote = progress.toRemote()
        assertEquals(85.0, remote.accuracy, 0.0001)

        val roundTrip = remote.toEntity()
        assertEquals(0.85f, roundTrip.accuracy, 0.0001f)
    }
}

