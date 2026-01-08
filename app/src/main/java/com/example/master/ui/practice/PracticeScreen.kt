package com.example.master.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PracticeScreen(
    viewModel: PracticeViewModel = hiltViewModel(),
    onStartLesson: (Int) -> Unit,
    onOpenFlashcards: (Int) -> Unit,
    onOpenShop: () -> Unit,
    onOpenMistakes: () -> Unit,
    onOpenWordle: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Luyện tập nhanh",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else {
            val recommended = state.recommendedLessons.firstOrNull()
            if (recommended != null) {
                RecommendedLessonCard(
                    lesson = recommended,
                    onStart = { onStartLesson(recommended.id) }
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Chưa có bài gợi ý", fontWeight = FontWeight.Bold)
                        Text(
                            "Học một bài để nhận đề xuất phù hợp.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }
        }

        QuickGrid(
            items = buildList {
                val flashcardLesson = state.recommendedLessons.firstOrNull()?.id ?: 1
                add(PracticeItem("Ôn flashcard", "Ghi nhớ từ vựng", Icons.Filled.FlashOn) { onOpenFlashcards(flashcardLesson) })

                state.recommendedLessons.getOrNull(0)?.let { lesson ->
                    add(PracticeItem("Nghe - Chọn", lesson.title, Icons.Filled.Headset) { onStartLesson(lesson.id) })
                }
                state.recommendedLessons.getOrNull(1)?.let { lesson ->
                    add(PracticeItem("Nói - Lặp lại", lesson.title, Icons.Filled.GraphicEq) { onStartLesson(lesson.id) })
                }
                add(PracticeItem("Ôn lỗi sai", "Xem lại các câu sai", Icons.Filled.AutoAwesome, onOpenMistakes))
                add(PracticeItem("Wordle Từ vựng", "Đoán từ theo Wordle", Icons.Filled.TextFields, onOpenWordle))
            }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Boost XP", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Mua booster để nhân điểm", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE0E7FF), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                        .clickable { onOpenShop() }
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFF6366F1))
                }
            }
        }
    }
}

@Composable
private fun RecommendedLessonCard(
    lesson: PracticeLessonItem,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5FF))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Bài gợi ý",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF475569)
            )
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Chủ đề: ${lesson.category}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280)
            )
            Button(
                onClick = onStart,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C6FFF))
            ) {
                Text("Bắt đầu ngay", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class PracticeItem(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun QuickGrid(items: List<PracticeItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { item.onClick() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(item.icon, contentDescription = null, tint = Color(0xFF6366F1))
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
