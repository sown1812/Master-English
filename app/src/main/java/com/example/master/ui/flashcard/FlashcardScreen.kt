package com.example.master.ui.flashcard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.VolumeUp
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FlashcardScreen(
    viewModel: FlashcardViewModel,
    onBack: () -> Unit,
    onPlayAudio: (text: String, audioUrl: String?, slow: Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFCF5FF), Color(0xFFE8F2FF))
                )
            )
    ) {
        when {
            uiState.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.cards.isEmpty() -> EmptyDeckState(onBack = onBack)

            else -> FlashcardContent(
                state = uiState,
                onBack = onBack,
                onFlip = viewModel::flipCard,
                onKnown = viewModel::markKnown,
                onUnknown = viewModel::markUnknown,
                onShuffle = viewModel::shuffleDeck,
                onRestart = viewModel::restartDeck,
                onReviewUnknown = viewModel::reviewUnknownOnly,
                onPlayAudio = onPlayAudio
            )
        }
    }
}

@Composable
private fun FlashcardContent(
    state: FlashcardUiState,
    onBack: () -> Unit,
    onFlip: () -> Unit,
    onKnown: () -> Unit,
    onUnknown: () -> Unit,
    onShuffle: () -> Unit,
    onRestart: () -> Unit,
    onReviewUnknown: () -> Unit,
    onPlayAudio: (text: String, audioUrl: String?, slow: Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Header(
            title = state.deckTitle,
            progress = if (state.totalCards == 0) 0f else (state.currentIndex + 1f) / state.totalCards.toFloat(),
            counterLabel = "${state.currentIndex + 1}/${state.totalCards}",
            known = state.knownCount,
            unknown = state.unknownCount,
            onBack = onBack,
            onShuffle = onShuffle,
            onRestart = onRestart
        )

        FlashcardView(
            state = state,
            onFlip = onFlip,
            onPlayAudio = onPlayAudio
        )

        ActionButtons(
            isCompleted = state.isCompleted,
            hasUnknown = state.unknownPool.isNotEmpty(),
            reviewingUnknown = state.reviewingUnknown,
            onUnknown = onUnknown,
            onKnown = onKnown,
            onRestart = onRestart,
            onReviewUnknown = onReviewUnknown
        )
    }
}

@Composable
private fun Header(
    title: String,
    progress: Float,
    counterLabel: String,
    known: Int,
    unknown: Int,
    onBack: () -> Unit,
    onShuffle: () -> Unit,
    onRestart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1D2758)
                    )
                    Text(
                        text = "Lật thẻ để ghi nhớ nhanh",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF6B7280)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onShuffle) {
                        Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle deck")
                    }
                    IconButton(onClick = onRestart) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Restart deck")
                    }
                }
            }
            Column {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    progress = { progress.coerceIn(0f, 1f) },
                    color = Color(0xFF6C5CE7),
                    trackColor = Color(0xFFE5E7EB)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = counterLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF4B5563)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatPill(label = "Nhớ", value = known, color = Color(0xFF10B981))
                        StatPill(label = "Chưa nhớ", value = unknown, color = Color(0xFFEF4444))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, RoundedCornerShape(percent = 50))
            )
            Text(
                text = "$label: $value",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = color
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun FlashcardView(
    state: FlashcardUiState,
    onFlip: () -> Unit,
    onPlayAudio: (text: String, audioUrl: String?, slow: Boolean) -> Unit
) {
    val card = state.currentCard
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clickable(enabled = card != null) { onFlip() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        if (card == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Không có thẻ nào",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF6B7280)
                )
            }
        } else {
            AnimatedContent(
                targetState = state.isFlipped,
                transitionSpec = {
                    (fadeIn(tween(150)) togetherWith fadeOut(tween(150)))
                },
                label = "flashcardFlip"
            ) { flipped ->
                if (!flipped) {
                    FlashcardFront(card = card, onPlayAudio = onPlayAudio)
                } else {
                    FlashcardBack(card = card)
                }
            }
        }
    }
}

@Composable
private fun FlashcardFront(
    card: Flashcard,
    onPlayAudio: (text: String, audioUrl: String?, slow: Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = card.partOfSpeech.ifBlank { "Từ vựng" },
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF6B7280)
            )
            Text(
                text = card.front,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = Color(0xFF1D2758)
            )
            Text(
                text = card.pronunciation,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF7C3AED)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chạm để lật thẻ",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF9CA3AF)
            )
            IconButton(onClick = { onPlayAudio(card.front, card.audioUrl, false) }) {
                Icon(Icons.Filled.VolumeUp, contentDescription = "Phát âm")
            }
        }
    }
}

@Composable
private fun FlashcardBack(card: Flashcard) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Nghĩa",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF6B7280)
            )
            Text(
                text = card.back,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF111827)
            )
            if (card.exampleSentence.isNotBlank()) {
                Text(
                    text = card.exampleSentence,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF1F2937)
                )
                if (card.exampleTranslation.isNotBlank()) {
                    Text(
                        text = card.exampleTranslation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
        Text(
            text = "Chạm để lật lại",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF9CA3AF)
        )
    }
}

@Composable
private fun ActionButtons(
    isCompleted: Boolean,
    hasUnknown: Boolean,
    reviewingUnknown: Boolean,
    onUnknown: () -> Unit,
    onKnown: () -> Unit,
    onRestart: () -> Unit,
    onReviewUnknown: () -> Unit
) {
    if (isCompleted) {
        CompletionCard(
            onRestart = onRestart,
            onReviewUnknown = onReviewUnknown,
            hasUnknown = hasUnknown,
            reviewingUnknown = reviewingUnknown
        )
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onUnknown,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE4E6))
        ) {
            Text(
                text = "Chưa nhớ",
                color = Color(0xFFEF4444),
                fontWeight = FontWeight.Bold
            )
        }
        Button(
            onClick = onKnown,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
        ) {
            Text(
                text = "Đã nhớ",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CompletionCard(
    onRestart: () -> Unit,
    onReviewUnknown: () -> Unit,
    hasUnknown: Boolean,
    reviewingUnknown: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Bạn đã lật hết thẻ!",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Nhấn \"Chưa nhớ\" để ôn lại từ khó hoặc shuffle để đổi thứ tự.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569)
            )
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C6FFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Ôn lại thẻ", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onReviewUnknown,
                enabled = hasUnknown,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB7185)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (reviewingUnknown) "Đang ôn thẻ sai" else "Ôn thẻ sai",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyDeckState(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Chưa có từ vựng trong bài này",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF111827),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Hãy hoàn thành bài học hoặc đồng bộ dữ liệu để có thẻ flashcard.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onBack) {
            Text(text = "Quay lại")
        }
    }
}
