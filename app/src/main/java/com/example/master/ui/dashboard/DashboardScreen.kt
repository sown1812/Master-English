package com.example.master.ui.dashboard

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Wysiwyg
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardRoute(viewModel: DashboardViewModel) {
    val state by viewModel.uiState
    DashboardScreen(state = state)
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF4E7FF), Color(0xFFE3F6FF))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            HeaderSection(state)
            Spacer(modifier = Modifier.height(20.dp))
            SummaryRow(state)
            Spacer(modifier = Modifier.height(20.dp))
            XpProgressCard(state.xpProgress)
            Spacer(modifier = Modifier.height(20.dp))
            WeeklyProgressCard(state.weeklyProgress)
            Spacer(modifier = Modifier.height(20.dp))
            AchievementsSection(state.achievements)
            Spacer(modifier = Modifier.height(20.dp))
            ChallengesSection(state.upcomingChallenges)
            Spacer(modifier = Modifier.height(20.dp))
            LeaderboardSection(state.leaderboard)
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HeaderSection(state: DashboardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = Color(0xFF40286A)
        )
        Text(
            text = "Theo dõi tiến độ học tập và thành tích của bạn",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6E56A3)
        )
    }
}

@Composable
private fun SummaryRow(state: DashboardUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Wysiwyg,
            iconTint = Color(0xFF815AC0),
            label = "Từ vựng",
            value = state.totalWordsLearned.toString()
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Star,
            iconTint = Color(0xFFFFC857),
            label = "Coins",
            value = state.totalCoins.toString()
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Bolt,
            iconTint = Color(0xFFFF6B6B),
            label = "Streak",
            value = "${state.streakDays}"
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.AutoGraph,
            iconTint = Color(0xFF48BB78),
            label = "Levels",
            value = state.levelsCompleted.toString()
        )
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF3A236B)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF6E56A3)
            )
        }
    }
}

@Composable
private fun XpProgressCard(progress: XpProgress) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8E4FF))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "XP Progress",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF3D2C7E)
            )
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(999.dp)),
                progress = { progress.ratio },
                trackColor = Color(0xFFD8D2FF),
                color = Color(0xFF6C5AE6)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = progress.levelLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF3D2C7E)
                )
                Text(
                    text = "${progress.currentXp} / ${progress.nextLevelXp} XP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6E56A3)
                )
            }
        }
    }
}

@Composable
private fun WeeklyProgressCard(progress: List<DailyProgress>) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hoạt động trong tuần",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF40286A)
                )
                Icon(
                    imageVector = Icons.Filled.Timeline,
                    contentDescription = null,
                    tint = Color(0xFF815AC0)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                progress.forEach { day ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEDE8FF)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height((120.dp * day.completion).coerceAtLeast(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF6C5AE6))
                            )
                        }
                        Text(
                            text = day.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF6E56A3)
                        )
                        Text(
                            text = "+${day.xpEarned} XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9C8BD4)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementsSection(achievements: List<AchievementSummary>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Thành tựu",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF40286A)
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            achievements.forEach { achievement ->
                AchievementCard(achievement)
            }
        }
    }
}

@Composable
private fun AchievementCard(achievement: AchievementSummary) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3D6))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFC857)
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column {
                    Text(
                        text = achievement.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF442C66)
                    )
                    Text(
                        text = "${achievement.completedCount}/${achievement.totalCount} hoàn thành",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7A639F)
                    )
                }
            }
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp)),
                progress = { achievement.progress.coerceIn(0f, 1f) },
                trackColor = Color(0xFFFFE7B8),
                color = Color(0xFFFF9F1C)
            )
        }
    }
}

@Composable
private fun ChallengesSection(challenges: List<DashboardChallenge>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Thử thách sắp tới",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF40286A)
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            challenges.forEach { challenge ->
                ChallengeCard(challenge)
            }
        }
    }
}

@Composable
private fun ChallengeCard(challenge: DashboardChallenge) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = challenge.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1E5A7A)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Phần thưởng: ${challenge.rewardCoins} coins",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2C6E8F)
                )
                Button(
                    onClick = { },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88A8))
                ) {
                    Text(text = "Tham gia", fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = "Thời gian còn lại: ${challenge.timeRemaining}",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF5597B4)
            )
        }
    }
}

@Composable
private fun LeaderboardSection(leaderboard: List<FriendProgress>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Bảng xếp hạng bạn bè",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF40286A)
        )
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                leaderboard.forEachIndexed { index, friend ->
                    LeaderboardRow(rank = index + 1, friend = friend)
                    if (index != leaderboard.lastIndex) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFE7E0FF))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, friend: FriendProgress) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF6E56A3)
            )
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Color(0xFF6C5AE6)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = friend.avatarInitial,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF3A236B)
                )
                Text(
                    text = "${friend.score} điểm",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7A639F)
                )
            }
        }
        val trendText = if (friend.trend >= 0) "+${friend.trend}" else friend.trend.toString()
        Text(
            text = trendText,
            style = MaterialTheme.typography.labelMedium,
            color = if (friend.trend >= 0) Color(0xFF48BB78) else Color(0xFFCC4B4B)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    DashboardScreen(state = DashboardUiState.sample())
}
