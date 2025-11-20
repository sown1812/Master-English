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
import androidx.compose.material.icons.automirrored.filled.Wysiwyg
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
    DashboardScreen(state = state, onRefreshLeaderboard = { viewModel.refreshLeaderboard() })
}

@Composable
fun DashboardScreen(
    state: DashboardScreenState,
    onRefreshLeaderboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.errorMessage != null && state.data == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = state.errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    val data = state.data ?: return

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
            HeaderSection(data)
            Spacer(modifier = Modifier.height(20.dp))
            SummaryRow(data)
            Spacer(modifier = Modifier.height(20.dp))
            XpProgressCard(data.xpProgress)
            Spacer(modifier = Modifier.height(20.dp))
            WeeklyProgressCard(data.weeklyProgress)
            Spacer(modifier = Modifier.height(20.dp))
            AchievementsSection(data.achievements)
            Spacer(modifier = Modifier.height(20.dp))
            ChallengesSection(data.upcomingChallenges)
            Spacer(modifier = Modifier.height(20.dp))
            LeaderboardSection(
                leaderboard = data.leaderboard,
                isLoading = state.leaderboardLoading,
                error = state.errorMessage,
                onRefresh = onRefreshLeaderboard
            )
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
            icon = Icons.AutoMirrored.Filled.Wysiwyg,
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
                    color = Color(0xFF6C5AE6)
                )
                Text(
                    text = "${progress.currentXp}/${progress.nextLevelXp} XP",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF6C5AE6)
                )
            }
        }
    }
}

@Composable
private fun WeeklyProgressCard(weekly: List<DailyProgress>) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Progress",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF3A236B)
                )
                Text(
                    text = "${weekly.sumOf { it.xpEarned }} XP",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF6C5AE6)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weekly.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height((80 * day.completion).coerceAtLeast(6f).dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF6C5AE6), Color(0xFF9B8BFF))
                                    )
                                )
                        )
                        Text(
                            text = day.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6E56A3)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementsSection(achievements: List<AchievementSummary>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Achievements",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF40286A)
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            achievements.forEach { achievement ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = achievement.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF3A236B)
                            )
                            Text(
                                text = "${(achievement.progress * 100).toInt()}% • ${achievement.completedCount}/${achievement.totalCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6E56A3)
                            )
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                progress = { achievement.progress },
                                trackColor = Color(0xFFE9DFFF),
                                color = Color(0xFF6C5AE6)
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFC857),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengesSection(challenges: List<DashboardChallenge>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Upcoming Challenges",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF40286A)
        )
        challenges.forEach { challenge ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF234163)
                    )
                    Text(
                        text = "Phần thưởng: ${challenge.rewardCoins} coins",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2C6E8F)
                    )
                    Text(
                        text = "Thời gian còn lại: ${challenge.timeRemaining}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF5597B4)
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardSection(
    leaderboard: List<FriendProgress>,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Bảng xếp hạng bạn bè",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF40286A)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                isLoading -> Text(
                    text = "Đang tải leaderboard...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
                error != null -> Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                else -> Spacer(modifier = Modifier.width(8.dp))
            }
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5AE6)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Làm mới", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isLoading && leaderboard.isEmpty()) {
                    repeat(3) { idx ->
                        LeaderboardSkeletonRow(rank = idx + 1)
                    }
                } else {
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

@Composable
private fun LeaderboardSkeletonRow(rank: Int) {
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
                color = Color(0xFFE7E0FF)
            ) {}
            Column {
                Box(
                    modifier = Modifier
                        .height(14.dp)
                        .width(80.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE7E0FF))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .height(10.dp)
                        .width(60.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE7E0FF))
                )
            }
        }
        Box(
            modifier = Modifier
                .height(12.dp)
                .width(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE7E0FF))
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    val sampleData = DashboardUiState(
        totalWordsLearned = 1280,
        totalCoins = 2450,
        streakDays = 18,
        levelsCompleted = 42,
        weeklyProgress = listOf(
            DailyProgress("Mon", 0.9f, 45),
            DailyProgress("Tue", 0.6f, 30),
            DailyProgress("Wed", 1f, 50),
            DailyProgress("Thu", 0.4f, 22),
            DailyProgress("Fri", 0.7f, 35),
            DailyProgress("Sat", 0.5f, 25),
            DailyProgress("Sun", 0.8f, 40)
        ),
        xpProgress = XpProgress(currentXp = 3400, nextLevelXp = 4000, levelLabel = "Level 40"),
        achievements = listOf(
            AchievementSummary(title = "Perfect Run", progress = 0.8f, completedCount = 4, totalCount = 5),
            AchievementSummary(title = "Grammar Guru", progress = 0.45f, completedCount = 9, totalCount = 20)
        ),
        upcomingChallenges = listOf(
            DashboardChallenge(title = "Weekend Marathon", rewardCoins = 150, timeRemaining = "18:23:10"),
            DashboardChallenge(title = "Streak Hero", rewardCoins = 200, timeRemaining = "2d 5h")
        ),
        leaderboard = listOf(
            FriendProgress(name = "Tuan", avatarInitial = "T", score = 5120, trend = +2),
            FriendProgress(name = "Lan", avatarInitial = "L", score = 5015, trend = -1),
            FriendProgress(name = "Minh", avatarInitial = "M", score = 4880, trend = +4)
        )
    )
    DashboardScreen(
        state = DashboardScreenState(
            isLoading = false,
            data = sampleData,
            leaderboardLoading = false
        ),
        onRefreshLeaderboard = {}
    )
}
