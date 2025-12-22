package com.example.server.routes

import com.example.server.auth.ensureUser
import com.example.server.dbQuery
import com.example.server.model.UpdateUserProfileRequest
import com.example.server.model.UserDto
import com.example.server.tables.Users
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*

fun Route.userRoutes() {
    route("/users") {
        get("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid user id")
            if (!call.ensureUser(id)) return@get
            val user = dbQuery {
                Users.selectAll().where { Users.userId eq id }.limit(1).firstOrNull()?.let {
                    UserDto(
                        userId = it[Users.userId],
                        email = it[Users.email],
                        displayName = it[Users.displayName],
                        avatarUrl = it[Users.avatarUrl],
                        currentLevel = it[Users.currentLevel],
                        totalXp = it[Users.totalXp],
                        coins = it[Users.coins],
                        streakDays = it[Users.streakDays],
                        lastStudyDate = it[Users.lastStudyDate],
                        longestStreak = it[Users.longestStreak],
                        wordsLearned = it[Users.wordsLearned],
                        lessonsCompleted = it[Users.lessonsCompleted],
                        exercisesCompleted = it[Users.exercisesCompleted],
                        isPremium = it[Users.isPremium],
                        premiumExpiryDate = it[Users.premiumExpiryDate]
                    )
                }
            }
            if (user == null) {
                call.respond(io.ktor.http.HttpStatusCode.NotFound, mapOf("error" to "User not found"))
            } else {
                call.respond(user)
            }
        }

        put("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid user id")
            if (!call.ensureUser(id)) return@put
            val body = call.receive<UpdateUserProfileRequest>()

            val displayName = body.displayName?.trim()?.takeIf { it.isNotBlank() }
            val avatarUrl = body.avatarUrl?.trim()?.takeIf { it.isNotBlank() }
            if (displayName == null && body.avatarUrl == null) {
                call.respond(io.ktor.http.HttpStatusCode.BadRequest, mapOf("error" to "No profile fields to update"))
                return@put
            }
            if (displayName != null && displayName.length > 50) {
                call.respond(io.ktor.http.HttpStatusCode.BadRequest, mapOf("error" to "displayName too long"))
                return@put
            }
            if (body.avatarUrl != null && avatarUrl != null && avatarUrl.length > 2048) {
                call.respond(io.ktor.http.HttpStatusCode.BadRequest, mapOf("error" to "avatarUrl too long"))
                return@put
            }

            val now = System.currentTimeMillis()
            val updated = dbQuery {
                Users.update({ Users.userId eq id }) {
                    if (displayName != null) it[Users.displayName] = displayName
                    if (body.avatarUrl != null) it[Users.avatarUrl] = avatarUrl
                    it[Users.updatedAt] = now
                    it[Users.lastSyncedAt] = now
                }
            }
            if (updated == 0) {
                call.respond(io.ktor.http.HttpStatusCode.NotFound, mapOf("error" to "User not found"))
            } else {
                call.respond(mapOf("status" to "updated"))
            }
        }
    }
}
