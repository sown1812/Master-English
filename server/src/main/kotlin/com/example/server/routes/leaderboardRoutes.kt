package com.example.server.routes

import com.example.server.model.LeaderboardEntryDto
import com.example.server.tables.Users
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.leaderboardRoutes() {
    get("/leaderboard") {
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
        val data = transaction {
            Users
                .selectAll()
                .orderBy(Users.totalXp to SortOrder.DESC)
                .limit(limit, 0)
                .map {
                    LeaderboardEntryDto(
                        userId = it[Users.userId],
                        displayName = it[Users.displayName],
                        totalXp = it[Users.totalXp],
                        coins = it[Users.coins],
                        streakDays = it[Users.streakDays]
                    )
                }
        }
        call.respond(HttpStatusCode.OK, data)
    }
}
