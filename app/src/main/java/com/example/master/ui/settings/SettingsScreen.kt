package com.example.master.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onLoggedOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingToggle(
            icon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
            title = "Thông báo",
            description = "Nhận nhắc nhở học và streak",
            checked = state.notificationsEnabled,
            onCheckedChange = { viewModel.toggleNotifications(it) }
        )

        ReminderCard(
            enabled = state.reminderEnabled,
            time = state.reminderTime,
            onToggle = viewModel::toggleReminder,
            onTimeChange = viewModel::updateReminderTime
        )

        ReminderCard(
            enabled = state.reminderEnabled,
            time = state.reminderTime,
            onToggle = viewModel::toggleReminder,
            onTimeChange = viewModel::updateReminderTime
        )

        SettingToggle(
            icon = { Icon(Icons.Filled.VolumeUp, contentDescription = null) },
            title = "Âm thanh",
            description = "Bật tắt hiệu ứng âm thanh",
            checked = state.soundEnabled,
            onCheckedChange = { viewModel.toggleSound(it) }
        )

        SettingToggle(
            icon = { Icon(Icons.Filled.DarkMode, contentDescription = null) },
            title = "Dark mode",
            description = "Chuyển giao diện tối",
            checked = state.darkMode,
            onCheckedChange = { viewModel.toggleDarkMode(it) }
        )

        SettingToggle(
            icon = { Icon(Icons.Filled.VolumeUp, contentDescription = null) },
            title = "Tự phát audio",
            description = "Tự đọc chậm khi vào bài mới",
            checked = state.autoPlayAudio,
            onCheckedChange = { viewModel.toggleAutoPlay(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Tài khoản", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Đăng xuất để đổi tài khoản khác.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
                Button(
                    onClick = {
                        if (!state.isProcessing) {
                            viewModel.logout { success ->
                                if (success) onLoggedOut()
                            }
                        }
                    },
                    enabled = !state.isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Icon(Icons.Filled.PowerSettingsNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đăng xuất", color = Color.White)
                }
                state.message?.let {
                    Text(text = it, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SettingToggle(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(42.dp)
                    .width(42.dp)
                    .background(Color(0xFFE0E7FF), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun ReminderCard(
    enabled: Boolean,
    time: String,
    onToggle: (Boolean) -> Unit,
    onTimeChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color(0xFF6366F1))
                    Column {
                        Text("Nhắc học hằng ngày", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Giờ nhắc: $time", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                    }
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Chọn giờ:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                Button(
                    onClick = { onTimeChange("20:00") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (time == "20:00") Color(0xFF6366F1) else Color(0xFFE5E7EB),
                        contentColor = if (time == "20:00") Color.White else Color(0xFF111827)
                    )
                ) { Text("20:00") }
                Button(
                    onClick = { onTimeChange("21:00") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (time == "21:00") Color(0xFF6366F1) else Color(0xFFE5E7EB),
                        contentColor = if (time == "21:00") Color.White else Color(0xFF111827)
                    )
                ) { Text("21:00") }
            }
            Text(
                text = "Bật nhắc giờ để duy trì streak. (Chưa kết nối hệ thống thông báo nền).",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF)
            )
        }
    }
}
