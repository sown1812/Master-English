package com.example.server.routes

import com.example.server.model.*
import com.example.server.tables.DailyChallenges
import com.example.server.tables.UserBoosters
import com.example.server.tables.UserQuests
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.gameStateRoutes() {
    route("/gamestate") {
        get("/{userId}") {
            val userId = call.parameters["userId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest, mapOf("error" to "Missing userId")
            )
            if (!call.isAuthorized(userId)) return@get
            val boosters = transaction {
                UserBoosters.selectAll().where { UserBoosters.userId eq userId }.map {
                    BoosterStateDto(
                        boosterKey = it[UserBoosters.boosterKey],
                        isOwned = it[UserBoosters.isOwned]
                    )
                }
            }
            val quests = transaction {
                UserQuests.selectAll().where { UserQuests.userId eq userId }.map {
                    QuestStateDto(
                        questKey = it[UserQuests.questKey],
                        isClaimed = it[UserQuests.isClaimed]
                    )
                }
            }
            val daily = transaction {
                DailyChallenges.selectAll().where { DailyChallenges.userId eq userId }
                    .limit(1)
                    .firstOrNull()
                    ?.let {
                        DailyChallengeStateDto(
                            status = it[DailyChallenges.status],
                            progress = it[DailyChallenges.progress],
                            target = it[DailyChallenges.target]
                        )
                    }
            }
            call.respond(GameStateResponse(boosters = boosters, quests = quests, daily = daily))
        }

        post("/{userId}/booster") {
            val userId = call.parameters["userId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest, mapOf("error" to "Missing userId")
            )
            if (!call.isAuthorized(userId)) return@post
            val req = call.receive<UpdateBoosterRequest>()
            transaction {
                UserBoosters.insertIgnore {
                    it[UserBoosters.userId] = userId
                    it[UserBoosters.boosterKey] = req.boosterKey
                    it[isOwned] = req.owned
                    it[updatedAt] = System.currentTimeMillis()
                }
                UserBoosters.update({ (UserBoosters.userId eq userId) and (UserBoosters.boosterKey eq req.boosterKey) }) {
                    it[isOwned] = req.owned
                    it[updatedAt] = System.currentTimeMillis()
                }
            }
            call.respond(mapOf("status" to "ok"))
        }

        post("/{userId}/quest") {
            val userId = call.parameters["userId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest, mapOf("error" to "Missing userId")
            )
            if (!call.isAuthorized(userId)) return@post
            val req = call.receive<UpdateQuestRequest>()
            transaction {
                UserQuests.insertIgnore {
                    it[UserQuests.userId] = userId
                    it[questKey] = req.questKey
                    it[isClaimed] = req.claimed
                    it[updatedAt] = System.currentTimeMillis()
                }
                UserQuests.update({ (UserQuests.userId eq userId) and (UserQuests.questKey eq req.questKey) }) {
                    it[isClaimed] = req.claimed
                    it[updatedAt] = System.currentTimeMillis()
                }
            }
            call.respond(mapOf("status" to "ok"))
        }

        post("/{userId}/daily") {
            val userId = call.parameters["userId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest, mapOf("error" to "Missing userId")
            )
            if (!call.isAuthorized(userId)) return@post
            val req = call.receive<UpdateDailyRequest>()
            transaction {
                DailyChallenges.insertIgnore {
                    it[DailyChallenges.userId] = userId
                    it[status] = req.status
                    it[progress] = req.progress
                    it[target] = req.target
                    it[updatedAt] = System.currentTimeMillis()
                }
                DailyChallenges.update({ DailyChallenges.userId eq userId }) {
                    it[status] = req.status
                    it[progress] = req.progress
                    it[target] = req.target
                    it[updatedAt] = System.currentTimeMillis()
                }
            }
            call.respond(mapOf("status" to "ok"))
        }
    }
}

private suspend fun ApplicationCall.isAuthorized(userId: String): Boolean {
    val token = request.headers["Authorization"]?.removePrefix("Bearer ")?.trim()
    return if (token == userId) {
        true
    } else {
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
        false
    }
}
