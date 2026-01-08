package com.example.master.ui.lesson.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.master.ui.lesson.Exercise
import com.example.master.ui.lesson.CorrectAnswerText

@Composable
fun WordTilesExercise(
    exercise: Exercise.WordTiles,
    onTileSelected: (String) -> Unit,
    onTileRemoved: (Int) -> Unit,
    showResult: Boolean,
    isCorrect: Boolean?,
    onPlayNormal: () -> Unit,
    onPlaySlow: () -> Unit
) {
    val availableTiles = buildAvailableTiles(exercise.tiles, exercise.selectedWords)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Translate the sentence",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF6B7280)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = exercise.question,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1F2937),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPlayNormal,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Text("Play", color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onPlaySlow,
                        border = BorderStroke(1.dp, Color(0xFF6366F1))
                    ) {
                        Text("Slow", color = Color(0xFF6366F1))
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Your answer",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF64748B)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (exercise.selectedWords.isEmpty()) {
                        Text(
                            text = "Tap the words below to build the sentence.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
                    } else {
                        exercise.selectedWords.forEachIndexed { index, word ->
                            TileChip(
                                text = word,
                                background = Color(0xFFEEF2FF),
                                contentColor = Color(0xFF1E1B4B),
                                enabled = !showResult
                            ) {
                                onTileRemoved(index)
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "Word bank",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF64748B)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableTiles.forEach { word ->
                TileChip(
                    text = word,
                    background = Color(0xFFF1F5F9),
                    contentColor = Color(0xFF0F172A),
                    enabled = !showResult
                ) {
                    onTileSelected(word)
                }
            }
        }

        if (showResult && isCorrect != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isCorrect) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            tint = if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                        Text(
                            text = if (isCorrect) "Correct!" else "Incorrect",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                    if (!isCorrect) {
                        CorrectAnswerText(exercise.correctAnswer)
                    }
                }
            }
        }
    }
}

@Composable
private fun TileChip(
    text: String,
    background: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = background,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Box(
            modifier = Modifier
                .clickable(enabled = enabled) { onClick() }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) contentColor else Color(0xFF94A3B8)
            )
        }
    }
}

private fun buildAvailableTiles(tiles: List<String>, selected: List<String>): List<String> {
    val remaining = tiles.toMutableList()
    selected.forEach { word -> remaining.remove(word) }
    return remaining
}
