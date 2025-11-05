package com.example.master.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun NotificationsRoute(viewModel: NotificationsViewModel) {
	val state by viewModel.uiState
	NotificationsScreen(
		state = state,
		onMarkAllRead = { viewModel.markAllRead() },
		onNotificationClick = { viewModel.onNotificationClick(it) }
	)
}

@Composable
fun NotificationsScreen(
	state: NotificationUiState,
	modifier: Modifier = Modifier,
	onMarkAllRead: () -> Unit = {},
	onNotificationClick: (NotificationItem) -> Unit = {}
) {
	Box(
		modifier = modifier
			.fillMaxSize()
			.background(
				Brush.verticalGradient(
					listOf(Color(0xFFF5F1FF), Color(0xFFE9F6FF))
				)
			)
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 20.dp)
				.padding(top = 28.dp, bottom = 32.dp),
			verticalArrangement = Arrangement.spacedBy(24.dp)
		) {
			NotificationHeader(unreadCount = state.unreadCount, onMarkAllRead = onMarkAllRead)
			HighlightsSection(highlights = state.highlights)
			NotificationFeed(
				notifications = state.notifications,
				onNotificationClick = onNotificationClick
			)
		}
	}
}

@Composable
private fun NotificationHeader(unreadCount: Int, onMarkAllRead: () -> Unit) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.SpaceBetween,
		verticalAlignment = Alignment.CenterVertically
	) {
		Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
			Text(
				text = "Thông báo",
				style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
				color = Color(0xFF2F1F53)
			)
			Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
				Text(
					text = if (unreadCount > 0) "$unreadCount chưa đọc" else "Không có thông báo mới",
					style = MaterialTheme.typography.bodyMedium,
					color = Color(0xFF645A82)
				)
				if (unreadCount > 0) {
					Surface(
						shape = RoundedCornerShape(50),
						color = Color(0xFF4B3DF0)
					) {
						Text(
							text = "Mới",
							color = Color.White,
							style = MaterialTheme.typography.labelMedium,
							modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
						)
					}
				}
			}
		}
		Button(
			onClick = onMarkAllRead,
			colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8E4FF), contentColor = Color(0xFF4B3DF0)),
			shape = RoundedCornerShape(16.dp),
			enabled = unreadCount > 0
		) {
			Icon(imageVector = Icons.Filled.NotificationsActive, contentDescription = null)
			Spacer(modifier = Modifier.width(6.dp))
			Text(text = "Đánh dấu đã đọc")
		}
	}
}

@Composable
private fun HighlightsSection(highlights: List<NotificationHighlight>) {
	if (highlights.isEmpty()) return

	Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
		Text(
			text = "Nổi bật hôm nay",
			style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
			color = Color(0xFF2F1F53)
		)
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.horizontalScroll(rememberScrollState()),
			horizontalArrangement = Arrangement.spacedBy(16.dp)
		) {
			highlights.forEach { highlight ->
				HighlightCard(highlight = highlight)
			}
		}
	}
}

@Composable
private fun HighlightCard(highlight: NotificationHighlight) {
	val (gradient, icon, accent) = when (highlight.type) {
		HighlightType.ACHIEVEMENT -> Triple(
			listOf(Color(0xFFFFE29F), Color(0xFFFFC857)),
			Icons.Filled.Star,
			Color(0xFFAF6900)
		)
		HighlightType.CHALLENGE -> Triple(
			listOf(Color(0xFF9CECFB), Color(0xFF65C7F7)),
			Icons.Filled.Flag,
			Color(0xFF0462A5)
		)
	}

	Card(
		modifier = Modifier
			.width(260.dp)
			.height(140.dp),
		shape = RoundedCornerShape(24.dp),
		colors = CardDefaults.cardColors(containerColor = Color.Transparent)
	) {
		Box(
			modifier = Modifier
				.background(Brush.verticalGradient(gradient))
				.fillMaxSize()
		) {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(20.dp),
				verticalArrangement = Arrangement.SpaceBetween
			) {
				Surface(
					shape = CircleShape,
					color = Color.White.copy(alpha = 0.3f)
				) {
					Icon(
						imageVector = icon,
						contentDescription = null,
						tint = Color.White,
						modifier = Modifier.padding(12.dp)
					)
				}
				Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
					Text(
						text = highlight.title,
						style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
						color = Color.White
					)
					Text(
						text = highlight.subtitle,
						style = MaterialTheme.typography.bodySmall,
						color = Color.White.copy(alpha = 0.9f)
					)
					Text(
						text = "Khám phá",
						style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
						color = accent
					)
				}
			}
		}
	}
}

@Composable
private fun NotificationFeed(
	notifications: List<NotificationItem>,
	onNotificationClick: (NotificationItem) -> Unit
) {
	Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
		Text(
			text = "Tất cả thông báo",
			style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
			color = Color(0xFF2F1F53)
		)
		notifications.forEachIndexed { index, notification ->
			NotificationCard(
				notification = notification,
				onClick = { onNotificationClick(notification) }
			)
			if (index != notifications.lastIndex) {
				Divider(color = Color(0xFFE5E0F5), thickness = 1.dp, modifier = Modifier.padding(horizontal = 8.dp))
			}
		}
	}
}

@Composable
private fun NotificationCard(
	notification: NotificationItem,
	onClick: () -> Unit
) {
	val (icon, containerColor, accent) = when (notification.category) {
		NotificationCategory.CHALLENGE -> Triple(Icons.Filled.Bolt, Color(0xFFE8F4FF), Color(0xFF2276D1))
		NotificationCategory.REWARD -> Triple(Icons.Filled.Star, Color(0xFFFFF4D9), Color(0xFFB27800))
		NotificationCategory.SOCIAL -> Triple(Icons.Filled.Celebration, Color(0xFFF5E8FF), Color(0xFF7B41C9))
		NotificationCategory.UPDATE -> Triple(Icons.Filled.CalendarToday, Color(0xFFEAF5EE), Color(0xFF3C8C55))
	}

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(20.dp))
			.background(if (notification.isUnread) Color(0xFFEEF0FF) else Color.White.copy(alpha = 0.8f))
			.clickable { onClick() }
			.padding(horizontal = 16.dp, vertical = 18.dp),
		horizontalArrangement = Arrangement.spacedBy(16.dp),
		verticalAlignment = Alignment.Top
	) {
		Surface(shape = CircleShape, color = containerColor) {
			Icon(
				imageVector = icon,
				contentDescription = null,
				tint = accent,
				modifier = Modifier.padding(12.dp)
			)
		}
		Column(
			modifier = Modifier.weight(1f),
			verticalArrangement = Arrangement.spacedBy(6.dp)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = notification.title,
					style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
					color = Color(0xFF2F1F53),
					maxLines = 2,
					overflow = TextOverflow.Ellipsis
				)
				Text(
					text = notification.time,
					style = MaterialTheme.typography.labelSmall,
					color = Color(0xFF8E86A9)
				)
			}
			Text(
				text = notification.description,
				style = MaterialTheme.typography.bodySmall,
				color = Color(0xFF645A82)
			)
		}
		if (notification.isUnread) {
			Box(
				modifier = Modifier
					.size(10.dp)
					.clip(CircleShape)
					.background(Color(0xFF4B3DF0))
			)
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun NotificationsScreenPreview() {
	NotificationsScreen(state = NotificationUiState.sample())
}
