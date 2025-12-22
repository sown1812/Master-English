package com.example.server.routes

import com.example.server.auth.ensureUser
import com.example.server.auth.requireFirebaseUser
import com.example.server.dbQuery
import com.example.server.model.LessonCompletedEvent
import com.example.server.model.ProgressDto
import com.example.server.model.SyncEventResult
import com.example.server.model.SyncEventsPayload
import com.example.server.model.SyncResponse
import com.example.server.model.UserDto
import com.example.server.tables.Lessons
import com.example.server.tables.SyncEvents
import com.example.server.tables.UserProgress
import com.example.server.tables.Users
import com.example.server.services.AchievementsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.sql.Connection
import kotlin.math.roundToInt

private const val PASS_ACCURACY_THRESHOLD = 0.7f
private const val FAIL_XP_REWARD = 10
private const val FAIL_COIN_REWARD = 4
private const val XP_LEVEL_DIVISOR = 100
private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L

fun Route.syncRoutes() {
    route("/sync") {
        post {
            val principal = call.requireFirebaseUser() ?: return@post
            val payload = call.receive<SyncEventsPayload>()
            if (!call.ensureUser(payload.userId)) return@post

            val response = dbQuery(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE) {
                ensureUserRow(
                    userId = payload.userId,
                    email = principal.email,
                    displayName = principal.name
                )

                val now = System.currentTimeMillis()
                val results = payload.lessonCompletions.map { event ->
                    applyLessonCompleted(
                        userId = payload.userId,
                        event = event,
                        now = now
                    )
                }

                val achievements = AchievementsService.recomputeAndApply(payload.userId, now)

                val user = Users.selectAll()
                    .where { Users.userId eq payload.userId }
                    .limit(1)
                    .firstOrNull()
                    ?.toUserDto()

                val progress = UserProgress.selectAll()
                    .where { UserProgress.userId eq payload.userId }
                    .orderBy(UserProgress.updatedAt to SortOrder.DESC)
                    .map { it.toProgressDto() }

                SyncResponse(
                    user = user,
                    progress = progress,
                    achievements = achievements,
                    results = results
                )
            }

            call.respond(HttpStatusCode.OK, response)
        }
    }
}

private fun ensureUserRow(userId: String, email: String?, displayName: String?) {
    val existing = Users.selectAll().where { Users.userId eq userId }.limit(1).firstOrNull()
    if (existing != null) return

    val now = System.currentTimeMillis()
    Users.insert {
        it[Users.userId] = userId
        it[Users.email] = email ?: ""
        it[Users.displayName] = displayName ?: email ?: "User"
        it[Users.avatarUrl] = null
        it[Users.currentLevel] = 1
        it[Users.totalXp] = 0
        it[Users.coins] = 100
        it[Users.streakDays] = 0
        it[Users.lastStudyDate] = 0
        it[Users.longestStreak] = 0
        it[Users.wordsLearned] = 0
        it[Users.lessonsCompleted] = 0
        it[Users.exercisesCompleted] = 0
        it[Users.isPremium] = false
        it[Users.premiumExpiryDate] = null
        it[Users.createdAt] = now
        it[Users.updatedAt] = now
        it[Users.lastSyncedAt] = now
    }
}

private fun applyLessonCompleted(userId: String, event: LessonCompletedEvent, now: Long): SyncEventResult {
    if (event.eventId.isBlank()) {
        return SyncEventResult(eventId = "", status = "rejected", error = "Missing eventId")
    }
    if (event.lessonId <= 0) {
        return SyncEventResult(eventId = event.eventId, status = "rejected", error = "Invalid lessonId")
    }
    if (event.score < 0) {
        return SyncEventResult(eventId = event.eventId, status = "rejected", error = "Score must be >= 0")
    }
    if (event.correctAnswers < 0 || event.wrongAnswers < 0) {
        return SyncEventResult(eventId = event.eventId, status = "rejected", error = "Answer counts must be >= 0")
    }

    val lessonExists = Lessons.selectAll().where { Lessons.id eq event.lessonId }.limit(1).any()
    if (!lessonExists) {
        return SyncEventResult(eventId = event.eventId, status = "rejected", error = "Lesson not found")
    }

    val inserted = SyncEvents.insertIgnore {
        it[SyncEvents.userId] = userId
        it[eventId] = event.eventId
        it[eventType] = "lesson_completed"
        it[occurredAt] = event.occurredAt
        it[processedAt] = now
    }
    if (inserted.insertedCount == 0) {
        return SyncEventResult(eventId = event.eventId, status = "duplicate")
    }

    val computed = computeLessonCompletion(event)

    val wasCompleted = UserProgress
        .selectAll()
        .where {
            (UserProgress.userId eq userId) and
                (UserProgress.lessonId eq event.lessonId) and
                UserProgress.wordId.isNull() and
                (UserProgress.isCompleted eq true)
        }
        .limit(1)
        .any()

    upsertLessonProgress(
        userId = userId,
        event = event,
        computed = computed,
        now = now
    )

    applyUserRewards(
        userId = userId,
        xp = computed.xp,
        coins = computed.coins,
        incrementLessonsCompleted = computed.isPassed && !wasCompleted,
        now = now
    )

    return SyncEventResult(eventId = event.eventId, status = "applied")
}

private data class ComputedCompletion(
    val isPassed: Boolean,
    val accuracyFraction: Float,
    val accuracyPercent: Double,
    val attempts: Int,
    val xp: Int,
    val coins: Int
)

private fun computeLessonCompletion(event: LessonCompletedEvent): ComputedCompletion {
    val attempts = (event.correctAnswers + event.wrongAnswers).coerceAtLeast(1)
    val accuracyFraction = (event.correctAnswers.toFloat() / attempts.toFloat()).coerceIn(0f, 1f)
    val isPassed = accuracyFraction >= PASS_ACCURACY_THRESHOLD

    val (xp, coins) = if (isPassed) {
        val baseXp = 40
        val accuracyBonus = (accuracyFraction * 60f).roundToInt()
        val performanceBonus = (event.score * 0.4f).roundToInt()
        val xp = (baseXp + accuracyBonus + performanceBonus).coerceAtLeast(10)
        val coins = when {
            accuracyFraction >= 0.9f -> 25
            accuracyFraction >= 0.75f -> 18
            accuracyFraction >= 0.6f -> 12
            else -> 6
        }
        xp to coins
    } else {
        FAIL_XP_REWARD to FAIL_COIN_REWARD
    }

    return ComputedCompletion(
        isPassed = isPassed,
        accuracyFraction = accuracyFraction,
        accuracyPercent = (accuracyFraction * 100f).toDouble().coerceIn(0.0, 100.0),
        attempts = attempts,
        xp = xp,
        coins = coins
    )
}

private fun upsertLessonProgress(
    userId: String,
    event: LessonCompletedEvent,
    computed: ComputedCompletion,
    now: Long
) {
    val existing = UserProgress
        .selectAll()
        .where {
            (UserProgress.userId eq userId) and
                (UserProgress.lessonId eq event.lessonId) and
                UserProgress.wordId.isNull()
        }
        .limit(1)
        .firstOrNull()

    val completedAt = if (computed.isPassed) now else null

    if (existing == null) {
        UserProgress.insert { row ->
            row[UserProgress.userId] = userId
            row[UserProgress.lessonId] = event.lessonId
            row[UserProgress.wordId] = null
            row[isCompleted] = computed.isPassed
            row[UserProgress.completedAt] = completedAt
            row[score] = event.score
            row[accuracy] = computed.accuracyPercent
            row[timeSpent] = event.timeSpent
            row[attempts] = computed.attempts
            row[correctAnswers] = event.correctAnswers
            row[wrongAnswers] = event.wrongAnswers
            row[xpEarned] = computed.xp
            row[coinsEarned] = computed.coins
            row[lastReviewDate] = null
            row[nextReviewDate] = null
            row[reviewCount] = 0
            row[easeFactor] = 2.5
            row[createdAt] = now
            row[updatedAt] = now
        }
    } else {
        val id = existing[UserProgress.id]
        UserProgress.update({ UserProgress.id eq id }) { row ->
            row[isCompleted] = computed.isPassed
            row[UserProgress.completedAt] = completedAt
            row[score] = event.score
            row[accuracy] = computed.accuracyPercent
            row[timeSpent] = event.timeSpent
            row[attempts] = computed.attempts
            row[correctAnswers] = event.correctAnswers
            row[wrongAnswers] = event.wrongAnswers
            row[xpEarned] = computed.xp
            row[coinsEarned] = computed.coins
            row[updatedAt] = now
        }
    }
}

private fun applyUserRewards(
    userId: String,
    xp: Int,
    coins: Int,
    incrementLessonsCompleted: Boolean,
    now: Long
) {
    val current = Users
        .selectAll()
        .where { Users.userId eq userId }
        .limit(1)
        .firstOrNull()
        ?: return

    val newTotalXp = current[Users.totalXp] + xp
    val newCoins = current[Users.coins] + coins
    val newLevel = (newTotalXp / XP_LEVEL_DIVISOR) + 1

    val currentStreak = current[Users.streakDays]
    val lastStudyDate = current[Users.lastStudyDate]
    val longestStreak = current[Users.longestStreak]

    val daysDifference = ((now - lastStudyDate) / ONE_DAY_MS).toInt()
    val newStreakDays = when {
        daysDifference == 0 -> currentStreak
        daysDifference == 1 -> currentStreak + 1
        else -> 1
    }
    val newLongestStreak = maxOf(longestStreak, newStreakDays)

    Users.update({ Users.userId eq userId }) { row ->
        row[Users.totalXp] = newTotalXp
        row[Users.coins] = newCoins
        row[Users.currentLevel] = newLevel
        row[Users.streakDays] = newStreakDays
        row[Users.lastStudyDate] = now
        row[Users.longestStreak] = newLongestStreak
        if (incrementLessonsCompleted) {
            row[Users.lessonsCompleted] = current[Users.lessonsCompleted] + 1
        }
        row[Users.updatedAt] = now
        row[Users.lastSyncedAt] = now
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
