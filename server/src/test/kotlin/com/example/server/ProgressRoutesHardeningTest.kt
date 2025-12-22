package com.example.server

import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressRoutesHardeningTest {

    @Test
    fun `rejects lesson progress writes (wordId null)`() = testApplication {
        val response = client.post("/progress") {
            header(HttpHeaders.Authorization, "Bearer u1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "userId": "u1",
                  "lessonId": 1,
                  "isCompleted": true,
                  "score": 10,
                  "accuracy": 80.0,
                  "timeSpent": 1000,
                  "attempts": 10,
                  "correctAnswers": 8,
                  "wrongAnswers": 2,
                  "xpEarned": 999,
                  "coinsEarned": 999,
                  "reviewCount": 0,
                  "easeFactor": 2.5
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}

