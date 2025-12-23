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
        UserEntity::class,
        UserProgressEntity::class,
        AchievementEntity::class,
        MistakeEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun wordDao(): WordDao
    abstract fun lessonDao(): LessonDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun userDao(): UserDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun achievementDao(): AchievementDao
    abstract fun mistakeDao(): MistakeDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private lateinit var appContext: Context
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                appContext = context.applicationContext
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "master_english_database"
                )
                    .addCallback(DatabaseCallback())
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
            val lessonDao = database.lessonDao()
            val wordDao = database.wordDao()
            val exerciseDao = database.exerciseDao()

            val existingLessons = runCatching { lessonDao.getTotalLessonsCount() }.getOrDefault(0)
            val existingWords = runCatching { wordDao.getTotalWordsCount() }.getOrDefault(0)
            val existingExercises = runCatching { exerciseDao.getTotalExercisesCount() }.getOrDefault(0)
            if (existingLessons > 0 && existingWords > 0 && existingExercises > 0) return

            val assetSeed = loadSeedFromAssets()
            if (assetSeed != null) {
                lessonDao.insertLessons(assetSeed.lessons)
                wordDao.insertWords(assetSeed.words)
                exerciseDao.insertExercises(assetSeed.exercises)
                return
            }

            // Fallback to bundled seed lists when backend is unavailable.
            lessonDao.insertLessons(getInitialLessons())
            wordDao.insertWords(getInitialWords())
            exerciseDao.insertExercises(getInitialExercises())
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No schema changes; data seeding happens in onOpen
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
                )
            )
        }
        private fun getInitialWords(): List<WordEntity> {
            return listOf(
                // Lesson 1 - Basics 1 (12 words)
                WordEntity("hello", "xin chào", "HEL-oh", "interjection", "Hello, how are you?", "Xin chào, b?n kh?e không?", lessonId = 1, difficulty = 1, category = "greetings"),
                WordEntity("hi", "chào", "hai", "interjection", "Hi! Nice to meet you.", "Chào! R?t vui du?c g?p b?n.", lessonId = 1, difficulty = 1, category = "greetings"),
                WordEntity("goodbye", "t?m bi?t", "gud-bai", "interjection", "Goodbye, see you soon!", "T?m bi?t, h?n g?p l?i!", lessonId = 1, difficulty = 1, category = "greetings"),
                WordEntity("please", "làm on", "pleez", "adverb", "Please help me.", "Làm on giúp tôi.", lessonId = 1, difficulty = 1, category = "politeness"),
                WordEntity("thank you", "c?m on", "thangk-yoo", "phrase", "Thank you very much!", "C?m on b?n r?t nhi?u!", lessonId = 1, difficulty = 1, category = "politeness"),
                WordEntity("yes", "vâng", "yes", "adverb", "Yes, I understand.", "Vâng, tôi hi?u.", lessonId = 1, difficulty = 1, category = "basics"),
                WordEntity("no", "không", "no", "adverb", "No, thank you.", "Không, c?m on.", lessonId = 1, difficulty = 1, category = "basics"),
                WordEntity("I", "tôi", "ai", "pronoun", "I am a student.", "Tôi là h?c sinh.", lessonId = 1, difficulty = 1, category = "pronoun"),
                WordEntity("you", "b?n", "yoo", "pronoun", "You are kind.", "B?n r?t t?t b?ng.", lessonId = 1, difficulty = 1, category = "pronoun"),
                WordEntity("am", "là (di v?i I)", "am", "verb", "I am Nam.", "Tôi là Nam.", lessonId = 1, difficulty = 1, category = "verb"),
                WordEntity("are", "là (di v?i you/we/they)", "ar", "verb", "You are my friend.", "B?n là b?n c?a tôi.", lessonId = 1, difficulty = 1, category = "verb"),
                WordEntity("name", "tên", "naym", "noun", "My name is Mai.", "Tên tôi là Mai.", lessonId = 1, difficulty = 1, category = "introductions"),
                
                // Lesson 2 - Basics 2 (12 words)
                WordEntity("he", "anh ?y", "hee", "pronoun", "He is a teacher.", "Anh ?y là giáo viên.", lessonId = 2, difficulty = 1, category = "pronoun"),
                WordEntity("she", "cô ?y", "shee", "pronoun", "She is a doctor.", "Cô ?y là bác si.", lessonId = 2, difficulty = 1, category = "pronoun"),
                WordEntity("we", "chúng tôi", "wee", "pronoun", "We are from Vietnam.", "Chúng tôi d?n t? Vi?t Nam.", lessonId = 2, difficulty = 1, category = "pronoun"),
                WordEntity("they", "h?", "thay", "pronoun", "They are students.", "H? là h?c sinh.", lessonId = 2, difficulty = 1, category = "pronoun"),
                WordEntity("man", "ngu?i dàn ông", "man", "noun", "The man is tall.", "Ngu?i dàn ông dó cao.", lessonId = 2, difficulty = 1, category = "people"),
                WordEntity("woman", "ph? n?", "wuh-muhn", "noun", "The woman drinks tea.", "Ngu?i ph? n? u?ng trà.", lessonId = 2, difficulty = 1, category = "people"),
                WordEntity("boy", "con trai", "boy", "noun", "The boy reads a book.", "C?u bé dang d?c sách.", lessonId = 2, difficulty = 1, category = "people"),
                WordEntity("girl", "con gái", "gurl", "noun", "The girl eats rice.", "Cô bé an com.", lessonId = 2, difficulty = 1, category = "people"),
                WordEntity("eat", "an", "eet", "verb", "We eat breakfast.", "Chúng tôi an sáng.", lessonId = 2, difficulty = 1, category = "verb"),
                WordEntity("drink", "u?ng", "drink", "verb", "They drink coffee.", "H? u?ng cà phê.", lessonId = 2, difficulty = 1, category = "verb"),
                WordEntity("read", "d?c", "reed", "verb", "I read every day.", "Tôi d?c sách m?i ngày.", lessonId = 2, difficulty = 1, category = "verb"),
                WordEntity("write", "vi?t", "rait", "verb", "She writes a letter.", "Cô ?y vi?t thu.", lessonId = 2, difficulty = 1, category = "verb"),
                
                // Lesson 3 - Phrases (12 words)
                WordEntity("excuse me", "xin l?i", "ex-kyooz mee", "phrase", "Excuse me, where is the bus?", "Xin l?i, tr?m xe ? dâu?", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("sorry", "xin l?i", "sor-ree", "adjective", "I am sorry.", "Tôi xin l?i.", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("good morning", "chào bu?i sáng", "gud MOR-ning", "phrase", "Good morning, everyone!", "Chào bu?i sáng m?i ngu?i!", lessonId = 3, difficulty = 1, category = "greetings"),
                WordEntity("good night", "chúc ng? ngon", "gud nait", "phrase", "Good night, see you tomorrow.", "Chúc ng? ngon, h?n g?p b?n ngày mai.", lessonId = 3, difficulty = 1, category = "greetings"),
                WordEntity("see you later", "h?n g?p l?i sau", "see-yoo-lay-ter", "phrase", "See you later!", "H?n g?p l?i sau!", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("see you soon", "g?p l?i s?m thôi", "see-yoo-soon", "phrase", "See you soon.", "G?p l?i b?n s?m thôi.", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("how are you", "b?n kh?e không", "how-are-yoo", "phrase", "Hi, how are you?", "Chào, b?n kh?e không?", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("I am fine", "tôi kh?e", "ai-am-fain", "phrase", "I am fine, thank you.", "Tôi kh?e, c?m on.", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("what is your name", "b?n tên gì", "wot-iz-yor-naym", "phrase", "What is your name?", "B?n tên gì?", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("nice to meet you", "r?t vui du?c g?p b?n", "nais-tu-meet-yoo", "phrase", "Nice to meet you!", "R?t vui du?c g?p b?n!", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("welcome", "chào m?ng", "wel-kum", "phrase", "Welcome to Hanoi!", "Chào m?ng d?n Hà N?i!", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("good luck", "chúc may m?n", "gud luhk", "phrase", "Good luck on your test!", "Chúc may m?n khi thi!", lessonId = 3, difficulty = 1, category = "phrases"),
                // Lesson 4 - Food & Drinks (12 words)
                WordEntity("water", "nu?c", "waw-ter", "noun", "I drink water.", "Tôi u?ng nu?c.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("coffee", "cà phê", "ko-fee", "noun", "She likes coffee.", "Cô ?y thích cà phê.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("tea", "trà", "tee", "noun", "Tea or coffee?", "Trà hay cà phê?", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("bread", "bánh mì", "bred", "noun", "I eat bread.", "Tôi an bánh mì.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("rice", "com", "rais", "noun", "We cook rice.", "Chúng tôi n?u com.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("soup", "súp", "soop", "noun", "The soup is hot.", "Bát súp nóng.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("apple", "táo", "ap-ul", "noun", "The apple is red.", "Qu? táo màu d?.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("banana", "chu?i", "buh-na-na", "noun", "Bananas are sweet.", "Chu?i ng?t.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("chicken", "gà", "chik-en", "noun", "I eat chicken.", "Tôi an th?t gà.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("fish", "cá", "fish", "noun", "Fish and rice.", "Cá và com.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("delicious", "ngon", "di-li-shus", "adjective", "The meal is delicious.", "B?a an ngon.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("hungry", "dói", "hun-gree", "adjective", "I am hungry.", "Tôi dang dói.", lessonId = 4, difficulty = 1, category = "food"),
                
                // Lesson 5 - Travel Essentials (6 words)
                WordEntity("bus", "xe buýt", "bus", "noun", "Take the bus.", "B?t xe buýt.", lessonId = 5, difficulty = 1, category = "travel"),
                WordEntity("train", "tàu h?a", "tray-n", "noun", "The train is late.", "Tàu h?a b? tr?.", lessonId = 5, difficulty = 1, category = "travel"),
                WordEntity("taxi", "taxi", "tak-see", "noun", "Call a taxi.", "G?i m?t chi?c taxi.", lessonId = 5, difficulty = 1, category = "travel"),
                WordEntity("airport", "sân bay", "air-port", "noun", "The airport is far.", "Sân bay khá xa.", lessonId = 5, difficulty = 1, category = "travel"),
                WordEntity("ticket", "vé", "tik-it", "noun", "I need a ticket.", "Tôi c?n vé.", lessonId = 5, difficulty = 1, category = "travel"),
                WordEntity("passport", "h? chi?u", "pass-port", "noun", "Show your passport.", "Xu?t trình h? chi?u.", lessonId = 5, difficulty = 1, category = "travel"),
                
                // Lesson 6 - Family (6 words)
                WordEntity("father", "cha", "fa-ther", "noun", "My father is kind.", "Cha tôi r?t t?t.", lessonId = 6, difficulty = 1, category = "family"),
                WordEntity("mother", "m?", "muh-ther", "noun", "My mother cooks.", "M? tôi n?u an.", lessonId = 6, difficulty = 1, category = "family"),
                WordEntity("brother", "anh/em trai", "bru-ther", "noun", "He is my brother.", "Anh ?y là anh trai tôi.", lessonId = 6, difficulty = 1, category = "family"),
                WordEntity("sister", "ch?/em gái", "sis-ter", "noun", "She is my sister.", "Cô ?y là ch? gái tôi.", lessonId = 6, difficulty = 1, category = "family"),
                WordEntity("son", "con trai", "sun", "noun", "This is my son.", "Ðây là con trai tôi.", lessonId = 6, difficulty = 1, category = "family"),
                WordEntity("daughter", "con gái", "daw-ter", "noun", "That is my daughter.", "Ðó là con gái tôi.", lessonId = 6, difficulty = 1, category = "family"),
                
                // Lesson 7 - Colors & Clothing (6 words)
                WordEntity("red", "màu d?", "red", "adjective", "The apple is red.", "Qu? táo màu d?.", lessonId = 7, difficulty = 1, category = "colors"),
                WordEntity("blue", "màu xanh duong", "blu", "adjective", "The sky is blue.", "B?u tr?i màu xanh.", lessonId = 7, difficulty = 1, category = "colors"),
                WordEntity("green", "màu xanh lá", "green", "adjective", "The leaf is green.", "Chi?c lá màu xanh lá.", lessonId = 7, difficulty = 1, category = "colors"),
                WordEntity("shirt", "áo so mi", "shurt", "noun", "I wear a shirt.", "Tôi m?c áo so mi.", lessonId = 7, difficulty = 1, category = "clothes"),
                WordEntity("pants", "qu?n dài", "pants", "noun", "These pants are new.", "Chi?c qu?n này m?i.", lessonId = 7, difficulty = 1, category = "clothes"),
                WordEntity("shoes", "dôi giày", "shooz", "noun", "I like these shoes.", "Tôi thích dôi giày này.", lessonId = 7, difficulty = 1, category = "clothes"),
                
                // Lesson 8 - Numbers & Time (6 words)
                WordEntity("one", "m?t", "wun", "number", "One apple, please.", "M?t qu? táo, làm on.", lessonId = 8, difficulty = 1, category = "numbers"),
                WordEntity("two", "hai", "too", "number", "Two tickets.", "Hai vé.", lessonId = 8, difficulty = 1, category = "numbers"),
                WordEntity("three", "ba", "three", "number", "Three cups of tea.", "Ba ly trà.", lessonId = 8, difficulty = 1, category = "numbers"),
                WordEntity("today", "hôm nay", "to-day", "noun", "See you today.", "H?n b?n hôm nay.", lessonId = 8, difficulty = 1, category = "time"),
                WordEntity("tomorrow", "ngày mai", "to-mor-row", "noun", "See you tomorrow.", "H?n b?n ngày mai.", lessonId = 8, difficulty = 1, category = "time"),
                WordEntity("yesterday", "hôm qua", "yes-ter-day", "noun", "Yesterday was busy.", "Hôm qua r?t b?n.", lessonId = 8, difficulty = 1, category = "time"),
                
                // Lesson 9 - School & Work (6 words)
                WordEntity("teacher", "giáo viên", "tee-cher", "noun", "She is a teacher.", "Cô ?y là giáo viên.", lessonId = 9, difficulty = 1, category = "school"),
                WordEntity("student", "h?c sinh", "stoo-dent", "noun", "I am a student.", "Tôi là h?c sinh.", lessonId = 9, difficulty = 1, category = "school"),
                WordEntity("school", "tru?ng h?c", "skool", "noun", "The school is big.", "Tru?ng h?c này l?n.", lessonId = 9, difficulty = 1, category = "school"),
                WordEntity("job", "công vi?c", "job", "noun", "I love my job.", "Tôi thích công vi?c.", lessonId = 9, difficulty = 1, category = "work"),
                WordEntity("office", "van phòng", "of-fis", "noun", "The office is near.", "Van phòng ? g?n.", lessonId = 9, difficulty = 1, category = "work"),
                WordEntity("meeting", "cu?c h?p", "mee-ting", "noun", "I have a meeting.", "Tôi có m?t cu?c h?p.", lessonId = 9, difficulty = 1, category = "work"),
                
                // Lesson 10 - Daily Routine (6 words)
                WordEntity("wake up", "th?c d?y", "wake-up", "verb", "I wake up early.", "Tôi th?c d?y s?m.", lessonId = 10, difficulty = 1, category = "routine"),
                WordEntity("breakfast", "b?a sáng", "brek-fust", "noun", "Breakfast at 7 am.", "An sáng lúc 7 gi?.", lessonId = 10, difficulty = 1, category = "routine"),
                WordEntity("lunch", "b?a trua", "lunch", "noun", "Lunch with friends.", "An trua v?i b?n.", lessonId = 10, difficulty = 1, category = "routine"),
                WordEntity("dinner", "b?a t?i", "din-ner", "noun", "Dinner at home.", "An t?i ? nhà.", lessonId = 10, difficulty = 1, category = "routine"),
                WordEntity("sleep", "ng?", "sleep", "verb", "I sleep at 11 pm.", "Tôi ng? lúc 11 gi?.", lessonId = 10, difficulty = 1, category = "routine"),
                WordEntity("exercise", "t?p th? d?c", "ek-ser-size", "verb", "I exercise every day.", "Tôi t?p th? d?c m?i ngày.", lessonId = 10, difficulty = 1, category = "routine"),
                
                // Lesson 11 - Transport & Directions (6 words)
                WordEntity("bus stop", "tr?m xe buýt", "bus-stop", "noun", "The bus stop is near.", "Tr?m xe buýt ? g?n.", lessonId = 11, difficulty = 2, category = "travel"),
                WordEntity("station", "nhà ga", "stay-shun", "noun", "Meet me at the station.", "G?p tôi ? nhà ga.", lessonId = 11, difficulty = 2, category = "travel"),
                WordEntity("ticket booth", "qu?y vé", "tik-it booth", "noun", "Buy tickets at the booth.", "Mua vé ? qu?y.", lessonId = 11, difficulty = 2, category = "travel"),
                WordEntity("turn left", "r? trái", "turn left", "phrase", "Turn left at the corner.", "R? trái ? góc du?ng.", lessonId = 11, difficulty = 2, category = "directions"),
                WordEntity("straight ahead", "di th?ng", "straight ahead", "phrase", "Go straight ahead 200 meters.", "Ði th?ng 200 mét.", lessonId = 11, difficulty = 2, category = "directions"),
                WordEntity("traffic jam", "k?t xe", "traf-ik jam", "noun", "There is a traffic jam.", "Ðang k?t xe.", lessonId = 11, difficulty = 2, category = "travel"),
                
                // Lesson 12 - Shopping & Money (6 words)
                WordEntity("price", "giá", "price", "noun", "What is the price?", "Giá bao nhiêu?", lessonId = 12, difficulty = 2, category = "shopping"),
                WordEntity("discount", "gi?m giá", "dis-count", "noun", "Do you have a discount?", "B?n có gi?m giá không?", lessonId = 12, difficulty = 2, category = "shopping"),
                WordEntity("cash", "ti?n m?t", "cash", "noun", "I pay with cash.", "Tôi tr? ti?n m?t.", lessonId = 12, difficulty = 2, category = "shopping"),
                WordEntity("card", "th?", "card", "noun", "Can I pay by card?", "Tôi có th? tr? b?ng th? không?", lessonId = 12, difficulty = 2, category = "shopping"),
                WordEntity("receipt", "hóa don", "re-seet", "noun", "Here is your receipt.", "Ðây là hóa don c?a b?n.", lessonId = 12, difficulty = 2, category = "shopping"),
                WordEntity("expensive", "d?t", "ex-pen-siv", "adjective", "That bag is expensive.", "Chi?c túi dó d?t.", lessonId = 12, difficulty = 2, category = "shopping"),
                
                // Lesson 13 - Restaurant & Cafe (6 words)
                WordEntity("menu", "th?c don", "men-yoo", "noun", "Can I see the menu?", "Cho tôi xem th?c don.", lessonId = 13, difficulty = 2, category = "food"),
                WordEntity("order", "g?i món", "or-der", "verb", "We will order now.", "Chúng tôi s? g?i món bây gi?.", lessonId = 13, difficulty = 2, category = "food"),
                WordEntity("reservation", "d?t bàn", "re-zer-vay-shun", "noun", "I have a reservation.", "Tôi dã d?t bàn tru?c.", lessonId = 13, difficulty = 2, category = "food"),
                WordEntity("bill", "hóa don", "bill", "noun", "Please bring the bill.", "Cho xin hóa don.", lessonId = 13, difficulty = 2, category = "food"),
                WordEntity("tip", "ti?n tip", "tip", "noun", "Leave a small tip.", "Ð? l?i chút ti?n tip.", lessonId = 13, difficulty = 2, category = "food"),
                WordEntity("delicious", "ngon", "di-li-shus", "adjective", "The soup is delicious.", "Món súp r?t ngon.", lessonId = 13, difficulty = 2, category = "food"),
                
                // Lesson 14 - Health & Doctor (6 words)
                WordEntity("fever", "s?t", "fee-ver", "noun", "I have a fever.", "Tôi b? s?t.", lessonId = 14, difficulty = 2, category = "health"),
                WordEntity("cough", "ho", "coff", "noun", "This cough is bad.", "Con ho này n?ng.", lessonId = 14, difficulty = 2, category = "health"),
                WordEntity("headache", "dau d?u", "hed-ake", "noun", "I have a headache.", "Tôi b? dau d?u.", lessonId = 14, difficulty = 2, category = "health"),
                WordEntity("medicine", "thu?c", "med-i-sin", "noun", "Take this medicine twice a day.", "U?ng thu?c này 2 l?n m?i ngày.", lessonId = 14, difficulty = 2, category = "health"),
                WordEntity("appointment", "l?ch h?n", "ap-point-ment", "noun", "I need a doctor appointment.", "Tôi c?n h?n bác si.", lessonId = 14, difficulty = 2, category = "health"),
                WordEntity("rest", "ngh? ngoi", "rest", "verb", "You should rest today.", "B?n nên ngh? ngoi hôm nay.", lessonId = 14, difficulty = 2, category = "health"),
                
                // Lesson 15 - Workplace (6 words)
                WordEntity("meeting room", "phòng h?p", "mee-ting room", "noun", "The meeting room is ready.", "Phòng h?p dã s?n sàng.", lessonId = 15, difficulty = 2, category = "work"),
                WordEntity("deadline", "h?n chót", "dead-line", "noun", "The deadline is Friday.", "H?n chót là th? Sáu.", lessonId = 15, difficulty = 2, category = "work"),
                WordEntity("task", "nhi?m v?", "task", "noun", "Assign the new task.", "Giao nhi?m v? m?i.", lessonId = 15, difficulty = 2, category = "work"),
                WordEntity("colleague", "d?ng nghi?p", "kol-leeg", "noun", "She is my colleague.", "Cô ?y là d?ng nghi?p c?a tôi.", lessonId = 15, difficulty = 2, category = "work"),
                WordEntity("report", "báo cáo", "ri-port", "noun", "Send the weekly report.", "G?i báo cáo h?ng tu?n.", lessonId = 15, difficulty = 2, category = "work"),
                WordEntity("present", "trình bày", "pre-zent", "verb", "I will present today.", "Tôi s? trình bày hôm nay.", lessonId = 15, difficulty = 2, category = "work"),
                
                // Lesson 16 - Home & Household (6 words)
                WordEntity("kitchen", "nhà b?p", "kitch-en", "noun", "The kitchen is clean.", "Nhà b?p s?ch.", lessonId = 16, difficulty = 1, category = "home"),
                WordEntity("living room", "phòng khách", "liv-ing room", "noun", "We sit in the living room.", "Chúng tôi ng?i ? phòng khách.", lessonId = 16, difficulty = 1, category = "home"),
                WordEntity("bedroom", "phòng ng?", "bed-room", "noun", "The bedroom is cozy.", "Phòng ng? ?m cúng.", lessonId = 16, difficulty = 1, category = "home"),
                WordEntity("vacuum", "hút b?i", "vac-yoom", "verb", "Please vacuum the floor.", "Làm on hút b?i sàn.", lessonId = 16, difficulty = 1, category = "home"),
                WordEntity("laundry", "gi?t d?", "lawn-dree", "noun", "Do the laundry on Sunday.", "Gi?t d? vào Ch? nh?t.", lessonId = 16, difficulty = 1, category = "home"),
                WordEntity("cleaning", "d?n d?p", "klee-ning", "noun", "Cleaning takes time.", "D?n d?p m?t th?i gian.", lessonId = 16, difficulty = 1, category = "home"),
                
                // Lesson 17 - Hobbies & Free Time (6 words)
                WordEntity("reading", "d?c sách", "ree-ding", "noun", "Reading is relaxing.", "Ð?c sách giúp thu giãn.", lessonId = 17, difficulty = 1, category = "hobby"),
                WordEntity("painting", "v? tranh", "paint-ing", "noun", "I like painting.", "Tôi thích v? tranh.", lessonId = 17, difficulty = 1, category = "hobby"),
                WordEntity("hiking", "di b? du?ng dài", "hi-king", "noun", "We go hiking on weekends.", "Chúng tôi di b? du?ng dài cu?i tu?n.", lessonId = 17, difficulty = 1, category = "hobby"),
                WordEntity("playing guitar", "choi guitar", "play-ing gui-tar", "verb", "He enjoys playing guitar.", "Anh ?y thích choi guitar.", lessonId = 17, difficulty = 1, category = "hobby"),
                WordEntity("swimming", "boi l?i", "swim-ing", "noun", "Swimming is my hobby.", "Boi l?i là s? thích c?a tôi.", lessonId = 17, difficulty = 1, category = "hobby"),
                WordEntity("gardening", "làm vu?n", "gar-den-ing", "noun", "Gardening is peaceful.", "Làm vu?n r?t yên bình.", lessonId = 17, difficulty = 1, category = "hobby"),
                
                // Lesson 18 - Technology & Devices (6 words)
                WordEntity("smartphone", "di?n tho?i thông minh", "smart-phone", "noun", "My smartphone is slow.", "Ði?n tho?i thông minh c?a tôi ch?m.", lessonId = 18, difficulty = 2, category = "technology"),
                WordEntity("laptop", "máy tính xách tay", "lap-top", "noun", "Charge your laptop.", "S?c máy tính xách tay.", lessonId = 18, difficulty = 2, category = "technology"),
                WordEntity("charger", "s?c", "char-jer", "noun", "I lost my charger.", "Tôi m?t s?c r?i.", lessonId = 18, difficulty = 2, category = "technology"),
                WordEntity("password", "m?t kh?u", "pass-word", "noun", "Reset your password.", "Ð?t l?i m?t kh?u.", lessonId = 18, difficulty = 2, category = "technology"),
                WordEntity("app", "?ng d?ng", "app", "noun", "Download the new app.", "T?i ?ng d?ng m?i.", lessonId = 18, difficulty = 2, category = "technology"),
                WordEntity("update", "c?p nh?t", "up-date", "verb", "Update the software.", "C?p nh?t ph?n m?m.", lessonId = 18, difficulty = 2, category = "technology"),
                
                // Lesson 19 - Weather & Events (6 words)
                WordEntity("sunny", "n?ng", "sun-ny", "adjective", "It is sunny today.", "Hôm nay tr?i n?ng.", lessonId = 19, difficulty = 1, category = "weather"),
                WordEntity("rainy", "mua", "ray-ny", "adjective", "The weather is rainy.", "Tr?i dang mua.", lessonId = 19, difficulty = 1, category = "weather"),
                WordEntity("storm", "bão", "storm", "noun", "A storm is coming.", "Bão dang d?n.", lessonId = 19, difficulty = 2, category = "weather"),
                WordEntity("forecast", "d? báo th?i ti?t", "for-cast", "noun", "Check the forecast.", "Ki?m tra d? báo th?i ti?t.", lessonId = 19, difficulty = 2, category = "weather"),
                WordEntity("picnic", "di choi ngoài tr?i", "pic-nic", "noun", "Plan a picnic this weekend.", "Lên k? ho?ch picnic cu?i tu?n này.", lessonId = 19, difficulty = 1, category = "events"),
                WordEntity("festival", "l? h?i", "fes-ti-val", "noun", "The festival is crowded.", "L? h?i dông dúc.", lessonId = 19, difficulty = 2, category = "events"),
                
                // Lesson 20 - Emergency & Help (6 words)
                WordEntity("emergency", "kh?n c?p", "e-mer-gen-cy", "noun", "Call in an emergency.", "G?i khi kh?n c?p.", lessonId = 20, difficulty = 3, category = "safety"),
                WordEntity("ambulance", "xe c?u thuong", "am-byu-lans", "noun", "Call an ambulance.", "G?i xe c?u thuong.", lessonId = 20, difficulty = 3, category = "safety"),
                WordEntity("police", "c?nh sát", "po-lice", "noun", "Call the police.", "G?i c?nh sát.", lessonId = 20, difficulty = 3, category = "safety"),
                WordEntity("fire", "cháy", "fire", "noun", "There is a fire!", "Có cháy!", lessonId = 20, difficulty = 3, category = "safety"),
                WordEntity("help", "giúp d?", "help", "verb", "Please help me!", "Làm on giúp tôi!", lessonId = 20, difficulty = 2, category = "safety"),
                WordEntity("lost", "l?c du?ng", "lost", "adjective", "I am lost.", "Tôi b? l?c.", lessonId = 20, difficulty = 2, category = "directions"),
                
                // Lesson 1 - Additional words to complete full 20-word set
                WordEntity(
                    id = 1001,
                    word = "good morning",
                    translation = "chào bu?i sáng",
                    pronunciation = "gud MOR-ning",
                    partOfSpeech = "phrase",
                    exampleSentence = "Good morning, class!",
                    exampleTranslation = "Chào bu?i sáng c? l?p!",
                    lessonId = 1,
                    difficulty = 1,
                    category = "greetings"
                ),
                WordEntity(
                    id = 1002,
                    word = "good afternoon",
                    translation = "chào bu?i chi?u",
                    pronunciation = "gud AF-ter-noon",
                    partOfSpeech = "phrase",
                    exampleSentence = "Good afternoon, how are you?",
                    exampleTranslation = "Chào bu?i chi?u, b?n kh?e không?",
                    lessonId = 1,
                    difficulty = 1,
                    category = "greetings"
                ),
                WordEntity(
                    id = 1003,
                    word = "good evening",
                    translation = "chào bu?i t?i",
                    pronunciation = "gud EEV-ning",
                    partOfSpeech = "phrase",
                    exampleSentence = "Good evening everyone.",
                    exampleTranslation = "Chào bu?i t?i m?i ngu?i.",
                    lessonId = 1,
                    difficulty = 1,
                    category = "greetings"
                ),
                WordEntity(
                    id = 1004,
                    word = "good night",
                    translation = "chúc ng? ngon",
                    pronunciation = "gud nait",
                    partOfSpeech = "phrase",
                    exampleSentence = "Good night and sweet dreams.",
                    exampleTranslation = "Chúc ng? ngon và mo d?p.",
                    lessonId = 1,
                    difficulty = 1,
                    category = "greetings"
                ),
                WordEntity(
                    id = 1005,
                    word = "nice",
                    translation = "t?t, d? ch?u",
                    pronunciation = "nais",
                    partOfSpeech = "adjective",
                    exampleSentence = "It is nice to meet you.",
                    exampleTranslation = "R?t vui du?c g?p b?n.",
                    lessonId = 1,
                    difficulty = 1,
                    category = "introductions"
                ),
                WordEntity(
                    id = 1006,
                    word = "meet",
                    translation = "g?p",
                    pronunciation = "meet",
                    partOfSpeech = "verb",
                    exampleSentence = "I want to meet new friends.",
                    exampleTranslation = "Tôi mu?n g?p b?n m?i.",
                    lessonId = 1,
                    difficulty = 1,
                    category = "introductions"
                ),
                WordEntity(
                    id = 1007,
                    word = "sorry",
                    translation = "xin l?i",
                    pronunciation = "SOR-ree",
                    partOfSpeech = "adjective",
                    exampleSentence = "I am sorry for being late.",
                    exampleTranslation = "Tôi xin l?i vì d?n tr?.",
                    lessonId = 1,
                    difficulty = 1,
                    category = "politeness"
                ),
                WordEntity(
                    id = 1008,
                    word = "excuse me",
                    translation = "xin phép / xin l?i",
                    pronunciation = "ex-kyooz mee",
                    partOfSpeech = "phrase",
                    exampleSentence = "Excuse me, where is the bus stop?",
                    exampleTranslation = "Xin l?i, tr?m xe ? dâu?",
                    lessonId = 1,
                    difficulty = 1,
                    category = "politeness"
                )
            )
        }
        private fun getInitialExercises(): List<ExerciseEntity> {
            return listOf(
                // Lesson 1 - Basics 1
                ExerciseEntity(
                    lessonId = 1, wordId = 1, type = "MULTIPLE_CHOICE",
                    question = "What is 'xin chào' in English?",
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
                
                // Lesson 2 - Basics 2
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
                
                // Lesson 3 - Phrases
                ExerciseEntity(
                    lessonId = 3, wordId = 26, type = "MULTIPLE_CHOICE",
                    question = "How do you say 'xin l?i' politely?",
                    correctAnswer = "sorry",
                    optionA = "sorry", optionB = "welcome", optionC = "good luck", optionD = "good night",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 3, wordId = 27, type = "MULTIPLE_CHOICE",
                    question = "Translate 'ch?o bu?i s?ng'",
                    correctAnswer = "good morning",
                    optionA = "good morning", optionB = "good night", optionC = "see you later", optionD = "excuse me",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 3, wordId = 28, type = "MULTIPLE_CHOICE",
                    question = "What is 'ch?c ng? ngon'?",
                    correctAnswer = "good night",
                    optionA = "good morning", optionB = "good night", optionC = "welcome", optionD = "good luck",
                    order = 3, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 3, wordId = 29, type = "TRANSLATION",
                    question = "Dich: H?n g?p l?i sau.",
                    correctAnswer = "See you later",
                    order = 4, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 3, wordId = 34, type = "TRANSLATION",
                    question = "Dich: R?t vui ???c g?p b?n.",
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
                        {"left":"welcome","right":"ch?o m?ng"},
                        {"left":"good luck","right":"ch?c may m?n"},
                        {"left":"good night","right":"ch?c ng? ngon"},
                        {"left":"excuse me","right":"xin l?i"}
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
                
                // Lesson 4 - Food & Drinks
                ExerciseEntity(
                    lessonId = 4, wordId = 37, type = "MULTIPLE_CHOICE",
                    question = "What is 'nuoc'?",
                    correctAnswer = "water",
                    optionA = "water", optionB = "coffee", optionC = "tea", optionD = "soup",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 4, wordId = 38, type = "MULTIPLE_CHOICE",
                    question = "Translate 'c? ph?'",
                    correctAnswer = "coffee",
                    optionA = "coffee", optionB = "bread", optionC = "rice", optionD = "fish",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 4, wordId = 41, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'com'?",
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
                    question = "Dich: T?i ?ang ??i.",
                    correctAnswer = "I am hungry",
                    order = 6, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 4, wordId = 43, type = "MATCHING",
                    question = "Match the foods",
                    correctAnswer = "",
                    matchPairs = """[
                        {"left":"apple","right":"tao"},
                        {"left":"banana","right":"chuoi"},
                        {"left":"chicken","right":"ga"},
                        {"left":"fish","right":"ca"}
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
                
                // Lesson 5 - Travel Essentials
                ExerciseEntity(
                    lessonId = 5, wordId = 54, type = "MULTIPLE_CHOICE",
                    question = "Translate 'h? chi?u'",
                    correctAnswer = "passport",
                    optionA = "ticket", optionB = "passport", optionC = "bus", optionD = "train",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 5, wordId = 53, type = "MULTIPLE_CHOICE",
                    question = "What is 've'?",
                    correctAnswer = "ticket",
                    optionA = "ticket", optionB = "airport", optionC = "passport", optionD = "taxi",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 5, wordId = 52, type = "TRANSLATION",
                    question = "Dich: San bay o dau?",
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
                
                // Lesson 6 - Family
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
                    question = "Dich: ??y l? con trai t?i.",
                    correctAnswer = "This is my son",
                    order = 4, difficulty = 1
                ),
                
                // Lesson 7 - Colors & Clothing
                ExerciseEntity(
                    lessonId = 7, wordId = 61, type = "MULTIPLE_CHOICE",
                    question = "What color is 'do'?",
                    correctAnswer = "red",
                    optionA = "red", optionB = "blue", optionC = "green", optionD = "white",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 7, wordId = 64, type = "MULTIPLE_CHOICE",
                    question = "Translate '?o s? mi'",
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
                        {"left":"red","right":"m?u ??"},
                        {"left":"blue","right":"m?u xanh d??ng"},
                        {"left":"green","right":"m?u xanh l?"},
                        {"left":"shirt","right":"?o s? mi"}
                    ]""".trimIndent(),
                    order = 4, difficulty = 1
                ),
                
                // Lesson 8 - Numbers & Time
                ExerciseEntity(
                    lessonId = 8, wordId = 67, type = "MULTIPLE_CHOICE",
                    question = "What number is 'mot'?",
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
                    question = "Dich: Hom qua toi rat ban.",
                    correctAnswer = "Yesterday I was busy",
                    order = 4, difficulty = 1
                ),
                
                // Lesson 9 - School & Work
                ExerciseEntity(
                    lessonId = 9, wordId = 73, type = "MULTIPLE_CHOICE",
                    question = "How do you say 'gi?o vi?n'?",
                    correctAnswer = "teacher",
                    optionA = "student", optionB = "teacher", optionC = "job", optionD = "office",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 9, wordId = 74, type = "MULTIPLE_CHOICE",
                    question = "Translate 'h?c sinh'",
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
                    question = "Dich: Toi co mot cu?c h?p.",
                    correctAnswer = "I have a meeting",
                    order = 4, difficulty = 1
                ),
                
                // Lesson 10 - Daily Routine
                ExerciseEntity(
                    lessonId = 10, wordId = 79, type = "MULTIPLE_CHOICE",
                    question = "How do you say 'ng? d?y'?",
                    correctAnswer = "wake up",
                    optionA = "wake up", optionB = "sleep", optionC = "breakfast", optionD = "dinner",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 10, wordId = 80, type = "MULTIPLE_CHOICE",
                    question = "Translate 'bu?i s?ng' as a meal",
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
                    question = "Dich: Toi t?p th? d?c moi ngay.",
                    correctAnswer = "I exercise every day",
                    order = 4, difficulty = 1
                ),
                
                // Lesson 11 - Transport & Directions
                ExerciseEntity(
                    lessonId = 11, wordId = 85, type = "MULTIPLE_CHOICE",
                    question = "Where is the 'tram xe bu?t'?",
                    correctAnswer = "bus stop",
                    optionA = "bus stop", optionB = "station", optionC = "traffic jam", optionD = "ticket booth",
                    order = 1, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 11, wordId = 88, type = "MULTIPLE_CHOICE",
                    question = "Translate 'r? tr?i'",
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
                    question = "Dich: Dang k?t xe.",
                    correctAnswer = "There is a traffic jam",
                    order = 4, difficulty = 2
                ),
                
                // Lesson 12 - Shopping & Money
                ExerciseEntity(
                    lessonId = 12, wordId = 91, type = "MULTIPLE_CHOICE",
                    question = "What is 'gia' in English?",
                    correctAnswer = "price",
                    optionA = "price", optionB = "discount", optionC = "receipt", optionD = "cash",
                    order = 1, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 12, wordId = 95, type = "MULTIPLE_CHOICE",
                    question = "Translate 'h?a ??n'",
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
                    question = "Dich: Ban co gi?m gi? khong?",
                    correctAnswer = "Do you have a discount?",
                    order = 4, difficulty = 2
                ),
                
                // Lesson 13 - Restaurant & Cafe
                ExerciseEntity(
                    lessonId = 13, wordId = 97, type = "MULTIPLE_CHOICE",
                    question = "Can I see the _____?",
                    correctAnswer = "menu",
                    optionA = "menu", optionB = "bill", optionC = "tip", optionD = "reservation",
                    order = 1, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 13, wordId = 100, type = "MULTIPLE_CHOICE",
                    question = "Translate 'h?a ??n' in a restaurant",
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
                    question = "Dich: De lai chut ti?n tip.",
                    correctAnswer = "Leave a small tip",
                    order = 4, difficulty = 2
                ),
                
                // Lesson 14 - Health & Doctor
                ExerciseEntity(
                    lessonId = 14, wordId = 103, type = "MULTIPLE_CHOICE",
                    question = "Translate 'sot'",
                    correctAnswer = "fever",
                    optionA = "fever", optionB = "cough", optionC = "headache", optionD = "rest",
                    order = 1, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 14, wordId = 105, type = "MULTIPLE_CHOICE",
                    question = "Which word means '?au ??u'?",
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
                    question = "Dich: Ban nen ngh? ng?i h?m nay.",
                    correctAnswer = "You should rest today",
                    order = 4, difficulty = 2
                ),
                
                // Lesson 15 - Workplace
                ExerciseEntity(
                    lessonId = 15, wordId = 109, type = "MULTIPLE_CHOICE",
                    question = "'Phong hop' la gi?",
                    correctAnswer = "meeting room",
                    optionA = "meeting room", optionB = "deadline", optionC = "task", optionD = "report",
                    order = 1, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 15, wordId = 110, type = "MULTIPLE_CHOICE",
                    question = "Translate 'h?n ch?t'",
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
                    question = "Dich: Toi se tr?nh b?y h?m nay.",
                    correctAnswer = "I will present today",
                    order = 4, difficulty = 2
                ),
                
                // Lesson 16 - Home & Household
                ExerciseEntity(
                    lessonId = 16, wordId = 115, type = "MULTIPLE_CHOICE",
                    question = "Translate 'nh? b?p'",
                    correctAnswer = "kitchen",
                    optionA = "kitchen", optionB = "bedroom", optionC = "living room", optionD = "laundry",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 16, wordId = 117, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'ph?ng ng?'?",
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
                    question = "Dich: D?n d?p m?t th?i gian.",
                    correctAnswer = "Cleaning takes time",
                    order = 4, difficulty = 1
                ),
                
                // Lesson 17 - Hobbies & Free Time
                ExerciseEntity(
                    lessonId = 17, wordId = 121, type = "MULTIPLE_CHOICE",
                    question = "Translate '??c s?ch'",
                    correctAnswer = "reading",
                    optionA = "reading", optionB = "painting", optionC = "hiking", optionD = "swimming",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 17, wordId = 125, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'b?i l?i'?",
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
                    question = "Dich: L?m v??n r?t y?n b?nh.",
                    correctAnswer = "Gardening is peaceful",
                    order = 4, difficulty = 1
                ),
                
                // Lesson 18 - Technology & Devices
                ExerciseEntity(
                    lessonId = 18, wordId = 127, type = "MULTIPLE_CHOICE",
                    question = "What is '?i?n tho?i th?ng minh'?",
                    correctAnswer = "smartphone",
                    optionA = "smartphone", optionB = "laptop", optionC = "charger", optionD = "password",
                    order = 1, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 18, wordId = 130, type = "MULTIPLE_CHOICE",
                    question = "Translate 'm?t kh?u'",
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
                    question = "Dich: C?p nh?t ph?n m?m.",
                    correctAnswer = "Update the software",
                    order = 4, difficulty = 2
                ),
                
                // Lesson 19 - Weather & Events
                ExerciseEntity(
                    lessonId = 19, wordId = 133, type = "MULTIPLE_CHOICE",
                    question = "How to say 'nang'?",
                    correctAnswer = "sunny",
                    optionA = "sunny", optionB = "rainy", optionC = "storm", optionD = "festival",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 19, wordId = 136, type = "MULTIPLE_CHOICE",
                    question = "Translate 'd? b?o th?i ti?t'",
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
                    question = "Dich: B?o ?ang ??n.",
                    correctAnswer = "A storm is coming",
                    order = 4, difficulty = 2
                ),
                
                // Lesson 20 - Emergency & Help
                ExerciseEntity(
                    lessonId = 20, wordId = 139, type = "MULTIPLE_CHOICE",
                    question = "Translate 'kh?n c?p'",
                    correctAnswer = "emergency",
                    optionA = "emergency", optionB = "ambulance", optionC = "police", optionD = "fire",
                    order = 1, difficulty = 3
                ),
                ExerciseEntity(
                    lessonId = 20, wordId = 141, type = "MULTIPLE_CHOICE",
                    question = "What is 'c?nh s?t'?",
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
                    question = "Dich: Goi xe c?u th??ng.",
                    correctAnswer = "Call an ambulance",
                    order = 4, difficulty = 3
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





