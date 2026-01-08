package com.example.master.ui.lesson

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MatchingExercise(
    exercise: Exercise.Matching,
    onPairMatched: (String, String) -> Unit,
    showResult: Boolean,
    isCorrect: Boolean?
) {
    var selectedLeft by remember { mutableStateOf<String?>(null) }
    var selectedRight by remember { mutableStateOf<String?>(null) }
    val rightOptions = remember(exercise.pairs) { exercise.pairs.map { it.right }.shuffled() }
    
    LaunchedEffect(selectedLeft, selectedRight) {
        if (selectedLeft != null && selectedRight != null) {
            onPairMatched(selectedLeft!!, selectedRight!!)
            selectedLeft = null
            selectedRight = null
        }
    }
    
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
                    text = "Match the pairs",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF6B7280)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = exercise.question,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1F2937)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Tap words to match them",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                exercise.pairs.forEach { pair ->
                    val matchedRight = exercise.selectedPairs[pair.left]
                    val isMatched = matchedRight != null
                    val isPairCorrect = matchedRight?.let { it == pair.right }
                    val isSelected = selectedLeft == pair.left
                    
                    MatchCard(
                        text = pair.left,
                        isSelected = isSelected,
                        isMatched = isMatched,
                        isCorrect = if (matchedRight != null) isPairCorrect else null,
                        enabled = !showResult && (!isMatched || isPairCorrect == false),
                        onClick = {
                            if (!isMatched || isPairCorrect == false) {
                                selectedLeft = if (isSelected) null else pair.left
                            }
                        }
                    )
                }
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rightOptions.forEach { translation ->
                    val matchedLeft = exercise.selectedPairs.entries.find { it.value == translation }?.key
                    val isMatched = matchedLeft != null
                    val isPairCorrect = matchedLeft?.let { left ->
                        exercise.pairs.find { it.left == left }?.right == translation
                    }
                    val isSelected = selectedRight == translation
                    
                    MatchCard(
                        text = translation,
                        isSelected = isSelected,
                        isMatched = isMatched,
                        isCorrect = if (matchedLeft != null) isPairCorrect else null,
                        enabled = !showResult && (!isMatched || isPairCorrect == false),
                        onClick = {
                            if (!isMatched || isPairCorrect == false) {
                                selectedRight = if (isSelected) null else translation
                            }
                        }
                    )
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
                    Text(
                        text = if (isCorrect) "All pairs matched correctly!" else "Some pairs are incorrect",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

@Composable
fun MatchCard(
    text: String,
    isSelected: Boolean,
    isMatched: Boolean,
    isCorrect: Boolean?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isCorrect == true -> Color(0xFFDCFCE7)
        isMatched -> Color(0xFFE0E7FF)
        isSelected -> Color(0xFFEEF2FF)
        else -> Color.White
    }
    
    val borderColor = when {
        isCorrect == true -> Color(0xFF10B981)
        isMatched -> Color(0xFF6366F1)
        isSelected -> Color(0xFF6366F1)
        else -> Color(0xFFE5E7EB)
    }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(2.dp, borderColor),
        enabled = enabled
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFF1F2937)
            )
        }
    }
}
