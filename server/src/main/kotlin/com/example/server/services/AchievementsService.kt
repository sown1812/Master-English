package com.example.server.services

import com.example.server.model.AchievementDto
import com.example.server.tables.Achievements
import com.example.server.tables.Exercises
import com.example.server.tables.Lessons
import com.example.server.tables.UserProgress
import com.example.server.tables.Users
import com.example.server.tables.Words
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.Calendar

object AchievementsService {

    private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
    private const val XP_LEVEL_DIVISOR = 100

    private data class AchievementDefinition(
        val achievementType: String,
        val title: String,
        val description: String,
        val target: Int,
        val xpReward: Int,
        val coinsReward: Int,
        val badgeUrl: String? = null
    )

    fun ensureUserAchievements(userId: String, now: Long) {
        val totalLessons = Lessons.selectAll().count().toInt().coerceAtLeast(1)
        val definitions = achievementDefinitions(totalLessons)

        val existingTypes = Achievements
            .selectAll()
            .where { Achievements.userId eq userId }
            .map { it[Achievements.achievementType] }
            .toSet()

        for (definition in definitions) {
            if (definition.achievementType in existingTypes) continue
            Achievements.insertIgnore { row ->
                row[Achievements.userId] = userId
                row[achievementType] = definition.achievementType
                row[title] = definition.title
                row[description] = definition.description
                row[isUnlocked] = false
                row[unlockedAt] = null
                row[progress] = 0
                row[target] = definition.target
                row[xpReward] = definition.xpReward
                row[coinsReward] = definition.coinsReward
                row[badgeUrl] = definition.badgeUrl
                row[createdAt] = now
            }
        }

        val questMasterTarget = totalLessons
        Achievements.update(
            where = { (Achievements.userId eq userId) and (Achievements.achievementType eq "QUEST_MASTER") }
        ) { row ->
            row[target] = questMasterTarget
        }
    }

    fun listUserAchievements(userId: String): List<AchievementDto> =
        Achievements
            .selectAll()
            .where { Achievements.userId eq userId }
            .orderBy(Achievements.id to SortOrder.ASC)
            .map { it.toAchievementDto() }

    fun recomputeAndApply(userId: String, now: Long): List<AchievementDto> {
        ensureUserAchievements(userId, now)

        val userRow = Users
            .selectAll()
            .where { Users.userId eq userId }
            .limit(1)
            .firstOrNull()
            ?: return listUserAchievements(userId)

        val totalLessons = Lessons.selectAll().count().toInt().coerceAtLeast(1)
        val snapshot = buildSnapshot(userId, now)

        syncUserProgressCounters(
            userId = userId,
            current = userRow,
            snapshot = snapshot,
            now = now
        )

        val lastStudyHour = hourOfDay(userRow[Users.lastStudyDate])
        val hasCareerLesson = snapshot.categories.any { category ->
            category.contains("work", ignoreCase = true) ||
                category.contains("career", ignoreCase = true) ||
                category.contains("technology", ignoreCase = true)
        }

        val achievementRows = Achievements
            .selectAll()
            .where { Achievements.userId eq userId }
            .toList()

        var xpDelta = 0
        var coinsDelta = 0

        for (row in achievementRows) {
            val id = row[Achievements.id]
            val type = row[Achievements.achievementType]
            val isUnlocked = row[Achievements.isUnlocked]
            val currentTarget = row[Achievements.target]
            val effectiveTarget = when (type) {
                "QUEST_MASTER" -> totalLessons
                else -> currentTarget
            }.coerceAtLeast(1)

            val computedProgress = computeProgress(
                achievementType = type,
                snapshot = snapshot,
                streakDays = userRow[Users.streakDays],
                lastStudyHour = lastStudyHour,
                totalLessons = totalLessons,
                hasCareerLesson = hasCareerLesson
            ).coerceAtLeast(0)

            val cappedProgress = computedProgress.coerceAtMost(effectiveTarget)

            if (!isUnlocked && computedProgress >= effectiveTarget) {
                val updated = Achievements.update({ (Achievements.id eq id) and (Achievements.isUnlocked eq false) }) { update ->
                    update[Achievements.target] = effectiveTarget
                    update[Achievements.progress] = effectiveTarget
                    update[Achievements.isUnlocked] = true
                    update[Achievements.unlockedAt] = now
                }
                if (updated > 0) {
                    xpDelta += row[Achievements.xpReward]
                    coinsDelta += row[Achievements.coinsReward]
                }
            } else if (!isUnlocked) {
                if (cappedProgress != row[Achievements.progress] || effectiveTarget != currentTarget) {
                    Achievements.update({ Achievements.id eq id }) { update ->
                        update[Achievements.target] = effectiveTarget
                        update[Achievements.progress] = cappedProgress
                    }
                }
            } else {
                if (effectiveTarget != currentTarget) {
                    Achievements.update({ Achievements.id eq id }) { update ->
                        update[Achievements.target] = effectiveTarget
                    }
                }
            }
        }

        if (xpDelta != 0 || coinsDelta != 0) {
            val newTotalXp = userRow[Users.totalXp] + xpDelta
            val newCoins = userRow[Users.coins] + coinsDelta
            val newLevel = (newTotalXp / XP_LEVEL_DIVISOR) + 1
            Users.update({ Users.userId eq userId }) { update ->
                update[Users.totalXp] = newTotalXp
                update[Users.coins] = newCoins
                update[Users.currentLevel] = newLevel
                update[Users.updatedAt] = now
                update[Users.lastSyncedAt] = now
            }
        }

        return listUserAchievements(userId)
    }

    private fun achievementDefinitions(totalLessons: Int): List<AchievementDefinition> =
        listOf(
            AchievementDefinition(
                achievementType = "FIRST_LESSON",
                title = "First Steps",
                description = "Complete your first lesson",
                target = 1,
                xpReward = 50,
                coinsReward = 20
            ),
            AchievementDefinition(
                achievementType = "LESSONS_5",
                title = "Quick Learner",
                description = "Complete 5 lessons",
                target = 5,
                xpReward = 100,
                coinsReward = 50
            ),
            AchievementDefinition(
                achievementType = "LESSONS_10",
                title = "Dedicated Student",
                description = "Complete 10 lessons",
                target = 10,
                xpReward = 200,
                coinsReward = 100
            ),
            AchievementDefinition(
                achievementType = "STREAK_3",
                title = "Consistent",
                description = "Maintain a 3-day streak",
                target = 3,
                xpReward = 75,
                coinsReward = 30
            ),
            AchievementDefinition(
                achievementType = "STREAK_7",
                title = "Streak Master",
                description = "Maintain a 7-day streak",
                target = 7,
                xpReward = 150,
                coinsReward = 60
            ),
            AchievementDefinition(
                achievementType = "WORDS_50",
                title = "Vocabulary Builder",
                description = "Learn 50 words",
                target = 50,
                xpReward = 80,
                coinsReward = 40
            ),
            AchievementDefinition(
                achievementType = "WORDS_100",
                title = "Word Master",
                description = "Learn 100 words",
                target = 100,
                xpReward = 100,
                coinsReward = 50
            ),
            AchievementDefinition(
                achievementType = "VOCAB_100",
                title = "Vocabulary Novice",
                description = "Learn 100 words",
                target = 100,
                xpReward = 120,
                coinsReward = 50
            ),
            AchievementDefinition(
                achievementType = "VOCAB_500",
                title = "Vocabulary Expert",
                description = "Learn 500 words",
                target = 500,
                xpReward = 250,
                coinsReward = 120
            ),
            AchievementDefinition(
                achievementType = "VOCAB_1000",
                title = "Vocabulary Master",
                description = "Learn 1000 words",
                target = 1000,
                xpReward = 500,
                coinsReward = 200
            ),
            AchievementDefinition(
                achievementType = "GRAMMAR_A1_A2",
                title = "Grammar Foundation",
                description = "Complete grammar foundations (A1-A2)",
                target = 5,
                xpReward = 150,
                coinsReward = 60
            ),
            AchievementDefinition(
                achievementType = "GRAMMAR_GURU",
                title = "Grammar Guru",
                description = "Complete all grammar quests",
                target = 15,
                xpReward = 250,
                coinsReward = 120
            ),
            AchievementDefinition(
                achievementType = "LISTENING_100",
                title = "Listening Pro",
                description = "Finish 100 listening exercises",
                target = 100,
                xpReward = 200,
                coinsReward = 80
            ),
            AchievementDefinition(
                achievementType = "SPEAKING_100",
                title = "Speaking Champion",
                description = "Complete 100 speaking practices",
                target = 100,
                xpReward = 220,
                coinsReward = 90
            ),
            AchievementDefinition(
                achievementType = "WRITING_50",
                title = "Writing Wizard",
                description = "Submit 50 writing/translation tasks",
                target = 50,
                xpReward = 180,
                coinsReward = 80
            ),
            AchievementDefinition(
                achievementType = "QUEST_STARTER",
                title = "Quest Starter",
                description = "Complete your first unit",
                target = 1,
                xpReward = 80,
                coinsReward = 30
            ),
            AchievementDefinition(
                achievementType = "QUEST_WARRIOR",
                title = "Quest Warrior",
                description = "Complete 10 units",
                target = 10,
                xpReward = 180,
                coinsReward = 90
            ),
            AchievementDefinition(
                achievementType = "QUEST_MASTER",
                title = "Quest Master",
                description = "Finish a full learning path",
                target = totalLessons,
                xpReward = 400,
                coinsReward = 180
            ),
            AchievementDefinition(
                achievementType = "MULTI_QUEST_HERO",
                title = "Multi-Quest Hero",
                description = "Learn from 2+ categories",
                target = 2,
                xpReward = 150,
                coinsReward = 70
            ),
            AchievementDefinition(
                achievementType = "CAREER_EXPERT",
                title = "Career Expert",
                description = "Finish a specialization/career lesson",
                target = 1,
                xpReward = 200,
                coinsReward = 90
            ),
            AchievementDefinition(
                achievementType = "PERFECT_WEEK",
                title = "Perfect Week",
                description = "Study 7 days in a row",
                target = 7,
                xpReward = 160,
                coinsReward = 70
            ),
            AchievementDefinition(
                achievementType = "SPEED_RUNNER",
                title = "Speed Runner",
                description = "Complete 10 lessons in one day",
                target = 10,
                xpReward = 220,
                coinsReward = 100
            ),
            AchievementDefinition(
                achievementType = "NIGHT_OWL",
                title = "Night Owl",
                description = "Study after 10pm",
                target = 1,
                xpReward = 120,
                coinsReward = 50
            ),
            AchievementDefinition(
                achievementType = "EARLY_BIRD",
                title = "Early Bird",
                description = "Study before 7am",
                target = 1,
                xpReward = 120,
                coinsReward = 50
            )
        )

    private data class AchievementSnapshot(
        val lessonsCompleted: Int,
        val wordsLearned: Int,
        val totalExercises: Int,
        val listeningCompleted: Int,
        val speakingCompleted: Int,
        val writingCompleted: Int,
        val categories: Set<String>,
        val completedLast24h: Int
    )

    private fun buildSnapshot(userId: String, now: Long): AchievementSnapshot {
        val completedLessonIds = UserProgress
            .selectAll()
            .where {
                (UserProgress.userId eq userId) and
                    UserProgress.wordId.isNull() and
                    (UserProgress.isCompleted eq true)
            }
            .map { it[UserProgress.lessonId] }
            .distinct()

        val lessonsCompleted = completedLessonIds.size

        val wordsLearned = if (completedLessonIds.isEmpty()) 0 else {
            Words.selectAll().where { Words.lessonId inList completedLessonIds }.count().toInt()
        }

        val totalExercises = if (completedLessonIds.isEmpty()) 0 else {
            Exercises.selectAll().where { Exercises.lessonId inList completedLessonIds }.count().toInt()
        }

        val listeningCompleted = countExercisesByType(completedLessonIds, listOf("LISTENING", "listening"))
        val speakingCompleted = countExercisesByType(completedLessonIds, listOf("SPEAKING", "speaking"))
        val writingCompleted = countExercisesByType(completedLessonIds, listOf("TRANSLATION", "translation"))

        val categories = if (completedLessonIds.isEmpty()) emptySet() else {
            Lessons
                .selectAll()
                .where { Lessons.id inList completedLessonIds }
                .map { it[Lessons.category] }
                .toSet()
        }

        val since = now - ONE_DAY_MS
        val completedLast24h = UserProgress
            .selectAll()
            .where {
                (UserProgress.userId eq userId) and
                    UserProgress.wordId.isNull() and
                    (UserProgress.isCompleted eq true) and
                    UserProgress.completedAt.isNotNull() and
                    (UserProgress.completedAt greaterEq since)
            }
            .count()
            .toInt()

        return AchievementSnapshot(
            lessonsCompleted = lessonsCompleted,
            wordsLearned = wordsLearned,
            totalExercises = totalExercises,
            listeningCompleted = listeningCompleted,
            speakingCompleted = speakingCompleted,
            writingCompleted = writingCompleted,
            categories = categories,
            completedLast24h = completedLast24h
        )
    }

    private fun countExercisesByType(lessonIds: List<Int>, types: List<String>): Int {
        if (lessonIds.isEmpty()) return 0
        return Exercises
            .selectAll()
            .where {
                (Exercises.lessonId inList lessonIds) and
                    (Exercises.type inList types)
            }
            .count()
            .toInt()
    }

    private fun syncUserProgressCounters(
        userId: String,
        current: ResultRow,
        snapshot: AchievementSnapshot,
        now: Long
    ) {
        val updatedLessons = maxOf(current[Users.lessonsCompleted], snapshot.lessonsCompleted)
        val updatedWords = maxOf(current[Users.wordsLearned], snapshot.wordsLearned)
        val updatedExercises = maxOf(current[Users.exercisesCompleted], snapshot.totalExercises)

        if (
            updatedLessons == current[Users.lessonsCompleted] &&
            updatedWords == current[Users.wordsLearned] &&
            updatedExercises == current[Users.exercisesCompleted]
        ) {
            return
        }

        Users.update({ Users.userId eq userId }) { update ->
            update[Users.lessonsCompleted] = updatedLessons
            update[Users.wordsLearned] = updatedWords
            update[Users.exercisesCompleted] = updatedExercises
            update[Users.updatedAt] = now
            update[Users.lastSyncedAt] = now
        }
    }

    private fun computeProgress(
        achievementType: String,
        snapshot: AchievementSnapshot,
        streakDays: Int,
        lastStudyHour: Int,
        totalLessons: Int,
        hasCareerLesson: Boolean
    ): Int =
        when (achievementType) {
            "FIRST_LESSON",
            "LESSONS_5",
            "LESSONS_10",
            "QUEST_STARTER",
            "QUEST_WARRIOR",
            "GRAMMAR_A1_A2",
            "GRAMMAR_GURU" -> snapshot.lessonsCompleted

            "QUEST_MASTER" -> snapshot.lessonsCompleted.coerceAtMost(totalLessons)

            "MULTI_QUEST_HERO" -> snapshot.categories.size

            "CAREER_EXPERT" -> if (hasCareerLesson) 1 else 0

            "WORDS_50",
            "WORDS_100",
            "VOCAB_100",
            "VOCAB_500",
            "VOCAB_1000" -> snapshot.wordsLearned

            "LISTENING_100" -> snapshot.listeningCompleted
            "SPEAKING_100" -> snapshot.speakingCompleted
            "WRITING_50" -> snapshot.writingCompleted

            "STREAK_3",
            "STREAK_7",
            "PERFECT_WEEK" -> streakDays

            "SPEED_RUNNER" -> snapshot.completedLast24h

            "NIGHT_OWL" -> if (lastStudyHour >= 22) 1 else 0
            "EARLY_BIRD" -> if (lastStudyHour in 4..6) 1 else 0

            else -> 0
        }

    private fun hourOfDay(timestamp: Long): Int {
        if (timestamp <= 0) return -1
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.HOUR_OF_DAY)
    }

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
}
