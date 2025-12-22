package com.example.server

import com.example.server.model.SyncEventsPayload
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncEventsPayloadContractTest {

    private val json = Json {
        ignoreUnknownKeys = false
        isLenient = false
    }

    @Test
    fun `decodes sync event payload produced by Gson client`() {
        val payloadJson =
            """
            {
              "userId": "testUser",
              "lessonCompletions": [
                {
                  "eventId": "e1",
                  "occurredAt": 1,
                  "lessonId": 1,
                  "score": 10,
                  "correctAnswers": 8,
                  "wrongAnswers": 2,
                  "timeSpent": 1000
                }
              ]
            }
            """.trimIndent()

        val payload = json.decodeFromString<SyncEventsPayload>(payloadJson)
        assertEquals("testUser", payload.userId)
        val event = payload.lessonCompletions.single()
        assertEquals("e1", event.eventId)
        assertEquals(1, event.lessonId)
        assertEquals(10, event.score)
        assertEquals(8, event.correctAnswers)
        assertEquals(2, event.wrongAnswers)
        assertEquals(1000, event.timeSpent)
    }
}

