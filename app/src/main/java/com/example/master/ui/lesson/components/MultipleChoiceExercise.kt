package com.example.master.ui.lesson

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MultipleChoiceExercise(
    exercise: Exercise.MultipleChoice,
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
        // Question Card
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
                    text = "Choose the correct answer",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF6B7280)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = exercise.question,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1F2937)
                )
                
                // Audio button if word is available
                exercise.word?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    IconButton(
                        onClick = { /* Play audio */ },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "Play pronunciation",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
        
        // Options
        exercise.options.forEach { option ->
            val isSelected = exercise.selectedAnswer == option
            val isCorrectAnswer = option == exercise.correctAnswer
            
            val backgroundColor = when {
                showResult && isCorrectAnswer -> Color(0xFFDCFCE7)
                showResult && isSelected && !isCorrectAnswer -> Color(0xFFFEE2E2)
                isSelected -> Color(0xFFEEF2FF)
                else -> Color.White
            }
            
            val borderColor = when {
                showResult && isCorrectAnswer -> Color(0xFF10B981)
                showResult && isSelected && !isCorrectAnswer -> Color(0xFFEF4444)
                isSelected -> Color(0xFF6366F1)
                else -> Color(0xFFE5E7EB)
            }
            
            Card(
                onClick = { if (!showResult) onAnswerSelected(option) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                border = BorderStroke(2.dp, borderColor),
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
                                contentDescription = "Correct",
                                tint = Color(0xFF10B981)
                            )
                        } else if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Wrong",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }
        }
        
        // Result Message
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
                            Text(
                                text = "Correct answer: ${exercise.correctAnswer}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B7280)
                            )
                        }
                        exercise.word?.exampleSentence?.let { example ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Example: $example",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }
        }
    }
}
