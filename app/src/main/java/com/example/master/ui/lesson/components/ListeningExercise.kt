package com.example.master.ui.lesson

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ListeningExercise(
    exercise: Exercise.Listening,
    onPlayNormal: () -> Unit,
    onPlaySlow: () -> Unit,
    onAnswerSelected: (String) -> Unit,
    showResult: Boolean,
    isCorrect: Boolean?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
                    text = "Listen carefully",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF6B7280)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPlayNormal,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Headphones,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play", color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onPlaySlow,
                        border = BorderStroke(1.dp, Color(0xFF6366F1))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Headphones,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Slow", color = Color(0xFF6366F1))
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = exercise.question,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1F2937),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        
        exercise.options.forEach { option ->
            val isSelected = exercise.selectedAnswer == option
            val isCorrectAnswer = option.equals(exercise.correctAnswer, ignoreCase = true)
            
            val background = when {
                showResult && isCorrectAnswer -> Color(0xFFDCFCE7)
                showResult && isSelected && !isCorrectAnswer -> Color(0xFFFEE2E2)
                isSelected -> Color(0xFFEEF2FF)
                else -> Color.White
            }
            
            val border = when {
                showResult && isCorrectAnswer -> Color(0xFF10B981)
                showResult && isSelected && !isCorrectAnswer -> Color(0xFFEF4444)
                isSelected -> Color(0xFF6366F1)
                else -> Color(0xFFE5E7EB)
            }
            
            Card(
                onClick = { if (!showResult) onAnswerSelected(option) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = background),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(2.dp, border),
                enabled = !showResult
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFF1F2937)
                    )
                    
                    if (showResult) {
                        if (isCorrectAnswer) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color(0xFF10B981)
                            )
                        } else if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
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
                            text = if (isCorrect) "Great ear!" else "Listen again and try once more",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                        if (!isCorrect) {
                            Text(
                                text = "Answer: ${exercise.correctAnswer}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }
        }
    }
}
