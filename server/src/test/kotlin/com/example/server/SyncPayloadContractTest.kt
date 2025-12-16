package com.example.server

import com.example.server.model.SyncPayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SyncPayloadContractTest {

    private val json = Json {
        ignoreUnknownKeys = false
        isLenient = false
    }

    @Test
    fun `decodes sync payload produced by Gson client`() {
        val payloadJson =
            """
            {
              "user": {
                "userId": "testUser",
                "email": "test@example.com",
                "displayName": "Test User",
                "currentLevel": 1,
                "totalXp": 123,
                "coins": 42,
                "streakDays": 0,
                "lastStudyDate": 0,
                "longestStreak": 0,
                "wordsLearned": 0,
                "lessonsCompleted": 0,
                "exercisesCompleted": 0,
                "isPremium": false
              },
              "progress": [
                {
                  "id": 1,
                  "userId": "testUser",
                  "lessonId": 1,
                  "isCompleted": true,
                  "score": 10,
                  "accuracy": 85.0,
                  "timeSpent": 1000,
                  "attempts": 10,
                  "correctAnswers": 8,
                  "wrongAnswers": 2,
                  "xpEarned": 20,
                  "coinsEarned": 5,
                  "reviewCount": 0,
                  "easeFactor": 2.5,
                  "createdAt": 123,
                  "updatedAt": 124
                }
              ],
              "achievements": [
                {
                  "id": 1,
                  "userId": "testUser",
                  "achievementType": "FIRST_LESSON",
                  "title": "First Steps",
                  "description": "Complete your first lesson",
                  "isUnlocked": false,
                  "progress": 0,
                  "target": 1,
                  "xpReward": 50,
                  "coinsReward": 20,
                  "createdAt": 123
                }
              ]
            }
            """.trimIndent()

        val payload = json.decodeFromString<SyncPayload>(payloadJson)
        assertEquals("testUser", payload.user.userId)
        assertEquals(123, payload.user.totalXp)

        val progress = payload.progress.single()
        assertEquals(85.0, progress.accuracy)
        assertNull(progress.completedAt)
        assertNull(progress.lastReviewDate)
        assertNull(progress.nextReviewDate)

        val achievement = payload.achievements.single()
        assertEquals("FIRST_LESSON", achievement.achievementType)
        assertNull(achievement.unlockedAt)
    }
}
