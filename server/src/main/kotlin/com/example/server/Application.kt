package com.example.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import com.typesafe.config.ConfigFactory
import com.example.server.routes.lessonRoutes

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) { json() }

        // Load DB config from env (DB_URL, DB_USER, DB_PASSWORD)
        val cfg = ConfigFactory.load()
        val dbUrl = System.getenv("DB_URL") ?: cfg.getString("database.url")
        val dbUser = System.getenv("DB_USER") ?: cfg.getString("database.user")
        val dbPwd  = System.getenv("DB_PASSWORD") ?: cfg.getString("database.password")
        // Run migrations then init pool
        Migrator.migrate(dbUrl, dbUser, dbPwd)
        DbFactory.init(dbUrl, dbUser, dbPwd)

        routing {
            get("/health") { call.respond(mapOf("status" to "ok")) }
            lessonRoutes()
        }
    }.start(wait = false)
}
