package com.example.master.ui.lesson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SpeakingExercise(
    exercise: Exercise.Speaking,
    onStartRecording: (String) -> Unit,
    onPlayPrompt: () -> Unit,
    onPlayPromptSlow: () -> Unit,
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
                    text = "Speak aloud",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF6B7280)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = exercise.prompt,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1F2937),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPlayPrompt,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Text("Play", color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onPlayPromptSlow,
                        border = BorderStroke(1.dp, Color(0xFF6366F1))
                    ) {
                        Text("Slow", color = Color(0xFF6366F1))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    onClick = { onStartRecording(exercise.prompt) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E7FF))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = "Tap to start speaking",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1F2937)
                        )
                    }
                }
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Your speech",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6B7280)
                )
                Text(
                    text = if (exercise.recognizedText.isBlank()) "Waiting for speech input..." else exercise.recognizedText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (exercise.recognizedText.isBlank()) Color(0xFF9CA3AF) else Color(0xFF1F2937)
                )
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isCorrect) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        tint = if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                    Column {
                        Text(
                            text = if (isCorrect) "Sounds great!" else "Let's try that again",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                        if (!isCorrect) {
                            Text(
                                text = "Expected: ${exercise.correctAnswer}",
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
