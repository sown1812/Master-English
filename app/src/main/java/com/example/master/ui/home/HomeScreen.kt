package com.example.master.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.master.R

@Composable
fun HomeRoute(
    homeViewModel: HomeViewModel
) {
    val state by homeViewModel.uiState
    HomeScreen(state = state)
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onPlayClick: () -> Unit = {},
    onDailyChallengeClick: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    onOpenStore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFF3D7), Color(0xFFFFE0F0))
                )
            )
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ProfileBar(userName = state.userName)
            StatusChips(
                coins = state.coins,
                streakDays = state.streakDays,
                rewardAvailable = state.streakRewardAvailable
            )
            HeroCard(
                level = state.level,
                difficulty = state.difficulty,
                progress = state.progress,
                maxLevel = state.maxLevel,
                onPlayClick = onPlayClick
            )
            DailyChallengeCard(
                challenge = state.dailyChallenge,
                countdown = state.nextChallengeCountdown,
                onClick = onDailyChallengeClick
            )
            AchievementsSection(
                totalScore = state.totalScore,
                badges = state.badges,
                onSeeAll = onOpenAchievements
            )
        }
        BottomNavigationBar(
            onStoreClick = onOpenStore,
            onHomeClick = {},
            onPlayClick = onPlayClick,
            onAchievementsClick = onOpenAchievements
        )
    }
}

@Composable
private fun ProfileBar(userName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFC857)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = Color(0xFF7A4E1C),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Xin chào",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6F4AA1)
                )
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF442C66)
                )
            }
        }
        IconButton(onClick = { }) {
            Icon(imageVector = Icons.Filled.Settings, contentDescription = null, tint = Color(0xFF442C66))
        }
    }
}

@Composable
private fun StatusChips(coins: Int, streakDays: Int, rewardAvailable: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatusChip(
            modifier = Modifier.weight(1f),
            title = "Coins",
            value = coins.toString(),
            icon = Icons.Filled.Star,
            background = Color(0xFFFFF3B0),
            accent = Color(0xFFFFD700)
        )
        StatusChip(
            modifier = Modifier.weight(1f),
            title = "Streak",
            value = "${streakDays} ngày",
            icon = Icons.Filled.Favorite,
            background = Color(0xFFFFD8DF),
            accent = if (rewardAvailable) Color(0xFFFF6B6B) else Color(0xFFB94D5D)
        )
    }
}

@Composable
private fun StatusChip(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    accent: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(text = title, style = MaterialTheme.typography.labelMedium, color = Color(0xFF7758A2))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF442C66)
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    level: Int,
    difficulty: Difficulty,
    progress: Float,
    maxLevel: Int,
    onPlayClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB7F2))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFB7F2))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DifficultyBadge(difficulty = difficulty)
                Text(
                    text = "Level $level",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF832B7A)
                    )
                )
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    progress = { progress.coerceIn(0f, 1f) },
                    trackColor = Color(0xFFFFE6FA),
                    color = Color(0xFFFA7CDE)
                )
                Text(
                    text = "Tiến độ: ${(progress * maxLevel).toInt()} / $maxLevel cấp độ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6E3C72)
                )
                Button(
                    onClick = onPlayClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6F91)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Bắt đầu học", fontWeight = FontWeight.Bold)
                }
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_home_black_24dp),
                contentDescription = null,
                tint = Color(0x33FFFFFF),
                modifier = Modifier
                    .size(180.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-30).dp)
            )
        }
    }
}

@Composable
private fun DifficultyBadge(difficulty: Difficulty) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(android.graphics.Color.parseColor(difficulty.colorHex)),
        tonalElevation = 6.dp
    ) {
        Text(
            text = difficulty.label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun DailyChallengeCard(
    challenge: DailyChallenge,
    countdown: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEDF3FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = Color(0xFF4C6FFF),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Daily Challenge",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF34418E)
                    )
                    Text(
                        text = "Hết hạn sau $countdown",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7381C8)
                    )
                }
            }
            Text(
                text = challenge.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFF34418E)
            )
            Text(
                text = "+${challenge.rewardCoins} coins",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFF8C42)
            )
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C6FFF))
            ) {
                Text(text = if (challenge.isAccepted) "Tiếp tục" else "Nhận thử thách", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AchievementsSection(
    totalScore: Int,
    badges: List<AchievementBadge>,
    onSeeAll: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Thành tích",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF442C66)
                )
                Text(
                    text = "Tổng điểm: $totalScore",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7D63A4)
                )
            }
            Button(
                onClick = onSeeAll,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6F91))
            ) {
                Text(text = "Xem tất cả", fontWeight = FontWeight.Bold)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            badges.forEach { badge ->
                AchievementChip(badge = badge)
            }
        }
    }
}

@Composable
private fun AchievementChip(badge: AchievementBadge) {
    val container = if (badge.unlocked) Color(0xFFFFFAE0) else Color(0xFFF1E6FF)
    val outline = if (badge.unlocked) Color(0xFFFFC857) else Color(0xFFB8A2E0)
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(container)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = outline,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = badge.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color(0xFF442C66)
            )
        }
        if (badge.unlocked) {
            Text(
                text = "Đạt được: ${badge.date}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF9869D1)
            )
        } else {
            Text(
                text = "Chưa mở khóa",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFA38CCB)
            )
        }
    }
}

@Composable
private fun BoxScope.BottomNavigationBar(
    onStoreClick: () -> Unit,
    onHomeClick: () -> Unit,
    onPlayClick: () -> Unit,
    onAchievementsClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
        tonalElevation = 12.dp,
        color = Color(0xFF6C41A1)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(icon = Icons.Filled.Store, label = "Shop", onClick = onStoreClick)
            BottomNavItem(icon = Icons.Filled.EmojiEvents, label = "Cup", onClick = onAchievementsClick)
            BottomNavItem(
                icon = Icons.Filled.Home,
                label = "Home",
                highlighted = true,
                onClick = onHomeClick
            )
            BottomNavItem(icon = Icons.Rounded.Notifications, label = "Quests", onClick = onAchievementsClick)
            BottomNavItem(icon = Icons.Filled.Person, label = "Me", onClick = onAchievementsClick)
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    highlighted: Boolean = false
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (highlighted) Color(0xFFFFC857) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) Color(0xFF6C41A1) else Color.White
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(state = HomeUiState.sample())
}
