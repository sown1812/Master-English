package com.example.master.ui.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.master.data.local.ChallengeStatus
import kotlinx.coroutines.flow.StateFlow

@Composable
fun DailyChallengeScreen(
    stateFlow: StateFlow<StoreUiState>,
    onStart: () -> Unit,
    onSubmit: () -> Unit
) {
    val state by stateFlow.collectAsState()
    val challenge = state.dailyChallenge

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFE8F7FF), Color(0xFFFDF6FF))
                )
            )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Thử thách hằng ngày",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFF1E3A5F)
                )
                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4A5568)
                )
                Text(
                    text = "Phần thưởng: ${challenge.rewardCoins} coins",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2C5282)
                )
                Text(
                    text = "Tiến độ: ${challenge.progress}/${challenge.target}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4F5D75)
                )
                Spacer(modifier = Modifier.height(12.dp))
                val (label, enabled, action) = when (challenge.status) {
                    ChallengeStatus.READY -> Triple("Bắt đầu", true, onStart)
                    ChallengeStatus.IN_PROGRESS -> Triple("Nộp kết quả", true, onSubmit)
                    ChallengeStatus.COMPLETED -> Triple("Đã hoàn thành", false, {})
                    ChallengeStatus.CLAIMED -> Triple("Đã nhận thưởng", false, {})
                }
                Button(
                    onClick = action,
                    enabled = enabled,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38B2AC))
                ) {
                    Text(text = label, fontWeight = FontWeight.Bold, color = Color.White)
                }
                val message = state.message
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
