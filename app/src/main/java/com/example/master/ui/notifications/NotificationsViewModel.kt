package com.example.master.ui.notifications

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class NotificationsViewModel : ViewModel() {
	private val _uiState = mutableStateOf(NotificationUiState.sample())

	val uiState: State<NotificationUiState> = _uiState

	fun markAllRead() {
		val updated = _uiState.value.copy(
			notifications = _uiState.value.notifications.map { it.copy(isUnread = false) },
			// tính lại số chưa đọc
			unreadCount = 0
		)
		_uiState.value = updated
	}

	fun onNotificationClick(item: NotificationItem) {
		val updatedList = _uiState.value.notifications.map {
			if (it.id == item.id) it.copy(isUnread = false) else it
		}
		val newUnread = updatedList.count { it.isUnread }
		_uiState.value = _uiState.value.copy(notifications = updatedList, unreadCount = newUnread)
	}
}