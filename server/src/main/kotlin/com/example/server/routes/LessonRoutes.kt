package com.example.server.routes

import com.example.server.model.LessonDto
import com.example.server.tables.Lessons
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.lessonRoutes() {
    route("/lessons") {
        get {
            val items = transaction {
                Lessons.selectAll().orderBy(Lessons.order to SortOrder.ASC).map {
                    LessonDto(
                        id = it[Lessons.id],
                        title = it[Lessons.title],
                        description = it[Lessons.description],
                        order = it[Lessons.order],
                        totalWords = it[Lessons.totalWords],
                        totalExercises = it[Lessons.totalExercises],
                        difficulty = it[Lessons.difficulty],
                        category = it[Lessons.category],
                        iconUrl = it[Lessons.iconUrl],
                        xpReward = it[Lessons.xpReward],
                        coinsReward = it[Lessons.coinsReward],
                        isUnlocked = it[Lessons.isUnlocked],
                        isPremium = it[Lessons.isPremium]
                    )
                }
            }
            call.respond(items)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(mapOf("error" to "Invalid id"))
                return@get
            }
            val item = transaction {
                Lessons.select { Lessons.id eq id }.limit(1).firstOrNull()?.let {
                    LessonDto(
                        id = it[Lessons.id],
                        title = it[Lessons.title],
                        description = it[Lessons.description],
                        order = it[Lessons.order],
                        totalWords = it[Lessons.totalWords],
                        totalExercises = it[Lessons.totalExercises],
                        difficulty = it[Lessons.difficulty],
                        category = it[Lessons.category],
                        iconUrl = it[Lessons.iconUrl],
                        xpReward = it[Lessons.xpReward],
                        coinsReward = it[Lessons.coinsReward],
                        isUnlocked = it[Lessons.isUnlocked],
                        isPremium = it[Lessons.isPremium]
                    )
                }
            }
            if (item == null) {
                call.respond(mapOf("error" to "Lesson not found"))
            } else {
                call.respond(item)
            }
        }
    }
}
