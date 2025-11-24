package com.example.server.routes

import com.example.server.auth.ensureUser
import com.example.server.auth.requireFirebaseUser
import com.example.server.model.SaveProgressRequest
import com.example.server.services.ProgressService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*

fun Route.progressRoutes() {
    val service = ProgressService()
    route("/progress") {
        post {
            val req = call.receive<SaveProgressRequest>()
            if (!call.ensureUser(req.userId)) return@post
            val result = service.saveProgress(req)
            result.fold(
                onSuccess = { id -> call.respond(mapOf("id" to id)) },
                onFailure = { err -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to err.message)) }
            )
        }

        get("/user/{userId}") {
            val userIdParam = call.parameters["userId"] ?: throw IllegalArgumentException("Invalid user id")
            if (!call.ensureUser(userIdParam)) return@get
            val items = service.getByUser(userIdParam)
            call.respond(items)
        }

        get("/lesson/{lessonId}") {
            val principal = call.requireFirebaseUser() ?: return@get
            val lessonId = call.parameters["lessonId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid lesson id")
            val items = service.getByLesson(principal.uid, lessonId)
            call.respond(items)
        }
    }
}
