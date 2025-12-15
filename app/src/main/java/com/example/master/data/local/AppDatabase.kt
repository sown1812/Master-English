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
        AchievementEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun wordDao(): WordDao
    abstract fun lessonDao(): LessonDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun userDao(): UserDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun achievementDao(): AchievementDao
    
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
                    .addMigrations(MIGRATION_1_2)
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
        
        private suspend fun seedDatabase(@Suppress("UNUSED_PARAMETER") database: AppDatabase) {
            // Lesson content now comes from the backend via ContentSyncManager; skip offline seeding.
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No schema changes; data seeding happens in onOpen
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
                    description = "Phòng ốc, việc nhà và đồ gia dụng",
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
                    description = "Thiết bị, ứng dụng và sự cố công nghệ",
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
                WordEntity("hello", "xin chao", "HEL-oh", "interjection", "Hello, how are you?", "Xin ch?o, b?n kh?e kh?ng?", lessonId = 1, difficulty = 1, category = "greetings"),
                WordEntity("hi", "chao", "hai", "interjection", "Hi! Nice to meet you.", "Ch?o! R?t vui ???c g?p b?n.", lessonId = 1, difficulty = 1, category = "greetings"),
                WordEntity("goodbye", "t?m bi?t", "gud-bai", "interjection", "Goodbye, see you soon!", "T?m bi?t, h?n g?p l?i!", lessonId = 1, difficulty = 1, category = "greetings"),
                WordEntity("please", "l?m ?n", "pleez", "adverb", "Please help me.", "L?m ?n gi?p t?i.", lessonId = 1, difficulty = 1, category = "politeness"),
                WordEntity("thank you", "c?m ?n", "thangk-yoo", "phrase", "Thank you very much!", "C?m ?n b?n r?t nhi?u!", lessonId = 1, difficulty = 1, category = "politeness"),
                WordEntity("yes", "vang", "yes", "adverb", "Yes, I understand.", "V?ng, t?i hi?u.", lessonId = 1, difficulty = 1, category = "basics"),
                WordEntity("no", "khong", "no", "adverb", "No, thank you.", "Khong, c?m ?n.", lessonId = 1, difficulty = 1, category = "basics"),
                WordEntity("I", "toi", "ai", "pronoun", "I am a student.", "T?i l? h?c sinh.", lessonId = 1, difficulty = 1, category = "pronoun"),
                WordEntity("you", "ban", "yoo", "pronoun", "You are kind.", "B?n r?t t?t b?ng.", lessonId = 1, difficulty = 1, category = "pronoun"),
                WordEntity("am", "l? (?i v?i I)", "am", "verb", "I am Nam.", "T?i l? Nam.", lessonId = 1, difficulty = 1, category = "verb"),
                WordEntity("are", "l? (?i v?i you/we/they)", "ar", "verb", "You are my friend.", "B?n l? b?n c?a t?i.", lessonId = 1, difficulty = 1, category = "verb"),
                WordEntity("name", "ten", "naym", "noun", "My name is Mai.", "T?n t?i l? Mai.", lessonId = 1, difficulty = 1, category = "introductions"),
                
                // Lesson 2 - Basics 2 (12 words)
                WordEntity("he", "anh ?y", "hee", "pronoun", "He is a teacher.", "Anh ?y l? gi?o vi?n.", lessonId = 2, difficulty = 1, category = "pronoun"),
                WordEntity("she", "c? ?y", "shee", "pronoun", "She is a doctor.", "C? ?y l? b?c s?.", lessonId = 2, difficulty = 1, category = "pronoun"),
                WordEntity("we", "ch?ng t?i", "wee", "pronoun", "We are from Vietnam.", "Ch?ng t?i ??n t? Vi?t Nam.", lessonId = 2, difficulty = 1, category = "pronoun"),
                WordEntity("they", "ho", "thay", "pronoun", "They are students.", "H? l? h?c sinh.", lessonId = 2, difficulty = 1, category = "pronoun"),
                WordEntity("man", "ng??i ??n ?ng", "man", "noun", "The man is tall.", "Ng??i ??n ?ng ?? cao.", lessonId = 2, difficulty = 1, category = "people"),
                WordEntity("woman", "ph? n?", "wuh-muhn", "noun", "The woman drinks tea.", "Nguoi ph? n? uong tra.", lessonId = 2, difficulty = 1, category = "people"),
                WordEntity("boy", "con trai", "boy", "noun", "The boy reads a book.", "C?u b? ?ang ??c s?ch.", lessonId = 2, difficulty = 1, category = "people"),
                WordEntity("girl", "con g?i", "gurl", "noun", "The girl eats rice.", "C? b? ?n c?m.", lessonId = 2, difficulty = 1, category = "people"),
                WordEntity("eat", "an", "eet", "verb", "We eat breakfast.", "Ch?ng t?i ?n s?ng.", lessonId = 2, difficulty = 1, category = "verb"),
                WordEntity("drink", "uong", "drink", "verb", "They drink coffee.", "H? u?ng c? ph?.", lessonId = 2, difficulty = 1, category = "verb"),
                WordEntity("read", "doc", "reed", "verb", "I read every day.", "T?i ??c s?ch m?i ng?y.", lessonId = 2, difficulty = 1, category = "verb"),
                WordEntity("write", "viet", "rait", "verb", "She writes a letter.", "C? ?y vi?t th?.", lessonId = 2, difficulty = 1, category = "verb"),
                
                // Lesson 3 - Phrases (12 words)
                WordEntity("excuse me", "xin l?i", "ex-kyooz mee", "phrase", "Excuse me, where is the bus?", "Xin l?i, tr?m xe ? ??u?", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("sorry", "xin l?i", "sor-ree", "adjective", "I am sorry.", "Toi xin l?i.", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("good morning", "ch?o bu?i s?ng", "gud MOR-ning", "phrase", "Good morning, everyone!", "Ch?o bu?i s?ng m?i ng??i!", lessonId = 3, difficulty = 1, category = "greetings"),
                WordEntity("good night", "ch?c ng? ngon", "gud nait", "phrase", "Good night, see you tomorrow.", "Ch?c ng? ngon, h?n g?p b?n ng?y mai.", lessonId = 3, difficulty = 1, category = "greetings"),
                WordEntity("see you later", "h?n g?p l?i sau", "see-yoo-lay-ter", "phrase", "See you later!", "H?n g?p l?i sau!", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("see you soon", "g?p l?i s?m th?i", "see-yoo-soon", "phrase", "See you soon.", "G?p l?i b?n s?m th?i.", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("how are you", "b?n kh?e kh?ng", "how-are-yoo", "phrase", "Hi, how are you?", "Chao, b?n kh?e kh?ng?", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("I am fine", "t?i kh?e", "ai-am-fain", "phrase", "I am fine, thank you.", "Toi khoe, c?m ?n.", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("what is your name", "b?n t?n g?", "wot-iz-yor-naym", "phrase", "What is your name?", "B?n t?n g??", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("nice to meet you", "r?t vui ???c g?p b?n", "nais-tu-meet-yoo", "phrase", "Nice to meet you!", "R?t vui ???c g?p b?n!", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("welcome", "ch?o m?ng", "wel-kum", "phrase", "Welcome to Hanoi!", "Ch?o m?ng ??n H? N?i!", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("good luck", "ch?c may m?n", "gud luhk", "phrase", "Good luck on your test!", "Ch?c may m?n khi thi!", lessonId = 3, difficulty = 1, category = "phrases"),
                // Lesson 4 - Food & Drinks (12 words)
                WordEntity("water", "nuoc", "waw-ter", "noun", "I drink water.", "T?i u?ng n??c.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("coffee", "c? ph?", "ko-fee", "noun", "She likes coffee.", "Co ay thich c? ph?.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("tea", "tra", "tee", "noun", "Tea or coffee?", "Tra hay c? ph??", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("bread", "b?nh m?", "bred", "noun", "I eat bread.", "Toi an b?nh m?.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("rice", "com", "rais", "noun", "We cook rice.", "Ch?ng t?i n?u c?m.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("soup", "sup", "soop", "noun", "The soup is hot.", "B?t s?p n?ng.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("apple", "tao", "ap-ul", "noun", "The apple is red.", "Qu? t?o m?u ??.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("banana", "chuoi", "buh-na-na", "noun", "Bananas are sweet.", "Chu?i ng?t.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("chicken", "ga", "chik-en", "noun", "I eat chicken.", "T?i ?n th?t g?.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("fish", "ca", "fish", "noun", "Fish and rice.", "C? v? c?m.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("delicious", "ngon", "di-li-shus", "adjective", "The meal is delicious.", "B?a ?n ngon.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("hungry", "doi", "hun-gree", "adjective", "I am hungry.", "T?i ?ang ??i.", lessonId = 4, difficulty = 1, category = "food"),
                
                // Lesson 5 - Travel Essentials (6 words)
                WordEntity("bus", "xe bu?t", "bus", "noun", "Take the bus.", "Bat xe bu?t.", lessonId = 5, difficulty = 1, category = "travel"),
                WordEntity("train", "t?u h?a", "tray-n", "noun", "The train is late.", "T?u h?a b? tr?.", lessonId = 5, difficulty = 1, category = "travel"),
                WordEntity("taxi", "taxi", "tak-see", "noun", "Call a taxi.", "G?i m?t chi?c taxi.", lessonId = 5, difficulty = 1, category = "travel"),
                WordEntity("airport", "s?n bay", "air-port", "noun", "The airport is far.", "S?n bay kh? xa.", lessonId = 5, difficulty = 1, category = "travel"),
                WordEntity("ticket", "ve", "tik-it", "noun", "I need a ticket.", "T?i c?n v?.", lessonId = 5, difficulty = 1, category = "travel"),
                WordEntity("passport", "h? chi?u", "pass-port", "noun", "Show your passport.", "Xuat trinh h? chi?u.", lessonId = 5, difficulty = 1, category = "travel"),
                
                // Lesson 6 - Family (6 words)
                WordEntity("father", "cha", "fa-ther", "noun", "My father is kind.", "Cha t?i r?t t?t.", lessonId = 6, difficulty = 1, category = "family"),
                WordEntity("mother", "me", "muh-ther", "noun", "My mother cooks.", "M? t?i n?u ?n.", lessonId = 6, difficulty = 1, category = "family"),
                WordEntity("brother", "anh/em trai", "bru-ther", "noun", "He is my brother.", "Anh ?y l? anh trai t?i.", lessonId = 6, difficulty = 1, category = "family"),
                WordEntity("sister", "ch?/em g?i", "sis-ter", "noun", "She is my sister.", "C? ?y l? ch? g?i t?i.", lessonId = 6, difficulty = 1, category = "family"),
                WordEntity("son", "con trai", "sun", "noun", "This is my son.", "??y l? con trai t?i.", lessonId = 6, difficulty = 1, category = "family"),
                WordEntity("daughter", "con g?i", "daw-ter", "noun", "That is my daughter.", "Do la con g?i toi.", lessonId = 6, difficulty = 1, category = "family"),
                
                // Lesson 7 - Colors & Clothing (6 words)
                WordEntity("red", "m?u ??", "red", "adjective", "The apple is red.", "Qu? t?o m?u ??.", lessonId = 7, difficulty = 1, category = "colors"),
                WordEntity("blue", "m?u xanh d??ng", "blu", "adjective", "The sky is blue.", "B?u tr?i m?u xanh.", lessonId = 7, difficulty = 1, category = "colors"),
                WordEntity("green", "m?u xanh l?", "green", "adjective", "The leaf is green.", "Chiec la m?u xanh l?.", lessonId = 7, difficulty = 1, category = "colors"),
                WordEntity("shirt", "?o s? mi", "shurt", "noun", "I wear a shirt.", "Toi mac ?o s? mi.", lessonId = 7, difficulty = 1, category = "clothes"),
                WordEntity("pants", "qu?n d?i", "pants", "noun", "These pants are new.", "Chi?c qu?n n?y m?i.", lessonId = 7, difficulty = 1, category = "clothes"),
                WordEntity("shoes", "??i gi?y", "shooz", "noun", "I like these shoes.", "Toi thich ??i gi?y nay.", lessonId = 7, difficulty = 1, category = "clothes"),
                
                // Lesson 8 - Numbers & Time (6 words)
                WordEntity("one", "mot", "wun", "number", "One apple, please.", "Mot qua tao, l?m ?n.", lessonId = 8, difficulty = 1, category = "numbers"),
                WordEntity("two", "hai", "too", "number", "Two tickets.", "Hai v?.", lessonId = 8, difficulty = 1, category = "numbers"),
                WordEntity("three", "ba", "three", "number", "Three cups of tea.", "Ba ly tr?.", lessonId = 8, difficulty = 1, category = "numbers"),
                WordEntity("today", "h?m nay", "to-day", "noun", "See you today.", "Hen ban h?m nay.", lessonId = 8, difficulty = 1, category = "time"),
                WordEntity("tomorrow", "ng?y mai", "to-mor-row", "noun", "See you tomorrow.", "Hen ban ng?y mai.", lessonId = 8, difficulty = 1, category = "time"),
                WordEntity("yesterday", "h?m qua", "yes-ter-day", "noun", "Yesterday was busy.", "H?m qua r?t b?n.", lessonId = 8, difficulty = 1, category = "time"),
                
                // Lesson 9 - School & Work (6 words)
                WordEntity("teacher", "gi?o vi?n", "tee-cher", "noun", "She is a teacher.", "Co ay la gi?o vi?n.", lessonId = 9, difficulty = 1, category = "school"),
                WordEntity("student", "h?c sinh", "stoo-dent", "noun", "I am a student.", "T?i l? h?c sinh.", lessonId = 9, difficulty = 1, category = "school"),
                WordEntity("school", "truong hoc", "skool", "noun", "The school is big.", "Tr??ng h?c n?y l?n.", lessonId = 9, difficulty = 1, category = "school"),
                WordEntity("job", "c?ng vi?c", "job", "noun", "I love my job.", "Toi thich c?ng vi?c.", lessonId = 9, difficulty = 1, category = "work"),
                WordEntity("office", "v?n ph?ng", "of-fis", "noun", "The office is near.", "V?n ph?ng ? g?n.", lessonId = 9, difficulty = 1, category = "work"),
                WordEntity("meeting", "cu?c h?p", "mee-ting", "noun", "I have a meeting.", "Toi co mot cu?c h?p.", lessonId = 9, difficulty = 1, category = "work"),
                
                // Lesson 10 - Daily Routine (6 words)
                WordEntity("wake up", "ng? d?y", "wake-up", "verb", "I wake up early.", "Toi ng? d?y som.", lessonId = 10, difficulty = 1, category = "routine"),
                WordEntity("breakfast", "bu?i s?ng", "brek-fust", "noun", "Breakfast at 7 am.", "?n s?ng l?c 7 gi?.", lessonId = 10, difficulty = 1, category = "routine"),
                WordEntity("lunch", "bu?i tr?a", "lunch", "noun", "Lunch with friends.", "?n tr?a v?i b?n.", lessonId = 10, difficulty = 1, category = "routine"),
                WordEntity("dinner", "bu?i t?i", "din-ner", "noun", "Dinner at home.", "?n t?i ? nh?.", lessonId = 10, difficulty = 1, category = "routine"),
                WordEntity("sleep", "ngu", "sleep", "verb", "I sleep at 11 pm.", "T?i ng? l?c 11 gi?.", lessonId = 10, difficulty = 1, category = "routine"),
                WordEntity("exercise", "t?p th? d?c", "ek-ser-size", "verb", "I exercise every day.", "Toi t?p th? d?c moi ngay.", lessonId = 10, difficulty = 1, category = "routine"),
                
                // Lesson 11 - Transport & Directions (6 words)
                WordEntity("bus stop", "tram xe bu?t", "bus-stop", "noun", "The bus stop is near.", "Tram xe bu?t o gan.", lessonId = 11, difficulty = 2, category = "travel"),
                WordEntity("station", "nh? ga", "stay-shun", "noun", "Meet me at the station.", "Gap toi o nh? ga.", lessonId = 11, difficulty = 2, category = "travel"),
                WordEntity("ticket booth", "qu?y v?", "tik-it booth", "noun", "Buy tickets at the booth.", "Mua v? ? qu?y.", lessonId = 11, difficulty = 2, category = "travel"),
                WordEntity("turn left", "r? tr?i", "turn left", "phrase", "Turn left at the corner.", "R? tr?i ? g?c ???ng.", lessonId = 11, difficulty = 2, category = "directions"),
                WordEntity("straight ahead", "?i th?ng", "straight ahead", "phrase", "Go straight ahead 200 meters.", "?i th?ng 200 m?t.", lessonId = 11, difficulty = 2, category = "directions"),
                WordEntity("traffic jam", "k?t xe", "traf-ik jam", "noun", "There is a traffic jam.", "Dang k?t xe.", lessonId = 11, difficulty = 2, category = "travel"),
                
                // Lesson 12 - Shopping & Money (6 words)
                WordEntity("price", "gia", "price", "noun", "What is the price?", "Gi? bao nhi?u?", lessonId = 12, difficulty = 2, category = "shopping"),
                WordEntity("discount", "gi?m gi?", "dis-count", "noun", "Do you have a discount?", "Ban co gi?m gi? khong?", lessonId = 12, difficulty = 2, category = "shopping"),
                WordEntity("cash", "ti?n m?t", "cash", "noun", "I pay with cash.", "Toi tra ti?n m?t.", lessonId = 12, difficulty = 2, category = "shopping"),
                WordEntity("card", "the", "card", "noun", "Can I pay by card?", "T?i c? th? tr? b?ng th? kh?ng?", lessonId = 12, difficulty = 2, category = "shopping"),
                WordEntity("receipt", "h?a ??n", "re-seet", "noun", "Here is your receipt.", "Day la h?a ??n cua ban.", lessonId = 12, difficulty = 2, category = "shopping"),
                WordEntity("expensive", "dat", "ex-pen-siv", "adjective", "That bag is expensive.", "Chi?c t?i ?? ??t.", lessonId = 12, difficulty = 2, category = "shopping"),
                
                // Lesson 13 - Restaurant & Cafe (6 words)
                WordEntity("menu", "th?c ??n", "men-yoo", "noun", "Can I see the menu?", "Cho toi xem th?c ??n.", lessonId = 13, difficulty = 2, category = "food"),
                WordEntity("order", "g?i m?n", "or-der", "verb", "We will order now.", "Chung toi se g?i m?n bay gio.", lessonId = 13, difficulty = 2, category = "food"),
                WordEntity("reservation", "??t b?n", "re-zer-vay-shun", "noun", "I have a reservation.", "Toi da ??t b?n truoc.", lessonId = 13, difficulty = 2, category = "food"),
                WordEntity("bill", "h?a ??n", "bill", "noun", "Please bring the bill.", "Cho xin h?a ??n.", lessonId = 13, difficulty = 2, category = "food"),
                WordEntity("tip", "ti?n tip", "tip", "noun", "Leave a small tip.", "De lai chut ti?n tip.", lessonId = 13, difficulty = 2, category = "food"),
                WordEntity("delicious", "ngon", "di-li-shus", "adjective", "The soup is delicious.", "M?n s?p r?t ngon.", lessonId = 13, difficulty = 2, category = "food"),
                
                // Lesson 14 - Health & Doctor (6 words)
                WordEntity("fever", "sot", "fee-ver", "noun", "I have a fever.", "T?i b? s?t.", lessonId = 14, difficulty = 2, category = "health"),
                WordEntity("cough", "ho", "coff", "noun", "This cough is bad.", "C?n ho n?y n?ng.", lessonId = 14, difficulty = 2, category = "health"),
                WordEntity("headache", "?au ??u", "hed-ake", "noun", "I have a headache.", "Toi bi ?au ??u.", lessonId = 14, difficulty = 2, category = "health"),
                WordEntity("medicine", "thuoc", "med-i-sin", "noun", "Take this medicine twice a day.", "U?ng thu?c n?y 2 l?n m?i ng?y.", lessonId = 14, difficulty = 2, category = "health"),
                WordEntity("appointment", "l?ch h?n", "ap-point-ment", "noun", "I need a doctor appointment.", "T?i c?n h?n b?c s?.", lessonId = 14, difficulty = 2, category = "health"),
                WordEntity("rest", "ngh? ng?i", "rest", "verb", "You should rest today.", "Ban nen ngh? ng?i h?m nay.", lessonId = 14, difficulty = 2, category = "health"),
                
                // Lesson 15 - Workplace (6 words)
                WordEntity("meeting room", "ph?ng h?p", "mee-ting room", "noun", "The meeting room is ready.", "Ph?ng h?p ?? s?n s?ng.", lessonId = 15, difficulty = 2, category = "work"),
                WordEntity("deadline", "h?n ch?t", "dead-line", "noun", "The deadline is Friday.", "H?n ch?t l? th? S?u.", lessonId = 15, difficulty = 2, category = "work"),
                WordEntity("task", "nhi?m v?", "task", "noun", "Assign the new task.", "Giao nhi?m v? moi.", lessonId = 15, difficulty = 2, category = "work"),
                WordEntity("colleague", "??ng nghi?p", "kol-leeg", "noun", "She is my colleague.", "Co ay la ??ng nghi?p cua toi.", lessonId = 15, difficulty = 2, category = "work"),
                WordEntity("report", "b?o c?o", "ri-port", "noun", "Send the weekly report.", "Gui b?o c?o hang tuan.", lessonId = 15, difficulty = 2, category = "work"),
                WordEntity("present", "tr?nh b?y", "pre-zent", "verb", "I will present today.", "Toi se tr?nh b?y h?m nay.", lessonId = 15, difficulty = 2, category = "work"),
                
                // Lesson 16 - Home & Household (6 words)
                WordEntity("kitchen", "nh? b?p", "kitch-en", "noun", "The kitchen is clean.", "Nh? b?p s?ch.", lessonId = 16, difficulty = 1, category = "home"),
                WordEntity("living room", "ph?ng kh?ch", "liv-ing room", "noun", "We sit in the living room.", "Chung toi ngoi o ph?ng kh?ch.", lessonId = 16, difficulty = 1, category = "home"),
                WordEntity("bedroom", "ph?ng ng?", "bed-room", "noun", "The bedroom is cozy.", "Ph?ng ng? ?m c?ng.", lessonId = 16, difficulty = 1, category = "home"),
                WordEntity("vacuum", "h?t b?i", "vac-yoom", "verb", "Please vacuum the floor.", "Lam on h?t b?i san.", lessonId = 16, difficulty = 1, category = "home"),
                WordEntity("laundry", "gi?t ??", "lawn-dree", "noun", "Do the laundry on Sunday.", "Gi?t ?? v?o Ch? nh?t.", lessonId = 16, difficulty = 1, category = "home"),
                WordEntity("cleaning", "d?n d?p", "klee-ning", "noun", "Cleaning takes time.", "D?n d?p m?t th?i gian.", lessonId = 16, difficulty = 1, category = "home"),
                
                // Lesson 17 - Hobbies & Free Time (6 words)
                WordEntity("reading", "??c s?ch", "ree-ding", "noun", "Reading is relaxing.", "??c s?ch gi?p th? gi?n.", lessonId = 17, difficulty = 1, category = "hobby"),
                WordEntity("painting", "v? tranh", "paint-ing", "noun", "I like painting.", "Toi thich v? tranh.", lessonId = 17, difficulty = 1, category = "hobby"),
                WordEntity("hiking", "?i b? ???ng d?i", "hi-king", "noun", "We go hiking on weekends.", "Ch?ng t?i ?i hiking cu?i tu?n.", lessonId = 17, difficulty = 1, category = "hobby"),
                WordEntity("playing guitar", "ch?i guitar", "play-ing gui-tar", "verb", "He enjoys playing guitar.", "Anh ay thich ch?i guitar.", lessonId = 17, difficulty = 1, category = "hobby"),
                WordEntity("swimming", "b?i l?i", "swim-ing", "noun", "Swimming is my hobby.", "B?i l?i l? s? th?ch c?a t?i.", lessonId = 17, difficulty = 1, category = "hobby"),
                WordEntity("gardening", "l?m v??n", "gar-den-ing", "noun", "Gardening is peaceful.", "L?m v??n r?t y?n b?nh.", lessonId = 17, difficulty = 1, category = "hobby"),
                
                // Lesson 18 - Technology & Devices (6 words)
                WordEntity("smartphone", "?i?n tho?i th?ng minh", "smart-phone", "noun", "My smartphone is slow.", "?i?n tho?i th?ng minh c?a t?i ch?m.", lessonId = 18, difficulty = 2, category = "technology"),
                WordEntity("laptop", "m?y t?nh x?ch tay", "lap-top", "noun", "Charge your laptop.", "Sac m?y t?nh x?ch tay.", lessonId = 18, difficulty = 2, category = "technology"),
                WordEntity("charger", "sac", "char-jer", "noun", "I lost my charger.", "T?i m?t s?c r?i.", lessonId = 18, difficulty = 2, category = "technology"),
                WordEntity("password", "m?t kh?u", "pass-word", "noun", "Reset your password.", "Dat lai m?t kh?u.", lessonId = 18, difficulty = 2, category = "technology"),
                WordEntity("app", "?ng d?ng", "app", "noun", "Download the new app.", "Tai ?ng d?ng moi.", lessonId = 18, difficulty = 2, category = "technology"),
                WordEntity("update", "c?p nh?t", "up-date", "verb", "Update the software.", "C?p nh?t ph?n m?m.", lessonId = 18, difficulty = 2, category = "technology"),
                
                // Lesson 19 - Weather & Events (6 words)
                WordEntity("sunny", "nang", "sun-ny", "adjective", "It is sunny today.", "H?m nay tr?i n?ng.", lessonId = 19, difficulty = 1, category = "weather"),
                WordEntity("rainy", "mua", "ray-ny", "adjective", "The weather is rainy.", "Tr?i ?ang m?a.", lessonId = 19, difficulty = 1, category = "weather"),
                WordEntity("storm", "bao", "storm", "noun", "A storm is coming.", "B?o ?ang ??n.", lessonId = 19, difficulty = 2, category = "weather"),
                WordEntity("forecast", "d? b?o th?i ti?t", "for-cast", "noun", "Check the forecast.", "Kiem tra d? b?o th?i ti?t.", lessonId = 19, difficulty = 2, category = "weather"),
                WordEntity("picnic", "?i ch?i ngo?i tr?i", "pic-nic", "noun", "Plan a picnic this weekend.", "L?n k? ho?ch picnic cu?i tu?n n?y.", lessonId = 19, difficulty = 1, category = "events"),
                WordEntity("festival", "l? h?i", "fes-ti-val", "noun", "The festival is crowded.", "L? h?i ??ng ??c.", lessonId = 19, difficulty = 2, category = "events"),
                
                // Lesson 20 - Emergency & Help (6 words)
                WordEntity("emergency", "kh?n c?p", "e-mer-gen-cy", "noun", "Call in an emergency.", "Goi khi kh?n c?p.", lessonId = 20, difficulty = 3, category = "safety"),
                WordEntity("ambulance", "xe c?u th??ng", "am-byu-lans", "noun", "Call an ambulance.", "Goi xe c?u th??ng.", lessonId = 20, difficulty = 3, category = "safety"),
                WordEntity("police", "c?nh s?t", "po-lice", "noun", "Call the police.", "Goi c?nh s?t.", lessonId = 20, difficulty = 3, category = "safety"),
                WordEntity("fire", "chay", "fire", "noun", "There is a fire!", "C? ch?y!", lessonId = 20, difficulty = 3, category = "safety"),
                WordEntity("help", "gi?p ??", "help", "verb", "Please help me!", "L?m ?n gi?p t?i!", lessonId = 20, difficulty = 2, category = "safety"),
                WordEntity("lost", "l?c ???ng", "lost", "adjective", "I am lost.", "T?i b? l?c.", lessonId = 20, difficulty = 2, category = "directions"),
                
                // Lesson 1 - Additional words to complete full 20-word set
                WordEntity(
                    id = 1001,
                    word = "good morning",
                    translation = "ch?o bu?i s?ng",
                    pronunciation = "gud MOR-ning",
                    partOfSpeech = "phrase",
                    exampleSentence = "Good morning, class!",
                    exampleTranslation = "Chao bu?i s?ng ca lop!",
                    lessonId = 1,
                    difficulty = 1,
                    category = "greetings"
                ),
                WordEntity(
                    id = 1002,
                    word = "good afternoon",
                    translation = "ch?o bu?i chi?u",
                    pronunciation = "gud AF-ter-noon",
                    partOfSpeech = "phrase",
                    exampleSentence = "Good afternoon, how are you?",
                    exampleTranslation = "Chao buoi chieu, b?n kh?e kh?ng?",
                    lessonId = 1,
                    difficulty = 1,
                    category = "greetings"
                ),
                WordEntity(
                    id = 1003,
                    word = "good evening",
                    translation = "chao bu?i t?i",
                    pronunciation = "gud EEV-ning",
                    partOfSpeech = "phrase",
                    exampleSentence = "Good evening everyone.",
                    exampleTranslation = "Chao bu?i t?i moi nguoi.",
                    lessonId = 1,
                    difficulty = 1,
                    category = "greetings"
                ),
                WordEntity(
                    id = 1004,
                    word = "good night",
                    translation = "ch?c ng? ngon",
                    pronunciation = "gud nait",
                    partOfSpeech = "phrase",
                    exampleSentence = "Good night and sweet dreams.",
                    exampleTranslation = "Ch?c ng? ngon v? m? ??p.",
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
                    exampleTranslation = "R?t vui ???c g?p b?n.",
                    lessonId = 1,
                    difficulty = 1,
                    category = "introductions"
                ),
                WordEntity(
                    id = 1006,
                    word = "meet",
                    translation = "gap",
                    pronunciation = "meet",
                    partOfSpeech = "verb",
                    exampleSentence = "I want to meet new friends.",
                    exampleTranslation = "T?i mu?n g?p b?n m?i.",
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
                    exampleTranslation = "Toi xin l?i vi den tre.",
                    lessonId = 1,
                    difficulty = 1,
                    category = "politeness"
                ),
                WordEntity(
                    id = 1008,
                    word = "excuse me",
                    translation = "xin phep / xin l?i",
                    pronunciation = "ex-kyooz mee",
                    partOfSpeech = "phrase",
                    exampleSentence = "Excuse me, where is the bus stop?",
                    exampleTranslation = "Xin l?i, tr?m xe ? ??u?",
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
                    question = "What is 'xin chao' in English?",
                    correctAnswer = "hello",
                    optionA = "hello", optionB = "goodbye", optionC = "thank you", optionD = "please",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 3, type = "MULTIPLE_CHOICE",
                    question = "How do you say 't?m bi?t'?",
                    correctAnswer = "goodbye",
                    optionA = "hello", optionB = "goodbye", optionC = "yes", optionD = "no",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 5, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'c?m ?n'?",
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
                    question = "Translate: T?i l? Nam.",
                    correctAnswer = "I am Nam",
                    order = 6, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 6, type = "MATCHING",
                    question = "Match the greetings",
                    correctAnswer = "",
                    matchPairs = """[
                        {"left":"hello","right":"xin chao"},
                        {"left":"goodbye","right":"t?m bi?t"},
                        {"left":"please","right":"l?m ?n"},
                        {"left":"yes","right":"vang"}
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
                    question = "How do you say 'ch?o bu?i s?ng'?",
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
                    question = "Choose the phrase meaning 'xin l?i'",
                    correctAnswer = "sorry",
                    optionA = "thank you", optionB = "sorry", optionC = "please", optionD = "excuse me",
                    order = 11, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 1004, type = "TRANSLATION",
                    question = "Dich: Chuc ngu ngon.",
                    correctAnswer = "Good night",
                    order = 12, difficulty = 1
                ),
                
                // Lesson 2 - Basics 2
                ExerciseEntity(
                    lessonId = 2, wordId = 13, type = "MULTIPLE_CHOICE",
                    question = "How do you say 'anh ?y'?",
                    correctAnswer = "he",
                    optionA = "he", optionB = "she", optionC = "they", optionD = "we",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 2, wordId = 14, type = "MULTIPLE_CHOICE",
                    question = "Translate 'c? ?y'",
                    correctAnswer = "she",
                    optionA = "he", optionB = "she", optionC = "girl", optionD = "woman",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 2, wordId = 15, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'ch?ng t?i'?",
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
                    question = "Dich: Day la mot co gai.",
                    correctAnswer = "This is a girl",
                    order = 6, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 2, wordId = 16, type = "MATCHING",
                    question = "Match pronouns",
                    correctAnswer = "",
                    matchPairs = """[
                        {"left":"he","right":"anh ?y"},
                        {"left":"she","right":"c? ?y"},
                        {"left":"we","right":"ch?ng t?i"},
                        {"left":"they","right":"ho"}
                    ]""".trimIndent(),
                    order = 7, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 2, wordId = 24, type = "LISTENING",
                    question = "Choose the word for 'viet'",
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
