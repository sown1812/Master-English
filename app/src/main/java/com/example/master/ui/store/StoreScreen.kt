package com.example.master.ui.store

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.master.data.local.ChallengeStatus
import com.example.master.ui.home.BoosterItem

@Composable
fun StoreRoute(
    viewModel: StoreViewModel
) {
    val state = viewModel.uiState.collectAsState()
    StoreScreen(
        state = state.value,
        onBuyBooster = viewModel::purchaseBooster,
        onClaimQuest = viewModel::claimQuest,
        onStartDaily = viewModel::startDailyChallenge,
        onSubmitDaily = { viewModel.submitDailyChallenge(score = 50) },
        onMessageShown = viewModel::clearMessage
    )
}

@Composable
fun StoreScreen(
    state: StoreUiState,
    onBuyBooster: (String) -> Unit,
    onClaimQuest: (String) -> Unit,
    onStartDaily: () -> Unit,
    onSubmitDaily: () -> Unit,
    onMessageShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFDF3FF), Color(0xFFE8F7FF))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Header(state.coins)
            if (state.isSyncing) {
                Text(
                    text = "Đang đồng bộ...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
            }
            DailyChallengeCard(state.dailyChallenge, onStartDaily, onSubmitDaily)
            BoostersSection(state.boosters, onBuyBooster)
            QuestsSection(state.quests, onClaimQuest)
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        )
    }
}

@Composable
private fun Header(coins: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Cửa hàng & Booster",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = Color(0xFF3C2A6E)
        )
        Text(
            text = "Coins hiện có: $coins",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B5CA5)
        )
    }
}

@Composable
private fun DailyChallengeCard(
    data: DailyChallengeUi,
    onStart: () -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF284B63)
            )
            Text(
                text = "Phần thưởng: ${data.rewardCoins} coins",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF3F6B8D)
            )
            Text(
                text = "Tiến độ: ${data.progress}/${data.target}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B778D)
            )
            val (label, action, enabled) = when (data.status) {
                ChallengeStatus.READY -> Triple("Bắt đầu", onStart, true)
                ChallengeStatus.IN_PROGRESS -> Triple("Nộp kết quả", onSubmit, true)
                ChallengeStatus.COMPLETED -> Triple("Hoàn thành", {}, false)
                ChallengeStatus.CLAIMED -> Triple("Đã nhận thưởng", {}, false)
            }
            Button(
                onClick = action,
                enabled = enabled,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A8B5))
            ) {
                Text(label, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun BoostersSection(
    boosters: List<BoosterItem>,
    onBuy: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Booster",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF3C2A6E)
        )
        boosters.forEach { booster ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = booster.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF223344)
                        )
                        Text(
                            text = booster.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5C6A7C)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onBuy(booster.title) },
                        enabled = !booster.isOwned,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5AE6))
                    ) {
                        Text(
                            text = if (booster.isOwned) "Đã mua" else "${booster.costCoins}c",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestsSection(
    quests: List<QuestUi>,
    onClaim: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Quest",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF3C2A6E)
        )
        quests.forEach { quest ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = quest.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF203040)
                    )
                    Text(
                        text = quest.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5C6A7C)
                    )
                    Text(
                        text = "Tiến độ: ${quest.stepsLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4E5A6C)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Thưởng: ${quest.rewardCoins} coins",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2F6F80)
                        )
                        Button(
                            onClick = { onClaim(quest.title) },
                            enabled = quest.isCompleted && !quest.isClaimed,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88A8))
                        ) {
                            Text(
                                text = when {
                                    quest.isClaimed -> "Đã nhận"
                                    quest.isCompleted -> "Nhận"
                                    else -> "Chưa xong"
                                },
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
