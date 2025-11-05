# Roadmap Project Sinh Viên - 3 Tháng

## 🎯 Mục tiêu
Tạo ứng dụng học tiếng Anh **đơn giản nhưng hoàn chỉnh** cho đồ án sinh viên

## ✅ Phạm vi Project (Scope)

### Tính năng CỐT LÕI (Bắt buộc)
1. ✅ **Học từ vựng** - 100-200 từ
2. ✅ **3 loại bài tập**:
   - Multiple Choice (Trắc nghiệm)
   - Fill in the Blank (Điền từ)
   - Matching (Nối từ)
3. ✅ **Phát âm** - Text-to-Speech
4. ✅ **Theo dõi tiến độ** - XP, Level, Progress
5. ✅ **Local Database** - Room (không cần backend)
6. ✅ **3 màn hình chính** - Home, Learn, Progress

### Tính năng PHỤ (Nếu còn thời gian)
- ⭐ Daily streak
- ⭐ Simple achievements
- ⭐ Coins system
- ⭐ Dark mode

### KHÔNG CẦN (Quá phức tạp cho 3 tháng)
- ❌ Backend/Server
- ❌ Authentication (Login/Register)
- ❌ Speech Recognition
- ❌ Social features
- ❌ Leaderboard
- ❌ In-app purchases

---

## 📅 Timeline 3 Tháng

### **Tháng 1: Foundation & Database** (Tuần 1-4)

#### Tuần 1-2: Setup & Database
- [ ] Setup project (đã có)
- [ ] Tạo Room Database
  - WordEntity (từ vựng)
  - LessonEntity (bài học)
  - UserProgressEntity (tiến độ)
- [ ] Tạo DAO interfaces
- [ ] Chuẩn bị 100-200 từ vựng (Excel → JSON → Database)

**Deliverable:** Database hoạt động, có thể query từ vựng

#### Tuần 3-4: Data Models & Repository
- [ ] Tạo data models
- [ ] Repository pattern
- [ ] Seed data vào database
- [ ] Test query data

**Deliverable:** Có thể load được từ vựng từ database

---

### **Tháng 2: Core Learning Features** (Tuần 5-8)

#### Tuần 5-6: Lesson Screen
- [ ] Tạo LessonScreen với Compose
- [ ] Implement 3 loại bài tập:
  - MultipleChoiceExercise.kt
  - FillBlankExercise.kt
  - MatchingExercise.kt
- [ ] Progress bar trong lesson
- [ ] Submit answer logic

**Deliverable:** Có thể làm bài tập và kiểm tra đáp án

#### Tuần 7-8: Audio & Scoring
- [ ] Tích hợp Android TTS (Text-to-Speech)
- [ ] Phát âm từ vựng
- [ ] Tính điểm XP
- [ ] Lưu progress vào database
- [ ] Correct/Incorrect feedback

**Deliverable:** Hoàn thành 1 lesson đầy đủ với âm thanh

---

### **Tháng 3: UI/UX & Polish** (Tuần 9-12)

#### Tuần 9-10: Complete All Screens
- [ ] Hoàn thiện Home Screen
  - Hiển thị level hiện tại
  - Danh sách lessons
  - Progress overview
- [ ] Hoàn thiện Progress Screen
  - Tổng từ đã học
  - XP chart
  - Completed lessons
- [ ] Navigation flow hoàn chỉnh

**Deliverable:** App có đầy đủ 3 màn hình hoạt động

#### Tuần 11: Testing & Bug Fixes
- [ ] Test toàn bộ app
- [ ] Fix bugs
- [ ] Optimize performance
- [ ] Add loading states
- [ ] Error handling

**Deliverable:** App ổn định, không crash

#### Tuần 12: Documentation & Presentation
- [ ] Viết README.md
- [ ] Tạo User Guide
- [ ] Record demo video
- [ ] Chuẩn bị slide thuyết trình
- [ ] APK file để demo

**Deliverable:** Sẵn sàng nộp đồ án

---

## 🗂️ Cấu trúc Project Đơn giản

```
app/src/main/java/com/example/master/
├── data/
│   ├── database/
│   │   ├── AppDatabase.kt
│   │   ├── entities/
│   │   │   ├── WordEntity.kt
│   │   │   ├── LessonEntity.kt
│   │   │   └── UserProgressEntity.kt
│   │   └── dao/
│   │       ├── WordDao.kt
│   │       ├── LessonDao.kt
│   │       └── ProgressDao.kt
│   └── repository/
│       └── LearningRepository.kt
│
├── ui/
│   ├── home/          (Đã có)
│   ├── lesson/        (CẦN TẠO)
│   │   ├── LessonScreen.kt
│   │   ├── LessonViewModel.kt
│   │   ├── components/
│   │   │   ├── MultipleChoiceExercise.kt
│   │   │   ├── FillBlankExercise.kt
│   │   │   └── MatchingExercise.kt
│   ├── progress/      (CẦN TẠO - đơn giản hóa Dashboard)
│   └── dashboard/     (Đã có - có thể tái sử dụng)
│
└── utils/
    ├── TTSManager.kt
    └── XPCalculator.kt
```

---

## 💾 Database Schema Đơn Giản

### WordEntity
```kotlin
@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey val id: Int,
    val word: String,              // "hello"
    val translation: String,       // "xin chào"
    val pronunciation: String,     // "həˈloʊ"
    val partOfSpeech: String,      // "noun", "verb", etc.
    val exampleSentence: String,   // "Hello, how are you?"
    val lessonId: Int              // Thuộc lesson nào
)
```

### LessonEntity
```kotlin
@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: Int,
    val title: String,             // "Greetings"
    val description: String,       // "Learn basic greetings"
    val order: Int,                // Thứ tự lesson
    val totalWords: Int,           // Số từ trong lesson
    val isUnlocked: Boolean        // Đã mở khóa chưa
)
```

### UserProgressEntity
```kotlin
@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey val id: Int = 1,   // Chỉ có 1 user (local)
    val currentLevel: Int,          // Level hiện tại
    val totalXP: Int,               // Tổng XP
    val wordsLearned: Int,          // Số từ đã học
    val lessonsCompleted: Int,      // Số bài đã hoàn thành
    val lastStudyDate: Long         // Timestamp
)
```

---

## 📝 Content Preparation

### 10 Lessons Đề xuất (200 từ)
1. **Greetings & Introductions** (20 từ)
2. **Numbers & Time** (20 từ)
3. **Family & Relationships** (20 từ)
4. **Food & Drinks** (20 từ)
5. **Colors & Shapes** (20 từ)
6. **Animals** (20 từ)
7. **Weather & Seasons** (20 từ)
8. **Body Parts** (20 từ)
9. **Common Verbs** (20 từ)
10. **Daily Activities** (20 từ)

### Cách chuẩn bị content:
1. Tạo file Excel với columns: word, translation, pronunciation, example
2. Convert sang JSON
3. Viết script để insert vào database

---

## 🎨 UI Screens (Đơn giản)

### 1. Home Screen (Đã có - cần đơn giản hóa)
```
┌─────────────────────────┐
│  👤 Alex    Level 5     │
│  ⭐ 250 XP  🔥 3 days   │
├─────────────────────────┤
│  📚 Your Lessons        │
│                         │
│  ✅ Lesson 1: Greetings│
│  ✅ Lesson 2: Numbers  │
│  🔒 Lesson 3: Family   │
│  🔒 Lesson 4: Food     │
│                         │
│  [Start Learning] 🎯   │
└─────────────────────────┘
```

### 2. Lesson Screen (CẦN TẠO)
```
┌─────────────────────────┐
│  Lesson 1: Greetings    │
│  Progress: ████░░ 4/10  │
├─────────────────────────┤
│                         │
│  🔊 [Play Audio]        │
│                         │
│  What is "Xin chào"?    │
│                         │
│  ○ A. Hello             │
│  ○ B. Goodbye           │
│  ○ C. Thank you         │
│  ○ D. Sorry             │
│                         │
│  [Check Answer]         │
└─────────────────────────┘
```

### 3. Progress Screen (CẦN TẠO)
```
┌─────────────────────────┐
│  📊 Your Progress       │
├─────────────────────────┤
│  Level: 5               │
│  XP: 250 / 500          │
│  ████████░░░░░░         │
│                         │
│  📚 Words Learned: 45   │
│  ✅ Lessons Done: 2/10  │
│  🔥 Streak: 3 days      │
│                         │
│  Recent Activity:       │
│  • Lesson 2 completed   │
│  • +50 XP earned        │
└─────────────────────────┘
```

---

## 🛠️ Tech Stack (Đơn giản)

### Bắt buộc
- ✅ Kotlin
- ✅ Jetpack Compose (đã có)
- ✅ Room Database
- ✅ ViewModel & LiveData/Flow
- ✅ Navigation Component (đã có)
- ✅ Android TTS (built-in)

### KHÔNG cần
- ❌ Retrofit (không có API)
- ❌ Firebase (không cần backend)
- ❌ Dagger/Hilt (quá phức tạp, dùng manual DI)
- ❌ WorkManager (không cần background tasks)

---

## 📋 Implementation Checklist

### Week 1-2: Database Setup ✅
```kotlin
// 1. Add dependencies trong build.gradle
dependencies {
    implementation "androidx.room:room-runtime:2.6.0"
    kapt "androidx.room:room-compiler:2.6.0"
    implementation "androidx.room:room-ktx:2.6.0"
}

// 2. Tạo entities
// 3. Tạo DAOs
// 4. Tạo AppDatabase
// 5. Seed initial data
```

### Week 3-4: Repository & ViewModels ✅
```kotlin
// 1. LearningRepository.kt
class LearningRepository(private val database: AppDatabase) {
    suspend fun getLessonWords(lessonId: Int): List<WordEntity>
    suspend fun updateProgress(progress: UserProgressEntity)
    suspend fun getProgress(): UserProgressEntity
}

// 2. Update ViewModels để dùng repository
```

### Week 5-6: Lesson Screen ✅
```kotlin
// 1. LessonScreen.kt - Main UI
// 2. LessonViewModel.kt - Logic
// 3. Exercise components
// 4. Answer checking logic
```

### Week 7-8: Audio & Scoring ✅
```kotlin
// 1. TTSManager.kt
class TTSManager(context: Context) {
    fun speak(text: String)
    fun stop()
}

// 2. XPCalculator.kt
object XPCalculator {
    fun calculate(correct: Boolean, difficulty: Int): Int {
        return if (correct) 10 * difficulty else 0
    }
}
```

### Week 9-10: Polish UI ✅
- Animations
- Loading states
- Error messages
- Empty states

### Week 11: Testing ✅
- Manual testing
- Fix bugs
- Performance check

### Week 12: Documentation ✅
- README.md
- User guide
- Demo video
- Presentation slides

---

## 📱 Minimum Features for Demo

### Must Have (Để pass đồ án)
1. ✅ 10 lessons với 100-200 từ
2. ✅ 3 loại bài tập hoạt động
3. ✅ Phát âm từ vựng
4. ✅ Lưu và hiển thị progress
5. ✅ Navigation giữa các màn hình
6. ✅ UI đẹp, không crash

### Nice to Have (Điểm cộng)
- ⭐ Animations mượt
- ⭐ Dark mode
- ⭐ Streak counter
- ⭐ Simple achievements
- ⭐ Export/Import progress

---

## 🎓 Báo cáo & Thuyết trình

### Nội dung báo cáo
1. **Giới thiệu**
   - Bối cảnh, mục tiêu
   - Phạm vi project

2. **Phân tích & Thiết kế**
   - Use case diagram
   - Database schema
   - UI/UX design

3. **Công nghệ sử dụng**
   - Android, Kotlin, Jetpack Compose
   - Room Database
   - MVVM architecture

4. **Tính năng chính**
   - Học từ vựng
   - Bài tập
   - Theo dõi tiến độ

5. **Kết quả & Demo**
   - Screenshots
   - Video demo
   - Link APK

6. **Hạn chế & Hướng phát triển**
   - Những gì chưa làm được
   - Kế hoạch tương lai

### Demo Presentation (10-15 phút)
1. **Giới thiệu** (2 phút)
2. **Demo app** (5 phút)
   - Mở app
   - Chọn lesson
   - Làm bài tập
   - Xem progress
3. **Technical overview** (3 phút)
   - Architecture
   - Database
   - Key features
4. **Q&A** (5 phút)

---

## 💡 Tips cho Project Sinh Viên

### 1. Quản lý thời gian
- ✅ Commit code mỗi ngày
- ✅ Làm theo tuần, không để deadline
- ✅ Test thường xuyên
- ✅ Document ngay khi code

### 2. Khi gặp khó khăn
- 🔍 Google/StackOverflow
- 📚 Android Documentation
- 💬 Hỏi thầy/bạn
- 🎥 YouTube tutorials

### 3. Tránh scope creep
- ❌ Không thêm tính năng phức tạp
- ❌ Không làm backend nếu không cần
- ✅ Focus vào core features
- ✅ Polish những gì đã có

### 4. Backup & Version Control
- ✅ Dùng Git/GitHub
- ✅ Commit thường xuyên
- ✅ Backup database files
- ✅ Giữ APK của mỗi version

---

## 📊 Tiêu chí đánh giá (ước tính)

### Chức năng (40%)
- ✅ App chạy được, không crash
- ✅ Các tính năng core hoạt động
- ✅ Database hoạt động đúng

### Giao diện (20%)
- ✅ UI đẹp, dễ dùng
- ✅ Responsive
- ✅ Consistent design

### Code quality (20%)
- ✅ Code sạch, có comment
- ✅ Architecture hợp lý (MVVM)
- ✅ Error handling

### Báo cáo & Demo (20%)
- ✅ Báo cáo đầy đủ
- ✅ Demo mượt mà
- ✅ Trả lời câu hỏi tốt

---

## 🎯 Expected Outcome

Sau 3 tháng, bạn sẽ có:

1. ✅ **Working Android App**
   - 10 lessons
   - 200 từ vựng
   - 3 loại bài tập
   - Progress tracking

2. ✅ **Complete Documentation**
   - Source code
   - README
   - User guide
   - Technical report

3. ✅ **Demo Materials**
   - APK file
   - Demo video
   - Presentation slides

4. ✅ **Learning Experience**
   - Android development
   - Database design
   - UI/UX design
   - Project management

---

## 🚀 Getting Started

### Next Steps (Ngay bây giờ):

1. **Week 1 Tasks:**
   ```
   [ ] Review code hiện tại
   [ ] Thêm Room dependencies
   [ ] Tạo WordEntity.kt
   [ ] Tạo LessonEntity.kt
   [ ] Tạo UserProgressEntity.kt
   ```

2. **Chuẩn bị content:**
   ```
   [ ] List 200 từ vựng cần học
   [ ] Phân chia thành 10 lessons
   [ ] Tạo file Excel/CSV
   ```

3. **Setup Git:**
   ```
   [ ] Tạo GitHub repository
   [ ] Commit code hiện tại
   [ ] Tạo branches: main, develop
   ```

---

## 📞 Support

Nếu cần hỗ trợ implementation:
1. Tôi có thể giúp tạo database entities
2. Tôi có thể giúp implement lesson screen
3. Tôi có thể giúp setup TTS
4. Tôi có thể review code

**Good luck với project! 🎓🚀**
