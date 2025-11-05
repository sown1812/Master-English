# 🚀 Setup Guide - Master English App

## ✅ Checklist Setup

### 1. Firebase Setup ✅
- [x] Tạo Firebase project
- [x] Add Android app
- [ ] **Download `google-services.json`** ← QUAN TRỌNG!
- [ ] **Đặt file vào `app/` folder** ← QUAN TRỌNG!
- [x] Enable Email/Password authentication
- [x] Create Firestore database (optional)

### 2. Code Setup ✅
- [x] MasterApplication.kt created
- [x] LoginFragment.kt created
- [x] RegisterFragment.kt created
- [x] LessonFragment.kt created
- [x] Navigation graph updated
- [x] MainActivity.kt updated
- [x] AndroidManifest.xml updated
- [x] Gradle files updated

### 3. Ready to Run 🎯
- [ ] Sync Gradle
- [ ] Build project
- [ ] Run app

---

## 📋 Bước tiếp theo

### Bước 1: Download google-services.json

1. Vào Firebase Console: https://console.firebase.google.com/
2. Chọn project "Master English"
3. Click vào icon ⚙️ (Settings) → Project settings
4. Scroll xuống phần "Your apps"
5. Click "Download google-services.json"
6. **ĐẶT FILE VÀO**: `d:\Master\app\google-services.json`

**Cấu trúc thư mục:**
```
Master/
├── app/
│   ├── google-services.json  ← ĐẶT Ở ĐÂY
│   ├── build.gradle.kts
│   └── src/
├── build.gradle.kts
└── settings.gradle.kts
```

### Bước 2: Sync Gradle

1. Mở Android Studio
2. Click **File → Sync Project with Gradle Files**
3. Đợi sync xong (1-2 phút)

### Bước 3: Build Project

1. Click **Build → Rebuild Project**
2. Đợi build xong
3. Kiểm tra không có lỗi

### Bước 4: Run App

1. Chọn device/emulator
2. Click **Run → Run 'app'** hoặc nhấn Shift+F10
3. App sẽ mở màn hình Login

---

## 🎮 Test Flow

### Test 1: Registration
1. Mở app → Màn Login
2. Click "Sign Up"
3. Nhập:
   - Name: Test User
   - Email: test@example.com
   - Password: 123456
   - Confirm: 123456
4. Click "Create Account"
5. ✅ Nếu thành công → Navigate to Home

### Test 2: Login
1. Logout (nếu đã login)
2. Màn Login
3. Nhập:
   - Email: test@example.com
   - Password: 123456
4. Click "Sign In"
5. ✅ Nếu thành công → Navigate to Home

### Test 3: Lesson
1. Ở Home screen
2. Click vào một lesson (cần update HomeFragment để navigate)
3. ✅ Màn Lesson hiển thị
4. Làm bài tập
5. ✅ Nhận XP và Coins khi hoàn thành

---

## 🔧 Troubleshooting

### Lỗi: "google-services.json not found"
**Giải pháp:**
- Kiểm tra file có đúng vị trí: `app/google-services.json`
- Sync Gradle lại
- Clean và Rebuild project

### Lỗi: "FirebaseApp initialization unsuccessful"
**Giải pháp:**
- Kiểm tra package name trong `google-services.json` = `com.example.master`
- Kiểm tra plugin `com.google.gms.google-services` đã apply
- Sync Gradle lại

### Lỗi: "Cannot resolve symbol R"
**Giải pháp:**
- Build → Clean Project
- Build → Rebuild Project
- File → Invalidate Caches / Restart

### Lỗi: Navigation action not found
**Giải pháp:**
- Kiểm tra `mobile_navigation.xml` có đầy đủ fragments
- Kiểm tra action IDs match với code
- Rebuild project

### App crash khi mở
**Giải pháp:**
- Xem Logcat để tìm lỗi
- Kiểm tra Firebase đã enable Email/Password
- Kiểm tra internet permission trong manifest

---

## 📱 Cách Navigate từ Home sang Lesson

Update file `HomeFragment.kt`:

```kotlin
// Trong HomeFragment.kt
import android.os.Bundle
import androidx.core.os.bundleOf

// Khi user click vào lesson
fun startLesson(lessonId: Int) {
    val bundle = bundleOf("lessonId" to lessonId)
    findNavController().navigate(
        R.id.action_home_to_lesson,
        bundle
    )
}
```

Hoặc trong Compose (HomeScreen.kt):

```kotlin
// Trong HomeScreen composable
Button(onClick = {
    onStartLesson(lessonId) // Pass to Fragment
}) {
    Text("Start Lesson")
}

// Trong HomeFragment
HomeScreen(
    state = uiState,
    onStartLesson = { lessonId ->
        val bundle = bundleOf("lessonId" to lessonId)
        findNavController().navigate(
            R.id.action_home_to_lesson,
            bundle
        )
    }
)
```

---

## 🎯 Expected Behavior

### 1. App Start
- App mở → Màn Login
- Nếu đã login trước → Tự động vào Home

### 2. Login Flow
```
Login Screen
  ↓ (click Sign Up)
Register Screen
  ↓ (create account)
Home Screen
```

### 3. Lesson Flow
```
Home Screen
  ↓ (click lesson)
Lesson Screen
  ↓ (do exercises)
Completion Dialog
  ↓ (click Continue)
Back to Home
```

### 4. Bottom Navigation
- Visible: Home, Dashboard, Notifications
- Hidden: Login, Register, Lesson

---

## 📊 Database Check

### Kiểm tra Database đã seed chưa:

1. Run app lần đầu
2. Android Studio → View → Tool Windows → App Inspection
3. Select "Database Inspector"
4. Chọn app process
5. Xem tables:
   - `lessons` → Should have 10 lessons
   - `words` → Should have 20 words (Lesson 1)
   - `exercises` → Should have 7 exercises

---

## 🎨 UI Preview

### Login Screen
- Gradient background (Purple)
- Email field
- Password field (with show/hide)
- Sign In button
- Link to Register

### Register Screen
- Gradient background (Purple to Pink)
- Name field
- Email field
- Password field
- Confirm Password field
- Create Account button
- Link to Login

### Lesson Screen
- Top bar with progress
- Hearts (lives)
- Exercise cards
- Check Answer button
- Feedback (green/red)
- Completion dialog

---

## 📚 Next Steps After Setup

1. **Test authentication** ✅
2. **Test lesson flow** ✅
3. **Add more words** (expand from 20 to 200)
4. **Add TTS** (Text-to-Speech)
5. **Add sound effects**
6. **Polish UI/UX**
7. **Add analytics**
8. **Test on real device**

---

## 🆘 Need Help?

### Common Issues:

**Q: App không build được?**
A: Sync Gradle → Clean → Rebuild

**Q: Firebase không hoạt động?**
A: Kiểm tra `google-services.json` và enable Authentication

**Q: Navigation lỗi?**
A: Kiểm tra `mobile_navigation.xml` và Fragment class names

**Q: Database trống?**
A: Clear app data → Reinstall app (database seed on first run)

---

## ✅ Final Checklist

- [ ] `google-services.json` in `app/` folder
- [ ] Firebase Authentication enabled
- [ ] Gradle synced successfully
- [ ] Project builds without errors
- [ ] App runs on emulator/device
- [ ] Can register new account
- [ ] Can login
- [ ] Can navigate to Home
- [ ] Can start a lesson
- [ ] Can complete exercises
- [ ] Receives XP and Coins

**Khi tất cả ✅ → App sẵn sàng! 🎉**

---

## 📞 Support

Nếu gặp vấn đề, check:
1. Logcat trong Android Studio
2. Firebase Console → Authentication → Users
3. Database Inspector → Tables
4. Build output → Errors

Good luck! 🚀
