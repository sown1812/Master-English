package com.example.master.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 1,
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
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "master_english_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
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
                        populateDatabase(database)
                    }
                }
            }
        }
        
        private suspend fun populateDatabase(database: AppDatabase) {
            val lessonDao = database.lessonDao()
            val wordDao = database.wordDao()
            val exerciseDao = database.exerciseDao()
            
            // Seed initial lessons
            val lessons = getInitialLessons()
            lessonDao.insertLessons(lessons)
            
            // Seed initial words
            val words = getInitialWords()
            wordDao.insertWords(words)
            
            // Seed initial exercises
            val exercises = getInitialExercises()
            exerciseDao.insertExercises(exercises)
        }
        
        private fun getInitialLessons(): List<LessonEntity> {
            return listOf(
                LessonEntity(
                    id = 1,
                    title = "Greetings & Introductions",
                    description = "Learn basic greetings and how to introduce yourself",
                    order = 1,
                    totalWords = 20,
                    totalExercises = 15,
                    difficulty = "EASY",
                    category = "basics",
                    xpReward = 50,
                    coinsReward = 10,
                    isUnlocked = true
                ),
                LessonEntity(
                    id = 2,
                    title = "Numbers & Time",
                    description = "Master numbers and telling time in English",
                    order = 2,
                    totalWords = 20,
                    totalExercises = 15,
                    difficulty = "EASY",
                    category = "basics",
                    xpReward = 50,
                    coinsReward = 10,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 3,
                    title = "Family & Relationships",
                    description = "Learn vocabulary about family members",
                    order = 3,
                    totalWords = 20,
                    totalExercises = 15,
                    difficulty = "EASY",
                    category = "basics",
                    xpReward = 50,
                    coinsReward = 10,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 4,
                    title = "Food & Drinks",
                    description = "Explore food and beverage vocabulary",
                    order = 4,
                    totalWords = 20,
                    totalExercises = 15,
                    difficulty = "MEDIUM",
                    category = "daily_life",
                    xpReward = 75,
                    coinsReward = 15,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 5,
                    title = "Colors & Shapes",
                    description = "Learn colors and basic shapes",
                    order = 5,
                    totalWords = 20,
                    totalExercises = 15,
                    difficulty = "EASY",
                    category = "basics",
                    xpReward = 50,
                    coinsReward = 10,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 6,
                    title = "Animals",
                    description = "Discover animal names in English",
                    order = 6,
                    totalWords = 20,
                    totalExercises = 15,
                    difficulty = "EASY",
                    category = "nature",
                    xpReward = 50,
                    coinsReward = 10,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 7,
                    title = "Weather & Seasons",
                    description = "Talk about weather and seasons",
                    order = 7,
                    totalWords = 20,
                    totalExercises = 15,
                    difficulty = "MEDIUM",
                    category = "nature",
                    xpReward = 75,
                    coinsReward = 15,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 8,
                    title = "Body Parts",
                    description = "Learn parts of the human body",
                    order = 8,
                    totalWords = 20,
                    totalExercises = 15,
                    difficulty = "MEDIUM",
                    category = "health",
                    xpReward = 75,
                    coinsReward = 15,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 9,
                    title = "Common Verbs",
                    description = "Master essential action verbs",
                    order = 9,
                    totalWords = 20,
                    totalExercises = 15,
                    difficulty = "MEDIUM",
                    category = "grammar",
                    xpReward = 75,
                    coinsReward = 15,
                    isUnlocked = false
                ),
                LessonEntity(
                    id = 10,
                    title = "Daily Activities",
                    description = "Describe your daily routine",
                    order = 10,
                    totalWords = 20,
                    totalExercises = 15,
                    difficulty = "HARD",
                    category = "daily_life",
                    xpReward = 100,
                    coinsReward = 20,
                    isUnlocked = false
                )
            )
        }
        
        private fun getInitialWords(): List<WordEntity> {
            // Lesson 1: Greetings & Introductions (20 words)
            return listOf(
                // Basic greetings
                WordEntity(word = "hello", translation = "xin chào", pronunciation = "həˈloʊ", partOfSpeech = "interjection", 
                    exampleSentence = "Hello, how are you?", exampleTranslation = "Xin chào, bạn khỏe không?", 
                    lessonId = 1, difficulty = 1, category = "greetings"),
                WordEntity(word = "hi", translation = "chào", pronunciation = "haɪ", partOfSpeech = "interjection",
                    exampleSentence = "Hi! Nice to meet you.", exampleTranslation = "Chào! Rất vui được gặp bạn.",
                    lessonId = 1, difficulty = 1, category = "greetings"),
                WordEntity(word = "goodbye", translation = "tạm biệt", pronunciation = "ɡʊdˈbaɪ", partOfSpeech = "interjection",
                    exampleSentence = "Goodbye, see you tomorrow!", exampleTranslation = "Tạm biệt, hẹn gặp lại ngày mai!",
                    lessonId = 1, difficulty = 1, category = "greetings"),
                WordEntity(word = "bye", translation = "tạm biệt", pronunciation = "baɪ", partOfSpeech = "interjection",
                    exampleSentence = "Bye! Have a great day.", exampleTranslation = "Tạm biệt! Chúc bạn một ngày tốt lành.",
                    lessonId = 1, difficulty = 1, category = "greetings"),
                WordEntity(word = "good morning", translation = "chào buổi sáng", pronunciation = "ɡʊd ˈmɔːrnɪŋ", partOfSpeech = "phrase",
                    exampleSentence = "Good morning! Did you sleep well?", exampleTranslation = "Chào buổi sáng! Bạn ngủ ngon không?",
                    lessonId = 1, difficulty = 1, category = "greetings"),
                WordEntity(word = "good afternoon", translation = "chào buổi chiều", pronunciation = "ɡʊd ˌæftərˈnuːn", partOfSpeech = "phrase",
                    exampleSentence = "Good afternoon, everyone!", exampleTranslation = "Chào buổi chiều, mọi người!",
                    lessonId = 1, difficulty = 1, category = "greetings"),
                WordEntity(word = "good evening", translation = "chào buổi tối", pronunciation = "ɡʊd ˈiːvnɪŋ", partOfSpeech = "phrase",
                    exampleSentence = "Good evening, sir.", exampleTranslation = "Chào buổi tối, thưa ông.",
                    lessonId = 1, difficulty = 1, category = "greetings"),
                WordEntity(word = "good night", translation = "chúc ngủ ngon", pronunciation = "ɡʊd naɪt", partOfSpeech = "phrase",
                    exampleSentence = "Good night, sweet dreams!", exampleTranslation = "Chúc ngủ ngon, mơ đẹp!",
                    lessonId = 1, difficulty = 1, category = "greetings"),
                
                // Introductions
                WordEntity(word = "name", translation = "tên", pronunciation = "neɪm", partOfSpeech = "noun",
                    exampleSentence = "My name is John.", exampleTranslation = "Tên tôi là John.",
                    lessonId = 1, difficulty = 1, category = "introductions"),
                WordEntity(word = "I", translation = "tôi", pronunciation = "aɪ", partOfSpeech = "pronoun",
                    exampleSentence = "I am a student.", exampleTranslation = "Tôi là một học sinh.",
                    lessonId = 1, difficulty = 1, category = "introductions"),
                WordEntity(word = "you", translation = "bạn", pronunciation = "juː", partOfSpeech = "pronoun",
                    exampleSentence = "You are very kind.", exampleTranslation = "Bạn rất tốt bụng.",
                    lessonId = 1, difficulty = 1, category = "introductions"),
                WordEntity(word = "am", translation = "là (dùng với I)", pronunciation = "æm", partOfSpeech = "verb",
                    exampleSentence = "I am happy.", exampleTranslation = "Tôi hạnh phúc.",
                    lessonId = 1, difficulty = 1, category = "introductions"),
                WordEntity(word = "is", translation = "là (dùng với he/she/it)", pronunciation = "ɪz", partOfSpeech = "verb",
                    exampleSentence = "She is my friend.", exampleTranslation = "Cô ấy là bạn tôi.",
                    lessonId = 1, difficulty = 1, category = "introductions"),
                WordEntity(word = "are", translation = "là (dùng với you/we/they)", pronunciation = "ɑːr", partOfSpeech = "verb",
                    exampleSentence = "They are students.", exampleTranslation = "Họ là học sinh.",
                    lessonId = 1, difficulty = 1, category = "introductions"),
                WordEntity(word = "nice", translation = "tốt, dễ chịu", pronunciation = "naɪs", partOfSpeech = "adjective",
                    exampleSentence = "Nice to meet you!", exampleTranslation = "Rất vui được gặp bạn!",
                    lessonId = 1, difficulty = 1, category = "introductions"),
                WordEntity(word = "meet", translation = "gặp", pronunciation = "miːt", partOfSpeech = "verb",
                    exampleSentence = "Let's meet tomorrow.", exampleTranslation = "Hãy gặp nhau vào ngày mai.",
                    lessonId = 1, difficulty = 1, category = "introductions"),
                WordEntity(word = "please", translation = "làm ơn", pronunciation = "pliːz", partOfSpeech = "adverb",
                    exampleSentence = "Please help me.", exampleTranslation = "Làm ơn giúp tôi.",
                    lessonId = 1, difficulty = 1, category = "politeness"),
                WordEntity(word = "thank you", translation = "cảm ơn", pronunciation = "θæŋk juː", partOfSpeech = "phrase",
                    exampleSentence = "Thank you for your help.", exampleTranslation = "Cảm ơn vì sự giúp đỡ của bạn.",
                    lessonId = 1, difficulty = 1, category = "politeness"),
                WordEntity(word = "sorry", translation = "xin lỗi", pronunciation = "ˈsɑːri", partOfSpeech = "adjective",
                    exampleSentence = "I'm sorry for being late.", exampleTranslation = "Tôi xin lỗi vì đến muộn.",
                    lessonId = 1, difficulty = 1, category = "politeness"),
                WordEntity(word = "excuse me", translation = "xin lỗi (để xin phép)", pronunciation = "ɪkˈskjuːz miː", partOfSpeech = "phrase",
                    exampleSentence = "Excuse me, where is the bathroom?", exampleTranslation = "Xin lỗi, phòng tắm ở đâu?",
                    lessonId = 1, difficulty = 1, category = "politeness")
            )
        }
        
        private fun getInitialExercises(): List<ExerciseEntity> {
            return listOf(
                // Lesson 1 Exercises - Multiple Choice
                ExerciseEntity(
                    lessonId = 1, wordId = 1, type = "MULTIPLE_CHOICE",
                    question = "What is 'Xin chào' in English?",
                    correctAnswer = "hello",
                    optionA = "hello", optionB = "goodbye", optionC = "thank you", optionD = "sorry",
                    order = 1, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 3, type = "MULTIPLE_CHOICE",
                    question = "How do you say 'Tạm biệt'?",
                    correctAnswer = "goodbye",
                    optionA = "hello", optionB = "goodbye", optionC = "good morning", optionD = "good night",
                    order = 2, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 18, type = "MULTIPLE_CHOICE",
                    question = "What does 'thank you' mean?",
                    correctAnswer = "cảm ơn",
                    optionA = "xin lỗi", optionB = "cảm ơn", optionC = "làm ơn", optionD = "tạm biệt",
                    order = 3, difficulty = 1
                ),
                
                // Fill in the blank exercises
                ExerciseEntity(
                    lessonId = 1, wordId = 9, type = "FILL_BLANK",
                    question = "My _____ is John.",
                    correctAnswer = "name",
                    hint = "What do you call yourself?",
                    order = 4, difficulty = 1
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 10, type = "FILL_BLANK",
                    question = "_____ am a student.",
                    correctAnswer = "I",
                    hint = "First person pronoun",
                    order = 5, difficulty = 1
                ),
                
                // Translation exercises
                ExerciseEntity(
                    lessonId = 1, wordId = 15, type = "TRANSLATION",
                    question = "Translate: Rất vui được gặp bạn!",
                    correctAnswer = "Nice to meet you!",
                    order = 6, difficulty = 2
                ),
                ExerciseEntity(
                    lessonId = 1, wordId = 5, type = "TRANSLATION",
                    question = "Translate: Good morning! Did you sleep well?",
                    correctAnswer = "Chào buổi sáng! Bạn ngủ ngon không?",
                    order = 7, difficulty = 2
                )
            )
        }
    }
}
