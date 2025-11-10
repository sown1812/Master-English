package com.example.server.routes

import com.example.server.model.ExerciseDto
import com.example.server.tables.Exercises
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.exerciseRoutes() {
    route("/exercises/{id}") {
        get {
            val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid id")
            val exercise = transaction {
                Exercises.selectAll().where { Exercises.id eq id }.limit(1).firstOrNull()?.let {
                    ExerciseDto(
                        id = it[Exercises.id],
                        lessonId = it[Exercises.lessonId],
                        wordId = it[Exercises.wordId],
                        type = it[Exercises.type],
                        question = it[Exercises.question],
                        correctAnswer = it[Exercises.correctAnswer],
                        optionA = it[Exercises.optionA],
                        optionB = it[Exercises.optionB],
                        optionC = it[Exercises.optionC],
                        optionD = it[Exercises.optionD],
                        matchPairs = it[Exercises.matchPairs],
                        hint = it[Exercises.hint],
                        explanation = it[Exercises.explanation],
                        order = it[Exercises.order],
                        difficulty = it[Exercises.difficulty]
                    )
                }
            }
            if (exercise == null) {
                call.respond(io.ktor.http.HttpStatusCode.NotFound, mapOf("error" to "Exercise not found"))
            } else {
                call.respond(exercise)
            }
        }
    }

    route("/lessons/{id}/exercises") {
        get {
            val lessonId = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid lesson id")
            val exercises = transaction {
                Exercises.selectAll().where { Exercises.lessonId eq lessonId }
                    .orderBy(Exercises.order to SortOrder.ASC)
                    .map {
                        ExerciseDto(
                            id = it[Exercises.id],
                            lessonId = it[Exercises.lessonId],
                            wordId = it[Exercises.wordId],
                            type = it[Exercises.type],
                            question = it[Exercises.question],
                            correctAnswer = it[Exercises.correctAnswer],
                            optionA = it[Exercises.optionA],
                            optionB = it[Exercises.optionB],
                            optionC = it[Exercises.optionC],
                            optionD = it[Exercises.optionD],
                            matchPairs = it[Exercises.matchPairs],
                            hint = it[Exercises.hint],
                            explanation = it[Exercises.explanation],
                            order = it[Exercises.order],
                            difficulty = it[Exercises.difficulty]
                        )
                    }
            }
            call.respond(exercises)
        }
    }
}
