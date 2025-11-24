package com.example.server.routes

import com.example.server.auth.ensureUser
import com.example.server.model.UserDto
import com.example.server.tables.Users
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.userRoutes() {
    route("/users") {
        get("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid user id")
            if (!call.ensureUser(id)) return@get
            val user = transaction {
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
            val body = call.receive<UserDto>()
            transaction {
                Users.update({ Users.userId eq id }) {
                    it[email] = body.email
                    it[displayName] = body.displayName
                    it[avatarUrl] = body.avatarUrl
                    it[currentLevel] = body.currentLevel
                    it[totalXp] = body.totalXp
                    it[coins] = body.coins
                    it[streakDays] = body.streakDays
                    it[lastStudyDate] = body.lastStudyDate
                    it[longestStreak] = body.longestStreak
                    it[wordsLearned] = body.wordsLearned
                    it[lessonsCompleted] = body.lessonsCompleted
                    it[exercisesCompleted] = body.exercisesCompleted
                    it[isPremium] = body.isPremium
                    it[premiumExpiryDate] = body.premiumExpiryDate
                    it[updatedAt] = System.currentTimeMillis()
                }
            }
            call.respond(mapOf("status" to "updated"))
        }
    }
}
