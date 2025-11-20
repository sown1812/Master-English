package com.example.server

import com.example.server.routes.exerciseRoutes
import com.example.server.routes.lessonRoutes
import com.example.server.routes.progressRoutes
import com.example.server.routes.userRoutes
import com.example.server.routes.wordRoutes
import com.example.server.routes.gameStateRoutes
import com.example.server.routes.leaderboardRoutes
import com.typesafe.config.ConfigFactory
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.module() {
    if (pluginOrNull(ContentNegotiation) == null) {
        install(ContentNegotiation) { json() }
    }
    if (pluginOrNull(StatusPages) == null) {
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                val status = when (cause) {
                    is IllegalArgumentException -> io.ktor.http.HttpStatusCode.BadRequest
                    else -> io.ktor.http.HttpStatusCode.InternalServerError
                }
                call.respond(status, mapOf("error" to (cause.message ?: "Unexpected error")))
            }
        }
    }

    routing {
        get("/health") { call.respond(mapOf("status" to "ok")) }
        lessonRoutes()
        wordRoutes()
        exerciseRoutes()
        userRoutes()
        progressRoutes()
        gameStateRoutes()
        leaderboardRoutes()
    }
}

fun main() {
    // Load DB config from env (DB_URL, DB_USER, DB_PASSWORD)
    val cfg = ConfigFactory.load()
    val dbUrl = System.getenv("DB_URL") ?: cfg.getString("database.url")
    val dbUser = System.getenv("DB_USER") ?: cfg.getString("database.user")
    val dbPwd  = System.getenv("DB_PASSWORD") ?: cfg.getString("database.password")
    // Run migrations then init pool
    Migrator.migrate(dbUrl, dbUser, dbPwd)
    DbFactory.init(dbUrl, dbUser, dbPwd)

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = false)
}
