package com.example.master.ui.lesson.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.master.ui.lesson.Exercise

@Composable
fun FlashcardExercise(
    exercise: Exercise.Flashcard,
    showResult: Boolean,
    onFlip: (Boolean) -> Unit,
    onRemembered: (Boolean) -> Unit
    ) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Flashcard",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF6366F1)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFE0E7FF), Color(0xFFF5F3FF))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onFlip(!exercise.isFlipped) }
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF6366F1)
                    )
                    Text(
                        text = if (exercise.isFlipped) exercise.backText else exercise.frontText,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        ),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (exercise.isFlipped) "Nhấn để xem mặt trước" else "Nhấn để lật thẻ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onRemembered(false) },
                    enabled = !showResult,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cần ôn lại")
                }
                Button(
                    onClick = { onRemembered(true) },
                    enabled = !showResult, // reuse bottom button after marking
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Text("Nhớ rồi", color = Color.White)
                }
            }

            if (exercise.explanation != null && showResult) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = exercise.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4B5563),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
