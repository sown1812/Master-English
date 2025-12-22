package com.example.master.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onLoggedOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleNotifications(true)
        }
    }

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
            onCheckedChange = { enabled ->
                if (!enabled) {
                    viewModel.toggleNotifications(false)
                    return@SettingToggle
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permission = Manifest.permission.POST_NOTIFICATIONS
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        permission
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        viewModel.toggleNotifications(true)
                    } else {
                        permissionLauncher.launch(permission)
                    }
                } else {
                    viewModel.toggleNotifications(true)
                }
            }
        )

        SettingToggle(
            icon = { Icon(Icons.Filled.VolumeUp, contentDescription = null) },
            title = "Âm thanh",
            description = "Bật tắt hiệu ứng âm thanh",
            checked = state.soundEnabled,
            onCheckedChange = { viewModel.toggleSound(it) }
        )

        SettingToggle(
            icon = { Icon(Icons.Filled.VolumeUp, contentDescription = null) },
            title = "Tự phát audio",
            description = "Tự phát chậm khi vào bài mới",
            checked = state.autoPlayAudio,
            onCheckedChange = { viewModel.toggleAutoPlay(it) }
        )

        SettingToggle(
            icon = { Icon(Icons.Filled.DarkMode, contentDescription = null) },
            title = "Dark mode",
            description = "Chuyển giao diện tối",
            checked = state.darkMode,
            onCheckedChange = { viewModel.toggleDarkMode(it) }
        )

        ReminderCard(
            enabled = state.reminderEnabled,
            time = state.reminderTime,
            onToggle = viewModel::toggleReminder,
            onTimeChange = viewModel::updateReminderTime
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Tải trước để học offline", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = "Lưu bài học và audio về máy, có thể học khi không có mạng.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
                Button(
                    onClick = { viewModel.prefetchOfflineContent() },
                    enabled = !state.isOfflineDownloading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text(if (state.isOfflineDownloading) "Đang tải..." else "Tải ngay", color = Color.White)
                }
                if (state.offlineStatus.isNotBlank()) {
                    Text(state.offlineStatus, style = MaterialTheme.typography.bodySmall, color = Color(0xFF0F172A))
                }
            }
        }

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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Icon(Icons.Filled.PowerSettingsNew, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đăng xuất", color = Color.White)
                }
                state.message?.let {
                    Text(it, color = Color(0xFF0F172A), style = MaterialTheme.typography.bodySmall)
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                icon()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text(description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                }
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
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, contentDescription = null)
                    Column {
                        Text("Nhắc giờ học", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Thiết lập nhắc giờ hằng ngày", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                    }
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            if (enabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Giờ nhắc: $time", style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = {
                            val (hour, minute) = parseTime(time)
                            TimePickerDialog(
                                context,
                                { _, selectedHour, selectedMinute ->
                                    onTimeChange(formatTime(selectedHour, selectedMinute))
                                },
                                hour,
                                minute,
                                true
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                    ) {
                        Text("Đổi giờ", color = Color.White)
                    }
                }
            }
        }
    }
}

private fun parseTime(time: String): Pair<Int, Int> {
    val parts = time.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return hour to minute
}

private fun formatTime(hour: Int, minute: Int): String {
    return String.format(Locale.US, "%02d:%02d", hour, minute)
}
