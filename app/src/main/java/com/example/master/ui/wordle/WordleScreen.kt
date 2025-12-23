package com.example.master.ui.wordle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@Composable
fun WordleRoute(
    viewModel: WordleViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    WordleScreen(
        state = state,
        onModeChange = viewModel::onModeChange,
        onDifficultyChange = viewModel::onDifficultyChange,
        onTopicChange = viewModel::onTopicChange,
        onKeyPress = viewModel::onKeyPress,
        onBackspace = viewModel::onBackspace,
        onSubmit = viewModel::onSubmitGuess,
        onUseHint = viewModel::useHint,
        onRestart = viewModel::restartGame
    )
}

@Composable
fun WordleScreen(
    state: WordleUiState,
    onModeChange: (WordleMode) -> Unit,
    onDifficultyChange: (WordleDifficulty) -> Unit,
    onTopicChange: (String?) -> Unit,
    onKeyPress: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onUseHint: () -> Unit,
    onRestart: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Wordle Từ vựng",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        SectionTitle("Chế độ")
        ChipRow(
            items = WordleMode.values().toList(),
            selected = state.mode,
            onSelected = onModeChange
        ) { mode ->
            when (mode) {
                WordleMode.DAILY -> "Daily"
                WordleMode.PRACTICE -> "Practice"
                WordleMode.TOPIC -> "Topic"
            }
        }

        SectionTitle("Độ khó")
        ChipRow(
            items = WordleDifficulty.values().toList(),
            selected = state.difficulty,
            onSelected = onDifficultyChange
        ) { difficulty ->
            when (difficulty) {
                WordleDifficulty.EASY -> "Easy"
                WordleDifficulty.MEDIUM -> "Medium"
                WordleDifficulty.HARD -> "Hard"
            }
        }

        if (state.mode == WordleMode.TOPIC) {
            SectionTitle("Chủ đề")
            TopicRow(
                topics = state.availableTopics,
                selected = state.selectedTopic,
                onSelected = onTopicChange
            )
        }

        if (state.mode == WordleMode.DAILY) {
            Text(
                text = "Streak: ${state.dailyStreak} | Đã chơi hôm nay: ${if (state.dailyCompleted) "Có" else "Chưa"}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569)
            )
        }

        Board(
            wordLength = state.wordLength,
            maxGuesses = state.maxGuesses,
            guesses = state.guesses,
            currentGuess = state.currentGuess
        )

        state.message?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        HintSection(
            hints = state.hints,
            hintsAllowed = state.hintsAllowed,
            onUseHint = onUseHint
        )

        Keyboard(
            keyboard = state.keyboard,
            onKeyPress = onKeyPress,
            onBackspace = onBackspace,
            onSubmit = onSubmit
        )

        if (state.status != WordleStatus.IN_PROGRESS) {
            ResultCard(state = state, onRestart = onRestart)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = Color(0xFF1F2937)
    )
}

@Composable
private fun <T> ChipRow(
    items: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    label: (T) -> String
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            FilterChip(
                selected = item == selected,
                onClick = { onSelected(item) },
                label = { Text(label(item)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF1E293B),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun TopicRow(
    topics: List<String>,
    selected: String?,
    onSelected: (String?) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelected(null) },
            label = { Text("Tất cả") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF0F766E),
                selectedLabelColor = Color.White
            )
        )
        topics.take(6).forEach { topic ->
            FilterChip(
                selected = selected == topic,
                onClick = { onSelected(topic) },
                label = { Text(topic) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF0F766E),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun Board(
    wordLength: Int,
    maxGuesses: Int,
    guesses: List<GuessResult>,
    currentGuess: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(maxGuesses) { rowIndex ->
            val guess = guesses.getOrNull(rowIndex)
            val letters = when {
                guess != null -> guess.word
                rowIndex == guesses.size -> currentGuess
                else -> ""
            }
            val results = guess?.results

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(wordLength) { colIndex ->
                    val letter = letters.getOrNull(colIndex)?.toString() ?: ""
                    val state = results?.getOrNull(colIndex)
                    LetterTile(letter = letter, state = state)
                }
            }
        }
    }
}

@Composable
private fun LetterTile(letter: String, state: LetterState?) {
    val background = when (state) {
        LetterState.CORRECT -> Color(0xFF16A34A)
        LetterState.PRESENT -> Color(0xFFF59E0B)
        LetterState.ABSENT -> Color(0xFF94A3B8)
        null -> Color(0xFFE2E8F0)
        else -> Color(0xFFE2E8F0)
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(background, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (state == null) Color(0xFF1F2937) else Color.White
        )
    }
}

@Composable
private fun HintSection(
    hints: List<HintEntry>,
    hintsAllowed: Int,
    onUseHint: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Gợi ý (${hints.size}/$hintsAllowed)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Button(
                    onClick = onUseHint,
                    enabled = hints.size < hintsAllowed,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                ) {
                    Text("Dùng hint")
                }
            }
            hints.forEach { hint ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Hint ${hint.level}: ${hint.text}")
                    hint.imageUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Keyboard(
    keyboard: Map<Char, LetterState>,
    onKeyPress: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        KeyboardRow("QWERTYUIOP", keyboard, onKeyPress)
        KeyboardRow("ASDFGHJKL", keyboard, onKeyPress)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onSubmit,
                modifier = Modifier.height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
            ) {
                Text("Enter", color = Color.White)
            }
            KeyboardRow("ZXCVBNM", keyboard, onKeyPress, Modifier.weight(1f))
            Button(
                onClick = onBackspace,
                modifier = Modifier.height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569))
            ) {
                Icon(Icons.Filled.Backspace, contentDescription = "Backspace", tint = Color.White)
            }
        }
    }
}

@Composable
private fun KeyboardRow(
    letters: String,
    keyboard: Map<Char, LetterState>,
    onKeyPress: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        letters.forEach { letter ->
            val state = keyboard[letter] ?: LetterState.UNKNOWN
            val color = when (state) {
                LetterState.CORRECT -> Color(0xFF16A34A)
                LetterState.PRESENT -> Color(0xFFF59E0B)
                LetterState.ABSENT -> Color(0xFF94A3B8)
                else -> Color(0xFFE2E8F0)
            }
            Button(
                onClick = { onKeyPress(letter) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color)
            ) {
                Text(letter.toString(), color = if (state == LetterState.UNKNOWN) Color(0xFF1F2937) else Color.White)
            }
        }
    }
}

@Composable
private fun ResultCard(state: WordleUiState, onRestart: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val title = if (state.status == WordleStatus.WON) "Chúc mừng!" else "Chưa đúng rồi"
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("Từ đúng: ${state.solution}", style = MaterialTheme.typography.bodyMedium)
            state.solutionTranslation?.let { Text("Nghĩa: $it") }
            state.solutionPronunciation?.let { Text("Phát âm: $it") }
            state.solutionExample?.takeIf { it.isNotBlank() }?.let { Text("Ví dụ: $it") }
            state.score?.let { Text("Điểm: $it", fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = onRestart) {
                Text("Chơi mới", textAlign = TextAlign.Center)
            }
        }
    }
}
