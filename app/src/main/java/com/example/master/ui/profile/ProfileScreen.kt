package com.example.master.ui.profile

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel
) {
    val state by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.profile != null -> ProfileContent(
                uiState = state,
                onRefresh = viewModel::refresh
            )
            else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.errorMessage ?: "Không có dữ liệu hồ sơ")
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = viewModel::refresh) {
                        Text("Thử lại")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onRefresh: () -> Unit
) {
    val profile = uiState.profile ?: return
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (profile.avatarUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(profile.avatarUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .height(64.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .height(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E7FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF4338CA)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(text = profile.email, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF6B7280))
                    Text(text = "Gia nhập: ${dateFormat.format(profile.createdAt)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        StatGrid(
            items = listOf(
                "Cấp độ" to profile.level.toString(),
                "XP" to profile.xp.toString(),
                "Coins" to profile.coins.toString(),
                "Streak" to "${profile.streakDays} ngày"
            )
        )

        ProgressCard(streakDays = profile.streakDays, xp = profile.xp)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Hoạt động gần đây", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = "Lần hoạt động cuối: ${dateFormat.format(profile.lastActive)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4B5563)
                )
                Text(
                    text = "Tiếp tục duy trì streak để nhận thưởng mỗi ngày!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
                Button(
                    onClick = onRefresh,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Làm mới", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun StatGrid(items: List<Pair<String, String>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Thành tích",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items.chunked(2).forEach { row ->
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (label, value) ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF111827)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(streakDays: Int, xp: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Tiến trình & streak",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Streak hiện tại: $streakDays ngày",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4B5563)
            )
            LinearProgressIndicator(
                progress = { (streakDays % 7 / 7f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(0xFF10B981),
                trackColor = Color(0xFFE5E7EB)
            )
            Text(
                text = "XP tích lũy: $xp",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4B5563)
            )
            Text(
                text = "Hoàn thành mục tiêu hằng ngày và duy trì streak để nhận thưởng.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280)
            )
        }
    }
}
