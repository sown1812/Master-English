package com.example.master.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun LessonScreen(
    viewModel: LessonViewModel,
    onLessonComplete: (LessonResult) -> Unit,
    onExit: () -> Unit,
    onPlayAudio: (text: String, audioUrl: String?, slow: Boolean) -> Unit,
    onRequestSpeechRecognition: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
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
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                val progress = if (uiState.totalExercises == 0) {
                    0f
                } else {
                    (uiState.currentExerciseIndex + 1).toFloat() / uiState.totalExercises.toFloat()
                }
                
                LessonTopBar(
                    title = uiState.lessonTitle,
                    progress = progress,
                    hearts = uiState.hearts,
                    totalHearts = uiState.totalHearts,
                    score = uiState.score,
                    accuracy = uiState.accuracy,
                    onExit = onExit
                )
                
                LessonStatsCard(
                    current = uiState.currentExerciseIndex + 1,
                    total = uiState.totalExercises,
                    correct = uiState.correctAnswers,
                    wrong = uiState.wrongAnswers
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val currentExercise = uiState.exercises.getOrNull(uiState.currentExerciseIndex)
                    
                    when (currentExercise) {
                        is Exercise.MultipleChoice -> {
                            MultipleChoiceExercise(
                                exercise = currentExercise,
                                onAnswerSelected = { viewModel.onEvent(LessonEvent.AnswerSelected(it)) },
                                showResult = uiState.showResult,
                                isCorrect = uiState.lastAnswerCorrect,
                                onPlayNormal = {
                                    val word = currentExercise.word?.word?.takeIf { it.isNotBlank() }
                                        ?: currentExercise.question
                                    onPlayAudio(word, currentExercise.word?.audioUrl, false)
                                },
                                onPlaySlow = {
                                    val word = currentExercise.word?.word?.takeIf { it.isNotBlank() }
                                        ?: currentExercise.question
                                    onPlayAudio(word, currentExercise.word?.audioUrl, true)
                                }
                            )
                        }
                        
                        is Exercise.FillBlank -> {
                            FillBlankExercise(
                                exercise = currentExercise,
                                onAnswerChanged = { viewModel.onEvent(LessonEvent.FillBlankAnswered(it)) },
                                showResult = uiState.showResult,
                                isCorrect = uiState.lastAnswerCorrect,
                                onPlayNormal = {
                                    val text = currentExercise.word?.word
                                        ?: currentExercise.question
                                    onPlayAudio(text, currentExercise.word?.audioUrl, false)
                                },
                                onPlaySlow = {
                                    val text = currentExercise.word?.word
                                        ?: currentExercise.question
                                    onPlayAudio(text, currentExercise.word?.audioUrl, true)
                                }
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
                                isCorrect = uiState.lastAnswerCorrect,
                                onPlayNormal = {
                                    onPlayAudio(currentExercise.question, currentExercise.word?.audioUrl, false)
                                },
                                onPlaySlow = {
                                    onPlayAudio(currentExercise.question, currentExercise.word?.audioUrl, true)
                                }
                            )
                        }
                        
                        is Exercise.Listening -> {
                            ListeningExercise(
                                exercise = currentExercise,
                                onPlayNormal = {
                                    onPlayAudio(
                                        currentExercise.word?.word ?: currentExercise.question,
                                        currentExercise.audioUrl ?: currentExercise.word?.audioUrl,
                                        false
                                    )
                                },
                                onPlaySlow = {
                                    onPlayAudio(
                                        currentExercise.word?.word ?: currentExercise.question,
                                        currentExercise.audioUrl ?: currentExercise.word?.audioUrl,
                                        true
                                    )
                                },
                                onAnswerSelected = { viewModel.onEvent(LessonEvent.AnswerSelected(it)) },
                                showResult = uiState.showResult,
                                isCorrect = uiState.lastAnswerCorrect
                            )
                        }
                        
                        is Exercise.Speaking -> {
                            SpeakingExercise(
                                exercise = currentExercise,
                                onStartRecording = { prompt ->
                                    onRequestSpeechRecognition(prompt)
                                },
                                onPlayPrompt = {
                                    val prompt = currentExercise.prompt.ifBlank { currentExercise.word?.word ?: "" }
                                    if (prompt.isNotBlank()) {
                                        onPlayAudio(prompt, currentExercise.word?.audioUrl, false)
                                    }
                                },
                                onPlayPromptSlow = {
                                    val prompt = currentExercise.prompt.ifBlank { currentExercise.word?.word ?: "" }
                                    if (prompt.isNotBlank()) {
                                        onPlayAudio(prompt, currentExercise.word?.audioUrl, true)
                                    }
                                },
                                showResult = uiState.showResult,
                                isCorrect = uiState.lastAnswerCorrect
                            )
                        }
                        
                        is Exercise.PictureMatching -> {
                            PictureMatchingExercise(
                                exercise = currentExercise,
                                onOptionSelected = { optionId ->
                                    viewModel.onEvent(LessonEvent.PictureOptionSelected(optionId))
                                },
                                showResult = uiState.showResult,
                                isCorrect = uiState.lastAnswerCorrect
                            )
                        }
                        
                        null -> {
                            Text(
                                text = "No exercise available",
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                
                if (uiState.showResult && (uiState.feedbackMessage != null || uiState.explanation != null)) {
                    FeedbackCard(
                        feedback = uiState.feedbackMessage,
                        explanation = uiState.explanation
                    )
                }
                
                BottomActionButton(
                    showResult = uiState.showResult,
                    isAnswerReady = uiState.isAnswerReady,
                    isLastExercise = uiState.currentExerciseIndex == uiState.totalExercises - 1,
                    onSubmit = { viewModel.onEvent(LessonEvent.SubmitAnswer) },
                    onNext = { viewModel.onEvent(LessonEvent.NextExercise) }
                )
            }
        }
    }
}

@Composable
private fun LessonStatsCard(
    current: Int,
    total: Int,
    correct: Int,
    wrong: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem(label = "Exercise", value = "$current / $total")
            StatItem(label = "Correct", value = correct.toString())
            StatItem(label = "Wrong", value = wrong.toString())
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF6B7280)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF1F2937)
        )
    }
}

@Composable
fun LessonTopBar(
    title: String,
    progress: Float,
    hearts: Int,
    totalHearts: Int,
    score: Int,
    accuracy: Float,
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
            
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "$hearts / $totalHearts",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = "Score: $score",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6366F1)
                )
                Text(
                    text = "Accuracy: ${(accuracy * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF10B981)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = Color(0xFF10B981),
            trackColor = Color(0xFFE5E7EB)
        )
    }
}

@Composable
fun FeedbackCard(
    feedback: String?,
    explanation: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            feedback?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1F2937)
                )
            }
            explanation?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

@Composable
fun BottomActionButton(
    showResult: Boolean,
    isAnswerReady: Boolean,
    isLastExercise: Boolean,
    onSubmit: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        val buttonText = when {
            showResult && isLastExercise -> "Finish Lesson"
            showResult -> "Continue"
            else -> "Check Answer"
        }
        
        Button(
            onClick = if (showResult) onNext else onSubmit,
            enabled = showResult || isAnswerReady,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (showResult) Color(0xFF10B981) else Color(0xFF6366F1),
                disabledContainerColor = Color(0xFFCBD5F5)
            )
        ) {
            Text(
                text = buttonText,
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
                    imageVector = if (result.isPassed) Icons.Filled.CheckCircle else Icons.Filled.Close,
                    contentDescription = null,
                    tint = if (result.isPassed) Color(0xFF10B981) else Color(0xFFEF4444),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (result.isPassed) "Lesson Complete!" else "Lesson Over",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge
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
                    text = "Accuracy: ${(result.accuracy * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
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
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Continue")
            }
        }
    )
}
