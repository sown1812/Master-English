# Navigation Implementation Guide

## Tổng quan
Ứng dụng Master English sử dụng Android Navigation Component với Bottom Navigation View để điều hướng giữa các màn hình chính.

## Cấu trúc Navigation

### 1. Bottom Navigation Menu (`bottom_nav_menu.xml`)
Ba tab chính:
- **Trang chủ** (Home) - Màn hình chính với các level và thử thách
- **Thành tích** (Dashboard) - Theo dõi tiến độ và thành tích
- **Thông báo** (Notifications) - Thông báo và cập nhật

### 2. Navigation Graph (`mobile_navigation.xml`)
Định nghĩa các fragment và navigation actions:
- `navigation_home` → HomeFragment
- `navigation_dashboard` → DashboardFragment  
- `navigation_notifications` → NotificationsFragment

### 3. Navigation Actions
Mỗi fragment có các action để chuyển đến fragment khác với animation:
- `action_home_to_dashboard`
- `action_home_to_notifications`
- `action_dashboard_to_home`
- `action_dashboard_to_notifications`
- `action_notifications_to_home`
- `action_notifications_to_dashboard`

### 4. Animations
Các animation được định nghĩa trong thư mục `res/anim/`:
- `slide_in_right.xml` - Trượt vào từ phải
- `slide_in_left.xml` - Trượt vào từ trái
- `slide_out_right.xml` - Trượt ra bên phải
- `slide_out_left.xml` - Trượt ra bên trái

## Cách sử dụng Navigation

### Trong Fragment
```kotlin
// Sử dụng action với animation
findNavController().navigate(R.id.action_home_to_dashboard)

// Hoặc navigate trực tiếp (không có animation tùy chỉnh)
findNavController().navigate(R.id.navigation_dashboard)
```

### Trong MainActivity
```kotlin
val navController = findNavController(R.id.nav_host_fragment_activity_main)
val appBarConfiguration = AppBarConfiguration(
    setOf(
        R.id.navigation_home, 
        R.id.navigation_dashboard, 
        R.id.navigation_notifications
    )
)
setupActionBarWithNavController(navController, appBarConfiguration)
navView.setupWithNavController(navController)
```

## Styling

### Colors
- **Selected**: #6366F1 (Indigo)
- **Unselected**: #94A3B8 (Slate Gray)
- **Background**: #FFFFFF (White)

### Bottom Navigation Properties
- `itemIconTint`: Màu icon thay đổi theo trạng thái
- `itemTextColor`: Màu text thay đổi theo trạng thái
- `labelVisibilityMode`: "labeled" - Luôn hiển thị label
- `elevation`: 8dp - Tạo bóng đổ

## Navigation Events trong HomeFragment

HomeFragment sử dụng `HomeNavigationEvent` để xử lý các sự kiện điều hướng:
- `NavigateToPlay` → Dashboard (màn chơi)
- `NavigateToDailyChallenge` → Notifications
- `NavigateToAchievements` → Dashboard
- `NavigateToStore` → Notifications
- `NavigateToQuest` → Dashboard
- `NavigateToBooster` → Notifications

## Best Practices

1. **Sử dụng Safe Args**: Để truyền dữ liệu an toàn giữa các fragment
2. **Single Activity**: Toàn bộ app chạy trong một Activity với nhiều Fragment
3. **Deep Links**: Có thể thêm deep links cho các destination
4. **Back Stack**: Navigation Component tự động quản lý back stack
5. **Animation**: Sử dụng animation để tạo trải nghiệm mượt mà

## Mở rộng

Để thêm màn hình mới:
1. Tạo Fragment và ViewModel mới
2. Thêm fragment vào `mobile_navigation.xml`
3. Thêm navigation actions nếu cần
4. Cập nhật Bottom Navigation Menu nếu là top-level destination
5. Thêm string resources cho title

## Troubleshooting

### Lỗi thường gặp:
- **IllegalArgumentException**: Kiểm tra ID trong navigation graph
- **Animation không hoạt động**: Đảm bảo file animation tồn tại trong `res/anim/`
- **Back button không hoạt động**: Kiểm tra `defaultNavHost="true"` trong NavHostFragment
