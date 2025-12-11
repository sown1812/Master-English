# Implementation Checklist - Phase 1 MVP

## 🎯 Mục tiêu Phase 1
Tạo một ứng dụng học tiếng Anh cơ bản với 10 bài học, người dùng có thể:
- Đăng ký/Đăng nhập
- Học 10 bài với 3-4 loại bài tập
- Kiếm XP và coins
- Track progress
- Nghe phát âm

---

## Week 1-2: Database & Content Foundation

### [ ] Task 1: Setup Room Database
```kotlin
// File: AppDatabase.kt
@Database(
    entities = [
        WordEntity::class,
        LessonEntity::class,
        ExerciseEntity::class,
        UserProgressEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase()
```

### [ ] Task 2: Create Entities
- WordEntity.kt
- LessonEntity.kt  
- ExerciseEntity.kt
- UserProgressEntity.kt

### [ ] Task 3: Create DAOs
- WordDao.kt
- LessonDao.kt
- ProgressDao.kt

### [ ] Task 4: Prepare Initial Content
- 100 từ vựng cơ bản
- 10 lessons với 5-7 exercises mỗi lesson
- JSON content files

---

## Week 3-4: Lesson Screen Implementation

### [ ] Task 5: Create Lesson Models
```kotlin
sealed class ExerciseType {
    data class MultipleChoice(...)
    data class FillBlank(...)
    data class Translation(...)
    data class Listening(...)
}
```

### [ ] Task 6: Build Lesson Screen UI
- LessonScreen.kt với Compose
- Exercise components
- Progress bar
- Hearts/Lives display

### [ ] Task 7: Lesson ViewModel Logic
- Load lesson data
- Handle answer submission
- Calculate score
- Update progress

---

## Week 5-6: Audio & Core Features

### [ ] Task 8: TTS Integration
- TTSManager.kt
- Play word pronunciation
- Play example sentences

### [ ] Task 9: XP System
- XPManager.kt
- Calculate XP based on performance
- Update user XP in database

### [ ] Task 10: Progress Tracking
- Save lesson completion
- Track accuracy
- Update dashboard data

---

## Week 7-8: Authentication & Polish

### [ ] Task 11: Firebase Setup
- Add Firebase to project
- Setup Authentication
- Setup Firestore

### [ ] Task 12: Login/Signup Screens
- LoginScreen.kt
- SignUpScreen.kt
- AuthViewModel.kt

### [ ] Task 13: Connect Everything
- Navigation flow
- Data sync
- Error handling

### [ ] Task 14: Testing & Bug Fixes
- Test all features
- Fix critical bugs
- Polish UI/UX

---

## Deliverables

✅ Working app với:
- 10 lessons hoàn chỉnh
- 3-4 exercise types
- Authentication
- Progress tracking
- Audio pronunciation
- XP & Coins system

**Timeline: 8 weeks**
**Team: 2 developers + 1 content creator**
