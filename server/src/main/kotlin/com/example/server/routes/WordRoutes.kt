package com.example.server.routes

import com.example.server.model.WordDto
import com.example.server.tables.Words
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.wordRoutes() {
    route("/words") {
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid id")
            val word = transaction {
                Words.selectAll().where { Words.id eq id }.limit(1).firstOrNull()?.let {
                    WordDto(
                        id = it[Words.id],
                        word = it[Words.word],
                        translation = it[Words.translation],
                        pronunciation = it[Words.pronunciation],
                        partOfSpeech = it[Words.partOfSpeech],
                        exampleSentence = it[Words.exampleSentence],
                        exampleTranslation = it[Words.exampleTranslation],
                        imageUrl = it[Words.imageUrl],
                        audioUrl = it[Words.audioUrl],
                        lessonId = it[Words.lessonId],
                        difficulty = it[Words.difficulty],
                        category = it[Words.category]
                    )
                }
            }
            if (word == null) {
                call.respond(io.ktor.http.HttpStatusCode.NotFound, mapOf("error" to "Word not found"))
            } else {
                call.respond(word)
            }
        }
    }

    route("/lessons/{id}/words") {
        get {
            val lessonId = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid lesson id")
            val words = transaction {
                Words.selectAll().where { Words.lessonId eq lessonId }
                    .orderBy(Words.id to SortOrder.ASC)
                    .map {
                        WordDto(
                            id = it[Words.id],
                            word = it[Words.word],
                            translation = it[Words.translation],
                            pronunciation = it[Words.pronunciation],
                            partOfSpeech = it[Words.partOfSpeech],
                            exampleSentence = it[Words.exampleSentence],
                            exampleTranslation = it[Words.exampleTranslation],
                            imageUrl = it[Words.imageUrl],
                            audioUrl = it[Words.audioUrl],
                            lessonId = it[Words.lessonId],
                            difficulty = it[Words.difficulty],
                            category = it[Words.category]
                        )
                    }
            }
            call.respond(words)
        }
    }
}
