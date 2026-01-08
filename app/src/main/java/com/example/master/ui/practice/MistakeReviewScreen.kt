package com.example.master.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MistakeReviewRoute(
    viewModel: MistakeReviewViewModel = hiltViewModel()
    ) {
    val state by viewModel.uiState.collectAsState()
    MistakeReviewScreen(
        state = state,
        onClearLesson = viewModel::clearLesson,
        onClearMistake = viewModel::clearMistake
    )
}

@Composable
fun MistakeReviewScreen(
    state: MistakeReviewUiState,
    onClearLesson: (Int) -> Unit,
    onClearMistake: (Int) -> Unit
) {
    val dateFormatter = rememberDateFormatter()
    var pendingClearLesson by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Ôn lại lỗi sai",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
        )
        Text(
            text = "Tổng: ${state.totalMistakes} lỗi • Nhóm theo bài học",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF475569)
        )

        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
            return
        }

        if (state.groups.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Không có lỗi cần ôn", fontWeight = FontWeight.Bold)
                    Text("Tuyệt vời! Tiếp tục giữ phong độ nhé.", color = Color(0xFF64748B))
                }
            }
            return
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.groups, key = { it.lessonId }) { group ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(group.lessonTitle, fontWeight = FontWeight.Bold)
                                Text(
                                    "${group.mistakes.size} lỗi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Button(
                                onClick = { pendingClearLesson = group.lessonId },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEF2FF))
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color(0xFF4338CA))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Đánh dấu đã nhớ", color = Color(0xFF4338CA))
                            }
                        }

                        group.mistakes.forEach { mistake ->
                            MistakeCard(
                                mistake = mistake,
                                dateFormatter = dateFormatter,
                                onClear = { onClearMistake(mistake.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (pendingClearLesson != null) {
        AlertDialog(
            onDismissRequest = { pendingClearLesson = null },
            title = { Text("Xóa nhóm lỗi") },
            text = { Text("Bạn có muốn xóa toàn bộ lỗi của bài này không?") },
            confirmButton = {
                Button(
                    onClick = {
                        val lessonId = pendingClearLesson
                        if (lessonId != null) onClearLesson(lessonId)
                        pendingClearLesson = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA))
                ) {
                    Text("Xóa", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { pendingClearLesson = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5E7EB))
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun MistakeCard(
    mistake: MistakeItemUi,
    dateFormatter: SimpleDateFormat,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = mistake.question,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text("Bạn trả lời: ${mistake.userAnswer}", color = Color(0xFFEA580C))
            Text("Đáp án đúng: ${mistake.correctAnswer}", color = Color(0xFF15803D))
            mistake.reason?.takeIf { it.isNotBlank() }?.let {
                Text("Gợi ý: $it", color = Color(0xFF334155))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormatter.format(Date(mistake.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
                Button(
                    onClick = onClear,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDCFCE7))
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF15803D))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Đã nhớ", color = Color(0xFF15803D))
                }
            }
        }
    }
}

@Composable
private fun rememberDateFormatter(): SimpleDateFormat =
    remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
