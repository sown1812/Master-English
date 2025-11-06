package com.example.master.ui.lesson

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun TranslationExercise(
    exercise: Exercise.Translation,
    onAnswerChanged: (String) -> Unit,
    showResult: Boolean,
    isCorrect: Boolean?,
    onPlayNormal: () -> Unit,
    onPlaySlow: () -> Unit
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
                
                exercise.word?.exampleTranslation?.let { translation ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Hint: $translation",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
        
        OutlinedTextField(
            value = exercise.userAnswer,
            onValueChange = onAnswerChanged,
            label = { Text("Your translation") },
            placeholder = { Text("Type your answer here...") },
            enabled = !showResult,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = false,
            maxLines = 3
        )
        
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
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RowIndicator(isCorrect = isCorrect)
                    if (!isCorrect) {
                        Text(
                            text = "Correct answer: ${exercise.correctAnswer}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowIndicator(isCorrect: Boolean) {
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
}
