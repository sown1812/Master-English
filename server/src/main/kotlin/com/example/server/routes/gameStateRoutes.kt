package com.example.server.routes

import com.example.server.auth.ensureUser
import com.example.server.auth.requireFirebaseUser
import com.example.server.dbQuery
import com.example.server.model.*
import com.example.server.tables.DailyChallenges
import com.example.server.tables.UserBoosters
import com.example.server.tables.UserQuests
import com.example.server.tables.Users
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import java.sql.Connection

fun Route.gameStateRoutes() {
    route("/gamestate") {
        get("/{userId}") {
            val userId = call.parameters["userId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest, mapOf("error" to "Missing userId")
            )
            if (!call.ensureUser(userId)) return@get

            val (boosters, quests, daily) = dbQuery {
                val boosters = UserBoosters.selectAll().where { UserBoosters.userId eq userId }.map {
                    BoosterStateDto(
                        boosterKey = it[UserBoosters.boosterKey],
                        isOwned = it[UserBoosters.isOwned]
                    )
                }
                val quests = UserQuests.selectAll().where { UserQuests.userId eq userId }.map {
                    QuestStateDto(
                        questKey = it[UserQuests.questKey],
                        isClaimed = it[UserQuests.isClaimed]
                    )
                }
                val daily = DailyChallenges.selectAll().where { DailyChallenges.userId eq userId }
                    .limit(1)
                    .firstOrNull()
                    ?.let {
                        DailyChallengeStateDto(
                            status = it[DailyChallenges.status],
                            progress = it[DailyChallenges.progress],
                            target = it[DailyChallenges.target]
                        )
                    }

                Triple(boosters, quests, daily)
            }
            call.respond(GameStateResponse(boosters = boosters, quests = quests, daily = daily))
        }

        post("/{userId}/booster") {
            val userId = call.parameters["userId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest, mapOf("error" to "Missing userId")
            )
            val principal = call.requireFirebaseUser() ?: return@post
            if (principal.uid != userId) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Forbidden"))
                return@post
            }
            val req = call.receive<UpdateBoosterRequest>()
            dbQuery(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE) {
                // Ensure user exists to satisfy FK
                if (Users.selectAll().where { Users.userId eq userId }.count() == 0L) {
                    Users.insertIgnore {
                        it[Users.userId] = userId
                        it[email] = principal.email ?: ""
                        it[displayName] = principal.name ?: "User"
                        it[createdAt] = System.currentTimeMillis()
                        it[updatedAt] = System.currentTimeMillis()
                        it[lastSyncedAt] = System.currentTimeMillis()
                        it[currentLevel] = 1
                        it[totalXp] = 0
                        it[coins] = 100
                        it[streakDays] = 0
                        it[lastStudyDate] = 0
                        it[longestStreak] = 0
                        it[wordsLearned] = 0
                        it[lessonsCompleted] = 0
                        it[exercisesCompleted] = 0
                        it[isPremium] = false
                    }
                }

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
            val principal = call.requireFirebaseUser() ?: return@post
            if (principal.uid != userId) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Forbidden"))
                return@post
            }
            val req = call.receive<UpdateQuestRequest>()
            dbQuery(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE) {
                // Ensure user exists
                 if (Users.selectAll().where { Users.userId eq userId }.count() == 0L) {
                    Users.insertIgnore {
                        it[Users.userId] = userId
                        it[email] = principal.email ?: ""
                        it[displayName] = principal.name ?: "User"
                        it[createdAt] = System.currentTimeMillis()
                        it[updatedAt] = System.currentTimeMillis()
                        it[lastSyncedAt] = System.currentTimeMillis()
                        it[currentLevel] = 1
                        it[totalXp] = 0
                        it[coins] = 100
                        it[streakDays] = 0
                        it[lastStudyDate] = 0
                        it[longestStreak] = 0
                        it[wordsLearned] = 0
                        it[lessonsCompleted] = 0
                        it[exercisesCompleted] = 0
                        it[isPremium] = false
                    }
                }

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
            val principal = call.requireFirebaseUser() ?: return@post
            if (principal.uid != userId) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Forbidden"))
                return@post
            }
            val req = call.receive<UpdateDailyRequest>()
            dbQuery(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE) {
                // Ensure user exists
                 if (Users.selectAll().where { Users.userId eq userId }.count() == 0L) {
                    Users.insertIgnore {
                        it[Users.userId] = userId
                        it[email] = principal.email ?: ""
                        it[displayName] = principal.name ?: "User"
                        it[createdAt] = System.currentTimeMillis()
                        it[updatedAt] = System.currentTimeMillis()
                        it[lastSyncedAt] = System.currentTimeMillis()
                        it[currentLevel] = 1
                        it[totalXp] = 0
                        it[coins] = 100
                        it[streakDays] = 0
                        it[lastStudyDate] = 0
                        it[longestStreak] = 0
                        it[wordsLearned] = 0
                        it[lessonsCompleted] = 0
                        it[exercisesCompleted] = 0
                        it[isPremium] = false
                    }
                }

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
