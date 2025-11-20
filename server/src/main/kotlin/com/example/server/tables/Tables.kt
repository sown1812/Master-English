package com.example.server.tables

import org.jetbrains.exposed.sql.Table

object Users : Table("users") {
    val userId = varchar("user_id", 64)
    val email = text("email")
    val displayName = text("display_name")
    val avatarUrl = text("avatar_url").nullable()
    val currentLevel = integer("current_level")
    val totalXp = integer("total_xp")
    val coins = integer("coins")
    val streakDays = integer("streak_days")
    val lastStudyDate = long("last_study_date")
    val longestStreak = integer("longest_streak")
    val wordsLearned = integer("words_learned")
    val lessonsCompleted = integer("lessons_completed")
    val exercisesCompleted = integer("exercises_completed")
    val isPremium = bool("is_premium")
    val premiumExpiryDate = long("premium_expiry_date").nullable()
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
    val lastSyncedAt = long("last_synced_at")
    override val primaryKey = PrimaryKey(userId)
}

object Lessons : Table("lessons") {
    val id = integer("id").autoIncrement()
    val title = text("title")
    val description = text("description")
    val order = integer("order")
    val totalWords = integer("total_words")
    val totalExercises = integer("total_exercises")
    val difficulty = text("difficulty")
    val category = text("category")
    val iconUrl = text("icon_url").nullable()
    val xpReward = integer("xp_reward")
    val coinsReward = integer("coins_reward")
    val isUnlocked = bool("is_unlocked")
    val isPremium = bool("is_premium")
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object Words : Table("words") {
    val id = integer("id").autoIncrement()
    val word = text("word")
    val translation = text("translation")
    val pronunciation = text("pronunciation")
    val partOfSpeech = text("part_of_speech")
    val exampleSentence = text("example_sentence")
    val exampleTranslation = text("example_translation")
    val imageUrl = text("image_url").nullable()
    val audioUrl = text("audio_url").nullable()
    val lessonId = reference("lesson_id", Lessons.id)
    val difficulty = integer("difficulty")
    val category = text("category")
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object Exercises : Table("exercises") {
    val id = integer("id").autoIncrement()
    val lessonId = reference("lesson_id", Lessons.id)
    val wordId = reference("word_id", Words.id).nullable()
    val type = text("type")
    val question = text("question")
    val correctAnswer = text("correct_answer")
    val optionA = text("option_a").nullable()
    val optionB = text("option_b").nullable()
    val optionC = text("option_c").nullable()
    val optionD = text("option_d").nullable()
    val matchPairs = text("match_pairs").nullable()
    val hint = text("hint").nullable()
    val explanation = text("explanation").nullable()
    val order = integer("order")
    val difficulty = integer("difficulty")
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object UserProgress : Table("user_progress") {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.userId)
    val lessonId = reference("lesson_id", Lessons.id)
    val wordId = reference("word_id", Words.id).nullable()
    val isCompleted = bool("is_completed")
    val completedAt = long("completed_at").nullable()
    val score = integer("score")
    val accuracy = double("accuracy")
    val timeSpent = long("time_spent")
    val attempts = integer("attempts")
    val correctAnswers = integer("correct_answers")
    val wrongAnswers = integer("wrong_answers")
    val xpEarned = integer("xp_earned")
    val coinsEarned = integer("coins_earned")
    val lastReviewDate = long("last_review_date").nullable()
    val nextReviewDate = long("next_review_date").nullable()
    val reviewCount = integer("review_count")
    val easeFactor = double("ease_factor")
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object Achievements : Table("achievements") {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.userId)
    val achievementType = text("achievement_type")
    val title = text("title")
    val description = text("description")
    val isUnlocked = bool("is_unlocked")
    val unlockedAt = long("unlocked_at").nullable()
    val progress = integer("progress")
    val target = integer("target")
    val xpReward = integer("xp_reward")
    val coinsReward = integer("coins_reward")
    val badgeUrl = text("badge_url").nullable()
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object UserBoosters : Table("user_boosters") {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.userId)
    val boosterKey = text("booster_key")
    val isOwned = bool("is_owned").default(false)
    val updatedAt = long("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object UserQuests : Table("user_quests") {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.userId)
    val questKey = text("quest_key")
    val isClaimed = bool("is_claimed").default(false)
    val updatedAt = long("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object DailyChallenges : Table("daily_challenges") {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.userId)
    val status = text("status") // READY, IN_PROGRESS, COMPLETED, CLAIMED
    val progress = integer("progress").default(0)
    val target = integer("target").default(5)
    val updatedAt = long("updated_at")
    override val primaryKey = PrimaryKey(id)
}
