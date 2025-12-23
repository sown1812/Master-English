package com.example.master.ui.lesson.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.master.ui.lesson.Exercise
import kotlinx.coroutines.delay

@Composable
fun SpeedMatchingExercise(
    exercise: Exercise.SpeedMatching,
    showResult: Boolean,
    onClueSelected: (String) -> Unit,
    onWordSelected: (String) -> Unit,
    onTick: () -> Unit
) {
    LaunchedEffect(exercise.timeLeftSec, exercise.isExpired, showResult) {
        if (!exercise.isExpired && !showResult && exercise.matchedIds.size < exercise.pairs.size) {
            delay(1000)
            onTick()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = exercise.question,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        LinearProgressIndicator(
            progress = {
                if (exercise.timeLimitSec == 0) 0f
                else (exercise.timeLeftSec.toFloat() / exercise.timeLimitSec.toFloat()).coerceIn(0f, 1f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = Color(0xFF10B981),
            trackColor = Color(0xFFE2E8F0)
        )

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Thời gian: ${exercise.timeLeftSec}s", color = Color(0xFF475569))
            Text("Combo: x${exercise.combo} | Điểm: ${exercise.scoreEarned}", color = Color(0xFF0F172A))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                exercise.pairs.forEach { pair ->
                    val isMatched = exercise.matchedIds.contains(pair.id)
                    SpeedMatchCard(
                        text = pair.clue,
                        imageUrl = pair.imageUrl,
                        selected = exercise.selectedClueId == pair.id,
                        matched = isMatched,
                        onClick = { if (!isMatched && !exercise.isExpired) onClueSelected(pair.id) }
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val wordIds = if (exercise.wordOrder.isNotEmpty()) exercise.wordOrder else exercise.pairs.map { it.id }
                wordIds.forEach { wordId ->
                    val pair = exercise.pairs.firstOrNull { it.id == wordId } ?: return@forEach
                    val isMatched = exercise.matchedIds.contains(pair.id)
                    SpeedMatchCard(
                        text = pair.word,
                        imageUrl = null,
                        selected = exercise.selectedWordId == pair.id,
                        matched = isMatched,
                        onClick = { if (!isMatched && !exercise.isExpired) onWordSelected(pair.id) }
                    )
                }
            }
        }

        if (exercise.isExpired) {
            Text(
                text = "Hết giờ!", 
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SpeedMatchCard(
    text: String,
    imageUrl: String?,
    selected: Boolean,
    matched: Boolean,
    onClick: () -> Unit
) {
    val background = when {
        matched -> Color(0xFFDCFCE7)
        selected -> Color(0xFFFEF3C7)
        else -> Color.White
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = background),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.size(10.dp))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(background, RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF1F2937)
                )
            }
        }
    }
}
