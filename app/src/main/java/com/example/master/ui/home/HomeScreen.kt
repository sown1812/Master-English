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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.master.R
import com.example.master.ui.store.QuestUi
import com.example.master.ui.store.StoreViewModel

private data class NavItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val highlighted: Boolean = false,
    val isPrimary: Boolean = false
)

@Composable
fun HomeRoute(
    homeViewModel: HomeViewModel,
    storeViewModel: StoreViewModel
) {
    val state by homeViewModel.uiState
    val storeState by storeViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(storeState.message) {
        storeState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            storeViewModel.clearMessage()
        }
    }

    HomeScreen(
        state = state,
        quests = storeState.quests,
        onFlashcardClick = { homeViewModel.onFlashcardsClicked() },
        onPlayClick = { homeViewModel.onPlayClicked() },
        onClaimQuest = storeViewModel::claimQuest,
        onBoosterClick = { homeViewModel.onBoosterSelected(it) },
        onThemeClick = { homeViewModel.onThemeSelected(it) },
        onLessonClick = { homeViewModel.onLessonSelected(it) },
        onUnlockLesson = { homeViewModel.unlockLesson(it) }
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    quests: List<QuestUi>,
    modifier: Modifier = Modifier,
    onFlashcardClick: () -> Unit = {},
    onPlayClick: () -> Unit = {},
    onClaimQuest: (String) -> Unit = {},
    onBoosterClick: (BoosterItem) -> Unit = {},
    onThemeClick: (ThemeOption) -> Unit = {},
    onLessonClick: (Int) -> Unit = {},
    onUnlockLesson: (LessonSummary) -> Unit = {}
) {
    var pendingUnlock by remember { mutableStateOf<LessonSummary?>(null) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
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
            NextLessonCard(
                lesson = state.nextLesson,
                progress = state.progress,
                maxLevel = state.maxLevel,
                onContinue = { lessonId ->
                    if (lessonId != null) {
                        onLessonClick(lessonId)
                    } else {
                        onPlayClick()
                    }
                },
                onFlashcardClick = onFlashcardClick
            )
            SectionsPathSection(
                sections = state.sections,
                onLessonClick = onLessonClick,
                onLockedLessonClick = { pendingUnlock = it }
            )
            QuestSection(quests = quests, onClaimQuest = onClaimQuest)
            AchievementsSection(
                totalScore = state.totalScore,
                badges = state.badges
            )
            BoosterCarousel(boosters = state.boosters, onBoosterClick = onBoosterClick)
            ThemeSelector(themes = state.themes, onThemeClick = onThemeClick)
        }
    }

    pendingUnlock?.let { lesson ->
        AlertDialog(
            onDismissRequest = { pendingUnlock = null },
            title = { Text("Mo khoa bai hoc") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ban muon mo bai \"${lesson.title}\"?")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_coin),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Chi phi: ${lesson.unlockCost} coins")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUnlockLesson(lesson)
                        pendingUnlock = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C6FFF))
                ) {
                    Text("Mo khoa", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { pendingUnlock = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5E7EB))
                ) {
                    Text("Huy")
                }
            }
        )
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
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_coin),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(28.dp)
                )
            },
            background = Color(0xFFFFF3B0)
        )
        StatusChip(
            modifier = Modifier.weight(1f),
            title = "Streak",
            value = "${streakDays} ngày",
            icon = {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFF6B6B),
                    modifier = Modifier.size(28.dp)
                )
            },
            background = Color(0xFFFFD8DF)
        )
    }
}

@Composable
private fun StatusChip(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: @Composable () -> Unit,
    background: Color
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
            icon()
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
private fun NextLessonCard(
    lesson: LessonSummary?,
    progress: Float,
    maxLevel: Int,
    onContinue: (Int?) -> Unit,
    onFlashcardClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE7FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Tiếp tục học",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF5B1E6A)
            )
            if (lesson != null) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFF4A1D57),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = lesson.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B4A78),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${lesson.totalWords} từ • ${lesson.totalExercises} bài",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6B4A78)
                )
            } else {
                Text(
                    text = "Chưa có bài học phù hợp.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B4A78)
                )
            }
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp)),
                progress = { progress.coerceIn(0f, 1f) },
                trackColor = Color(0xFFF6DDF8),
                color = Color(0xFFB83EA5)
            )
            Text(
                text = "Tiến độ tổng: ${(progress * maxLevel).toInt()} / $maxLevel bài",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B4A78)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onContinue(lesson?.id) },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB83EA5)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Học tiếp", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onFlashcardClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C6FFF)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Flashcard", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SectionsPathSection(
    sections: List<SectionUi>,
    onLessonClick: (Int) -> Unit,
    onLockedLessonClick: (LessonSummary) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Lộ trình học",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF442C66)
        )
        if (sections.isEmpty()) {
            Text(
                text = "Chưa có bài học. Vui lòng đồng bộ nội dung.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7D63A4)
            )
        } else {
            sections.forEach { section ->
                SectionCard(
                    section = section,
                    onLessonClick = onLessonClick,
                    onLockedLessonClick = onLockedLessonClick
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    section: SectionUi,
    onLessonClick: (Int) -> Unit,
    onLockedLessonClick: (LessonSummary) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1F2937)
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFF3E8FF)
            ) {
                Text(
                    text = section.cefrLevel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF6D28D9),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        section.units.forEach { unit ->
            UnitLevelsCard(
                unit = unit,
                onLessonClick = onLessonClick,
                onLockedLessonClick = onLockedLessonClick
            )
        }
    }
}

@Composable
private fun UnitLevelsCard(
    unit: UnitUi,
    onLessonClick: (Int) -> Unit,
    onLockedLessonClick: (LessonSummary) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = unit.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1F2937)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                unit.levels.forEach { level ->
                    LevelCircle(
                        level = level,
                        onLessonClick = onLessonClick,
                        onLockedLessonClick = onLockedLessonClick
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelCircle(
    level: LevelUi,
    onLessonClick: (Int) -> Unit,
    onLockedLessonClick: (LessonSummary) -> Unit
) {
    val (bg, fg) = when (level.status) {
        LevelStatus.COMPLETED -> Color(0xFFFBBF24) to Color.White
        LevelStatus.IN_PROGRESS -> Color(0xFF10B981) to Color.White
        LevelStatus.AVAILABLE -> Color(0xFF10B981) to Color.White
        LevelStatus.LOCKED -> Color(0xFFE5E7EB) to Color(0xFF94A3B8)
    }
    val enabled = level.status != LevelStatus.LOCKED
    val lessonId = level.lessonIds.firstOrNull()

    Surface(
        modifier = Modifier
            .size(42.dp)
            .clickable(enabled = lessonId != null) {
                if (lessonId == null) return@clickable
                if (level.status == LevelStatus.LOCKED && !level.isUnlocked) {
                    onLockedLessonClick(
                        LessonSummary(
                            id = lessonId,
                            title = level.lessonTitle,
                            description = "",
                            difficulty = "EASY",
                            totalWords = 0,
                            totalExercises = 0,
                            isUnlocked = false,
                            unlockCost = level.unlockCost
                        )
                    )
                } else {
                    onLessonClick(lessonId)
                }
            },
        shape = RoundedCornerShape(999.dp),
        color = bg
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = level.order.toString(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = fg
            )
        }
    }
}

@Composable
private fun DifficultyPill(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f)) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
@Composable
private fun AchievementsSection(
    totalScore: Int,
    badges: List<AchievementBadge>
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
        }
        if (badges.isEmpty()) {
            Text(
                text = "Hoàn thành bài học để nhận huy hiệu đầu tiên.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7D63A4)
            )
        } else {
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
private fun QuestSection(
    quests: List<QuestUi>,
    onClaimQuest: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Quests hôm nay",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF442C66)
        )
        if (quests.isEmpty()) {
            Text(
                text = "Chưa có nhiệm vụ mới. Học bài để mở khóa.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7D63A4)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                quests.forEach { quest ->
                    QuestCard(quest = quest, onClaimQuest = onClaimQuest)
                }
            }
        }
    }
}

@Composable
private fun QuestCard(
    quest: QuestUi,
    onClaimQuest: (String) -> Unit
) {
    val actionEnabled = quest.isCompleted && !quest.isClaimed
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4ECFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tiến độ: ${quest.stepsLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6C41A1)
                )
                Button(
                    onClick = { onClaimQuest(quest.key) },
                    enabled = actionEnabled,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6C5AE6),
                        disabledContainerColor = Color(0xFFC7C9D9)
                    )
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
                painter = painterResource(id = R.drawable.ic_coin),
                contentDescription = null,
                tint = Color.Unspecified,
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
        if (boosters.isEmpty()) {
            Text(
                text = "Chưa có booster mới. Ghé shop để xem ưu đãi.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7D63A4)
            )
        } else {
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
        if (themes.isEmpty()) {
            Text(
                text = "Chưa có giao diện mới.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7D63A4)
            )
        } else {
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

