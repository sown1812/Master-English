package com.example.server

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class LessonRoutesTest {
    @Test
    fun testHealth() = testApplication {
        // Application module defined in application.conf will be loaded automatically.
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
