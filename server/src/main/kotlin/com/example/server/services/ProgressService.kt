package com.example.server.services

import com.example.server.model.ProgressDto
import com.example.server.model.SaveProgressRequest
import com.example.server.tables.Lessons
import com.example.server.tables.UserProgress
import com.example.server.tables.Users
import com.example.server.tables.Words
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class ProgressService {
    fun saveProgress(req: SaveProgressRequest): Result<Int> = transaction {
        val errors = validate(req)
        if (errors.isNotEmpty()) return@transaction Result.failure(IllegalArgumentException(errors.joinToString("; ")))

        val now = System.currentTimeMillis()
        val id = insertProgress(req, now)
        Result.success(id)
    }

    fun getByUser(userId: String): List<ProgressDto> = transaction {
        UserProgress.selectAll().where { UserProgress.userId eq userId }
            .orderBy(UserProgress.updatedAt, SortOrder.DESC)
            .map { it.toProgressDto() }
    }

    fun getByLesson(userId: String, lessonId: Int): List<ProgressDto> = transaction {
        UserProgress.selectAll()
            .where { (UserProgress.lessonId eq lessonId) and (UserProgress.userId eq userId) }
            .orderBy(UserProgress.updatedAt, SortOrder.DESC)
            .map { it.toProgressDto() }
    }

    private fun validate(req: SaveProgressRequest): List<String> {
        val errors = mutableListOf<String>()

        val userExists = Users.selectAll().where { Users.userId eq req.userId }.limit(1).any()
        if (!userExists) errors += "User does not exist"

        val lessonExists = Lessons.selectAll().where { Lessons.id eq req.lessonId }.limit(1).any()
        if (!lessonExists) errors += "Lesson does not exist"

        if (req.wordId != null) {
            val wordRow = Words.selectAll().where { Words.id eq req.wordId }.limit(1).firstOrNull()
            if (wordRow == null) {
                errors += "Word does not exist"
            } else if (wordRow[Words.lessonId] != req.lessonId) {
                errors += "Word does not belong to the provided lesson"
            }
        }

        if (req.score < 0) errors += "Score must be >= 0"
        if (req.accuracy !in 0.0..100.0) errors += "Accuracy must be between 0 and 100"
        if (req.timeSpent < 0) errors += "Time spent must be >= 0"
        if (req.attempts < 0) errors += "Attempts must be >= 0"
        if (req.correctAnswers < 0) errors += "Correct answers must be >= 0"
        if (req.wrongAnswers < 0) errors += "Wrong answers must be >= 0"
        if (req.xpEarned < 0) errors += "XP earned must be >= 0"
        if (req.coinsEarned < 0) errors += "Coins earned must be >= 0"
        if (req.reviewCount < 0) errors += "Review count must be >= 0"
        if (req.easeFactor <= 0) errors += "Ease factor must be > 0"

        return errors
    }
}

private fun insertProgress(req: SaveProgressRequest, now: Long): Int =
    UserProgress.insert { row ->
        row[userId] = req.userId
        row[lessonId] = req.lessonId
        row[wordId] = req.wordId
        row[isCompleted] = req.isCompleted
        row[completedAt] = if (req.isCompleted) now else null
        row[score] = req.score
        row[accuracy] = req.accuracy
        row[timeSpent] = req.timeSpent
        row[attempts] = req.attempts
        row[correctAnswers] = req.correctAnswers
        row[wrongAnswers] = req.wrongAnswers
        row[xpEarned] = req.xpEarned
        row[coinsEarned] = req.coinsEarned
        row[lastReviewDate] = now
        row[nextReviewDate] = null
        row[reviewCount] = req.reviewCount
        row[easeFactor] = req.easeFactor
        row[createdAt] = now
        row[updatedAt] = now
    }[UserProgress.id]

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
