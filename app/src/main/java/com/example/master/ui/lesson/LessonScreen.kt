package com.example.master.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LessonScreen(
    viewModel: LessonViewModel,
    onLessonComplete: (LessonResult) -> Unit,
    onExit: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Show completion dialog
    if (uiState.isCompleted) {
        LessonCompletionDialog(
            result = viewModel.getLessonResult(),
            onDismiss = { onLessonComplete(viewModel.getLessonResult()) }
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF0F9FF), Color(0xFFE0F2FE))
                )
            )
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Bar
                LessonTopBar(
                    title = uiState.lessonTitle,
                    progress = (uiState.currentExerciseIndex + 1).toFloat() / uiState.totalExercises,
                    hearts = uiState.hearts,
                    onExit = onExit
                )
                
                // Exercise Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val currentExercise = uiState.exercises.getOrNull(uiState.currentExerciseIndex)
                    
                    when (currentExercise) {
                        is Exercise.MultipleChoice -> {
                            MultipleChoiceExercise(
                                exercise = currentExercise,
                                onAnswerSelected = { viewModel.onEvent(LessonEvent.AnswerSelected(it)) },
                                showResult = uiState.showResult,
                                isCorrect = uiState.lastAnswerCorrect
                            )
                        }
                        is Exercise.FillBlank -> {
                            FillBlankExercise(
                                exercise = currentExercise,
                                onAnswerChanged = { viewModel.onEvent(LessonEvent.FillBlankAnswered(it)) },
                                showResult = uiState.showResult,
                                isCorrect = uiState.lastAnswerCorrect
                            )
                        }
                        is Exercise.Matching -> {
                            MatchingExercise(
                                exercise = currentExercise,
                                onPairMatched = { left, right -> 
                                    viewModel.onEvent(LessonEvent.PairMatched(left, right)) 
                                },
                                showResult = uiState.showResult,
                                isCorrect = uiState.lastAnswerCorrect
                            )
                        }
                        is Exercise.Translation -> {
                            TranslationExercise(
                                exercise = currentExercise,
                                onAnswerChanged = { viewModel.onEvent(LessonEvent.FillBlankAnswered(it)) },
                                showResult = uiState.showResult,
                                isCorrect = uiState.lastAnswerCorrect
                            )
                        }
                        null -> {
                            Text("No exercise available", modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
                
                // Bottom Action Button
                BottomActionButton(
                    showResult = uiState.showResult,
                    onSubmit = { viewModel.onEvent(LessonEvent.SubmitAnswer) },
                    onNext = { viewModel.onEvent(LessonEvent.NextExercise) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonTopBar(
    title: String,
    progress: Float,
    hearts: Int,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExit) {
                Icon(Icons.Filled.Close, contentDescription = "Exit")
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = hearts.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = Color(0xFF10B981),
            trackColor = Color(0xFFE5E7EB),
        )
    }
}

@Composable
fun BottomActionButton(
    showResult: Boolean,
    onSubmit: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Button(
            onClick = if (showResult) onNext else onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (showResult) Color(0xFF10B981) else Color(0xFF6366F1)
            )
        ) {
            Text(
                text = if (showResult) "Continue" else "Check Answer",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun LessonCompletionDialog(
    result: LessonResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (result.isPassed) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                    contentDescription = null,
                    tint = if (result.isPassed) Color(0xFF10B981) else Color(0xFFEF4444),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (result.isPassed) "Lesson Complete!" else "Try Again",
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Score: ${result.correctAnswers}/${result.totalExercises}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Accuracy: ${(result.accuracy * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (result.isPassed) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("+${result.xpEarned} XP", fontWeight = FontWeight.Bold)
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFBBF24))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("+${result.coinsEarned} Coins", fontWeight = FontWeight.Bold)
                            Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = Color(0xFFF59E0B))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Continue")
            }
        }
    )
}
