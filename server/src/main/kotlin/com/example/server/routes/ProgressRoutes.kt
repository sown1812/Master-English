package com.example.server.routes

import com.example.server.model.ProgressDto
import com.example.server.model.SaveProgressRequest
import com.example.server.tables.UserProgress
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.progressRoutes() {
    route("/progress") {
        post {
            val req = call.receive<SaveProgressRequest>()
            val now = System.currentTimeMillis()
            val id = transaction {
                UserProgress.insert { row ->
                    row[UserProgress.userId] = req.userId
                    row[UserProgress.lessonId] = req.lessonId
                    row[UserProgress.wordId] = req.wordId
                    row[UserProgress.isCompleted] = req.isCompleted
                    row[UserProgress.completedAt] = if (req.isCompleted) now else null
                    row[UserProgress.score] = req.score
                    row[UserProgress.accuracy] = req.accuracy
                    row[UserProgress.timeSpent] = req.timeSpent
                    row[UserProgress.attempts] = req.attempts
                    row[UserProgress.correctAnswers] = req.correctAnswers
                    row[UserProgress.wrongAnswers] = req.wrongAnswers
                    row[UserProgress.xpEarned] = req.xpEarned
                    row[UserProgress.coinsEarned] = req.coinsEarned
                    row[UserProgress.lastReviewDate] = now
                    row[UserProgress.nextReviewDate] = null
                    row[UserProgress.reviewCount] = req.reviewCount
                    row[UserProgress.easeFactor] = req.easeFactor
                    row[UserProgress.createdAt] = now
                    row[UserProgress.updatedAt] = now
                }[UserProgress.id]
            }
            call.respond(mapOf("id" to id))
        }

        get("/user/{userId}") {
            val userIdParam = call.parameters["userId"] ?: throw IllegalArgumentException("Invalid user id")
            val items = transaction {
                UserProgress.selectAll().where { UserProgress.userId eq userIdParam }
                    .orderBy(UserProgress.updatedAt, SortOrder.DESC)
                    .map {
                        ProgressDto(
                            id = it[UserProgress.id],
                            userId = it[UserProgress.userId],
                            lessonId = it[UserProgress.lessonId],
                            wordId = it[UserProgress.wordId],
                            isCompleted = it[UserProgress.isCompleted],
                            completedAt = it[UserProgress.completedAt],
                            score = it[UserProgress.score],
                            accuracy = it[UserProgress.accuracy],
                            timeSpent = it[UserProgress.timeSpent],
                            attempts = it[UserProgress.attempts],
                            correctAnswers = it[UserProgress.correctAnswers],
                            wrongAnswers = it[UserProgress.wrongAnswers],
                            xpEarned = it[UserProgress.xpEarned],
                            coinsEarned = it[UserProgress.coinsEarned],
                            lastReviewDate = it[UserProgress.lastReviewDate],
                            nextReviewDate = it[UserProgress.nextReviewDate],
                            reviewCount = it[UserProgress.reviewCount],
                            easeFactor = it[UserProgress.easeFactor],
                            createdAt = it[UserProgress.createdAt],
                            updatedAt = it[UserProgress.updatedAt]
                        )
                    }
            }
            call.respond(items)
        }

        get("/lesson/{lessonId}") {
            val lessonId = call.parameters["lessonId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid lesson id")
            val items = transaction {
                UserProgress.selectAll().where { UserProgress.lessonId eq lessonId }
                    .orderBy(UserProgress.updatedAt, SortOrder.DESC)
                    .map {
                        ProgressDto(
                            id = it[UserProgress.id],
                            userId = it[UserProgress.userId],
                            lessonId = it[UserProgress.lessonId],
                            wordId = it[UserProgress.wordId],
                            isCompleted = it[UserProgress.isCompleted],
                            completedAt = it[UserProgress.completedAt],
                            score = it[UserProgress.score],
                            accuracy = it[UserProgress.accuracy],
                            timeSpent = it[UserProgress.timeSpent],
                            attempts = it[UserProgress.attempts],
                            correctAnswers = it[UserProgress.correctAnswers],
                            wrongAnswers = it[UserProgress.wrongAnswers],
                            xpEarned = it[UserProgress.xpEarned],
                            coinsEarned = it[UserProgress.coinsEarned],
                            lastReviewDate = it[UserProgress.lastReviewDate],
                            nextReviewDate = it[UserProgress.nextReviewDate],
                            reviewCount = it[UserProgress.reviewCount],
                            easeFactor = it[UserProgress.easeFactor],
                            createdAt = it[UserProgress.createdAt],
                            updatedAt = it[UserProgress.updatedAt]
                        )
                    }
            }
            call.respond(items)
        }
    }
}
