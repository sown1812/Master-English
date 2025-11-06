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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun PictureMatchingExercise(
    exercise: Exercise.PictureMatching,
    onOptionSelected: (String) -> Unit,
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
                    text = "Picture matching",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF6B7280)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = exercise.question,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1F2937),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        
        val optionRows = exercise.options.chunked(2)
        optionRows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { option ->
                    PictureOptionCard(
                        option = option,
                        isSelected = exercise.selectedOptionId == option.id,
                        isCorrectOption = option.id.equals(exercise.correctAnswer, ignoreCase = true),
                        showResult = showResult,
                        onClick = { if (!showResult) onOptionSelected(option.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
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
                            text = if (isCorrect) "Great choice!" else "Look closely at the details",
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
                    }
                }
            }
        }
    }
}

@Composable
private fun PictureOptionCard(
    option: PictureOption,
    isSelected: Boolean,
    isCorrectOption: Boolean,
    showResult: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = when {
        showResult && isCorrectOption -> Color(0xFFDCFCE7)
        showResult && isSelected && !isCorrectOption -> Color(0xFFFEE2E2)
        isSelected -> Color(0xFFEEF2FF)
        else -> Color.White
    }
    
    val border = when {
        showResult && isCorrectOption -> Color(0xFF10B981)
        showResult && isSelected && !isCorrectOption -> Color(0xFFEF4444)
        isSelected -> Color(0xFF6366F1)
        else -> Color(0xFFE5E7EB)
    }
    
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        border = BorderStroke(2.dp, border),
        enabled = !showResult
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = option.imageUrl,
                contentDescription = option.label,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = option.label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1F2937)
            )
        }
    }
}
