package com.example.master.ui.lesson

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun CorrectAnswerText(answer: String, modifier: Modifier = Modifier) {
    Text(
        text = "Correct answer: $answer",
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFF6B7280),
        modifier = modifier
    )
}
