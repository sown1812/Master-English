package com.example.master.ui.lesson

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FillBlankExercise(
    exercise: Exercise.FillBlank,
    onAnswerChanged: (String) -> Unit,
    showResult: Boolean,
    isCorrect: Boolean?,
    onPlayNormal: () -> Unit,
    onPlaySlow: () -> Unit
) {
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
                    text = "Fill in the blank",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF6B7280)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = exercise.question,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1F2937)
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
                
                exercise.hint?.let { hint ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Hint: $hint",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }
        }
        
        AnswerField(
            answer = exercise.userAnswer,
            enabled = !showResult
        )
        OnScreenKeyboard(
            answer = exercise.userAnswer,
            enabled = !showResult,
            onAnswerChanged = onAnswerChanged
        )
        
        if (showResult && isCorrect != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCorrect) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        tint = if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isCorrect) "Correct!" else "Incorrect",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                        if (!isCorrect) {
                            CorrectAnswerText(exercise.correctAnswer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerField(
    answer: String,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Your answer",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (answer.isBlank()) "Tap keyboard below..." else answer,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (answer.isBlank()) Color(0xFF94A3B8) else Color(0xFF0F172A)
            )
            if (!enabled) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Answer locked",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
private fun OnScreenKeyboard(
    answer: String,
    enabled: Boolean,
    onAnswerChanged: (String) -> Unit
) {
    val rows = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                row.forEachIndexed { index, c ->
                    KeyboardKey(
                        label = c.toString(),
                        enabled = enabled,
                        onClick = {
                            onAnswerChanged(answer + c.lowercase())
                        }
                    )
                    if (index < row.length - 1) {
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            KeyboardKey(
                label = "Space",
                enabled = enabled,
                width = 120.dp,
                onClick = {
                    if (answer.isNotEmpty() && !answer.endsWith(" ")) {
                        onAnswerChanged(answer + " ")
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            KeyboardIconKey(
                enabled = enabled,
                onClick = {
                    if (answer.isNotEmpty()) {
                        onAnswerChanged(answer.dropLast(1))
                    }
                }
            )
        }
    }
}

@Composable
private fun KeyboardKey(
    label: String,
    enabled: Boolean,
    width: Dp = 34.dp,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .width(width)
            .height(42.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEF2FF)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, color = Color(0xFF1F2937))
    }
}

@Composable
private fun KeyboardIconKey(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .width(54.dp)
            .height(42.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCBD5F5)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(Icons.Filled.Backspace, contentDescription = null, tint = Color(0xFF1F2937))
    }
}

