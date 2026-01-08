package com.example.server

import com.example.server.auth.configureFirebaseAuth
import com.example.server.routes.exerciseRoutes
import com.example.server.routes.healthRoutes
import com.example.server.routes.lessonRoutes
import com.example.server.routes.progressRoutes
import com.example.server.routes.userRoutes
import com.example.server.routes.wordRoutes
import com.example.server.routes.gameStateRoutes
import com.example.server.routes.leaderboardRoutes
import com.example.server.routes.syncRoutes
import com.typesafe.config.ConfigFactory
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerializationException

fun Application.module() {
    if (pluginOrNull(ContentNegotiation) == null) {
        install(ContentNegotiation) { json() }
    }
    if (pluginOrNull(StatusPages) == null) {
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                val status = when (cause) {
                    is BadRequestException,
                    is ContentTransformationException,
                    is SerializationException,
                    is IllegalArgumentException -> io.ktor.http.HttpStatusCode.BadRequest
                    is PayloadTooLargeException -> io.ktor.http.HttpStatusCode.PayloadTooLarge
                    is RateLimitExceededException -> io.ktor.http.HttpStatusCode.TooManyRequests
                    else -> io.ktor.http.HttpStatusCode.InternalServerError
                }
                if (status == io.ktor.http.HttpStatusCode.InternalServerError) {
                    call.application.log.error("Unhandled error", cause)
                }
                call.respond(status, mapOf("error" to (cause.message ?: "Unexpected error")))
            }
        }
    }

    if (pluginOrNull(ForwardedHeaders) == null) {
        install(ForwardedHeaders)
    }
    if (pluginOrNull(XForwardedHeaders) == null) {
        install(XForwardedHeaders)
    }

    configureRequestLimits()
    configureFirebaseAuth()

    routing {
        healthRoutes()
        lessonRoutes()
        wordRoutes()
        exerciseRoutes()
        leaderboardRoutes()
        authenticate("firebaseAuth") {
            userRoutes()
            progressRoutes()
            gameStateRoutes()
            syncRoutes()
        }
    }
}

fun main() {
    // Load DB config from env (DB_URL, DB_USER, DB_PASSWORD)
    val cfg = ConfigFactory.load()
    var dbUrl = System.getenv("DB_URL") ?: cfg.getString("database.url")
    // Fix for Render/Heroku URLs which don't have jdbc: prefix
    if (dbUrl.startsWith("postgres://")) {
        dbUrl = dbUrl.replace("postgres://", "jdbc:postgresql://")
    } else if (dbUrl.startsWith("postgresql://")) {
        dbUrl = dbUrl.replace("postgresql://", "jdbc:postgresql://")
    }
    val dbUser = System.getenv("DB_USER") ?: cfg.getString("database.user")
    val dbPwd  = System.getenv("DB_PASSWORD") ?: cfg.getString("database.password")
    // Run migrations then init pool
    Migrator.migrate(dbUrl, dbUser, dbPwd)
    DbFactory.init(dbUrl, dbUser, dbPwd)

    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        module()
    }.start(wait = false)
}
