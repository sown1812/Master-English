package com.example.master.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.master.data.local.dao.*
import com.example.master.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        WordEntity::class,
        LessonEntity::class,
        ExerciseEntity::class,
        SectionEntity::class,
        UnitEntity::class,
        LevelEntity::class,
        UserEntity::class,
        UserProgressEntity::class,
        AchievementEntity::class,
        MistakeEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun wordDao(): WordDao
    abstract fun lessonDao(): LessonDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun sectionDao(): SectionDao
    abstract fun unitDao(): UnitDao
    abstract fun levelDao(): LevelDao
    abstract fun userDao(): UserDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun achievementDao(): AchievementDao
    abstract fun mistakeDao(): MistakeDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        @Volatile
        private var hasSeeded: Boolean = false
        private lateinit var appContext: Context
        private val seedLessons by lazy { getInitialLessons() }
        private val seedWords by lazy { getInitialWords() }
        private val seedExercises by lazy { getInitialExercises() }
        private val seedWordTiles by lazy { getWordTileExercises() }
        private val seedLevels by lazy { getInitialLevels() }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                appContext = context.applicationContext
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "master_english_database"
                )
                    .addCallback(DatabaseCallback())
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDatabase(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDatabase(database)
                    }
                }
            }
        }
        
        private suspend fun seedDatabase(database: AppDatabase) {
            if (hasSeeded) return
            val lessonDao = database.lessonDao()
            val wordDao = database.wordDao()
            val exerciseDao = database.exerciseDao()
            val sectionDao = database.sectionDao()
            val unitDao = database.unitDao()
            val levelDao = database.levelDao()

            val existingLessons = runCatching { lessonDao.getTotalLessonsCount() }.getOrDefault(0)
            val existingWords = runCatching { wordDao.getTotalWordsCount() }.getOrDefault(0)
            val existingExercises = runCatching { exerciseDao.getTotalExercisesCount() }.getOrDefault(0)
            if (existingLessons > 0 && existingWords > 0 && existingExercises > 0) {
                ensureAdvancedContent(lessonDao, wordDao, exerciseDao)
                ensureLessonContent(wordDao, exerciseDao)
                ensureWordTileExercises(exerciseDao)
                ensureCurriculum(sectionDao, unitDao, levelDao, lessonDao)
                hasSeeded = true
                return
            }

            val assetSeed = loadSeedFromAssets()
            if (assetSeed != null) {
                lessonDao.insertLessons(assetSeed.lessons)
                wordDao.insertWords(assetSeed.words)
                exerciseDao.insertExercises(assetSeed.exercises)
                ensureCurriculum(sectionDao, unitDao, levelDao, lessonDao)
                hasSeeded = true
                return
            }

            lessonDao.insertLessons(seedLessons)
            wordDao.insertWords(seedWords)
            exerciseDao.insertExercises(seedExercises)
            ensureWordTileExercises(exerciseDao)
            ensureCurriculum(sectionDao, unitDao, levelDao, lessonDao)
            hasSeeded = true
        }

        private suspend fun ensureAdvancedContent(
            lessonDao: LessonDao,
            wordDao: WordDao,
            exerciseDao: ExerciseDao
        ) {
            val advancedLessons = seedLessons.filter { it.id >= 21 }
            if (advancedLessons.isEmpty()) return

            val missingLessons = advancedLessons.filter { lesson ->
                runCatching { lessonDao.getLessonById(lesson.id) }.getOrNull() == null
            }
            if (missingLessons.isEmpty()) return

            lessonDao.insertLessons(missingLessons)

            val missingIds = missingLessons.map { it.id }.toSet()
            val words = seedWords.filter { it.lessonId in missingIds }
            val exercises = seedExercises.filter { it.lessonId in missingIds }

            if (words.isNotEmpty()) {
                wordDao.insertWords(words)
            }
            if (exercises.isNotEmpty()) {
                exerciseDao.insertExercises(exercises)
            }
        }

        private suspend fun ensureWordTileExercises(exerciseDao: ExerciseDao) {
            val tileExercises = seedWordTiles
            if (tileExercises.isEmpty()) return

            val insertList = mutableListOf<ExerciseEntity>()
            val byLesson = tileExercises.groupBy { it.lessonId }
            byLesson.forEach { (lessonId, exercises) ->
                val existing = runCatching {
                    exerciseDao.getExercisesCountByLessonAndType(lessonId, "WORD_TILES")
                }.getOrDefault(0)
                if (existing == 0) {
                    insertList.addAll(exercises)
                }
            }

            if (insertList.isNotEmpty()) {
                exerciseDao.insertExercises(insertList)
            }
        }

        private suspend fun ensureCurriculum(
            sectionDao: SectionDao,
            unitDao: UnitDao,
            levelDao: LevelDao,
            lessonDao: LessonDao
        ) {
            val sectionsCount = runCatching { sectionDao.getSectionsCount() }.getOrDefault(0)
            val unitsCount = runCatching { unitDao.getUnitsCount() }.getOrDefault(0)
            val levelsCount = runCatching { levelDao.getLevelsCount() }.getOrDefault(0)

            val seedSections = getInitialSections()
            val seedUnits = getInitialUnits()
            if (sectionsCount < seedSections.size) {
                sectionDao.insertSections(seedSections)
            }
            if (unitsCount < seedUnits.size) {
                unitDao.insertUnits(seedUnits)
            }
            levelDao.insertLevels(seedLevels)

            val lessons = runCatching { lessonDao.getAllLessonsList() }.getOrDefault(emptyList())
            if (lessons.isEmpty()) return

            val levelIdByLesson = seedLevels.associateBy { it.id }
            lessons.forEach { lesson ->
                val mappedLevelId = levelIdByLesson[lesson.id]?.id ?: 0
                if (mappedLevelId != 0 && lesson.levelId != mappedLevelId) {
                    lessonDao.updateLesson(lesson.copy(levelId = mappedLevelId))
                }
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mistakes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `lessonId` INTEGER NOT NULL,
                        `exerciseId` INTEGER,
                        `wordId` INTEGER,
                        `question` TEXT NOT NULL,
                        `userAnswer` TEXT NOT NULL,
                        `correctAnswer` TEXT NOT NULL,
                        `reason` TEXT,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_mistakes_userId ON mistakes(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_mistakes_lessonId ON mistakes(lessonId)")
            }
        }
        
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE words ADD COLUMN `synonyms` TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE words ADD COLUMN `antonyms` TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE words ADD COLUMN `collocations` TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE words ADD COLUMN `frequencyRank` INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE words ADD COLUMN `cefrLevel` TEXT NOT NULL DEFAULT 'A1'")
                database.execSQL("ALTER TABLE words ADD COLUMN `usageNotes` TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sections` (
                        `id` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `cefrLevel` TEXT NOT NULL,
                        `order` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `units` (
                        `id` INTEGER NOT NULL,
                        `sectionId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `topic` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `order` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `levels` (
                        `id` INTEGER NOT NULL,
                        `unitId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `order` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL("ALTER TABLE lessons ADD COLUMN `levelId` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }

        private suspend fun ensureLessonContent(
            wordDao: WordDao,
            exerciseDao: ExerciseDao
        ) {
            val seedExercisesAll = seedExercises + seedWordTiles
            val lessonIds = (seedWords.map { it.lessonId } + seedExercisesAll.map { it.lessonId })
                .filter { it > 0 }
                .distinct()
            lessonIds.forEach { lessonId ->
                val seedWordCount = seedWords.count { it.lessonId == lessonId }
                val seedExerciseCount = seedExercisesAll.count { it.lessonId == lessonId }
                val existingWordCount = runCatching {
                    wordDao.getWordsCountByLesson(lessonId)
                }.getOrDefault(0)
                val existingExerciseCount = runCatching {
                    exerciseDao.getExercisesCountByLesson(lessonId)
                }.getOrDefault(0)
                if (seedWordCount > 0 && existingWordCount != seedWordCount) {
                    wordDao.deleteWordsByLesson(lessonId)
                    wordDao.insertWords(seedWords.filter { it.lessonId == lessonId })
                }
                if (seedExerciseCount > 0 && existingExerciseCount != seedExerciseCount) {
                    exerciseDao.deleteExercisesByLesson(lessonId)
                    exerciseDao.insertExercises(seedExercisesAll.filter { it.lessonId == lessonId })
                }
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                updateLessonText(database)
                updateWordText(database)
                updateExerciseText(database)
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS grammar_examples")
                database.execSQL("DROP TABLE IF EXISTS grammar_lessons")
                database.execSQL("DROP INDEX IF EXISTS index_grammar_examples_lessonId")
                database.execSQL("DROP INDEX IF EXISTS index_grammar_lessons_level")
                database.execSQL("DROP INDEX IF EXISTS index_grammar_lessons_category")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                updateLessonText(database)
                updateWordText(database)
                updateExerciseText(database)
            }
        }

        private fun updateLessonText(database: SupportSQLiteDatabase) {
            seedLessons.forEach { lesson ->
                database.execSQL(
                    """
                    UPDATE lessons
                    SET title = ?, description = ?
                    WHERE id = ?
                    """.trimIndent(),
                    arrayOf(lesson.title, lesson.description, lesson.id)
                )
            }
        }

        private fun updateWordText(database: SupportSQLiteDatabase) {
            seedWords.forEach { word ->
                database.execSQL(
                    """
                    UPDATE words
                    SET translation = ?,
                        pronunciation = ?,
                        partOfSpeech = ?,
                        exampleSentence = ?,
                        exampleTranslation = ?,
                        category = ?,
                        difficulty = ?
                    WHERE lessonId = ? AND word = ?
                    """.trimIndent(),
                    arrayOf(
                        word.translation,
                        word.pronunciation,
                        word.partOfSpeech,
                        word.exampleSentence,
                        word.exampleTranslation,
                        word.category,
                        word.difficulty,
                        word.lessonId,
                        word.word
                    )
                )
            }
        }

        private fun updateExerciseText(database: SupportSQLiteDatabase) {
            val exercises = seedExercises + seedWordTiles
            exercises.forEach { exercise ->
                database.execSQL(
                    """
                    UPDATE exercises
                    SET question = ?,
                        correctAnswer = ?,
                        optionA = ?,
                        optionB = ?,
                        optionC = ?,
                        optionD = ?,
                        matchPairs = ?,
                        hint = ?,
                        explanation = ?
                    WHERE lessonId = ? AND `order` = ? AND type = ?
                    """.trimIndent(),
                    arrayOf(
                        exercise.question,
                        exercise.correctAnswer,
                        exercise.optionA,
                        exercise.optionB,
                        exercise.optionC,
                        exercise.optionD,
                        exercise.matchPairs,
                        exercise.hint,
                        exercise.explanation,
                        exercise.lessonId,
                        exercise.order,
                        exercise.type
                    )
                )
            }
        }
        
        private fun loadSeedFromAssets(): SeedPayload? {
            if (!::appContext.isInitialized) return null
            return runCatching {
                appContext.assets.open("seed_data.json").use { input ->
                    val json = input.bufferedReader().readText()
                    val type = object : TypeToken<SeedPayload>() {}.type
                    Gson().fromJson<SeedPayload>(json, type)
                }
            }.getOrNull()?.takeIf {
                it.lessons.isNotEmpty() && it.words.isNotEmpty() && it.exercises.isNotEmpty()
            }
        }

        private fun getInitialSections(): List<SectionEntity> {
            return listOf(
                SectionEntity(
                    id = 1,
                    title = "A1 Foundation",
                    description = "Core basics and everyday phrases",
                    cefrLevel = "A1",
                    order = 1
                ),
                SectionEntity(
                    id = 2,
                    title = "A2 Everyday",
                    description = "Daily topics and survival language",
                    cefrLevel = "A2",
                    order = 2
                ),
                SectionEntity(
                    id = 3,
                    title = "B1 Progress",
                    description = "Intermediate communication",
                    cefrLevel = "B1",
                    order = 3
                ),
                SectionEntity(
                    id = 4,
                    title = "B2 Mastery",
                    description = "Upper-intermediate fluency and nuance",
                    cefrLevel = "B2",
                    order = 4
                )
            )
        }

        private fun getInitialUnits(): List<UnitEntity> {
            return listOf(
                UnitEntity(
                    id = 1,
                    sectionId = 1,
                    title = "Basics",
                    topic = "basics",
                    description = "Greetings and introductions",
                    order = 1
                ),
                UnitEntity(
                    id = 2,
                    sectionId = 1,
                    title = "Daily Life",
                    topic = "daily_life",
                    description = "Food, routines, and everyday tasks",
                    order = 2
                ),
                UnitEntity(
                    id = 3,
                    sectionId = 2,
                    title = "Travel & City",
                    topic = "travel",
                    description = "Directions, transport, and shopping",
                    order = 1
                ),
                UnitEntity(
                    id = 4,
                    sectionId = 2,
                    title = "Health & Work",
                    topic = "work_health",
                    description = "Health and workplace essentials",
                    order = 2
                ),
                UnitEntity(
                    id = 5,
                    sectionId = 2,
                    title = "Lifestyle",
                    topic = "lifestyle",
                    description = "Hobbies, weather, and events",
                    order = 3
                ),
                UnitEntity(
                    id = 6,
                    sectionId = 3,
                    title = "B1 Fluency",
                    topic = "b1_fluency",
                    description = "Opinions, negotiations, and complex ideas",
                    order = 1
                ),
                UnitEntity(
                    id = 7,
                    sectionId = 4,
                    title = "B2 Professional",
                    topic = "b2_professional",
                    description = "Workplace communication and reporting",
                    order = 1
                ),
                UnitEntity(
                    id = 8,
                    sectionId = 4,
                    title = "B2 Global Topics",
                    topic = "b2_global",
                    description = "Debates, culture, and media",
                    order = 2
                )
            )
        }

        private fun getInitialLevels(): List<LevelEntity> {
            val lessonUnitMap = mapOf(
                1 to 1, 2 to 1, 3 to 1,
                4 to 2, 5 to 2, 6 to 2, 7 to 2, 8 to 2, 9 to 2, 10 to 2,
                11 to 3, 12 to 3, 13 to 3,
                14 to 4, 15 to 4, 16 to 4,
                17 to 5, 18 to 5, 19 to 5, 20 to 5,
                21 to 6, 22 to 6, 23 to 6, 24 to 6, 25 to 6,
                26 to 7, 27 to 7, 28 to 7,
                29 to 8, 30 to 8
            )
            return lessonUnitMap.entries
                .sortedBy { it.key }
                .map { (lessonId, unitId) ->
                    LevelEntity(
                        id = lessonId,
                        unitId = unitId,
                        title = "Level $lessonId",
                        order = lessonId
                    )
                }
        }
        private fun getInitialLessons(): List<LessonEntity> {
            return listOf(
                LessonEntity(
                    id = 1,
                    title = "Basics 1",
                    description = "Greetings, to be, and short self-intros like Duolingo Unit 1",
                    order = 1,
                    totalWords = 20,
                    totalExercises = 12,
                    difficulty = "EASY",
                    category = "basics",
                    xpReward = 65,
                    coinsReward = 15,
                    isUnlocked = true
                ),
                LessonEntity(
                    id = 2,
                    title = "Basics 2",
                    description = "Simple people words and short sentences",
                    order = 2,
                    totalWords = 12,
                    totalExercises = 8,
                    difficulty = "EASY",
                    category = "basics",
                    xpReward = 45,
                    coinsReward = 10,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 3,
                    title = "Phrases",
                    description = "Common phrases from early Duolingo steps",
                    order = 3,
                    totalWords = 12,
                    totalExercises = 8,
                    difficulty = "EASY",
                    category = "basics",
                    xpReward = 50,
                    coinsReward = 12,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 4,
                    title = "Food & Drinks",
                    description = "Ordering and naming common foods",
                    order = 4,
                    totalWords = 12,
                    totalExercises = 8,
                    difficulty = "MEDIUM",
                    category = "daily_life",
                    xpReward = 60,
                    coinsReward = 14,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 5,
                    title = "Travel Essentials",
                    description = "Tickets, airport, and getting around",
                    order = 5,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "MEDIUM",
                    category = "travel",
                    xpReward = 55,
                    coinsReward = 12,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 6,
                    title = "Family",
                    description = "Immediate family members",
                    order = 6,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "EASY",
                    category = "family",
                    xpReward = 45,
                    coinsReward = 10,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 7,
                    title = "Colors & Clothing",
                    description = "Simple colors and clothing words",
                    order = 7,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "EASY",
                    category = "basics",
                    xpReward = 45,
                    coinsReward = 10,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 8,
                    title = "Numbers & Time",
                    description = "Counting to three and simple time words",
                    order = 8,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "MEDIUM",
                    category = "basics",
                    xpReward = 50,
                    coinsReward = 12,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 9,
                    title = "School & Work",
                    description = "Classroom and office essentials",
                    order = 9,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "MEDIUM",
                    category = "daily_life",
                    xpReward = 55,
                    coinsReward = 12,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 10,
                    title = "Daily Routine",
                    description = "Morning and evening habits",
                    order = 10,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "MEDIUM",
                    category = "daily_life",
                    xpReward = 60,
                    coinsReward = 14,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 11,
                    title = "Transport & Directions",
                    description = "Xe cộ, chỉ đường khi di chuyển trong thành phố",
                    order = 11,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "MEDIUM",
                    category = "travel",
                    xpReward = 60,
                    coinsReward = 14,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 12,
                    title = "Shopping & Money",
                    description = "Mua sắm, giá cả và thanh toán",
                    order = 12,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "MEDIUM",
                    category = "daily_life",
                    xpReward = 60,
                    coinsReward = 14,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 13,
                    title = "Restaurant & Cafe",
                    description = "Gọi món, đặt bàn và phản hồi chất lượng",
                    order = 13,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "MEDIUM",
                    category = "food",
                    xpReward = 65,
                    coinsReward = 15,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 14,
                    title = "Health & Doctor",
                    description = "Triệu chứng, khám bệnh và lời khuyên sức khỏe",
                    order = 14,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "MEDIUM",
                    category = "health",
                    xpReward = 65,
                    coinsReward = 15,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 15,
                    title = "Workplace",
                    description = "Nơi làm việc, cuộc họp và nhiệm vụ hằng ngày",
                    order = 15,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "MEDIUM",
                    category = "work",
                    xpReward = 65,
                    coinsReward = 15,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 16,
                    title = "Home & Household",
                    description = "Phòng ốc, việc nhà và đồ dùng gia đình",
                    order = 16,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "EASY",
                    category = "home",
                    xpReward = 55,
                    coinsReward = 12,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 17,
                    title = "Hobbies & Free Time",
                    description = "Sở thích, thể thao và hoạt động giải trí",
                    order = 17,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "EASY",
                    category = "hobby",
                    xpReward = 55,
                    coinsReward = 12,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 18,
                    title = "Technology & Devices",
                    description = "Thiết bị, ứng dụng và sử dụng công nghệ",
                    order = 18,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "MEDIUM",
                    category = "technology",
                    xpReward = 65,
                    coinsReward = 15,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 19,
                    title = "Weather & Events",
                    description = "Thời tiết, thiên nhiên và sự kiện ngoài trời",
                    order = 19,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "MEDIUM",
                    category = "nature",
                    xpReward = 65,
                    coinsReward = 15,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 20,
                    title = "Emergency & Help",
                    description = "Tình huống khẩn cấp và yêu cầu hỗ trợ",
                    order = 20,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "HARD",
                    category = "safety",
                    xpReward = 75,
                    coinsReward = 18,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 21,
                    title = "Advanced Conversations",
                    description = "Quan điểm, thỏa thuận và giải thích ý kiến",
                    order = 21,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "HARD",
                    category = "advanced",
                    xpReward = 85,
                    coinsReward = 20,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 22,
                    title = "Business Communication",
                    description = "Email công việc, đề xuất và thời hạn",
                    order = 22,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "HARD",
                    category = "advanced",
                    xpReward = 90,
                    coinsReward = 22,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 23,
                    title = "Academic Skills",
                    description = "Giải thích, phân tích và trích dẫn học thuật",
                    order = 23,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "HARD",
                    category = "advanced",
                    xpReward = 95,
                    coinsReward = 24,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 24,
                    title = "Weather & Seasons",
                    description = "Mùa và thời tiết cơ bản",
                    order = 24,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "MEDIUM",
                    category = "nature",
                    xpReward = 70,
                    coinsReward = 16,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 25,
                    title = "Feelings & Emotions",
                    description = "Cảm xúc và trạng thái hàng ngày",
                    order = 25,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "MEDIUM",
                    category = "lifestyle",
                    xpReward = 70,
                    coinsReward = 16,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 26,
                    title = "Shopping Details",
                    description = "Kích cỡ, chất lượng và hoàn tiền",
                    order = 26,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "MEDIUM",
                    category = "shopping",
                    xpReward = 72,
                    coinsReward = 16,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 27,
                    title = "City Directions",
                    description = "Hỏi đường và chỉ đường nâng cao",
                    order = 27,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "MEDIUM",
                    category = "travel",
                    xpReward = 72,
                    coinsReward = 16,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 28,
                    title = "Work Meetings",
                    description = "Lịch họp và ra quyết định",
                    order = 28,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "MEDIUM",
                    category = "work",
                    xpReward = 75,
                    coinsReward = 18,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 29,
                    title = "Health & Wellness",
                    description = "Sức khỏe, thói quen tốt và phục hồi",
                    order = 29,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "MEDIUM",
                    category = "health",
                    xpReward = 75,
                    coinsReward = 18,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 30,
                    title = "B1 Conversations",
                    description = "Bày tỏ ý kiến và đề xuất",
                    order = 30,
                    totalWords = 6,
                    totalExercises = 4,
                    difficulty = "HARD",
                    category = "advanced",
                    xpReward = 100,
                    coinsReward = 25,
                    isUnlocked = false
                )
            )
        }
        private fun getInitialWords(): List<WordEntity> {
            return listOf(
                WordEntity("hello", "xin chào", "/həlˈoʊ/", "interjection", "Hello, how are you?", "Xin chào, bạn khỏe không?", lessonId = 1, difficulty = 1, category = "greetings"),
                WordEntity("hi", "chào", "/hˈaɪ/", "interjection", "Hi! Nice to meet you.", "Chào! Rất vui được gặp bạn.", lessonId = 1, difficulty = 1, category = "greetings"),
                WordEntity("goodbye", "tạm biệt", "/gˌʊdbˈaɪ/", "interjection", "Goodbye, see you soon!", "Tạm biệt, hẹn gặp lại!", lessonId = 1, difficulty = 1, category = "greetings"),
                WordEntity("please", "làm ơn", "/plˈiz/", "adverb", "Please help me.", "Làm ơn giúp tôi.", lessonId = 1, difficulty = 1, category = "politeness"),
                WordEntity("thank you", "cảm ơn", "/θˈæŋk jˈu/", "phrase", "Thank you very much!", "Cảm ơn bạn rất nhiều!", lessonId = 1, difficulty = 1, category = "politeness"),
                WordEntity("yes", "vâng", "/jˈɛs/", "adverb", "Yes, I understand.", "Vâng, tôi hiểu.", lessonId = 1, difficulty = 1, category = "basics"),
                WordEntity("no", "không", "/nˈoʊ/", "adverb", "No, thank you.", "Không, cảm ơn.", lessonId = 1, difficulty = 1, category = "basics"),
                WordEntity("I", "tôi", "/ˈaɪ/", "pronoun", "I am a student.", "Tôi là học sinh.", lessonId = 1, difficulty = 1, category = "pronoun"),
                WordEntity("you", "bạn", "/jˈu/", "pronoun", "You are kind.", "Bạn rất tốt bụng.", lessonId = 1, difficulty = 1, category = "pronoun"),
                WordEntity("am", "là (đi với I)", "/ˈæm/", "verb", "I am Nam.", "Tôi là Nam.", lessonId = 1, difficulty = 1, category = "verb"),
                WordEntity("are", "là (đi với you/we/they)", "/ˈɑɹ/", "verb", "You are my friend.", "Bạn là bạn của tôi.", lessonId = 1, difficulty = 1, category = "verb"),
                WordEntity("name", "tên", "/nˈeɪm/", "noun", "My name is Mai.", "Tên tôi là Mai.", lessonId = 1, difficulty = 1, category = "introductions"),
                WordEntity("my", "của tôi", "/mˈaɪ/", "pronoun", "My name is Linh.", "Tên tôi là Linh.", lessonId = 1, difficulty = 1, category = "introductions"),
                WordEntity("your", "của bạn", "/jˈɔr/", "pronoun", "What is your name?", "Tên bạn là gì?", lessonId = 1, difficulty = 1, category = "introductions"),
                WordEntity("he", "anh ấy", "/hˈi/", "pronoun", "He is a teacher.", "Anh ấy là giáo viên.", lessonId = 1, difficulty = 1, category = "pronoun"),
                WordEntity("she", "cô ấy", "/ʃˈi/", "pronoun", "She is kind.", "Cô ấy tốt.", lessonId = 1, difficulty = 1, category = "pronoun"),
                WordEntity("we", "chúng tôi", "/wˈi/", "pronoun", "We are friends.", "Chúng tôi là bạn.", lessonId = 1, difficulty = 1, category = "pronoun"),
                WordEntity("they", "họ", "/ðˈeɪ/", "pronoun", "They are students.", "Họ là học sinh.", lessonId = 1, difficulty = 1, category = "pronoun"),
                WordEntity("is", "là (đi với he/she/it)", "/ˈɪz/", "verb", "She is here.", "Cô ấy ở đây.", lessonId = 1, difficulty = 1, category = "verb"),
                WordEntity("friend", "bạn bè", "/fɹˈɛnd/", "noun", "He is my friend.", "Anh ấy là bạn tôi.", lessonId = 1, difficulty = 1, category = "basics"),
                
                WordEntity("he", "anh ấy", "/hˈi/", "pronoun", "He is a teacher.", "Anh ấy là giáo viên.", lessonId = 2, difficulty = 1, category = "pronoun"),
                WordEntity("she", "cô ấy", "/ʃˈi/", "pronoun", "She is a doctor.", "Cô ấy là bác sĩ.", lessonId = 2, difficulty = 1, category = "pronoun"),
                WordEntity("we", "chúng tôi", "/wˈi/", "pronoun", "We are from Vietnam.", "Chúng tôi đến từ Việt Nam.", lessonId = 2, difficulty = 1, category = "pronoun"),
                WordEntity("they", "họ", "/ðˈeɪ/", "pronoun", "They are students.", "Họ là học sinh.", lessonId = 2, difficulty = 1, category = "pronoun"),
                WordEntity("man", "người đàn ông", "/mˈæn/", "noun", "The man is tall.", "Người đàn ông đó cao.", lessonId = 2, difficulty = 1, category = "people"),
                WordEntity("woman", "phụ nữ", "/wˈʊmən/", "noun", "The woman drinks tea.", "Người phụ nữ uống trà.", lessonId = 2, difficulty = 1, category = "people"),
                WordEntity("boy", "con trai", "/bˈɔɪ/", "noun", "The boy reads a book.", "Cậu bé đang đọc sách.", lessonId = 2, difficulty = 1, category = "people"),
                WordEntity("girl", "con gái", "/gˈɝl/", "noun", "The girl eats rice.", "Cô bé ăn cơm.", lessonId = 2, difficulty = 1, category = "people"),
                WordEntity("eat", "ăn", "/ˈit/", "verb", "We eat breakfast.", "Chúng tôi ăn sáng.", lessonId = 2, difficulty = 1, category = "verb"),
                WordEntity("drink", "uống", "/dɹˈɪŋk/", "verb", "They drink coffee.", "Họ uống cà phê.", lessonId = 2, difficulty = 1, category = "verb"),
                WordEntity("read", "đọc", "/ɹˈɛd/", "verb", "I read every day.", "Tôi đọc sách mỗi ngày.", lessonId = 2, difficulty = 1, category = "verb"),
                WordEntity("write", "viết", "/ɹˈaɪt/", "verb", "She writes a letter.", "Cô ấy viết thư.", lessonId = 2, difficulty = 1, category = "verb"),
                
                WordEntity("excuse me", "xin lỗi", "/ɪkskjˈus mˈi/", "phrase", "Excuse me, where is the bus?", "Xin lỗi, trạm xe ở đâu?", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("sorry", "xin lỗi", "/sˈɑɹi/", "adjective", "I am sorry.", "Tôi xin lỗi.", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("good morning", "chào buổi sáng", "/gˈʊd mˈɔɹnɪŋ/", "phrase", "Good morning, everyone!", "Chào buổi sáng mọi người!", lessonId = 3, difficulty = 1, category = "greetings"),
                WordEntity("good night", "chúc ngủ ngon", "/gˈʊd nˈaɪt/", "phrase", "Good night, see you tomorrow.", "Chúc ngủ ngon, hẹn gặp bạn ngày mai.", lessonId = 3, difficulty = 1, category = "greetings"),
                WordEntity("see you later", "hẹn gặp lại sau", "/sˈi jˈu lˈeɪtɚ/", "phrase", "See you later!", "Hẹn gặp lại sau!", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("see you soon", "gặp lại sớm thôi", "/sˈi jˈu sˈun/", "phrase", "See you soon.", "Gặp lại bạn sớm thôi.", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("how are you", "bạn khỏe không", "/hˈaʊ ˈɑɹ jˈu/", "phrase", "Hi, how are you?", "Chào, bạn khỏe không?", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("I am fine", "tôi khỏe", "/ˈaɪ ˈæm fˈaɪn/", "phrase", "I am fine, thank you.", "Tôi khỏe, cảm ơn.", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("what is your name", "bạn tên gì", "/wˈʌt ˈɪz jˈɔɹ nˈeɪm/", "phrase", "What is your name?", "Bạn tên gì?", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("nice to meet you", "rất vui được gặp bạn", "/nˈaɪs tˈu mˈit jˈu/", "phrase", "Nice to meet you!", "Rất vui được gặp bạn!", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("welcome", "chào mừng", "/wˈɛlkəm/", "phrase", "Welcome to Hanoi!", "Chào mừng đến Hà Nội!", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("good luck", "chúc may mắn", "/gˈʊd lˈʌk/", "phrase", "Good luck on your test!", "Chúc may mắn khi thi!", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("water", "nước", "/wˈɔtɚ/", "noun", "I drink water.", "Tôi uống nước.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("coffee", "cà phê", "/kˈɑfi/", "noun", "She likes coffee.", "Cô ấy thích cà phê.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("tea", "trà", "/tˈi/", "noun", "Tea or coffee?", "Trà hay cà phê?", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("bread", "bánh mì", "/bɹˈɛd/", "noun", "I eat bread.", "Tôi ăn bánh mì.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("rice", "cơm", "/ɹˈaɪs/", "noun", "We cook rice.", "Chúng tôi nấu cơm.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("soup", "súp", "/sˈup/", "noun", "The soup is hot.", "Bát súp nóng.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("apple", "táo", "/ˈæpəl/", "noun", "The apple is red.", "Quả táo màu đỏ.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("banana", "chuối", "/bənˈænə/", "noun", "Bananas are sweet.", "Chuối ngọt.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("chicken", "gà", "/tʃˈɪkən/", "noun", "I eat chicken.", "Tôi ăn thịt gà.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("fish", "cá", "/fˈɪʃ/", "noun", "Fish and rice.", "Cá và cơm.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("delicious", "ngon", "/dɪlˈɪʃəs/", "adjective", "The meal is delicious.", "Bữa ăn ngon.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("hungry", "đói", "/hˈʌŋgɹi/", "adjective", "I am hungry.", "Tôi đang đói.", lessonId = 4, difficulty = 1, category = "food"),
                
                WordEntity("bus", "xe buýt", "/bˈʌs/", "noun", "Take the bus.", "Bắt xe buýt.", lessonId = 5, difficulty = 1, category = "travel"),
                WordEntity("train", "tàu hỏa", "/tɹˈeɪn/", "noun", "The train is late.", "Tàu hỏa bị trễ.", lessonId = 5, difficulty = 1, category = "travel"),
                WordEntity("taxi", "taxi", "/tˈæksi/", "noun", "Call a taxi.", "Gọi một chiếc taxi.", lessonId = 5, difficulty = 1, category = "travel"),
                WordEntity("airport", "sân bay", "/ˈɛɹpˌɔɹt/", "noun", "The airport is far.", "Sân bay khá xa.", lessonId = 5, difficulty = 1, category = "travel"),
                WordEntity("ticket", "vé", "/tˈɪkət/", "noun", "I need a ticket.", "Tôi cần vé.", lessonId = 5, difficulty = 1, category = "travel"),
                WordEntity("passport", "hộ chiếu", "/pˈæspˌɔɹt/", "noun", "Show your passport.", "Xuất trình hộ chiếu.", lessonId = 5, difficulty = 1, category = "travel"),
                
                WordEntity("father", "cha", "/fˈɑðɚ/", "noun", "My father is kind.", "Cha tôi rất tốt.", lessonId = 6, difficulty = 1, category = "family"),
                WordEntity("mother", "mẹ", "/mˈʌðɚ/", "noun", "My mother cooks.", "Mẹ tôi nấu ăn.", lessonId = 6, difficulty = 1, category = "family"),
                WordEntity("brother", "anh/em trai", "/bɹˈʌðɚ/", "noun", "He is my brother.", "Anh ấy là anh trai tôi.", lessonId = 6, difficulty = 1, category = "family"),
                WordEntity("sister", "chị/em gái", "/sˈɪstɚ/", "noun", "She is my sister.", "Cô ấy là chị gái tôi.", lessonId = 6, difficulty = 1, category = "family"),
                WordEntity("son", "con trai", "/sˈʌn/", "noun", "This is my son.", "Đây là con trai tôi.", lessonId = 6, difficulty = 1, category = "family"),
                WordEntity("daughter", "con gái", "/dˈɔtɚ/", "noun", "That is my daughter.", "Đó là con gái tôi.", lessonId = 6, difficulty = 1, category = "family"),
                
                WordEntity("red", "màu đỏ", "/ɹˈɛd/", "adjective", "The apple is red.", "Quả táo màu đỏ.", lessonId = 7, difficulty = 1, category = "colors"),
                WordEntity("blue", "màu xanh dương", "/blˈu/", "adjective", "The sky is blue.", "Bầu trời màu xanh.", lessonId = 7, difficulty = 1, category = "colors"),
                WordEntity("green", "màu xanh lá", "/gɹˈin/", "adjective", "The leaf is green.", "Chiếc lá màu xanh lá.", lessonId = 7, difficulty = 1, category = "colors"),
                WordEntity("shirt", "áo sơ mi", "/ʃˈɝt/", "noun", "I wear a shirt.", "Tôi mặc áo sơ mi.", lessonId = 7, difficulty = 1, category = "clothes"),
                WordEntity("pants", "quần dài", "/pˈænts/", "noun", "These pants are new.", "Chiếc quần này mới.", lessonId = 7, difficulty = 1, category = "clothes"),
                WordEntity("shoes", "đôi giày", "/ʃˈuz/", "noun", "I like these shoes.", "Tôi thích đôi giày này.", lessonId = 7, difficulty = 1, category = "clothes"),
                
                WordEntity("one", "một", "/wˈʌn/", "number", "One apple, please.", "Một quả táo, làm ơn.", lessonId = 8, difficulty = 1, category = "numbers"),
                WordEntity("two", "hai", "/tˈu/", "number", "Two tickets.", "Hai vé.", lessonId = 8, difficulty = 1, category = "numbers"),
                WordEntity("three", "ba", "/θɹˈi/", "number", "Three cups of tea.", "Ba ly trà.", lessonId = 8, difficulty = 1, category = "numbers"),
                WordEntity("today", "hôm nay", "/tədˈeɪ/", "noun", "See you today.", "Hẹn bạn hôm nay.", lessonId = 8, difficulty = 1, category = "time"),
                WordEntity("tomorrow", "ngày mai", "/təmˈɑɹˌoʊ/", "noun", "See you tomorrow.", "Hẹn bạn ngày mai.", lessonId = 8, difficulty = 1, category = "time"),
                WordEntity("yesterday", "hôm qua", "/jˈɛstɚdˌeɪ/", "noun", "Yesterday was busy.", "Hôm qua rất bận.", lessonId = 8, difficulty = 1, category = "time"),
                
                WordEntity("teacher", "giáo viên", "/tˈitʃɚ/", "noun", "She is a teacher.", "Cô ấy là giáo viên.", lessonId = 9, difficulty = 1, category = "school"),
                WordEntity("student", "học sinh", "/stˈudənt/", "noun", "I am a student.", "Tôi là học sinh.", lessonId = 9, difficulty = 1, category = "school"),
                WordEntity("school", "trường học", "/skˈul/", "noun", "The school is big.", "Trường học này lớn.", lessonId = 9, difficulty = 1, category = "school"),
                WordEntity("job", "công việc", "/dʒˈɑb/", "noun", "I love my job.", "Tôi thích công việc.", lessonId = 9, difficulty = 1, category = "work"),
                WordEntity("office", "văn phòng", "/ˈɔfɪs/", "noun", "The office is near.", "Văn phòng ở gần.", lessonId = 9, difficulty = 1, category = "work"),
                WordEntity("meeting", "cuộc họp", "/mˈitɪŋ/", "noun", "I have a meeting.", "Tôi có một cuộc họp.", lessonId = 9, difficulty = 1, category = "work"),
                
                WordEntity("wake up", "thức dậy", "/wˈeɪk ˈʌp/", "verb", "I wake up early.", "Tôi thức dậy sớm.", lessonId = 10, difficulty = 1, category = "routine"),
                WordEntity("breakfast", "bữa sáng", "/bɹˈɛkfəst/", "noun", "Breakfast at 7 am.", "Ăn sáng lúc 7 giờ.", lessonId = 10, difficulty = 1, category = "routine"),
                WordEntity("lunch", "bữa trưa", "/lˈʌntʃ/", "noun", "Lunch with friends.", "Ăn trưa với bạn.", lessonId = 10, difficulty = 1, category = "routine"),
                WordEntity("dinner", "bữa tối", "/dˈɪnɚ/", "noun", "Dinner at home.", "Ăn tối ở nhà.", lessonId = 10, difficulty = 1, category = "routine"),
                WordEntity("sleep", "ngủ", "/slˈip/", "verb", "I sleep at 11 pm.", "Tôi ngủ lúc 11 giờ.", lessonId = 10, difficulty = 1, category = "routine"),
                WordEntity("exercise", "tập thể dục", "/ˈɛksɚsˌaɪz/", "verb", "I exercise every day.", "Tôi tập thể dục mỗi ngày.", lessonId = 10, difficulty = 1, category = "routine"),
                
                WordEntity("bus stop", "trạm xe buýt", "/bˈʌs stˈɑp/", "noun", "The bus stop is near.", "Trạm xe buýt ở gần.", lessonId = 11, difficulty = 2, category = "travel"),
                WordEntity("station", "nhà ga", "/stˈeɪʃən/", "noun", "Meet me at the station.", "Gặp tôi ở nhà ga.", lessonId = 11, difficulty = 2, category = "travel"),
                WordEntity("ticket booth", "quầy vé", "/tˈɪkət bˈuθ/", "noun", "Buy tickets at the booth.", "Mua vé ở quầy.", lessonId = 11, difficulty = 2, category = "travel"),
                WordEntity("turn left", "rẽ trái", "/tˈɝn lˈɛft/", "phrase", "Turn left at the corner.", "Rẽ trái ở góc đường.", lessonId = 11, difficulty = 2, category = "directions"),
                WordEntity("straight ahead", "đi thẳng", "/stɹˈeɪt əhˈɛd/", "phrase", "Go straight ahead 200 meters.", "Đi thẳng 200 mét.", lessonId = 11, difficulty = 2, category = "directions"),
                WordEntity("traffic jam", "kẹt xe", "/tɹˈæfɪk dʒˈæm/", "noun", "There is a traffic jam.", "Đang kẹt xe.", lessonId = 11, difficulty = 2, category = "travel"),
                
                WordEntity("price", "giá", "/pɹˈaɪs/", "noun", "What is the price?", "Giá bao nhiêu?", lessonId = 12, difficulty = 2, category = "shopping"),
                WordEntity("discount", "giảm giá", "/dɪskˈaʊnt/", "noun", "Do you have a discount?", "Bạn có giảm giá không?", lessonId = 12, difficulty = 2, category = "shopping"),
                WordEntity("cash", "tiền mặt", "/kˈæʃ/", "noun", "I pay with cash.", "Tôi trả tiền mặt.", lessonId = 12, difficulty = 2, category = "shopping"),
                WordEntity("card", "thẻ", "/kˈɑɹd/", "noun", "Can I pay by card?", "Tôi có thể trả bằng thẻ không?", lessonId = 12, difficulty = 2, category = "shopping"),
                WordEntity("receipt", "hóa đơn", "/ɹɪsˈit/", "noun", "Here is your receipt.", "Đây là hóa đơn của bạn.", lessonId = 12, difficulty = 2, category = "shopping"),
                WordEntity("expensive", "đắt", "/ɪkspˈɛnsɪv/", "adjective", "That bag is expensive.", "Chiếc túi đó đắt.", lessonId = 12, difficulty = 2, category = "shopping"),
                
                WordEntity("menu", "thực đơn", "/mˈɛnju/", "noun", "Can I see the menu?", "Cho tôi xem thực đơn.", lessonId = 13, difficulty = 2, category = "food"),
                WordEntity("order", "gọi món", "/ˈɔɹdɚ/", "verb", "We will order now.", "Chúng tôi sẽ gọi món bây giờ.", lessonId = 13, difficulty = 2, category = "food"),
                WordEntity("reservation", "đặt bàn", "/ɹˌɛzɚvˈeɪʃən/", "noun", "I have a reservation.", "Tôi đã đặt bàn trước.", lessonId = 13, difficulty = 2, category = "food"),
                WordEntity("bill", "hóa đơn", "/bˈɪl/", "noun", "Please bring the bill.", "Cho xin hóa đơn.", lessonId = 13, difficulty = 2, category = "food"),
                WordEntity("tip", "tiền tip", "/tˈɪp/", "noun", "Leave a small tip.", "Để lại chút tiền tip.", lessonId = 13, difficulty = 2, category = "food"),
                WordEntity("delicious", "ngon", "/dɪlˈɪʃəs/", "adjective", "The soup is delicious.", "Món súp rất ngon.", lessonId = 13, difficulty = 2, category = "food"),
                
                WordEntity("fever", "sốt", "/fˈivɚ/", "noun", "I have a fever.", "Tôi bị sốt.", lessonId = 14, difficulty = 2, category = "health"),
                WordEntity("cough", "ho", "/kˈɑf/", "noun", "This cough is bad.", "Cơn ho này nặng.", lessonId = 14, difficulty = 2, category = "health"),
                WordEntity("headache", "đau đầu", "/hˈɛdˌeɪk/", "noun", "I have a headache.", "Tôi bị đau đầu.", lessonId = 14, difficulty = 2, category = "health"),
                WordEntity("medicine", "thuốc", "/mˈɛdəsən/", "noun", "Take this medicine twice a day.", "Uống thuốc này 2 lần mỗi ngày.", lessonId = 14, difficulty = 2, category = "health"),
                WordEntity("appointment", "lịch hẹn", "/əpˈɔɪntmənt/", "noun", "I need a doctor appointment.", "Tôi cần hẹn bác sĩ.", lessonId = 14, difficulty = 2, category = "health"),
                WordEntity("rest", "nghỉ ngơi", "/ɹˈɛst/", "verb", "You should rest today.", "Bạn nên nghỉ ngơi hôm nay.", lessonId = 14, difficulty = 2, category = "health"),
                
                WordEntity("meeting room", "phòng họp", "/mˈitɪŋ ɹˈum/", "noun", "The meeting room is ready.", "Phòng họp đã sẵn sàng.", lessonId = 15, difficulty = 2, category = "work"),
                WordEntity("deadline", "hạn chót", "/dˈɛdlˌaɪn/", "noun", "The deadline is Friday.", "Hạn chót là thứ Sáu.", lessonId = 15, difficulty = 2, category = "work"),
                WordEntity("task", "nhiệm vụ", "/tˈæsk/", "noun", "Assign the new task.", "Giao nhiệm vụ mới.", lessonId = 15, difficulty = 2, category = "work"),
                WordEntity("colleague", "đồng nghiệp", "/kˈɑlig/", "noun", "She is my colleague.", "Cô ấy là đồng nghiệp của tôi.", lessonId = 15, difficulty = 2, category = "work"),
                WordEntity("report", "báo cáo", "/ɹipˈɔɹt/", "noun", "Send the weekly report.", "Gửi báo cáo hằng tuần.", lessonId = 15, difficulty = 2, category = "work"),
                WordEntity("present", "trình bày", "/pɹˈɛzənt/", "verb", "I will present today.", "Tôi sẽ trình bày hôm nay.", lessonId = 15, difficulty = 2, category = "work"),
                
                WordEntity("kitchen", "nhà bếp", "/kˈɪtʃən/", "noun", "The kitchen is clean.", "Nhà bếp sạch.", lessonId = 16, difficulty = 1, category = "home"),
                WordEntity("living room", "phòng khách", "/lˈɪvɪŋ ɹˈum/", "noun", "We sit in the living room.", "Chúng tôi ngồi ở phòng khách.", lessonId = 16, difficulty = 1, category = "home"),
                WordEntity("bedroom", "phòng ngủ", "/bˈɛdɹˌum/", "noun", "The bedroom is cozy.", "Phòng ngủ ấm cúng.", lessonId = 16, difficulty = 1, category = "home"),
                WordEntity("vacuum", "hút bơi", "/vˈækjum/", "verb", "Please vacuum the floor.", "Làm ơn hút bơi sàn.", lessonId = 16, difficulty = 1, category = "home"),
                WordEntity("laundry", "giặt đồ", "/lˈɔndɹi/", "noun", "Do the laundry on Sunday.", "Giặt đồ vào Chủ nhật.", lessonId = 16, difficulty = 1, category = "home"),
                WordEntity("cleaning", "dọn dẹp", "/klˈinɪŋ/", "noun", "Cleaning takes time.", "Dọn dẹp một thời gian.", lessonId = 16, difficulty = 1, category = "home"),
                
                WordEntity("reading", "đọc sách", "/ɹˈidɪŋ/", "noun", "Reading is relaxing.", "Đọc sách giúp thư giãn.", lessonId = 17, difficulty = 1, category = "hobby"),
                WordEntity("painting", "vẽ tranh", "/pˈeɪntɪŋ/", "noun", "I like painting.", "Tôi thích vẽ tranh.", lessonId = 17, difficulty = 1, category = "hobby"),
                WordEntity("hiking", "đi bộ đường dài", "/hˈaɪkɪŋ/", "noun", "We go hiking on weekends.", "Chúng tôi đi bộ đường dài cuối tuần.", lessonId = 17, difficulty = 1, category = "hobby"),
                WordEntity("playing guitar", "chơi guitar", "/plˈeɪɪŋ gɪtˈɑɹ/", "verb", "He enjoys playing guitar.", "Anh ấy thích chơi guitar.", lessonId = 17, difficulty = 1, category = "hobby"),
                WordEntity("swimming", "bơi lội", "/swˈɪmɪŋ/", "noun", "Swimming is my hobby.", "Bơi lội là sở thích của tôi.", lessonId = 17, difficulty = 1, category = "hobby"),
                WordEntity("gardening", "làm vườn", "/gˈɑɹdənɪŋ/", "noun", "Gardening is peaceful.", "Làm vườn rất yên bình.", lessonId = 17, difficulty = 1, category = "hobby"),
                
                WordEntity("smartphone", "điện thoại thông minh", "/smˈɑɹtfˌoʊn/", "noun", "My smartphone is slow.", "Điện thoại thông minh của tôi chậm.", lessonId = 18, difficulty = 2, category = "technology"),
                WordEntity("laptop", "máy tính xách tay", "/lˈæptˌɑp/", "noun", "Charge your laptop.", "Sạc máy tính xách tay.", lessonId = 18, difficulty = 2, category = "technology"),
                WordEntity("charger", "sạc", "/tʃˈɑɹdʒɚ/", "noun", "I lost my charger.", "Tôi một sạc rồi.", lessonId = 18, difficulty = 2, category = "technology"),
                WordEntity("password", "mật khẩu", "/pˈæswˌɝd/", "noun", "Reset your password.", "Đặt lại mật khẩu.", lessonId = 18, difficulty = 2, category = "technology"),
                WordEntity("app", "ứng dụng", "/ˈæp/", "noun", "Download the new app.", "Tải ứng dụng mới.", lessonId = 18, difficulty = 2, category = "technology"),
                WordEntity("update", "cập nhật", "/əpdˈeɪt/", "verb", "Update the software.", "Cập nhật phần mềm.", lessonId = 18, difficulty = 2, category = "technology"),
                
                WordEntity("sunny", "nặng", "/sˈʌni/", "adjective", "It is sunny today.", "Hôm nay trời nặng.", lessonId = 19, difficulty = 1, category = "weather"),
                WordEntity("rainy", "mưa", "/ɹˈeɪni/", "adjective", "The weather is rainy.", "Trời đang mưa.", lessonId = 19, difficulty = 1, category = "weather"),
                WordEntity("storm", "bão", "/stˈɔɹm/", "noun", "A storm is coming.", "Bão đang đến.", lessonId = 19, difficulty = 2, category = "weather"),
                WordEntity("forecast", "dự báo thời tiết", "/fˈɔɹkˌæst/", "noun", "Check the forecast.", "Kiểm tra dự báo thời tiết.", lessonId = 19, difficulty = 2, category = "weather"),
                WordEntity("picnic", "đi chơi ngoài trời", "/pˈɪknˌɪk/", "noun", "Plan a picnic this weekend.", "Lên kế hoạch picnic cuối tuần này.", lessonId = 19, difficulty = 1, category = "events"),
                WordEntity("festival", "lễ hội", "/fˈɛstəvəl/", "noun", "The festival is crowded.", "Lễ hội đông đúc.", lessonId = 19, difficulty = 2, category = "events"),
                
                WordEntity("emergency", "khẩn cấp", "/ɪmˈɝdʒənsi/", "noun", "Call in an emergency.", "Gọi khi khẩn cấp.", lessonId = 20, difficulty = 3, category = "safety"),
                WordEntity("ambulance", "xe cứu thương", "/ˈæmbjələns/", "noun", "Call an ambulance.", "Gọi xe cứu thương.", lessonId = 20, difficulty = 3, category = "safety"),
                WordEntity("police", "cảnh sát", "/pəlˈis/", "noun", "Call the police.", "Gọi cảnh sát.", lessonId = 20, difficulty = 3, category = "safety"),
                WordEntity("fire", "cháy", "/fˈaɪɚ/", "noun", "There is a fire!", "Có cháy!", lessonId = 20, difficulty = 3, category = "safety"),
                WordEntity("help", "giúp đỡ", "/hˈɛlp/", "verb", "Please help me!", "Làm ơn giúp tôi!", lessonId = 20, difficulty = 2, category = "safety"),
                WordEntity("lost", "lạc đường", "/lˈɔst/", "adjective", "I am lost.", "Tôi bị lạc.", lessonId = 20, difficulty = 2, category = "directions"),
                
                WordEntity(id = 2001, word = "negotiate", translation = "thương lượng", pronunciation = "/nɪˈgoʊʃiˌeɪt/", partOfSpeech = "verb", exampleSentence = "We need to negotiate the terms.", exampleTranslation = "Chúng ta cần thương lượng điều khoản.", lessonId = 21, difficulty = 3, category = "advanced"),
                WordEntity(id = 2002, word = "compromise", translation = "thỏa hiệp", pronunciation = "/ˈkɑmprəˌmaɪz/", partOfSpeech = "noun", exampleSentence = "A compromise helps both sides.", exampleTranslation = "Thỏa hiệp giúp cả hai bên.", lessonId = 21, difficulty = 3, category = "advanced"),
                WordEntity(id = 2003, word = "assumption", translation = "giả định", pronunciation = "/əˈsʌmpʃən/", partOfSpeech = "noun", exampleSentence = "That assumption is incorrect.", exampleTranslation = "Giả định đó không đúng.", lessonId = 21, difficulty = 3, category = "advanced"),
                WordEntity(id = 2004, word = "clarify", translation = "làm rõ", pronunciation = "/ˈklærəˌfaɪ/", partOfSpeech = "verb", exampleSentence = "Please clarify your point.", exampleTranslation = "Hay làm rõ y của bạn.", lessonId = 21, difficulty = 3, category = "advanced"),
                WordEntity(id = 2005, word = "perspective", translation = "góc nhìn", pronunciation = "/pərˈspɛktɪv/", partOfSpeech = "noun", exampleSentence = "Consider another perspective.", exampleTranslation = "Hay xem tu góc nhìn khac.", lessonId = 21, difficulty = 3, category = "advanced"),
                WordEntity(id = 2006, word = "reinforce", translation = "củng cố", pronunciation = "/ˌriɪnˈfɔrs/", partOfSpeech = "verb", exampleSentence = "Examples reinforce the idea.", exampleTranslation = "Ví dụ giúp củng cố ý tưởng.", lessonId = 21, difficulty = 3, category = "advanced"),
                
                WordEntity(id = 2007, word = "inquiry", translation = "yêu cầu thông tin", pronunciation = "/ɪnˈkwaɪri/", partOfSpeech = "noun", exampleSentence = "We received an inquiry.", exampleTranslation = "Chúng tôi nhận được yêu cầu thông tin.", lessonId = 22, difficulty = 3, category = "advanced"),
                WordEntity(id = 2008, word = "deadline", translation = "thời hạn", pronunciation = "/ˈdɛdˌlaɪn/", partOfSpeech = "noun", exampleSentence = "The deadline is Friday.", exampleTranslation = "Thời hạn là thứ Sáu.", lessonId = 22, difficulty = 3, category = "advanced"),
                WordEntity(id = 2009, word = "deliverable", translation = "sản phẩm giao", pronunciation = "/dɪˈlɪvərəbəl/", partOfSpeech = "noun", exampleSentence = "This is a key deliverable.", exampleTranslation = "Đây là sản phẩm giao quan trọng.", lessonId = 22, difficulty = 3, category = "advanced"),
                WordEntity(id = 2010, word = "proposal", translation = "đề xuất", pronunciation = "/prəˈpoʊzəl/", partOfSpeech = "noun", exampleSentence = "Send the proposal today.", exampleTranslation = "Gui đề xuất hom nay.", lessonId = 22, difficulty = 3, category = "advanced"),
                WordEntity(id = 2011, word = "follow-up", translation = "theo dõi", pronunciation = "/ˈfɑloʊˌʌp/", partOfSpeech = "noun", exampleSentence = "I will send a follow-up.", exampleTranslation = "Tôi sẽ gửi thư theo dõi.", lessonId = 22, difficulty = 3, category = "advanced"),
                WordEntity(id = 2012, word = "stakeholder", translation = "bên liên quan", pronunciation = "/ˈsteɪkˌhoʊldər/", partOfSpeech = "noun", exampleSentence = "Update the stakeholders.", exampleTranslation = "Cập nhật cho các bên liên quan.", lessonId = 22, difficulty = 3, category = "advanced"),
                
                WordEntity(id = 2013, word = "hypothesis", translation = "giả thuyết", pronunciation = "/haɪˈpɑθəsɪs/", partOfSpeech = "noun", exampleSentence = "Test the hypothesis.", exampleTranslation = "Kiểm chứng giả thuyết.", lessonId = 23, difficulty = 3, category = "advanced"),
                WordEntity(id = 2014, word = "evidence", translation = "bằng chứng", pronunciation = "/ˈɛvɪdəns/", partOfSpeech = "noun", exampleSentence = "We need more evidence.", exampleTranslation = "Chúng ta cần thêm bằng chứng.", lessonId = 23, difficulty = 3, category = "advanced"),
                WordEntity(id = 2015, word = "methodology", translation = "phương pháp", pronunciation = "/ˌmɛθəˈdɑlədʒi/", partOfSpeech = "noun", exampleSentence = "Describe the methodology.", exampleTranslation = "Mô tả phương pháp.", lessonId = 23, difficulty = 3, category = "advanced"),
                WordEntity(id = 2016, word = "critique", translation = "phê bình", pronunciation = "/krɪˈtik/", partOfSpeech = "noun", exampleSentence = "Write a brief critique.", exampleTranslation = "Viet mot bai phê bình ngan.", lessonId = 23, difficulty = 3, category = "advanced"),
                WordEntity(id = 2017, word = "abstract", translation = "tóm tắt", pronunciation = "/ˈæbstrækt/", partOfSpeech = "noun", exampleSentence = "Read the abstract first.", exampleTranslation = "Đọc phần tóm tắt trước.", lessonId = 23, difficulty = 3, category = "advanced"),
                WordEntity(id = 2018, word = "citation", translation = "trích dẫn", pronunciation = "/saɪˈteɪʃən/", partOfSpeech = "noun", exampleSentence = "Add a proper citation.", exampleTranslation = "Them trích dẫn dung cach.", lessonId = 23, difficulty = 3, category = "advanced"),

                WordEntity(id = 2101, word = "spring", translation = "mùa xuân", pronunciation = "/sprɪŋ/", partOfSpeech = "noun", exampleSentence = "Spring is warm.", exampleTranslation = "Mùa xuân ấm áp.", lessonId = 24, difficulty = 2, category = "nature"),
                WordEntity(id = 2102, word = "summer", translation = "mùa hè", pronunciation = "/ˈsʌmɚ/", partOfSpeech = "noun", exampleSentence = "Summer is hot.", exampleTranslation = "Mùa hè nóng.", lessonId = 24, difficulty = 2, category = "nature"),
                WordEntity(id = 2103, word = "autumn", translation = "mùa thu", pronunciation = "/ˈɔtəm/", partOfSpeech = "noun", exampleSentence = "Autumn is cool.", exampleTranslation = "Mùa thu mát.", lessonId = 24, difficulty = 2, category = "nature"),
                WordEntity(id = 2104, word = "winter", translation = "mùa đông", pronunciation = "/ˈwɪntɚ/", partOfSpeech = "noun", exampleSentence = "Winter is cold.", exampleTranslation = "Mùa đông lạnh.", lessonId = 24, difficulty = 2, category = "nature"),
                WordEntity(id = 2105, word = "warm", translation = "ấm áp", pronunciation = "/wɔrm/", partOfSpeech = "adjective", exampleSentence = "The weather is warm.", exampleTranslation = "Thời tiết ấm áp.", lessonId = 24, difficulty = 2, category = "nature"),
                WordEntity(id = 2106, word = "cold", translation = "lạnh", pronunciation = "/koʊld/", partOfSpeech = "adjective", exampleSentence = "It is cold today.", exampleTranslation = "Hôm nay trời lạnh.", lessonId = 24, difficulty = 2, category = "nature"),

                WordEntity(id = 2107, word = "happy", translation = "vui", pronunciation = "/ˈhæpi/", partOfSpeech = "adjective", exampleSentence = "I feel happy today.", exampleTranslation = "Hôm nay tôi cảm thấy vui.", lessonId = 25, difficulty = 2, category = "lifestyle"),
                WordEntity(id = 2108, word = "sad", translation = "buồn", pronunciation = "/sæd/", partOfSpeech = "adjective", exampleSentence = "She feels sad.", exampleTranslation = "Cô ấy cảm thấy buồn.", lessonId = 25, difficulty = 2, category = "lifestyle"),
                WordEntity(id = 2109, word = "angry", translation = "tức giận", pronunciation = "/ˈæŋgri/", partOfSpeech = "adjective", exampleSentence = "He is angry.", exampleTranslation = "Anh ấy tức giận.", lessonId = 25, difficulty = 2, category = "lifestyle"),
                WordEntity(id = 2110, word = "excited", translation = "háo hức", pronunciation = "/ɪkˈsaɪtɪd/", partOfSpeech = "adjective", exampleSentence = "We are excited.", exampleTranslation = "Chúng tôi háo hức.", lessonId = 25, difficulty = 2, category = "lifestyle"),
                WordEntity(id = 2111, word = "worried", translation = "lo lắng", pronunciation = "/ˈwɝid/", partOfSpeech = "adjective", exampleSentence = "I am worried about the test.", exampleTranslation = "Tôi lo lắng về bài kiểm tra.", lessonId = 25, difficulty = 2, category = "lifestyle"),
                WordEntity(id = 2112, word = "tired", translation = "mệt mỏi", pronunciation = "/ˈtaɪɚd/", partOfSpeech = "adjective", exampleSentence = "I am tired.", exampleTranslation = "Tôi mệt mỏi.", lessonId = 25, difficulty = 2, category = "lifestyle"),

                WordEntity(id = 2113, word = "size", translation = "kích cỡ", pronunciation = "/saɪz/", partOfSpeech = "noun", exampleSentence = "What size do you need?", exampleTranslation = "Bạn cần kích cỡ nào?", lessonId = 26, difficulty = 2, category = "shopping"),
                WordEntity(id = 2114, word = "fit", translation = "vừa", pronunciation = "/fɪt/", partOfSpeech = "verb", exampleSentence = "This shirt fits me.", exampleTranslation = "Áo này vừa với tôi.", lessonId = 26, difficulty = 2, category = "shopping"),
                WordEntity(id = 2115, word = "refund", translation = "hoàn tiền", pronunciation = "/ˈriˌfʌnd/", partOfSpeech = "noun", exampleSentence = "Can I get a refund?", exampleTranslation = "Tôi có thể hoàn tiền không?", lessonId = 26, difficulty = 2, category = "shopping"),
                WordEntity(id = 2116, word = "quality", translation = "chất lượng", pronunciation = "/ˈkwɑləti/", partOfSpeech = "noun", exampleSentence = "The quality is good.", exampleTranslation = "Chất lượng tốt.", lessonId = 26, difficulty = 2, category = "shopping"),
                WordEntity(id = 2117, word = "brand", translation = "thương hiệu", pronunciation = "/brænd/", partOfSpeech = "noun", exampleSentence = "This brand is popular.", exampleTranslation = "Thương hiệu này phổ biến.", lessonId = 26, difficulty = 2, category = "shopping"),
                WordEntity(id = 2118, word = "price tag", translation = "nhãn giá", pronunciation = "/ˈpraɪs ˌtæg/", partOfSpeech = "noun", exampleSentence = "Check the price tag.", exampleTranslation = "Kiểm tra nhãn giá.", lessonId = 26, difficulty = 2, category = "shopping"),

                WordEntity(id = 2119, word = "intersection", translation = "ngã tư", pronunciation = "/ˌɪntɚˈsɛkʃən/", partOfSpeech = "noun", exampleSentence = "Turn left at the intersection.", exampleTranslation = "Rẽ trái ở ngã tư.", lessonId = 27, difficulty = 2, category = "travel"),
                WordEntity(id = 2120, word = "turn right", translation = "rẽ phải", pronunciation = "/tɝn raɪt/", partOfSpeech = "phrase", exampleSentence = "Turn right here.", exampleTranslation = "Rẽ phải ở đây.", lessonId = 27, difficulty = 2, category = "travel"),
                WordEntity(id = 2121, word = "crosswalk", translation = "vạch qua đường", pronunciation = "/ˈkrɔsˌwɔk/", partOfSpeech = "noun", exampleSentence = "Use the crosswalk.", exampleTranslation = "Hãy đi qua vạch qua đường.", lessonId = 27, difficulty = 2, category = "travel"),
                WordEntity(id = 2122, word = "near", translation = "gần", pronunciation = "/nɪr/", partOfSpeech = "adjective", exampleSentence = "The bank is near.", exampleTranslation = "Ngân hàng ở gần.", lessonId = 27, difficulty = 2, category = "travel"),
                WordEntity(id = 2123, word = "far", translation = "xa", pronunciation = "/fɑr/", partOfSpeech = "adjective", exampleSentence = "The station is far.", exampleTranslation = "Nhà ga ở xa.", lessonId = 27, difficulty = 2, category = "travel"),
                WordEntity(id = 2124, word = "opposite", translation = "đối diện", pronunciation = "/ˈɑpəzɪt/", partOfSpeech = "adjective", exampleSentence = "The shop is opposite the park.", exampleTranslation = "Cửa hàng đối diện công viên.", lessonId = 27, difficulty = 2, category = "travel"),

                WordEntity(id = 2125, word = "agenda", translation = "chương trình", pronunciation = "/əˈdʒɛndə/", partOfSpeech = "noun", exampleSentence = "Review the agenda.", exampleTranslation = "Xem chương trình họp.", lessonId = 28, difficulty = 2, category = "work"),
                WordEntity(id = 2126, word = "schedule", translation = "lịch trình", pronunciation = "/ˈskɛdʒuːl/", partOfSpeech = "noun", exampleSentence = "The schedule is tight.", exampleTranslation = "Lịch trình dày.", lessonId = 28, difficulty = 2, category = "work"),
                WordEntity(id = 2127, word = "minutes", translation = "biên bản", pronunciation = "/ˈmɪnɪts/", partOfSpeech = "noun", exampleSentence = "Send the meeting minutes.", exampleTranslation = "Gửi biên bản cuộc họp.", lessonId = 28, difficulty = 2, category = "work"),
                WordEntity(id = 2128, word = "presenter", translation = "người trình bày", pronunciation = "/prɪˈzɛntɚ/", partOfSpeech = "noun", exampleSentence = "The presenter is ready.", exampleTranslation = "Người trình bày đã sẵn sàng.", lessonId = 28, difficulty = 2, category = "work"),
                WordEntity(id = 2129, word = "decision", translation = "quyết định", pronunciation = "/dɪˈsɪʒən/", partOfSpeech = "noun", exampleSentence = "We made a decision.", exampleTranslation = "Chúng ta đã đưa ra quyết định.", lessonId = 28, difficulty = 2, category = "work"),
                WordEntity(id = 2130, word = "action item", translation = "việc cần làm", pronunciation = "/ˈækʃən ˌaɪtəm/", partOfSpeech = "noun", exampleSentence = "List the action items.", exampleTranslation = "Liệt kê các việc cần làm.", lessonId = 28, difficulty = 2, category = "work"),

                WordEntity(id = 2131, word = "healthy", translation = "khỏe mạnh", pronunciation = "/ˈhɛlθi/", partOfSpeech = "adjective", exampleSentence = "She stays healthy.", exampleTranslation = "Cô ấy luôn khỏe mạnh.", lessonId = 29, difficulty = 2, category = "health"),
                WordEntity(id = 2132, word = "stress", translation = "căng thẳng", pronunciation = "/strɛs/", partOfSpeech = "noun", exampleSentence = "Stress is bad.", exampleTranslation = "Căng thẳng không tốt.", lessonId = 29, difficulty = 2, category = "health"),
                WordEntity(id = 2133, word = "diet", translation = "chế độ ăn", pronunciation = "/ˈdaɪət/", partOfSpeech = "noun", exampleSentence = "Follow a balanced diet.", exampleTranslation = "Theo chế độ ăn cân bằng.", lessonId = 29, difficulty = 2, category = "health"),
                WordEntity(id = 2134, word = "recovery", translation = "hồi phục", pronunciation = "/rɪˈkʌvɚi/", partOfSpeech = "noun", exampleSentence = "Rest helps recovery.", exampleTranslation = "Nghỉ ngơi giúp hồi phục.", lessonId = 29, difficulty = 2, category = "health"),
                WordEntity(id = 2135, word = "symptom", translation = "triệu chứng", pronunciation = "/ˈsɪmptəm/", partOfSpeech = "noun", exampleSentence = "Describe your symptoms.", exampleTranslation = "Mô tả triệu chứng của bạn.", lessonId = 29, difficulty = 2, category = "health"),
                WordEntity(id = 2136, word = "hydrate", translation = "uống đủ nước", pronunciation = "/ˈhaɪdreɪt/", partOfSpeech = "verb", exampleSentence = "Remember to hydrate.", exampleTranslation = "Nhớ uống đủ nước.", lessonId = 29, difficulty = 2, category = "health"),

                WordEntity(id = 2137, word = "agree", translation = "đồng ý", pronunciation = "/əˈgri/", partOfSpeech = "verb", exampleSentence = "I agree with you.", exampleTranslation = "Tôi đồng ý với bạn.", lessonId = 30, difficulty = 3, category = "advanced"),
                WordEntity(id = 2138, word = "disagree", translation = "không đồng ý", pronunciation = "/ˌdɪsəˈgri/", partOfSpeech = "verb", exampleSentence = "I disagree.", exampleTranslation = "Tôi không đồng ý.", lessonId = 30, difficulty = 3, category = "advanced"),
                WordEntity(id = 2139, word = "opinion", translation = "ý kiến", pronunciation = "/əˈpɪnjən/", partOfSpeech = "noun", exampleSentence = "Share your opinion.", exampleTranslation = "Chia sẻ ý kiến của bạn.", lessonId = 30, difficulty = 3, category = "advanced"),
                WordEntity(id = 2140, word = "suggest", translation = "đề xuất", pronunciation = "/səˈdʒɛst/", partOfSpeech = "verb", exampleSentence = "I suggest a break.", exampleTranslation = "Tôi đề xuất nghỉ một chút.", lessonId = 30, difficulty = 3, category = "advanced"),
                WordEntity(id = 2141, word = "explain", translation = "giải thích", pronunciation = "/ɪkˈspleɪn/", partOfSpeech = "verb", exampleSentence = "Please explain.", exampleTranslation = "Hãy giải thích.", lessonId = 30, difficulty = 3, category = "advanced"),
                WordEntity(id = 2142, word = "prefer", translation = "ưu tiên", pronunciation = "/prɪˈfɝ/", partOfSpeech = "verb", exampleSentence = "I prefer this option.", exampleTranslation = "Tôi ưu tiên lựa chọn này.", lessonId = 30, difficulty = 3, category = "advanced"),

                WordEntity(
                    id = 1001,
                    word = "good morning",
                    translation = "chào buổi sáng",
                    pronunciation = "/gˈʊd mˈɔɹnɪŋ/",
                    partOfSpeech = "phrase",
                    exampleSentence = "Good morning, class!",
                    exampleTranslation = "Chào buổi sáng cả lớp!",
                    lessonId = 1,
                    difficulty = 1,
                    category = "greetings"
                ),
                WordEntity(
                    id = 1002,
                    word = "good afternoon",
                    translation = "chào buổi chiều",
                    pronunciation = "/gˈʊd ˌæftɚnˈun/",
                    partOfSpeech = "phrase",
                    exampleSentence = "Good afternoon, how are you?",
                    exampleTranslation = "Chào buổi chiều, bạn khỏe không?",
                    lessonId = 1,
                    difficulty = 1,
                    category = "greetings"
                ),
                WordEntity(
                    id = 1003,
                    word = "good evening",
                    translation = "chào buổi tối",
                    pronunciation = "/gˈʊd ˈivnɪŋ/",
                    partOfSpeech = "phrase",
                    exampleSentence = "Good evening everyone.",
                    exampleTranslation = "Chào buổi tối mọi người.",
                    lessonId = 1,
                    difficulty = 1,
                    category = "greetings"
                ),
                WordEntity(
                    id = 1004,
                    word = "good night",
                    translation = "chúc ngủ ngon",
                    pronunciation = "/gˈʊd nˈaɪt/",
                    partOfSpeech = "phrase",
                    exampleSentence = "Good night and sweet dreams.",
                    exampleTranslation = "Chúc ngủ ngon và mơ đẹp.",
                    lessonId = 1,
                    difficulty = 1,
                    category = "greetings"
                ),
                WordEntity(
                    id = 1005,
                    word = "nice",
                    translation = "tốt, dễ chịu",
                    pronunciation = "/nˈaɪs/",
                    partOfSpeech = "adjective",
                    exampleSentence = "It is nice to meet you.",
                    exampleTranslation = "Rất vui được gặp bạn.",
                    lessonId = 1,
                    difficulty = 1,
                    category = "introductions"
                ),
                WordEntity(
                    id = 1006,
                    word = "meet",
                    translation = "gặp",
                    pronunciation = "/mˈit/",
                    partOfSpeech = "verb",
                    exampleSentence = "I want to meet new friends.",
                    exampleTranslation = "Tôi muốn gặp bạn mới.",
                    lessonId = 1,
                    difficulty = 1,
                    category = "introductions"
                ),
                WordEntity(
                    id = 1007,
                    word = "sorry",
                    translation = "xin lỗi",
                    pronunciation = "/sˈɑɹi/",
                    partOfSpeech = "adjective",
                    exampleSentence = "I am sorry for being late.",
                    exampleTranslation = "Tôi xin lỗi vì đến trễ.",
                    lessonId = 1,
                    difficulty = 1,
                    category = "politeness"
                ),
                WordEntity(
                    id = 1008,
                    word = "excuse me",
                    translation = "xin phép / xin lỗi",
                    pronunciation = "/ɪkskjˈus mˈi/",
                    partOfSpeech = "phrase",
                    exampleSentence = "Excuse me, where is the bus stop?",
                    exampleTranslation = "Xin lỗi, trạm xe ở đâu?",
                    lessonId = 1,
                    difficulty = 1,
                    category = "politeness"
                )
            )
        }
        private fun getInitialExercises(): List<ExerciseEntity> {
            return listOf(
                ExerciseEntity(
                    lessonId = 1, wordId = 1, type = "MULTIPLE_CHOICE",
                    question = "What is 'xin chào' in Englishọ",
                    correctAnswer = "hello",
                    optionA = "hello", optionB = "goodbye", optionC = "thank you", optionD = "please",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 3, type = "MULTIPLE_CHOICE",
                    question = "How do you say 'tạm biệt'?",
                    correctAnswer = "goodbye",
                    optionA = "hello", optionB = "goodbye", optionC = "yes", optionD = "no",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 5, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'cảm ơn'?",
                    correctAnswer = "thank you",
                    optionA = "please", optionB = "thank you", optionC = "sorry", optionD = "hi",
                    order = 3, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 12, type = "FILL_BLANK",
                    question = "My ____ is Anna.",
                    correctAnswer = "name",
                    hint = "Use it to introduce yourself",
                    order = 4, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 8, type = "FILL_BLANK",
                    question = "_____ am a student.",
                    correctAnswer = "I",
                    hint = "First person pronoun",
                    order = 5, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 10, type = "TRANSLATION",
                    question = "Translate: Tôi là Nam.",
                    correctAnswer = "I am Nam",
                    order = 6, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 6, type = "MATCHING",
                    question = "Match the greetings",
                    correctAnswer = "",
                    matchPairs = """[
                        {"left":"hello","right":"xin chào"},
                        {"left":"goodbye","right":"tạm biệt"},
                        {"left":"please","right":"làm ơn"},
                        {"left":"yes","right":"vâng"}
                    ]""".trimIndent(),
                    order = 7, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 7, type = "LISTENING",
                    question = "Choose what you hear",
                    correctAnswer = "no",
                    optionA = "no", optionB = "yes", optionC = "hello", optionD = "thank you",
                    order = 8, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 1001, type = "MULTIPLE_CHOICE",
                    question = "How do you say 'chào buổi sáng'?",
                    correctAnswer = "good morning",
                    optionA = "good morning", optionB = "good evening", optionC = "good night", optionD = "goodbye",
                    order = 9, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 1005, type = "FILL_BLANK",
                    question = "It is ____ to meet you.",
                    correctAnswer = "nice",
                    hint = "Think of a polite compliment",
                    order = 10, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 1007, type = "LISTENING",
                    question = "Choose the phrase meaning 'xin lỗi'",
                    correctAnswer = "sorry",
                    optionA = "thank you", optionB = "sorry", optionC = "please", optionD = "excuse me",
                    order = 11, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 1004, type = "TRANSLATION",
                    question = "Dịch: Chúc ngủ ngon.",
                    correctAnswer = "Good night",
                    order = 12, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 9, type = "WORD_TILES",
                    question = "Dịch: Bạn là học sinh.",
                    correctAnswer = "You are a student",
                    matchPairs = """["You","are","a","student"]""",
                    order = 13, difficulty = 1
                ),
                
                ExerciseEntity(
                    lessonId = 2, wordId = 13, type = "MULTIPLE_CHOICE",
                    question = "How do you say 'anh ấy'?",
                    correctAnswer = "he",
                    optionA = "he", optionB = "she", optionC = "they", optionD = "we",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 2, wordId = 14, type = "MULTIPLE_CHOICE",
                    question = "Translate 'cô ấy'",
                    correctAnswer = "she",
                    optionA = "he", optionB = "she", optionC = "girl", optionD = "woman",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 2, wordId = 15, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'chúng tôi'?",
                    correctAnswer = "we",
                    optionA = "they", optionB = "you", optionC = "we", optionD = "I",
                    order = 3, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 2, wordId = 21, type = "FILL_BLANK",
                    question = "They ____ breakfast.",
                    correctAnswer = "eat",
                    hint = "Use the base verb",
                    order = 4, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 2, wordId = 22, type = "FILL_BLANK",
                    question = "We ____ coffee.",
                    correctAnswer = "drink",
                    hint = "Think of beverages",
                    order = 5, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 2, wordId = 20, type = "TRANSLATION",
                    question = "Dịch: Đây là một cô gái.",
                    correctAnswer = "This is a girl",
                    order = 6, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 2, wordId = 16, type = "MATCHING",
                    question = "Match pronouns",
                    correctAnswer = "",
                    matchPairs = """[
                        {"left":"he","right":"anh ấy"},
                        {"left":"she","right":"cô ấy"},
                        {"left":"we","right":"chúng tôi"},
                        {"left":"they","right":"họ"}
                    ]""".trimIndent(),
                    order = 7, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 2, wordId = 24, type = "LISTENING",
                    question = "Choose the word for 'viết'",
                    correctAnswer = "write",
                    optionA = "read", optionB = "write", optionC = "eat", optionD = "woman",
                    order = 8, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 2, wordId = 15, type = "WORD_TILES",
                    question = "Dịch: Chúng tôi uống cà phê.",
                    correctAnswer = "We drink coffee",
                    matchPairs = """["We","drink","coffee"]""",
                    order = 9, difficulty = 1
                ),
                
                ExerciseEntity(
                    lessonId = 3, wordId = 26, type = "MULTIPLE_CHOICE",
                    question = "How do you say 'xin lỗi' politely?",
                    correctAnswer = "sorry",
                    optionA = "sorry", optionB = "welcome", optionC = "good luck", optionD = "good night",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 3, wordId = 27, type = "MULTIPLE_CHOICE",
                    question = "Translate 'chào buổi sáng'",
                    correctAnswer = "good morning",
                    optionA = "good morning", optionB = "good night", optionC = "see you later", optionD = "excuse me",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 3, wordId = 28, type = "MULTIPLE_CHOICE",
                    question = "What is 'chúc ngủ ngon'?",
                    correctAnswer = "good night",
                    optionA = "good morning", optionB = "good night", optionC = "welcome", optionD = "good luck",
                    order = 3, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 3, wordId = 29, type = "TRANSLATION",
                    question = "Dịch: Hẹn gặp lại sau.",
                    correctAnswer = "See you later",
                    order = 4, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 3, wordId = 34, type = "TRANSLATION",
                    question = "Dịch: Rất vui được gặp bạn.",
                    correctAnswer = "Nice to meet you",
                    order = 5, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 3, wordId = 32, type = "FILL_BLANK",
                    question = "How are you? _____.",
                    correctAnswer = "I am fine",
                    hint = "Short answer with 'fine'",
                    order = 6, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 3, wordId = 35, type = "MATCHING",
                    question = "Match the phrases",
                    correctAnswer = "",
                    matchPairs = """[
                        {"left":"welcome","right":"chào mừng"},
                        {"left":"good luck","right":"chúc may mắn"},
                        {"left":"good night","right":"chúc ngủ ngon"},
                        {"left":"excuse me","right":"xin lỗi"}
                    ]""".trimIndent(),
                    order = 7, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 3, wordId = 27, type = "LISTENING",
                    question = "Listen and pick the correct phrase",
                    correctAnswer = "good morning",
                    optionA = "good morning", optionB = "good night", optionC = "see you soon", optionD = "sorry",
                    order = 8, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 3, wordId = 34, type = "WORD_TILES",
                    question = "Dịch: Rất vui được gặp bạn.",
                    correctAnswer = "Nice to meet you",
                    matchPairs = """["Nice","to","meet","you"]""",
                    order = 9, difficulty = 1
                ),
                
                ExerciseEntity(
                    lessonId = 4, wordId = 37, type = "MULTIPLE_CHOICE",
                    question = "What is 'nước'?",
                    correctAnswer = "water",
                    optionA = "water", optionB = "coffee", optionC = "tea", optionD = "soup",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 4, wordId = 38, type = "MULTIPLE_CHOICE",
                    question = "Translate 'cà phê'",
                    correctAnswer = "coffee",
                    optionA = "coffee", optionB = "bread", optionC = "rice", optionD = "fish",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 4, wordId = 41, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'cơm'?",
                    correctAnswer = "rice",
                    optionA = "rice", optionB = "bread", optionC = "apple", optionD = "tea",
                    order = 3, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 4, wordId = 39, type = "FILL_BLANK",
                    question = "I would like a cup of ____.",
                    correctAnswer = "tea",
                    hint = "A hot drink",
                    order = 4, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 4, wordId = 42, type = "FILL_BLANK",
                    question = "The ____ is hot.",
                    correctAnswer = "soup",
                    hint = "A warm dish",
                    order = 5, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 4, wordId = 48, type = "TRANSLATION",
                    question = "Dịch: Tôi đang đói.",
                    correctAnswer = "I am hungry",
                    order = 6, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 4, wordId = 43, type = "MATCHING",
                    question = "Match the foods",
                    correctAnswer = "",
                    matchPairs = """[
                        {"left":"apple","right":"táo"},
                        {"left":"banana","right":"chuối"},
                        {"left":"chicken","right":"gà"},
                        {"left":"fish","right":"cá"}
                    ]""".trimIndent(),
                    order = 7, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 4, wordId = 47, type = "LISTENING",
                    question = "Listen and choose the word",
                    correctAnswer = "delicious",
                    optionA = "hungry", optionB = "delicious", optionC = "water", optionD = "rice",
                    order = 8, difficulty = 1
                ),
                
                ExerciseEntity(
                    lessonId = 5, wordId = 54, type = "MULTIPLE_CHOICE",
                    question = "Translate 'hộ chiếu'",
                    correctAnswer = "passport",
                    optionA = "ticket", optionB = "passport", optionC = "bus", optionD = "train",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 5, wordId = 53, type = "MULTIPLE_CHOICE",
                    question = "What is 'vé'?",
                    correctAnswer = "ticket",
                    optionA = "ticket", optionB = "airport", optionC = "passport", optionD = "taxi",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 5, wordId = 52, type = "TRANSLATION",
                    question = "Dịch: Sân bay ở đâu?",
                    correctAnswer = "Where is the airport?",
                    order = 3, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 5, wordId = 50, type = "FILL_BLANK",
                    question = "I need a ____ to Hanoi.",
                    correctAnswer = "train",
                    hint = "Not a bus",
                    order = 4, difficulty = 1
                ),
                
                ExerciseEntity(
                    lessonId = 6, wordId = 55, type = "MULTIPLE_CHOICE",
                    question = "How do you say 'cha'?",
                    correctAnswer = "father",
                    optionA = "father", optionB = "mother", optionC = "son", optionD = "brother",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 6, wordId = 56, type = "MULTIPLE_CHOICE",
                    question = "Translate 'me'",
                    correctAnswer = "mother",
                    optionA = "mother", optionB = "daughter", optionC = "sister", optionD = "brother",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 6, wordId = 58, type = "FILL_BLANK",
                    question = "She is my ____.",
                    correctAnswer = "sister",
                    hint = "Female sibling",
                    order = 3, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 6, wordId = 59, type = "TRANSLATION",
                    question = "Dịch: Đây là con trai tôi.",
                    correctAnswer = "This is my son",
                    order = 4, difficulty = 1
                ),
                
                ExerciseEntity(
                    lessonId = 7, wordId = 61, type = "MULTIPLE_CHOICE",
                    question = "What color is 'đỏ'?",
                    correctAnswer = "red",
                    optionA = "red", optionB = "blue", optionC = "green", optionD = "white",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 7, wordId = 64, type = "MULTIPLE_CHOICE",
                    question = "Translate 'áo sơ mi'",
                    correctAnswer = "shirt",
                    optionA = "pants", optionB = "shirt", optionC = "shoes", optionD = "hat",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 7, wordId = 66, type = "FILL_BLANK",
                    question = "These ____ are new.",
                    correctAnswer = "shoes",
                    hint = "You wear them on your feet",
                    order = 3, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 7, wordId = 62, type = "MATCHING",
                    question = "Match colors",
                    correctAnswer = "",
                    matchPairs = """[
                        {"left":"red","right":"màu đỏ"},
                        {"left":"blue","right":"màu xanh dương"},
                        {"left":"green","right":"màu xanh lá"},
                        {"left":"shirt","right":"áo sơ mi"}
                    ]""".trimIndent(),
                    order = 4, difficulty = 1
                ),
                
                ExerciseEntity(
                    lessonId = 8, wordId = 67, type = "MULTIPLE_CHOICE",
                    question = "What number is 'một'?",
                    correctAnswer = "one",
                    optionA = "one", optionB = "two", optionC = "three", optionD = "four",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 8, wordId = 69, type = "MULTIPLE_CHOICE",
                    question = "Translate 'ba'",
                    correctAnswer = "three",
                    optionA = "two", optionB = "three", optionC = "one", optionD = "today",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 8, wordId = 71, type = "FILL_BLANK",
                    question = "See you ____.",
                    correctAnswer = "tomorrow",
                    hint = "Not today",
                    order = 3, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 8, wordId = 72, type = "TRANSLATION",
                    question = "Dịch: Hôm qua tôi rất bận.",
                    correctAnswer = "Yesterday I was busy",
                    order = 4, difficulty = 1
                ),
                
                ExerciseEntity(
                    lessonId = 9, wordId = 73, type = "MULTIPLE_CHOICE",
                    question = "How do you say 'giáo viên'?",
                    correctAnswer = "teacher",
                    optionA = "student", optionB = "teacher", optionC = "job", optionD = "office",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 9, wordId = 74, type = "MULTIPLE_CHOICE",
                    question = "Translate 'học sinh'",
                    correctAnswer = "student",
                    optionA = "student", optionB = "teacher", optionC = "meeting", optionD = "office",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 9, wordId = 75, type = "FILL_BLANK",
                    question = "She goes to ____ every day.",
                    correctAnswer = "school",
                    hint = "A place to study",
                    order = 3, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 9, wordId = 78, type = "TRANSLATION",
                    question = "Dịch: Tôi có một cuộc họp.",
                    correctAnswer = "I have a meeting",
                    order = 4, difficulty = 1
                ),
                
                ExerciseEntity(
                    lessonId = 10, wordId = 79, type = "MULTIPLE_CHOICE",
                    question = "How do you say 'ngủ dậy'?",
                    correctAnswer = "wake up",
                    optionA = "wake up", optionB = "sleep", optionC = "breakfast", optionD = "dinner",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 10, wordId = 80, type = "MULTIPLE_CHOICE",
                    question = "Translate 'buổi sẵng' as a meal",
                    correctAnswer = "breakfast",
                    optionA = "lunch", optionB = "dinner", optionC = "breakfast", optionD = "exercise",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 10, wordId = 83, type = "FILL_BLANK",
                    question = "I ____ after dinner.",
                    correctAnswer = "sleep",
                    hint = "End of the day",
                    order = 3, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 10, wordId = 84, type = "TRANSLATION",
                    question = "Dịch: Tôi tập thể dục mỗi ngày.",
                    correctAnswer = "I exercise every day",
                    order = 4, difficulty = 1
                ),
                
                ExerciseEntity(
                    lessonId = 11, wordId = 85, type = "MULTIPLE_CHOICE",
                    question = "Where is the 'trạm xe buýt'?",
                    correctAnswer = "bus stop",
                    optionA = "bus stop", optionB = "station", optionC = "traffic jam", optionD = "ticket booth",
                    order = 1, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 11, wordId = 88, type = "MULTIPLE_CHOICE",
                    question = "Translate 'rẽ trời'",
                    correctAnswer = "turn left",
                    optionA = "turn left", optionB = "turn right", optionC = "straight ahead", optionD = "stop",
                    order = 2, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 11, wordId = 89, type = "FILL_BLANK",
                    question = "Go ______ for 200 meters.",
                    correctAnswer = "straight ahead",
                    hint = "No turns",
                    order = 3, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 11, wordId = 90, type = "TRANSLATION",
                    question = "Dịch: Đang kẹt xe.",
                    correctAnswer = "There is a traffic jam",
                    order = 4, difficulty = 2
                ),
                
                ExerciseEntity(
                    lessonId = 12, wordId = 91, type = "MULTIPLE_CHOICE",
                    question = "What is 'gia' in Englishọ",
                    correctAnswer = "price",
                    optionA = "price", optionB = "discount", optionC = "receipt", optionD = "cash",
                    order = 1, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 12, wordId = 95, type = "MULTIPLE_CHOICE",
                    question = "Translate 'hóa đơn'",
                    correctAnswer = "receipt",
                    optionA = "receipt", optionB = "card", optionC = "cash", optionD = "price",
                    order = 2, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 12, wordId = 93, type = "FILL_BLANK",
                    question = "I pay with ____.",
                    correctAnswer = "cash",
                    hint = "Not card",
                    order = 3, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 12, wordId = 92, type = "TRANSLATION",
                    question = "Dịch: Bạn có giảm giá không?",
                    correctAnswer = "Do you have a discount?",
                    order = 4, difficulty = 2
                ),
                
                ExerciseEntity(
                    lessonId = 13, wordId = 97, type = "MULTIPLE_CHOICE",
                    question = "Can I see the _____?",
                    correctAnswer = "menu",
                    optionA = "menu", optionB = "bill", optionC = "tip", optionD = "reservation",
                    order = 1, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 13, wordId = 100, type = "MULTIPLE_CHOICE",
                    question = "Translate 'hóa đơn' in a restaurant",
                    correctAnswer = "bill",
                    optionA = "bill", optionB = "order", optionC = "menu", optionD = "tip",
                    order = 2, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 13, wordId = 98, type = "FILL_BLANK",
                    question = "We would like to ______ now.",
                    correctAnswer = "order",
                    hint = "Place a request",
                    order = 3, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 13, wordId = 101, type = "TRANSLATION",
                    question = "Dịch: Để lại chút tiền tip.",
                    correctAnswer = "Leave a small tip",
                    order = 4, difficulty = 2
                ),
                
                ExerciseEntity(
                    lessonId = 14, wordId = 103, type = "MULTIPLE_CHOICE",
                    question = "Translate 'sot'",
                    correctAnswer = "fever",
                    optionA = "fever", optionB = "cough", optionC = "headache", optionD = "rest",
                    order = 1, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 14, wordId = 105, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'đau đầu'?",
                    correctAnswer = "headache",
                    optionA = "headache", optionB = "medicine", optionC = "appointment", optionD = "rest",
                    order = 2, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 14, wordId = 106, type = "FILL_BLANK",
                    question = "Take this ______ twice a day.",
                    correctAnswer = "medicine",
                    hint = "Treatment",
                    order = 3, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 14, wordId = 108, type = "TRANSLATION",
                    question = "Dịch: Bạn nên nghỉ ngơi hôm nay.",
                    correctAnswer = "You should rest today",
                    order = 4, difficulty = 2
                ),
                
                ExerciseEntity(
                    lessonId = 15, wordId = 109, type = "MULTIPLE_CHOICE",
                    question = "'Phòng họp' là gì?",
                    correctAnswer = "meeting room",
                    optionA = "meeting room", optionB = "deadline", optionC = "task", optionD = "report",
                    order = 1, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 15, wordId = 110, type = "MULTIPLE_CHOICE",
                    question = "Translate 'hẹn chọt'",
                    correctAnswer = "deadline",
                    optionA = "deadline", optionB = "task", optionC = "report", optionD = "present",
                    order = 2, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 15, wordId = 113, type = "FILL_BLANK",
                    question = "Send the weekly _____ by Friday.",
                    correctAnswer = "report",
                    hint = "Document",
                    order = 3, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 15, wordId = 114, type = "TRANSLATION",
                    question = "Dịch: Tôi sẽ trình bày hôm nay.",
                    correctAnswer = "I will present today",
                    order = 4, difficulty = 2
                ),
                
                ExerciseEntity(
                    lessonId = 16, wordId = 115, type = "MULTIPLE_CHOICE",
                    question = "Translate 'nhọ bếp'",
                    correctAnswer = "kitchen",
                    optionA = "kitchen", optionB = "bedroom", optionC = "living room", optionD = "laundry",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 16, wordId = 117, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'phòng ngủ'?",
                    correctAnswer = "bedroom",
                    optionA = "bedroom", optionB = "kitchen", optionC = "vacuum", optionD = "cleaning",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 16, wordId = 118, type = "FILL_BLANK",
                    question = "Please ______ the floor.",
                    correctAnswer = "vacuum",
                    hint = "Use a machine",
                    order = 3, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 16, wordId = 120, type = "TRANSLATION",
                    question = "Dịch: Dọn dẹp một thời gian.",
                    correctAnswer = "Cleaning takes time",
                    order = 4, difficulty = 1
                ),
                
                ExerciseEntity(
                    lessonId = 17, wordId = 121, type = "MULTIPLE_CHOICE",
                    question = "Translate 'đọc sách'",
                    correctAnswer = "reading",
                    optionA = "reading", optionB = "painting", optionC = "hiking", optionD = "swimming",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 17, wordId = 125, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'bơi lội'?",
                    correctAnswer = "swimming",
                    optionA = "swimming", optionB = "gardening", optionC = "painting", optionD = "reading",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 17, wordId = 123, type = "FILL_BLANK",
                    question = "We go _____ every weekend.",
                    correctAnswer = "hiking",
                    hint = "Walking long distance",
                    order = 3, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 17, wordId = 126, type = "TRANSLATION",
                    question = "Dịch: Làm vườn rất yên bình.",
                    correctAnswer = "Gardening is peaceful",
                    order = 4, difficulty = 1
                ),
                
                ExerciseEntity(
                    lessonId = 18, wordId = 127, type = "MULTIPLE_CHOICE",
                    question = "What is 'điện thoại thông minh'?",
                    correctAnswer = "smartphone",
                    optionA = "smartphone", optionB = "laptop", optionC = "charger", optionD = "password",
                    order = 1, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 18, wordId = 130, type = "MULTIPLE_CHOICE",
                    question = "Translate 'mật khẩu'",
                    correctAnswer = "password",
                    optionA = "password", optionB = "app", optionC = "update", optionD = "laptop",
                    order = 2, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 18, wordId = 129, type = "FILL_BLANK",
                    question = "I lost my ______.",
                    correctAnswer = "charger",
                    hint = "It powers the device",
                    order = 3, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 18, wordId = 132, type = "TRANSLATION",
                    question = "Dịch: Cập nhật phần mềm.",
                    correctAnswer = "Update the software",
                    order = 4, difficulty = 2
                ),
                
                ExerciseEntity(
                    lessonId = 19, wordId = 133, type = "MULTIPLE_CHOICE",
                    question = "How to say 'nắng'?",
                    correctAnswer = "sunny",
                    optionA = "sunny", optionB = "rainy", optionC = "storm", optionD = "festival",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 19, wordId = 136, type = "MULTIPLE_CHOICE",
                    question = "Translate 'dự báo thời tiết'",
                    correctAnswer = "forecast",
                    optionA = "forecast", optionB = "storm", optionC = "picnic", optionD = "sunny",
                    order = 2, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 19, wordId = 137, type = "FILL_BLANK",
                    question = "Plan a ______ this weekend.",
                    correctAnswer = "picnic",
                    hint = "Outdoor meal",
                    order = 3, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 19, wordId = 135, type = "TRANSLATION",
                    question = "Dịch: Bão đang đến.",
                    correctAnswer = "A storm is coming",
                    order = 4, difficulty = 2
                ),
                
                ExerciseEntity(
                    lessonId = 20, wordId = 139, type = "MULTIPLE_CHOICE",
                    question = "Translate 'khẩn cấp'",
                    correctAnswer = "emergency",
                    optionA = "emergency", optionB = "ambulance", optionC = "police", optionD = "fire",
                    order = 1, difficulty = 3
                ),
                ExerciseEntity(
                    lessonId = 20, wordId = 141, type = "MULTIPLE_CHOICE",
                    question = "What is 'cảnh sát'?",
                    correctAnswer = "police",
                    optionA = "police", optionB = "ambulance", optionC = "fire", optionD = "help",
                    order = 2, difficulty = 3
                ),
                ExerciseEntity(
                    lessonId = 20, wordId = 143, type = "FILL_BLANK",
                    question = "Please _____ me!",
                    correctAnswer = "help",
                    hint = "Request assistance",
                    order = 3, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 20, wordId = 140, type = "TRANSLATION",
                    question = "Dịch: Gọi xe cứu thương.",
                    correctAnswer = "Call an ambulance",
                    order = 4, difficulty = 3
                ),

                ExerciseEntity(
                    lessonId = 21, wordId = 2001, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'thương lượng'?",
                    correctAnswer = "negotiate",
                    optionA = "negotiate", optionB = "clarify", optionC = "assumption", optionD = "reinforce",
                    order = 1, difficulty = 3
                ),
                ExerciseEntity(
                    lessonId = 21, wordId = 2005, type = "MULTIPLE_CHOICE",
                    question = "Choose the word for 'góc nhìn'.",
                    correctAnswer = "perspective",
                    optionA = "compromise", optionB = "perspective", optionC = "assumption", optionD = "reinforce",
                    order = 2, difficulty = 3
                ),
                ExerciseEntity(
                    lessonId = 21, wordId = 2004, type = "FILL_BLANK",
                    question = "Please ____ your point.",
                    correctAnswer = "clarify",
                    hint = "Make it clear",
                    order = 3, difficulty = 3
                ),
                ExerciseEntity(
                    lessonId = 21, wordId = 2002, type = "TRANSLATION",
                    question = "Dịch: Chúng ta cần thỏa hiệp.",
                    correctAnswer = "We need to compromise",
                    order = 4, difficulty = 3
                ),

                ExerciseEntity(
                    lessonId = 22, wordId = 2008, type = "MULTIPLE_CHOICE",
                    question = "Translate 'thời hạn'.",
                    correctAnswer = "deadline",
                    optionA = "deadline", optionB = "inquiry", optionC = "proposal", optionD = "stakeholder",
                    order = 1, difficulty = 3
                ),
                ExerciseEntity(
                    lessonId = 22, wordId = 2010, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'đề xuất'?",
                    correctAnswer = "proposal",
                    optionA = "proposal", optionB = "deliverable", optionC = "follow-up", optionD = "stakeholder",
                    order = 2, difficulty = 3
                ),
                ExerciseEntity(
                    lessonId = 22, wordId = 2009, type = "FILL_BLANK",
                    question = "Send the ____ by Monday.",
                    correctAnswer = "deliverable",
                    hint = "Project output",
                    order = 3, difficulty = 3
                ),
                ExerciseEntity(
                    lessonId = 22, wordId = 2011, type = "TRANSLATION",
                    question = "Dịch: Tôi sẽ gửi thư theo dõi.",
                    correctAnswer = "I will send a follow-up",
                    order = 4, difficulty = 3
                ),

                ExerciseEntity(
                    lessonId = 23, wordId = 2014, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'bằng chứng'?",
                    correctAnswer = "evidence",
                    optionA = "evidence", optionB = "hypothesis", optionC = "citation", optionD = "critique",
                    order = 1, difficulty = 3
                ),
                ExerciseEntity(
                    lessonId = 23, wordId = 2018, type = "MULTIPLE_CHOICE",
                    question = "Translate 'trích dẫn'.",
                    correctAnswer = "citation",
                    optionA = "abstract", optionB = "citation", optionC = "methodology", optionD = "evidence",
                    order = 2, difficulty = 3
                ),
                ExerciseEntity(
                    lessonId = 23, wordId = 2017, type = "FILL_BLANK",
                    question = "Write the ____ first.",
                    correctAnswer = "abstract",
                    hint = "Short summary",
                    order = 3, difficulty = 3
                ),
                ExerciseEntity(
                    lessonId = 23, wordId = 2013, type = "TRANSLATION",
                    question = "Dịch: Kiểm chứng giả thuyết.",
                    correctAnswer = "Test the hypothesis",
                    order = 4, difficulty = 3
                ),

                ExerciseEntity(
                    lessonId = 24, wordId = 2104, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'mùa đông'?",
                    correctAnswer = "winter",
                    optionA = "spring", optionB = "summer", optionC = "winter", optionD = "autumn",
                    order = 1, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 24, wordId = 2105, type = "MULTIPLE_CHOICE",
                    question = "Translate 'ấm áp'",
                    correctAnswer = "warm",
                    optionA = "warm", optionB = "cold", optionC = "spring", optionD = "winter",
                    order = 2, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 24, wordId = 2106, type = "FILL_BLANK",
                    question = "It is ____ today.",
                    correctAnswer = "cold",
                    hint = "Opposite of warm",
                    order = 3, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 24, wordId = 2102, type = "TRANSLATION",
                    question = "Dịch: Mùa hè nóng.",
                    correctAnswer = "Summer is hot",
                    order = 4, difficulty = 2
                ),

                ExerciseEntity(
                    lessonId = 25, wordId = 2108, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'buồn'?",
                    correctAnswer = "sad",
                    optionA = "sad", optionB = "happy", optionC = "excited", optionD = "tired",
                    order = 1, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 25, wordId = 2111, type = "MULTIPLE_CHOICE",
                    question = "Translate 'lo lắng'",
                    correctAnswer = "worried",
                    optionA = "worried", optionB = "angry", optionC = "happy", optionD = "tired",
                    order = 2, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 25, wordId = 2107, type = "FILL_BLANK",
                    question = "I feel ____ today.",
                    correctAnswer = "happy",
                    hint = "Positive feeling",
                    order = 3, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 25, wordId = 2112, type = "TRANSLATION",
                    question = "Dịch: Tôi mệt mỏi.",
                    correctAnswer = "I am tired",
                    order = 4, difficulty = 2
                ),

                ExerciseEntity(
                    lessonId = 26, wordId = 2113, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'kích cỡ'?",
                    correctAnswer = "size",
                    optionA = "size", optionB = "brand", optionC = "quality", optionD = "refund",
                    order = 1, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 26, wordId = 2115, type = "MULTIPLE_CHOICE",
                    question = "Translate 'hoàn tiền'",
                    correctAnswer = "refund",
                    optionA = "refund", optionB = "price tag", optionC = "size", optionD = "fit",
                    order = 2, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 26, wordId = 2118, type = "FILL_BLANK",
                    question = "Check the ____.",
                    correctAnswer = "price tag",
                    hint = "Sticker with the price",
                    order = 3, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 26, wordId = 2114, type = "TRANSLATION",
                    question = "Dịch: Áo này vừa với tôi.",
                    correctAnswer = "This shirt fits me",
                    order = 4, difficulty = 2
                ),

                ExerciseEntity(
                    lessonId = 27, wordId = 2119, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'ngã tư'?",
                    correctAnswer = "intersection",
                    optionA = "intersection", optionB = "crosswalk", optionC = "opposite", optionD = "near",
                    order = 1, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 27, wordId = 2124, type = "MULTIPLE_CHOICE",
                    question = "Translate 'đối diện'",
                    correctAnswer = "opposite",
                    optionA = "opposite", optionB = "near", optionC = "far", optionD = "intersection",
                    order = 2, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 27, wordId = 2123, type = "FILL_BLANK",
                    question = "The station is ____.",
                    correctAnswer = "far",
                    hint = "Not near",
                    order = 3, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 27, wordId = 2120, type = "TRANSLATION",
                    question = "Dịch: Rẽ phải ở đây.",
                    correctAnswer = "Turn right here",
                    order = 4, difficulty = 2
                ),

                ExerciseEntity(
                    lessonId = 28, wordId = 2127, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'biên bản'?",
                    correctAnswer = "minutes",
                    optionA = "agenda", optionB = "schedule", optionC = "minutes", optionD = "decision",
                    order = 1, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 28, wordId = 2129, type = "MULTIPLE_CHOICE",
                    question = "Translate 'quyết định'",
                    correctAnswer = "decision",
                    optionA = "decision", optionB = "agenda", optionC = "minutes", optionD = "presenter",
                    order = 2, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 28, wordId = 2125, type = "FILL_BLANK",
                    question = "Review the ____.",
                    correctAnswer = "agenda",
                    hint = "Meeting plan",
                    order = 3, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 28, wordId = 2127, type = "TRANSLATION",
                    question = "Dịch: Gửi biên bản cuộc họp.",
                    correctAnswer = "Send the meeting minutes",
                    order = 4, difficulty = 2
                ),

                ExerciseEntity(
                    lessonId = 29, wordId = 2135, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'triệu chứng'?",
                    correctAnswer = "symptom",
                    optionA = "symptom", optionB = "stress", optionC = "diet", optionD = "recovery",
                    order = 1, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 29, wordId = 2132, type = "MULTIPLE_CHOICE",
                    question = "Translate 'căng thẳng'",
                    correctAnswer = "stress",
                    optionA = "stress", optionB = "healthy", optionC = "diet", optionD = "symptom",
                    order = 2, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 29, wordId = 2134, type = "FILL_BLANK",
                    question = "Rest helps ____.",
                    correctAnswer = "recovery",
                    hint = "Getting better",
                    order = 3, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 29, wordId = 2131, type = "TRANSLATION",
                    question = "Dịch: Cô ấy luôn khỏe mạnh.",
                    correctAnswer = "She stays healthy",
                    order = 4, difficulty = 2
                ),

                ExerciseEntity(
                    lessonId = 30, wordId = 2137, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'đồng ý'?",
                    correctAnswer = "agree",
                    optionA = "agree", optionB = "disagree", optionC = "opinion", optionD = "prefer",
                    order = 1, difficulty = 3
                ),
                ExerciseEntity(
                    lessonId = 30, wordId = 2139, type = "MULTIPLE_CHOICE",
                    question = "Translate 'ý kiến'",
                    correctAnswer = "opinion",
                    optionA = "opinion", optionB = "suggest", optionC = "explain", optionD = "agree",
                    order = 2, difficulty = 3
                ),
                ExerciseEntity(
                    lessonId = 30, wordId = 2141, type = "FILL_BLANK",
                    question = "Please ____.",
                    correctAnswer = "explain",
                    hint = "Make it clear",
                    order = 3, difficulty = 3
                ),
                ExerciseEntity(
                    lessonId = 30, wordId = 2140, type = "TRANSLATION",
                    question = "Dịch: Tôi đề xuất nghỉ một chút.",
                    correctAnswer = "I suggest a break",
                    order = 4, difficulty = 3
                )
            )
        }

        private fun getWordTileExercises(): List<ExerciseEntity> {
            return listOf(
                ExerciseEntity(
                    lessonId = 1, wordId = 9, type = "WORD_TILES",
                    question = "Dịch: Bạn là học sinh.",
                    correctAnswer = "You are a student",
                    matchPairs = """["You","are","a","student"]""",
                    order = 13, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 9, type = "WORD_TILES",
                    question = "Translate: I am a student.",
                    correctAnswer = "Tôi là học sinh",
                    matchPairs = """["Tôi","là","học","sinh"]""",
                    order = 14, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 2, wordId = 15, type = "WORD_TILES",
                    question = "Dịch: Chúng tôi uống cà phê.",
                    correctAnswer = "We drink coffee",
                    matchPairs = """["We","drink","coffee"]""",
                    order = 9, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 3, wordId = 34, type = "WORD_TILES",
                    question = "Dịch: Rất vui được gặp bạn.",
                    correctAnswer = "Nice to meet you",
                    matchPairs = """["Nice","to","meet","you"]""",
                    order = 9, difficulty = 1
                )
            )
        }
    }
}

private data class SeedPayload(
    val lessons: List<LessonEntity>,
    val words: List<WordEntity>,
    val exercises: List<ExerciseEntity>
)





















