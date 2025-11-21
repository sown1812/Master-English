package com.example.master.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.material3.contentColorFor
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

private data class NavItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val highlighted: Boolean = false,
    val isPrimary: Boolean = false
)

@Composable
fun HomeRoute(homeViewModel: HomeViewModel) {
    val state by homeViewModel.uiState

    HomeScreen(
        state = state,
        onPlayClick = { homeViewModel.onPlayClicked() },
        onDailyChallengeClick = { homeViewModel.onDailyChallengeClicked() },
        onOpenAchievements = { homeViewModel.onAchievementsClicked() },
        onOpenStore = { homeViewModel.onStoreClicked() },
        onQuestClick = { homeViewModel.onQuestSelected(it) },
        onBoosterClick = { homeViewModel.onBoosterSelected(it) },
        onThemeClick = { homeViewModel.onThemeSelected(it) }
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    onPlayClick: () -> Unit = {},
    onDailyChallengeClick: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    onOpenStore: () -> Unit = {},
    onQuestClick: (Quest) -> Unit = {},
    onBoosterClick: (BoosterItem) -> Unit = {},
    onThemeClick: (ThemeOption) -> Unit = {}
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
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 120.dp),
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
            QuestSection(quests = state.quests, onQuestClick = onQuestClick)
            BoosterCarousel(boosters = state.boosters, onBoosterClick = onBoosterClick)
            ThemeSelector(themes = state.themes, onThemeClick = onThemeClick)
        }
    }
}

@Composable
private fun ProfileBar(userName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF40286A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFC857)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color(0xFF7A4E1C),
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Xin chào trở lại",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )
                }
            }
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C41C8)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Filled.Settings, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Tùy chỉnh", color = Color.White, fontWeight = FontWeight.Bold)
            }
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
private fun QuestSection(quests: List<Quest>, onQuestClick: (Quest) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Quests hôm nay",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF442C66)
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            quests.forEach { quest ->
                QuestCard(quest = quest, onClick = { onQuestClick(quest) })
            }
        }
    }
}

@Composable
private fun QuestCard(quest: Quest, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4ECFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.TaskAlt,
                    contentDescription = null,
                    tint = Color(0xFF6C41A1),
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quest.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF442C66)
                    )
                    Text(
                        text = quest.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7D63A4)
                    )
                }
                RewardChip(value = quest.rewardCoins)
            }
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp)),
                progress = { quest.progress.coerceIn(0f, 1f) },
                trackColor = Color(0xFFE8DAFF),
                color = Color(0xFFB197F5)
            )
            Text(
                text = "Tiến độ: ${quest.stepsLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF6C41A1)
            )
        }
    }
}

@Composable
private fun RewardChip(value: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFFFFF3B0)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = Color(0xFFFFC857),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "+$value",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF442C66)
            )
        }
    }
}

@Composable
private fun BoosterCarousel(boosters: List<BoosterItem>, onBoosterClick: (BoosterItem) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Boosters & Hints",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF442C66)
            )
            Icon(
                imageVector = Icons.Filled.TipsAndUpdates,
                contentDescription = null,
                tint = Color(0xFFFF8C42),
                modifier = Modifier.size(20.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            boosters.forEach { booster ->
                BoosterCard(booster = booster, onClick = { onBoosterClick(booster) })
            }
        }
    }
}

@Composable
private fun BoosterCard(booster: BoosterItem, onClick: () -> Unit) {
    val containerColor = if (booster.isOwned) Color(0xFFE3FCEF) else Color(0xFFFFF5E6)
    val accentColor = if (booster.isOwned) Color(0xFF2ECC71) else Color(0xFFFF8C42)
    Card(
        modifier = Modifier.width(200.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = booster.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF442C66)
                )
                Icon(
                    imageVector = if (booster.isOwned) Icons.Filled.EmojiEvents else Icons.Filled.Star,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = booster.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7D63A4)
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (booster.isOwned) "Đã sở hữu" else "${booster.costCoins} coins",
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { onClick() },
                    enabled = !booster.isOwned,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        disabledContainerColor = Color(0xFFB8B8B8)
                    )
                ) {
                    Text(
                        text = if (booster.isOwned) "Đang dùng" else "Mua",
                        color = if (booster.isOwned) Color.White.copy(alpha = 0.6f) else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSelector(themes: List<ThemeOption>, onThemeClick: (ThemeOption) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Giao diện",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF442C66)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            themes.forEach { theme ->
                ThemeCard(theme = theme, onClick = { onThemeClick(theme) })
            }
        }
    }
}

@Composable
private fun ThemeCard(theme: ThemeOption, onClick: () -> Unit) {
    val primaryColor = Color(android.graphics.Color.parseColor(theme.primaryColor))
    val secondaryColor = Color(android.graphics.Color.parseColor(theme.secondaryColor))
    val gradient = Brush.verticalGradient(listOf(primaryColor, secondaryColor))
    Card(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(gradient)
        ) {}
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = theme.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF442C66)
                )
                if (!theme.isUnlocked) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = Color(0xFF7D63A4),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = if (theme.isUnlocked) "${if (theme.isSelected) "Đang chọn" else "Đã mở khóa"}" else "Cần 500 coins",
                style = MaterialTheme.typography.labelMedium,
                color = if (theme.isSelected) Color(0xFF2ECC71) else Color(0xFF7D63A4)
            )
            Button(
                onClick = { onClick() },
                enabled = theme.isUnlocked,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = contentColorFor(primaryColor),
                    disabledContainerColor = Color(0xFFB8B8B8)
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.ColorLens,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (theme.isSelected) "Đang dùng" else "Áp dụng",
                    fontWeight = FontWeight.Bold
                )
            }
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
    val items = listOf(
        NavItem(icon = Icons.Filled.Home, label = "Home", onClick = onHomeClick, highlighted = true),
        NavItem(icon = Icons.Filled.AutoAwesome, label = "Dashboard", onClick = onAchievementsClick),
        NavItem(icon = Icons.Filled.PlayArrow, label = "Learn", onClick = onPlayClick, isPrimary = true),
        NavItem(icon = Icons.Filled.Store, label = "Shop", onClick = onStoreClick),
        NavItem(icon = Icons.Rounded.Notifications, label = "Alerts", onClick = onAchievementsClick)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        color = Color.Transparent
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    when {
                        item.isPrimary -> PrimaryNavFab(item)
                        else -> BottomNavIcon(
                            icon = item.icon,
                            highlighted = item.highlighted,
                            onClick = item.onClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    highlighted: Boolean = false
) {
    Surface(
        shape = CircleShape,
        color = if (highlighted) Color(0xFFEEF0FF) else Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onClick() }
                .padding(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) Color(0xFF4B3DF0) else Color(0xFF8B8BA7)
            )
        }
    }
}

@Composable
private fun PrimaryNavFab(item: NavItem) {
    Surface(
        shape = CircleShape,
        color = Color(0xFF4B3DF0),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xFF4B3DF0))
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .clickable { item.onClick() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = item.icon, contentDescription = null, tint = Color.White)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(state = HomeUiState.sample())
}
