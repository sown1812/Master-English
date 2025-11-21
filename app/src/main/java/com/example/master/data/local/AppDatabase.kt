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
        
        private suspend fun seedDatabase(database: AppDatabase) {
            val lessonDao = database.lessonDao()
            val wordDao = database.wordDao()
            val exerciseDao = database.exerciseDao()
            
            val payload = loadSeedFromAssets() ?: SeedPayload(
                lessons = getInitialLessons(),
                words = getInitialWords(),
                exercises = getInitialExercises()
            )
            
            val currentLessons = runCatching { lessonDao.getTotalLessonsCount() }.getOrDefault(0)
            val currentWords = runCatching { wordDao.getTotalWordsCount() }.getOrDefault(0)
            val currentExercises = runCatching { exerciseDao.getTotalExercisesCount() }.getOrDefault(0)
            
            if (currentLessons < payload.lessons.size && payload.lessons.isNotEmpty()) {
                lessonDao.insertLessons(payload.lessons)
            }
            if (currentWords < payload.words.size && payload.words.isNotEmpty()) {
                wordDao.insertWords(payload.words)
            }
            if (currentExercises < payload.exercises.size && payload.exercises.isNotEmpty()) {
                exerciseDao.insertExercises(payload.exercises)
            }
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
                    totalWords = 12,
                    totalExercises = 8,
                    difficulty = "EASY",
                    category = "basics",
                    xpReward = 45,
                    coinsReward = 10,
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
                WordEntity("hello", "xin chao", "HEL-oh", "interjection", "Hello, how are you?", "Xin chao, ban khoe khong?", lessonId = 1, difficulty = 1, category = "greetings"),
                WordEntity("hi", "chao", "hai", "interjection", "Hi! Nice to meet you.", "Chao! Rat vui duoc gap ban.", lessonId = 1, difficulty = 1, category = "greetings"),
                WordEntity("goodbye", "tam biet", "gud-bai", "interjection", "Goodbye, see you soon!", "Tam biet, hen gap lai!", lessonId = 1, difficulty = 1, category = "greetings"),
                WordEntity("please", "lam on", "pleez", "adverb", "Please help me.", "Lam on giup toi.", lessonId = 1, difficulty = 1, category = "politeness"),
                WordEntity("thank you", "cam on", "thangk-yoo", "phrase", "Thank you very much!", "Cam on ban rat nhieu!", lessonId = 1, difficulty = 1, category = "politeness"),
                WordEntity("yes", "vang", "yes", "adverb", "Yes, I understand.", "Vang, toi hieu.", lessonId = 1, difficulty = 1, category = "basics"),
                WordEntity("no", "khong", "no", "adverb", "No, thank you.", "Khong, cam on.", lessonId = 1, difficulty = 1, category = "basics"),
                WordEntity("I", "toi", "ai", "pronoun", "I am a student.", "Toi la hoc sinh.", lessonId = 1, difficulty = 1, category = "pronoun"),
                WordEntity("you", "ban", "yoo", "pronoun", "You are kind.", "Ban rat tot bung.", lessonId = 1, difficulty = 1, category = "pronoun"),
                WordEntity("am", "la (di voi I)", "am", "verb", "I am Nam.", "Toi la Nam.", lessonId = 1, difficulty = 1, category = "verb"),
                WordEntity("are", "la (di voi you/we/they)", "ar", "verb", "You are my friend.", "Ban la ban cua toi.", lessonId = 1, difficulty = 1, category = "verb"),
                WordEntity("name", "ten", "naym", "noun", "My name is Mai.", "Ten toi la Mai.", lessonId = 1, difficulty = 1, category = "introductions"),
                
                // Lesson 2 - Basics 2 (12 words)
                WordEntity("he", "anh ay", "hee", "pronoun", "He is a teacher.", "Anh ay la giao vien.", lessonId = 2, difficulty = 1, category = "pronoun"),
                WordEntity("she", "co ay", "shee", "pronoun", "She is a doctor.", "Co ay la bac si.", lessonId = 2, difficulty = 1, category = "pronoun"),
                WordEntity("we", "chung toi", "wee", "pronoun", "We are from Vietnam.", "Chung toi den tu Viet Nam.", lessonId = 2, difficulty = 1, category = "pronoun"),
                WordEntity("they", "ho", "thay", "pronoun", "They are students.", "Ho la hoc sinh.", lessonId = 2, difficulty = 1, category = "pronoun"),
                WordEntity("man", "nguoi dan ong", "man", "noun", "The man is tall.", "Nguoi dan ong do cao.", lessonId = 2, difficulty = 1, category = "people"),
                WordEntity("woman", "phu nu", "wuh-muhn", "noun", "The woman drinks tea.", "Nguoi phu nu uong tra.", lessonId = 2, difficulty = 1, category = "people"),
                WordEntity("boy", "con trai", "boy", "noun", "The boy reads a book.", "Cau be dang doc sach.", lessonId = 2, difficulty = 1, category = "people"),
                WordEntity("girl", "con gai", "gurl", "noun", "The girl eats rice.", "Co be an com.", lessonId = 2, difficulty = 1, category = "people"),
                WordEntity("eat", "an", "eet", "verb", "We eat breakfast.", "Chung toi an sang.", lessonId = 2, difficulty = 1, category = "verb"),
                WordEntity("drink", "uong", "drink", "verb", "They drink coffee.", "Ho uong ca phe.", lessonId = 2, difficulty = 1, category = "verb"),
                WordEntity("read", "doc", "reed", "verb", "I read every day.", "Toi doc sach moi ngay.", lessonId = 2, difficulty = 1, category = "verb"),
                WordEntity("write", "viet", "rait", "verb", "She writes a letter.", "Co ay viet thu.", lessonId = 2, difficulty = 1, category = "verb"),
                
                // Lesson 3 - Phrases (12 words)
                WordEntity("excuse me", "xin loi", "ex-kyooz mee", "phrase", "Excuse me, where is the bus?", "Xin loi, tram xe o dau?", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("sorry", "xin loi", "sor-ree", "adjective", "I am sorry.", "Toi xin loi.", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("good morning", "chao buoi sang", "gud MOR-ning", "phrase", "Good morning, everyone!", "Chao buoi sang moi nguoi!", lessonId = 3, difficulty = 1, category = "greetings"),
                WordEntity("good night", "chuc ngu ngon", "gud nait", "phrase", "Good night, see you tomorrow.", "Chuc ngu ngon, hen gap ban ngay mai.", lessonId = 3, difficulty = 1, category = "greetings"),
                WordEntity("see you later", "hen gap lai sau", "see-yoo-lay-ter", "phrase", "See you later!", "Hen gap lai sau!", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("see you soon", "gap lai som thoi", "see-yoo-soon", "phrase", "See you soon.", "Gap lai ban som thoi.", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("how are you", "ban khoe khong", "how-are-yoo", "phrase", "Hi, how are you?", "Chao, ban khoe khong?", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("I am fine", "toi khoe", "ai-am-fain", "phrase", "I am fine, thank you.", "Toi khoe, cam on.", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("what is your name", "ban ten gi", "wot-iz-yor-naym", "phrase", "What is your name?", "Ban ten gi?", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("nice to meet you", "rat vui duoc gap ban", "nais-tu-meet-yoo", "phrase", "Nice to meet you!", "Rat vui duoc gap ban!", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("welcome", "chao mung", "wel-kum", "phrase", "Welcome to Hanoi!", "Chao mung den Ha Noi!", lessonId = 3, difficulty = 1, category = "phrases"),
                WordEntity("good luck", "chuc may man", "gud luhk", "phrase", "Good luck on your test!", "Chuc may man khi thi!", lessonId = 3, difficulty = 1, category = "phrases"),
                // Lesson 4 - Food & Drinks (12 words)
                WordEntity("water", "nuoc", "waw-ter", "noun", "I drink water.", "Toi uong nuoc.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("coffee", "ca phe", "ko-fee", "noun", "She likes coffee.", "Co ay thich ca phe.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("tea", "tra", "tee", "noun", "Tea or coffee?", "Tra hay ca phe?", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("bread", "banh mi", "bred", "noun", "I eat bread.", "Toi an banh mi.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("rice", "com", "rais", "noun", "We cook rice.", "Chung toi nau com.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("soup", "sup", "soop", "noun", "The soup is hot.", "Bat sup nong.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("apple", "tao", "ap-ul", "noun", "The apple is red.", "Qua tao mau do.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("banana", "chuoi", "buh-na-na", "noun", "Bananas are sweet.", "Chuoi ngot.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("chicken", "ga", "chik-en", "noun", "I eat chicken.", "Toi an thit ga.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("fish", "ca", "fish", "noun", "Fish and rice.", "Ca va com.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("delicious", "ngon", "di-li-shus", "adjective", "The meal is delicious.", "Bua an ngon.", lessonId = 4, difficulty = 1, category = "food"),
                WordEntity("hungry", "doi", "hun-gree", "adjective", "I am hungry.", "Toi dang doi.", lessonId = 4, difficulty = 1, category = "food"),
                
                // Lesson 5 - Travel Essentials (6 words)
                WordEntity("bus", "xe buyt", "bus", "noun", "Take the bus.", "Bat xe buyt.", lessonId = 5, difficulty = 1, category = "travel"),
                WordEntity("train", "tau hoa", "tray-n", "noun", "The train is late.", "Tau hoa bi tre.", lessonId = 5, difficulty = 1, category = "travel"),
                WordEntity("taxi", "taxi", "tak-see", "noun", "Call a taxi.", "Goi mot chiec taxi.", lessonId = 5, difficulty = 1, category = "travel"),
                WordEntity("airport", "san bay", "air-port", "noun", "The airport is far.", "San bay kha xa.", lessonId = 5, difficulty = 1, category = "travel"),
                WordEntity("ticket", "ve", "tik-it", "noun", "I need a ticket.", "Toi can ve.", lessonId = 5, difficulty = 1, category = "travel"),
                WordEntity("passport", "ho chieu", "pass-port", "noun", "Show your passport.", "Xuat trinh ho chieu.", lessonId = 5, difficulty = 1, category = "travel"),
                
                // Lesson 6 - Family (6 words)
                WordEntity("father", "cha", "fa-ther", "noun", "My father is kind.", "Cha toi rat tot.", lessonId = 6, difficulty = 1, category = "family"),
                WordEntity("mother", "me", "muh-ther", "noun", "My mother cooks.", "Me toi nau an.", lessonId = 6, difficulty = 1, category = "family"),
                WordEntity("brother", "anh/em trai", "bru-ther", "noun", "He is my brother.", "Anh ay la anh trai toi.", lessonId = 6, difficulty = 1, category = "family"),
                WordEntity("sister", "chi/em gai", "sis-ter", "noun", "She is my sister.", "Co ay la chi gai toi.", lessonId = 6, difficulty = 1, category = "family"),
                WordEntity("son", "con trai", "sun", "noun", "This is my son.", "Day la con trai toi.", lessonId = 6, difficulty = 1, category = "family"),
                WordEntity("daughter", "con gai", "daw-ter", "noun", "That is my daughter.", "Do la con gai toi.", lessonId = 6, difficulty = 1, category = "family"),
                
                // Lesson 7 - Colors & Clothing (6 words)
                WordEntity("red", "mau do", "red", "adjective", "The apple is red.", "Qua tao mau do.", lessonId = 7, difficulty = 1, category = "colors"),
                WordEntity("blue", "mau xanh duong", "blu", "adjective", "The sky is blue.", "Bau troi mau xanh.", lessonId = 7, difficulty = 1, category = "colors"),
                WordEntity("green", "mau xanh la", "green", "adjective", "The leaf is green.", "Chiec la mau xanh la.", lessonId = 7, difficulty = 1, category = "colors"),
                WordEntity("shirt", "ao so mi", "shurt", "noun", "I wear a shirt.", "Toi mac ao so mi.", lessonId = 7, difficulty = 1, category = "clothes"),
                WordEntity("pants", "quan dai", "pants", "noun", "These pants are new.", "Chiec quan nay moi.", lessonId = 7, difficulty = 1, category = "clothes"),
                WordEntity("shoes", "doi giay", "shooz", "noun", "I like these shoes.", "Toi thich doi giay nay.", lessonId = 7, difficulty = 1, category = "clothes"),
                
                // Lesson 8 - Numbers & Time (6 words)
                WordEntity("one", "mot", "wun", "number", "One apple, please.", "Mot qua tao, lam on.", lessonId = 8, difficulty = 1, category = "numbers"),
                WordEntity("two", "hai", "too", "number", "Two tickets.", "Hai ve.", lessonId = 8, difficulty = 1, category = "numbers"),
                WordEntity("three", "ba", "three", "number", "Three cups of tea.", "Ba ly tra.", lessonId = 8, difficulty = 1, category = "numbers"),
                WordEntity("today", "hom nay", "to-day", "noun", "See you today.", "Hen ban hom nay.", lessonId = 8, difficulty = 1, category = "time"),
                WordEntity("tomorrow", "ngay mai", "to-mor-row", "noun", "See you tomorrow.", "Hen ban ngay mai.", lessonId = 8, difficulty = 1, category = "time"),
                WordEntity("yesterday", "hom qua", "yes-ter-day", "noun", "Yesterday was busy.", "Hom qua rat ban.", lessonId = 8, difficulty = 1, category = "time"),
                
                // Lesson 9 - School & Work (6 words)
                WordEntity("teacher", "giao vien", "tee-cher", "noun", "She is a teacher.", "Co ay la giao vien.", lessonId = 9, difficulty = 1, category = "school"),
                WordEntity("student", "hoc sinh", "stoo-dent", "noun", "I am a student.", "Toi la hoc sinh.", lessonId = 9, difficulty = 1, category = "school"),
                WordEntity("school", "truong hoc", "skool", "noun", "The school is big.", "Truong hoc nay lon.", lessonId = 9, difficulty = 1, category = "school"),
                WordEntity("job", "cong viec", "job", "noun", "I love my job.", "Toi thich cong viec.", lessonId = 9, difficulty = 1, category = "work"),
                WordEntity("office", "van phong", "of-fis", "noun", "The office is near.", "Van phong o gan.", lessonId = 9, difficulty = 1, category = "work"),
                WordEntity("meeting", "cuoc hop", "mee-ting", "noun", "I have a meeting.", "Toi co mot cuoc hop.", lessonId = 9, difficulty = 1, category = "work"),
                
                // Lesson 10 - Daily Routine (6 words)
                WordEntity("wake up", "ngu day", "wake-up", "verb", "I wake up early.", "Toi ngu day som.", lessonId = 10, difficulty = 1, category = "routine"),
                WordEntity("breakfast", "buoi sang", "brek-fust", "noun", "Breakfast at 7 am.", "An sang luc 7 gio.", lessonId = 10, difficulty = 1, category = "routine"),
                WordEntity("lunch", "buoi trua", "lunch", "noun", "Lunch with friends.", "An trua voi ban.", lessonId = 10, difficulty = 1, category = "routine"),
                WordEntity("dinner", "buoi toi", "din-ner", "noun", "Dinner at home.", "An toi o nha.", lessonId = 10, difficulty = 1, category = "routine"),
                WordEntity("sleep", "ngu", "sleep", "verb", "I sleep at 11 pm.", "Toi ngu luc 11 gio.", lessonId = 10, difficulty = 1, category = "routine"),
                WordEntity("exercise", "tap the duc", "ek-ser-size", "verb", "I exercise every day.", "Toi tap the duc moi ngay.", lessonId = 10, difficulty = 1, category = "routine"),
                
                // Lesson 11 - Transport & Directions (6 words)
                WordEntity("bus stop", "tram xe buyt", "bus-stop", "noun", "The bus stop is near.", "Tram xe buyt o gan.", lessonId = 11, difficulty = 2, category = "travel"),
                WordEntity("station", "nha ga", "stay-shun", "noun", "Meet me at the station.", "Gap toi o nha ga.", lessonId = 11, difficulty = 2, category = "travel"),
                WordEntity("ticket booth", "quay ve", "tik-it booth", "noun", "Buy tickets at the booth.", "Mua ve o quay.", lessonId = 11, difficulty = 2, category = "travel"),
                WordEntity("turn left", "re trai", "turn left", "phrase", "Turn left at the corner.", "Re trai o goc duong.", lessonId = 11, difficulty = 2, category = "directions"),
                WordEntity("straight ahead", "di thang", "straight ahead", "phrase", "Go straight ahead 200 meters.", "Di thang 200 met.", lessonId = 11, difficulty = 2, category = "directions"),
                WordEntity("traffic jam", "ket xe", "traf-ik jam", "noun", "There is a traffic jam.", "Dang ket xe.", lessonId = 11, difficulty = 2, category = "travel"),
                
                // Lesson 12 - Shopping & Money (6 words)
                WordEntity("price", "gia", "price", "noun", "What is the price?", "Gia bao nhieu?", lessonId = 12, difficulty = 2, category = "shopping"),
                WordEntity("discount", "giam gia", "dis-count", "noun", "Do you have a discount?", "Ban co giam gia khong?", lessonId = 12, difficulty = 2, category = "shopping"),
                WordEntity("cash", "tien mat", "cash", "noun", "I pay with cash.", "Toi tra tien mat.", lessonId = 12, difficulty = 2, category = "shopping"),
                WordEntity("card", "the", "card", "noun", "Can I pay by card?", "Toi co the tra bang the khong?", lessonId = 12, difficulty = 2, category = "shopping"),
                WordEntity("receipt", "hoa don", "re-seet", "noun", "Here is your receipt.", "Day la hoa don cua ban.", lessonId = 12, difficulty = 2, category = "shopping"),
                WordEntity("expensive", "dat", "ex-pen-siv", "adjective", "That bag is expensive.", "Chiec tui do dat.", lessonId = 12, difficulty = 2, category = "shopping"),
                
                // Lesson 13 - Restaurant & Cafe (6 words)
                WordEntity("menu", "thuc don", "men-yoo", "noun", "Can I see the menu?", "Cho toi xem thuc don.", lessonId = 13, difficulty = 2, category = "food"),
                WordEntity("order", "goi mon", "or-der", "verb", "We will order now.", "Chung toi se goi mon bay gio.", lessonId = 13, difficulty = 2, category = "food"),
                WordEntity("reservation", "dat ban", "re-zer-vay-shun", "noun", "I have a reservation.", "Toi da dat ban truoc.", lessonId = 13, difficulty = 2, category = "food"),
                WordEntity("bill", "hoa don", "bill", "noun", "Please bring the bill.", "Cho xin hoa don.", lessonId = 13, difficulty = 2, category = "food"),
                WordEntity("tip", "tien tip", "tip", "noun", "Leave a small tip.", "De lai chut tien tip.", lessonId = 13, difficulty = 2, category = "food"),
                WordEntity("delicious", "ngon", "di-li-shus", "adjective", "The soup is delicious.", "Mon sup rat ngon.", lessonId = 13, difficulty = 2, category = "food"),
                
                // Lesson 14 - Health & Doctor (6 words)
                WordEntity("fever", "sot", "fee-ver", "noun", "I have a fever.", "Toi bi sot.", lessonId = 14, difficulty = 2, category = "health"),
                WordEntity("cough", "ho", "coff", "noun", "This cough is bad.", "Con ho nay nang.", lessonId = 14, difficulty = 2, category = "health"),
                WordEntity("headache", "dau dau", "hed-ake", "noun", "I have a headache.", "Toi bi dau dau.", lessonId = 14, difficulty = 2, category = "health"),
                WordEntity("medicine", "thuoc", "med-i-sin", "noun", "Take this medicine twice a day.", "Uong thuoc nay 2 lan moi ngay.", lessonId = 14, difficulty = 2, category = "health"),
                WordEntity("appointment", "lich hen", "ap-point-ment", "noun", "I need a doctor appointment.", "Toi can hen bac si.", lessonId = 14, difficulty = 2, category = "health"),
                WordEntity("rest", "nghi ngoi", "rest", "verb", "You should rest today.", "Ban nen nghi ngoi hom nay.", lessonId = 14, difficulty = 2, category = "health"),
                
                // Lesson 15 - Workplace (6 words)
                WordEntity("meeting room", "phong hop", "mee-ting room", "noun", "The meeting room is ready.", "Phong hop da san sang.", lessonId = 15, difficulty = 2, category = "work"),
                WordEntity("deadline", "han chot", "dead-line", "noun", "The deadline is Friday.", "Han chot la thu Sau.", lessonId = 15, difficulty = 2, category = "work"),
                WordEntity("task", "nhiem vu", "task", "noun", "Assign the new task.", "Giao nhiem vu moi.", lessonId = 15, difficulty = 2, category = "work"),
                WordEntity("colleague", "dong nghiep", "kol-leeg", "noun", "She is my colleague.", "Co ay la dong nghiep cua toi.", lessonId = 15, difficulty = 2, category = "work"),
                WordEntity("report", "bao cao", "ri-port", "noun", "Send the weekly report.", "Gui bao cao hang tuan.", lessonId = 15, difficulty = 2, category = "work"),
                WordEntity("present", "trinh bay", "pre-zent", "verb", "I will present today.", "Toi se trinh bay hom nay.", lessonId = 15, difficulty = 2, category = "work"),
                
                // Lesson 16 - Home & Household (6 words)
                WordEntity("kitchen", "nha bep", "kitch-en", "noun", "The kitchen is clean.", "Nha bep sach.", lessonId = 16, difficulty = 1, category = "home"),
                WordEntity("living room", "phong khach", "liv-ing room", "noun", "We sit in the living room.", "Chung toi ngoi o phong khach.", lessonId = 16, difficulty = 1, category = "home"),
                WordEntity("bedroom", "phong ngu", "bed-room", "noun", "The bedroom is cozy.", "Phong ngu am cung.", lessonId = 16, difficulty = 1, category = "home"),
                WordEntity("vacuum", "hut bui", "vac-yoom", "verb", "Please vacuum the floor.", "Lam on hut bui san.", lessonId = 16, difficulty = 1, category = "home"),
                WordEntity("laundry", "giat do", "lawn-dree", "noun", "Do the laundry on Sunday.", "Giat do vao Chu nhat.", lessonId = 16, difficulty = 1, category = "home"),
                WordEntity("cleaning", "don dep", "klee-ning", "noun", "Cleaning takes time.", "Don dep mat thoi gian.", lessonId = 16, difficulty = 1, category = "home"),
                
                // Lesson 17 - Hobbies & Free Time (6 words)
                WordEntity("reading", "doc sach", "ree-ding", "noun", "Reading is relaxing.", "Doc sach giup thu gian.", lessonId = 17, difficulty = 1, category = "hobby"),
                WordEntity("painting", "ve tranh", "paint-ing", "noun", "I like painting.", "Toi thich ve tranh.", lessonId = 17, difficulty = 1, category = "hobby"),
                WordEntity("hiking", "di bo duong dai", "hi-king", "noun", "We go hiking on weekends.", "Chung toi di hiking cuoi tuan.", lessonId = 17, difficulty = 1, category = "hobby"),
                WordEntity("playing guitar", "choi guitar", "play-ing gui-tar", "verb", "He enjoys playing guitar.", "Anh ay thich choi guitar.", lessonId = 17, difficulty = 1, category = "hobby"),
                WordEntity("swimming", "boi loi", "swim-ing", "noun", "Swimming is my hobby.", "Boi loi la so thich cua toi.", lessonId = 17, difficulty = 1, category = "hobby"),
                WordEntity("gardening", "lam vuon", "gar-den-ing", "noun", "Gardening is peaceful.", "Lam vuon rat yen binh.", lessonId = 17, difficulty = 1, category = "hobby"),
                
                // Lesson 18 - Technology & Devices (6 words)
                WordEntity("smartphone", "dien thoai thong minh", "smart-phone", "noun", "My smartphone is slow.", "Dien thoai thong minh cua toi cham.", lessonId = 18, difficulty = 2, category = "technology"),
                WordEntity("laptop", "may tinh xach tay", "lap-top", "noun", "Charge your laptop.", "Sac may tinh xach tay.", lessonId = 18, difficulty = 2, category = "technology"),
                WordEntity("charger", "sac", "char-jer", "noun", "I lost my charger.", "Toi mat sac roi.", lessonId = 18, difficulty = 2, category = "technology"),
                WordEntity("password", "mat khau", "pass-word", "noun", "Reset your password.", "Dat lai mat khau.", lessonId = 18, difficulty = 2, category = "technology"),
                WordEntity("app", "ung dung", "app", "noun", "Download the new app.", "Tai ung dung moi.", lessonId = 18, difficulty = 2, category = "technology"),
                WordEntity("update", "cap nhat", "up-date", "verb", "Update the software.", "Cap nhat phan mem.", lessonId = 18, difficulty = 2, category = "technology"),
                
                // Lesson 19 - Weather & Events (6 words)
                WordEntity("sunny", "nang", "sun-ny", "adjective", "It is sunny today.", "Hom nay troi nang.", lessonId = 19, difficulty = 1, category = "weather"),
                WordEntity("rainy", "mua", "ray-ny", "adjective", "The weather is rainy.", "Troi dang mua.", lessonId = 19, difficulty = 1, category = "weather"),
                WordEntity("storm", "bao", "storm", "noun", "A storm is coming.", "Bao dang den.", lessonId = 19, difficulty = 2, category = "weather"),
                WordEntity("forecast", "du bao thoi tiet", "for-cast", "noun", "Check the forecast.", "Kiem tra du bao thoi tiet.", lessonId = 19, difficulty = 2, category = "weather"),
                WordEntity("picnic", "di choi ngoai troi", "pic-nic", "noun", "Plan a picnic this weekend.", "Len ke hoach picnic cuoi tuan nay.", lessonId = 19, difficulty = 1, category = "events"),
                WordEntity("festival", "le hoi", "fes-ti-val", "noun", "The festival is crowded.", "Le hoi dong duc.", lessonId = 19, difficulty = 2, category = "events"),
                
                // Lesson 20 - Emergency & Help (6 words)
                WordEntity("emergency", "khan cap", "e-mer-gen-cy", "noun", "Call in an emergency.", "Goi khi khan cap.", lessonId = 20, difficulty = 3, category = "safety"),
                WordEntity("ambulance", "xe cuu thuong", "am-byu-lans", "noun", "Call an ambulance.", "Goi xe cuu thuong.", lessonId = 20, difficulty = 3, category = "safety"),
                WordEntity("police", "canh sat", "po-lice", "noun", "Call the police.", "Goi canh sat.", lessonId = 20, difficulty = 3, category = "safety"),
                WordEntity("fire", "chay", "fire", "noun", "There is a fire!", "Co chay!", lessonId = 20, difficulty = 3, category = "safety"),
                WordEntity("help", "giup do", "help", "verb", "Please help me!", "Lam on giup toi!", lessonId = 20, difficulty = 2, category = "safety"),
                WordEntity("lost", "lac duong", "lost", "adjective", "I am lost.", "Toi bi lac.", lessonId = 20, difficulty = 2, category = "directions")
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
                    question = "How do you say 'tam biet'?",
                    correctAnswer = "goodbye",
                    optionA = "hello", optionB = "goodbye", optionC = "yes", optionD = "no",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 5, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'cam on'?",
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
                    question = "Translate: Toi la Nam.",
                    correctAnswer = "I am Nam",
                    order = 6, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 6, type = "MATCHING",
                    question = "Match the greetings",
                    correctAnswer = "",
                    matchPairs = """[
                        {"left":"hello","right":"xin chao"},
                        {"left":"goodbye","right":"tam biet"},
                        {"left":"please","right":"lam on"},
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
                
                // Lesson 2 - Basics 2
                ExerciseEntity(
                    lessonId = 2, wordId = 13, type = "MULTIPLE_CHOICE",
                    question = "How do you say 'anh ay'?",
                    correctAnswer = "he",
                    optionA = "he", optionB = "she", optionC = "they", optionD = "we",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 2, wordId = 14, type = "MULTIPLE_CHOICE",
                    question = "Translate 'co ay'",
                    correctAnswer = "she",
                    optionA = "he", optionB = "she", optionC = "girl", optionD = "woman",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 2, wordId = 15, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'chung toi'?",
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
                        {"left":"he","right":"anh ay"},
                        {"left":"she","right":"co ay"},
                        {"left":"we","right":"chung toi"},
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
                    question = "How do you say 'xin loi' politely?",
                    correctAnswer = "sorry",
                    optionA = "sorry", optionB = "welcome", optionC = "good luck", optionD = "good night",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 3, wordId = 27, type = "MULTIPLE_CHOICE",
                    question = "Translate 'chao buoi sang'",
                    correctAnswer = "good morning",
                    optionA = "good morning", optionB = "good night", optionC = "see you later", optionD = "excuse me",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 3, wordId = 28, type = "MULTIPLE_CHOICE",
                    question = "What is 'chuc ngu ngon'?",
                    correctAnswer = "good night",
                    optionA = "good morning", optionB = "good night", optionC = "welcome", optionD = "good luck",
                    order = 3, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 3, wordId = 29, type = "TRANSLATION",
                    question = "Dich: Hen gap lai sau.",
                    correctAnswer = "See you later",
                    order = 4, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 3, wordId = 34, type = "TRANSLATION",
                    question = "Dich: Rat vui duoc gap ban.",
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
                        {"left":"welcome","right":"chao mung"},
                        {"left":"good luck","right":"chuc may man"},
                        {"left":"good night","right":"chuc ngu ngon"},
                        {"left":"excuse me","right":"xin loi"}
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
                    question = "Translate 'ca phe'",
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
                    question = "Dich: Toi dang doi.",
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
                    question = "Translate 'ho chieu'",
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
                    question = "Dich: Day la con trai toi.",
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
                    question = "Translate 'ao so mi'",
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
                        {"left":"red","right":"mau do"},
                        {"left":"blue","right":"mau xanh duong"},
                        {"left":"green","right":"mau xanh la"},
                        {"left":"shirt","right":"ao so mi"}
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
                    question = "How do you say 'giao vien'?",
                    correctAnswer = "teacher",
                    optionA = "student", optionB = "teacher", optionC = "job", optionD = "office",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 9, wordId = 74, type = "MULTIPLE_CHOICE",
                    question = "Translate 'hoc sinh'",
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
                    question = "Dich: Toi co mot cuoc hop.",
                    correctAnswer = "I have a meeting",
                    order = 4, difficulty = 1
                ),
                
                // Lesson 10 - Daily Routine
                ExerciseEntity(
                    lessonId = 10, wordId = 79, type = "MULTIPLE_CHOICE",
                    question = "How do you say 'ngu day'?",
                    correctAnswer = "wake up",
                    optionA = "wake up", optionB = "sleep", optionC = "breakfast", optionD = "dinner",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 10, wordId = 80, type = "MULTIPLE_CHOICE",
                    question = "Translate 'buoi sang' as a meal",
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
                    question = "Dich: Toi tap the duc moi ngay.",
                    correctAnswer = "I exercise every day",
                    order = 4, difficulty = 1
                ),
                
                // Lesson 11 - Transport & Directions
                ExerciseEntity(
                    lessonId = 11, wordId = 85, type = "MULTIPLE_CHOICE",
                    question = "Where is the 'tram xe buyt'?",
                    correctAnswer = "bus stop",
                    optionA = "bus stop", optionB = "station", optionC = "traffic jam", optionD = "ticket booth",
                    order = 1, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 11, wordId = 88, type = "MULTIPLE_CHOICE",
                    question = "Translate 're trai'",
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
                    question = "Dich: Dang ket xe.",
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
                    question = "Translate 'hoa don'",
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
                    question = "Dich: Ban co giam gia khong?",
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
                    question = "Translate 'hoa don' in a restaurant",
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
                    question = "Dich: De lai chut tien tip.",
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
                    question = "Which word means 'dau dau'?",
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
                    question = "Dich: Ban nen nghi ngoi hom nay.",
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
                    question = "Translate 'han chot'",
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
                    question = "Dich: Toi se trinh bay hom nay.",
                    correctAnswer = "I will present today",
                    order = 4, difficulty = 2
                ),
                
                // Lesson 16 - Home & Household
                ExerciseEntity(
                    lessonId = 16, wordId = 115, type = "MULTIPLE_CHOICE",
                    question = "Translate 'nha bep'",
                    correctAnswer = "kitchen",
                    optionA = "kitchen", optionB = "bedroom", optionC = "living room", optionD = "laundry",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 16, wordId = 117, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'phong ngu'?",
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
                    question = "Dich: Don dep mat thoi gian.",
                    correctAnswer = "Cleaning takes time",
                    order = 4, difficulty = 1
                ),
                
                // Lesson 17 - Hobbies & Free Time
                ExerciseEntity(
                    lessonId = 17, wordId = 121, type = "MULTIPLE_CHOICE",
                    question = "Translate 'doc sach'",
                    correctAnswer = "reading",
                    optionA = "reading", optionB = "painting", optionC = "hiking", optionD = "swimming",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 17, wordId = 125, type = "MULTIPLE_CHOICE",
                    question = "Which word means 'boi loi'?",
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
                    question = "Dich: Lam vuon rat yen binh.",
                    correctAnswer = "Gardening is peaceful",
                    order = 4, difficulty = 1
                ),
                
                // Lesson 18 - Technology & Devices
                ExerciseEntity(
                    lessonId = 18, wordId = 127, type = "MULTIPLE_CHOICE",
                    question = "What is 'dien thoai thong minh'?",
                    correctAnswer = "smartphone",
                    optionA = "smartphone", optionB = "laptop", optionC = "charger", optionD = "password",
                    order = 1, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 18, wordId = 130, type = "MULTIPLE_CHOICE",
                    question = "Translate 'mat khau'",
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
                    question = "Dich: Cap nhat phan mem.",
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
                    question = "Translate 'du bao thoi tiet'",
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
                    question = "Dich: Bao dang den.",
                    correctAnswer = "A storm is coming",
                    order = 4, difficulty = 2
                ),
                
                // Lesson 20 - Emergency & Help
                ExerciseEntity(
                    lessonId = 20, wordId = 139, type = "MULTIPLE_CHOICE",
                    question = "Translate 'khan cap'",
                    correctAnswer = "emergency",
                    optionA = "emergency", optionB = "ambulance", optionC = "police", optionD = "fire",
                    order = 1, difficulty = 3
                ),
                ExerciseEntity(
                    lessonId = 20, wordId = 141, type = "MULTIPLE_CHOICE",
                    question = "What is 'canh sat'?",
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
                    question = "Dich: Goi xe cuu thuong.",
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
