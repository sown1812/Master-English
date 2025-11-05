package com.example.master.ui.notifications

import androidx.compose.runtime.Immutable

@Immutable
data class NotificationUiState(
    val unreadCount: Int,
    val highlights: List<NotificationHighlight>,
    val notifications: List<NotificationItem>
) {
    companion object {
        fun sample(): NotificationUiState = NotificationUiState(
            unreadCount = 4,
            highlights = listOf(
                NotificationHighlight(
                    title = "Chuỗi học 7 ngày!",
                    subtitle = "Duy trì streak để nhận +150 coins",
                    type = HighlightType.ACHIEVEMENT
                ),
                NotificationHighlight(
                    title = "Thử thách mới",
                    subtitle = "Daily Challenge đã mở khóa",
                    type = HighlightType.CHALLENGE
                )
            ),
            notifications = listOf(
                NotificationItem(
                    id = "1",
                    category = NotificationCategory.CHALLENGE,
                    title = "Bạn còn 2 giờ để hoàn thành Weekend Marathon",
                    description = "Hoàn thành 3 bài quiz Super Hard để nhận phần thưởng.",
                    time = "10 phút trước",
                    isUnread = true
                ),
                NotificationItem(
                    id = "2",
                    category = NotificationCategory.REWARD,
                    title = "Bạn nhận được +60 coins từ thử thách hôm qua",
                    description = "Coins đã được cộng vào ví của bạn.",
                    time = "1 giờ trước",
                    isUnread = true
                ),
                NotificationItem(
                    id = "3",
                    category = NotificationCategory.SOCIAL,
                    title = "Lan đã vượt bạn trong bảng xếp hạng",
                    description = "Cố gắng hoàn thành thêm 2 level để lấy lại vị trí!",
                    time = "3 giờ trước",
                    isUnread = false
                ),
                NotificationItem(
                    id = "4",
                    category = NotificationCategory.UPDATE,
                    title = "Đã có gói từ vựng TOEIC mới",
                    description = "Khám phá 120 từ mới theo chủ đề Business ngay bây giờ.",
                    time = "Hôm qua",
                    isUnread = false
                )
            )
        )
    }
}

@Immutable
data class NotificationHighlight(
    val title: String,
    val subtitle: String,
    val type: HighlightType
)

@Immutable
data class NotificationItem(
    val id: String,
    val category: NotificationCategory,
    val title: String,
    val description: String,
    val time: String,
    val isUnread: Boolean
)

enum class HighlightType { ACHIEVEMENT, CHALLENGE }

enum class NotificationCategory { CHALLENGE, REWARD, SOCIAL, UPDATE }
