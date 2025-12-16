package com.example.server.routes

import com.example.server.auth.ensureUser
import com.example.server.dbQuery
import com.example.server.model.AchievementDto
import com.example.server.model.ProgressDto
import com.example.server.model.SyncPayload
import com.example.server.model.SyncResponse
import com.example.server.model.UserDto
import com.example.server.tables.Achievements
import com.example.server.tables.UserProgress
import com.example.server.tables.Users
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

fun Route.syncRoutes() {
    route("/sync") {
        post {
            val payload = call.receive<SyncPayload>()
            if (!call.ensureUser(payload.user.userId)) return@post

            val response = dbQuery {
                upsertUser(payload.user)
                replaceProgress(payload.user.userId, payload.progress)
                replaceAchievements(payload.user.userId, payload.achievements)

                val user = Users.selectAll()
                    .where { Users.userId eq payload.user.userId }
                    .limit(1)
                    .firstOrNull()
                    ?.toUserDto()

                val progress = UserProgress.selectAll()
                    .where { UserProgress.userId eq payload.user.userId }
                    .orderBy(UserProgress.updatedAt to SortOrder.DESC)
                    .map { it.toProgressDto() }

                val achievements = Achievements.selectAll()
                    .where { Achievements.userId eq payload.user.userId }
                    .orderBy(Achievements.id to SortOrder.ASC)
                    .map { it.toAchievementDto() }

                SyncResponse(
                    user = user,
                    progress = progress,
                    achievements = achievements
                )
            }

            call.respond(HttpStatusCode.OK, response)
        }
    }
}

private fun upsertUser(user: UserDto) {
    val now = System.currentTimeMillis()
    Users.insertIgnore {
        it[userId] = user.userId
        it[email] = user.email
        it[displayName] = user.displayName
        it[avatarUrl] = user.avatarUrl
        it[currentLevel] = user.currentLevel
        it[totalXp] = user.totalXp
        it[coins] = user.coins
        it[streakDays] = user.streakDays
        it[lastStudyDate] = user.lastStudyDate
        it[longestStreak] = user.longestStreak
        it[wordsLearned] = user.wordsLearned
        it[lessonsCompleted] = user.lessonsCompleted
        it[exercisesCompleted] = user.exercisesCompleted
        it[isPremium] = user.isPremium
        it[premiumExpiryDate] = user.premiumExpiryDate
        it[createdAt] = now
        it[updatedAt] = now
        it[lastSyncedAt] = now
    }

    Users.update({ Users.userId eq user.userId }) {
        it[email] = user.email
        it[displayName] = user.displayName
        it[avatarUrl] = user.avatarUrl
        it[currentLevel] = user.currentLevel
        it[totalXp] = user.totalXp
        it[coins] = user.coins
        it[streakDays] = user.streakDays
        it[lastStudyDate] = user.lastStudyDate
        it[longestStreak] = user.longestStreak
        it[wordsLearned] = user.wordsLearned
        it[lessonsCompleted] = user.lessonsCompleted
        it[exercisesCompleted] = user.exercisesCompleted
        it[isPremium] = user.isPremium
        it[premiumExpiryDate] = user.premiumExpiryDate
        it[updatedAt] = now
        it[lastSyncedAt] = now
    }
}

private fun replaceProgress(userId: String, progress: List<ProgressDto>) {
    UserProgress.deleteWhere { UserProgress.userId eq userId }
    UserProgress.batchInsert(progress) { item ->
        this[UserProgress.userId] = item.userId
        this[UserProgress.lessonId] = item.lessonId
        this[UserProgress.wordId] = item.wordId
        this[UserProgress.isCompleted] = item.isCompleted
        this[UserProgress.completedAt] = item.completedAt
        this[UserProgress.score] = item.score
        this[UserProgress.accuracy] = item.accuracy
        this[UserProgress.timeSpent] = item.timeSpent
        this[UserProgress.attempts] = item.attempts
        this[UserProgress.correctAnswers] = item.correctAnswers
        this[UserProgress.wrongAnswers] = item.wrongAnswers
        this[UserProgress.xpEarned] = item.xpEarned
        this[UserProgress.coinsEarned] = item.coinsEarned
        this[UserProgress.lastReviewDate] = item.lastReviewDate
        this[UserProgress.nextReviewDate] = item.nextReviewDate
        this[UserProgress.reviewCount] = item.reviewCount
        this[UserProgress.easeFactor] = item.easeFactor
        this[UserProgress.createdAt] = item.createdAt
        this[UserProgress.updatedAt] = item.updatedAt
    }
}

private fun replaceAchievements(userId: String, achievements: List<AchievementDto>) {
    Achievements.deleteWhere { Achievements.userId eq userId }
    Achievements.batchInsert(achievements) { item ->
        this[Achievements.userId] = item.userId
        this[Achievements.achievementType] = item.achievementType
        this[Achievements.title] = item.title
        this[Achievements.description] = item.description
        this[Achievements.isUnlocked] = item.isUnlocked
        this[Achievements.unlockedAt] = item.unlockedAt
        this[Achievements.progress] = item.progress
        this[Achievements.target] = item.target
        this[Achievements.xpReward] = item.xpReward
        this[Achievements.coinsReward] = item.coinsReward
        this[Achievements.badgeUrl] = item.badgeUrl
        this[Achievements.createdAt] = item.createdAt
    }
}

private fun ResultRow.toUserDto() = UserDto(
    userId = this[Users.userId],
    email = this[Users.email],
    displayName = this[Users.displayName],
    avatarUrl = this[Users.avatarUrl],
    currentLevel = this[Users.currentLevel],
    totalXp = this[Users.totalXp],
    coins = this[Users.coins],
    streakDays = this[Users.streakDays],
    lastStudyDate = this[Users.lastStudyDate],
    longestStreak = this[Users.longestStreak],
    wordsLearned = this[Users.wordsLearned],
    lessonsCompleted = this[Users.lessonsCompleted],
    exercisesCompleted = this[Users.exercisesCompleted],
    isPremium = this[Users.isPremium],
    premiumExpiryDate = this[Users.premiumExpiryDate]
)

private fun ResultRow.toProgressDto() = ProgressDto(
    id = this[UserProgress.id],
    userId = this[UserProgress.userId],
    lessonId = this[UserProgress.lessonId],
    wordId = this[UserProgress.wordId],
    isCompleted = this[UserProgress.isCompleted],
    completedAt = this[UserProgress.completedAt],
    score = this[UserProgress.score],
    accuracy = this[UserProgress.accuracy],
    timeSpent = this[UserProgress.timeSpent],
    attempts = this[UserProgress.attempts],
    correctAnswers = this[UserProgress.correctAnswers],
    wrongAnswers = this[UserProgress.wrongAnswers],
    xpEarned = this[UserProgress.xpEarned],
    coinsEarned = this[UserProgress.coinsEarned],
    lastReviewDate = this[UserProgress.lastReviewDate],
    nextReviewDate = this[UserProgress.nextReviewDate],
    reviewCount = this[UserProgress.reviewCount],
    easeFactor = this[UserProgress.easeFactor],
    createdAt = this[UserProgress.createdAt],
    updatedAt = this[UserProgress.updatedAt]
)

private fun ResultRow.toAchievementDto() = AchievementDto(
    id = this[Achievements.id],
    userId = this[Achievements.userId],
    achievementType = this[Achievements.achievementType],
    title = this[Achievements.title],
    description = this[Achievements.description],
    isUnlocked = this[Achievements.isUnlocked],
    unlockedAt = this[Achievements.unlockedAt],
    progress = this[Achievements.progress],
    target = this[Achievements.target],
    xpReward = this[Achievements.xpReward],
    coinsReward = this[Achievements.coinsReward],
    badgeUrl = this[Achievements.badgeUrl],
    createdAt = this[Achievements.createdAt]
)
