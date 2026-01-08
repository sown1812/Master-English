package com.example.master.ui.wordle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun WordleRoute(
    viewModel: WordleViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    WordleScreen(
        state = state,
        onKeyPress = viewModel::onKeyPress,
        onBackspace = viewModel::onBackspace,
        onSubmit = viewModel::onSubmitGuess,
        onRestart = viewModel::restartGame
    )
}

@Composable
fun WordleScreen(
    state: WordleUiState,
    onKeyPress: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onRestart: () -> Unit
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Wordle",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Text(
                text = "Lượt: ${state.guesses.size}/${state.maxGuesses}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF)
            )

            Board(
                wordLength = state.wordLength,
                maxGuesses = state.maxGuesses,
                guesses = state.guesses,
                currentGuess = state.currentGuess
            )

            LegendRow()

            if (state.status == WordleStatus.IN_PROGRESS &&
                state.guesses.size == state.maxGuesses - 1
            ) {
                state.hintText?.let {
                    Text(it, color = Color(0xFFEAB308))
                }
            }

            state.message?.let {
                Text(it, color = Color(0xFFF87171))
            }

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(wordLength) { colIndex ->
                    val letter = letters.getOrNull(colIndex)?.toString() ?: ""
                    val state = results?.getOrNull(colIndex)
                    LetterTile(letter = letter, state = state)
                    if (colIndex < wordLength - 1) {
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LegendItem(color = Color(0xFF4C8A4B), label = "Đúng vị trí")
        LegendItem(color = Color(0xFFB59B2B), label = "Có trong từ")
        LegendItem(color = Color(0xFF3A3A3C), label = "Không có")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
    }
}

@Composable
private fun LetterTile(letter: String, state: LetterState?) {
    val background = when (state) {
        LetterState.CORRECT -> Color(0xFF4C8A4B)
        LetterState.PRESENT -> Color(0xFFB59B2B)
        LetterState.ABSENT -> Color(0xFF3A3A3C)
        null -> Color(0xFF121212)
        else -> Color(0xFF121212)
    }
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(background, RoundedCornerShape(6.dp))
            .border(2.dp, Color(0xFF3A3A3C), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
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
        KeyboardRow("QWERTYUIOP", keyboard, onKeyPress, modifier = Modifier.fillMaxWidth())
        KeyboardRow("ASDFGHJKL", keyboard, onKeyPress, modifier = Modifier.fillMaxWidth())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            KeyboardActionKey(
                label = "Enter",
                onClick = onSubmit,
                background = Color(0xFF3A3A3C),
                contentColor = Color.White,
                width = 64.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
            KeyboardRow("ZXCVBNM", keyboard, onKeyPress)
            Spacer(modifier = Modifier.width(6.dp))
            KeyboardIconKey(
                onClick = onBackspace,
                background = Color(0xFF3A3A3C),
                contentColor = Color.White,
                width = 52.dp
            )
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
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        letters.forEach { letter ->
            val state = keyboard[letter] ?: LetterState.UNKNOWN
            val color = when (state) {
                LetterState.CORRECT -> Color(0xFF4C8A4B)
                LetterState.PRESENT -> Color(0xFFB59B2B)
                LetterState.ABSENT -> Color(0xFF3A3A3C)
                else -> Color(0xFF4B4B4F)
            }
            KeyboardLetterKey(
                letter = letter,
                state = state,
                background = color,
                onClick = onKeyPress
            )
            if (letter != letters.last()) {
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}

@Composable
private fun KeyboardLetterKey(
    letter: Char,
    state: LetterState,
    background: Color,
    onClick: (Char) -> Unit
) {
    Button(
        onClick = { onClick(letter) },
        modifier = Modifier
            .width(34.dp)
            .height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = background),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = letter.toString(),
            color = Color.White
        )
    }
}

@Composable
private fun KeyboardActionKey(
    label: String,
    onClick: () -> Unit,
    background: Color,
    contentColor: Color,
    width: Dp
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(width)
            .height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = background),
        contentPadding = PaddingValues(horizontal = 6.dp)
    ) {
        Text(label, color = contentColor)
    }
}

@Composable
private fun KeyboardIconKey(
    onClick: () -> Unit,
    background: Color,
    contentColor: Color,
    width: Dp
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(width)
            .height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = background),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(Icons.Filled.Backspace, contentDescription = "Backspace", tint = contentColor)
    }
}

@Composable
private fun ResultCard(state: WordleUiState, onRestart: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val title = if (state.status == WordleStatus.WON) "Chúc mừng!" else "Chưa đúng rồi"
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                "Từ đúng: ${state.solution}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD1D5DB)
            )
            state.solutionTranslation?.takeIf { it.isNotBlank() }?.let {
                Text("Nghĩa: $it", color = Color(0xFFD1D5DB))
            }
            state.solutionExample?.takeIf { it.isNotBlank() }?.let {
                Text("Ví dụ: $it", color = Color(0xFF9CA3AF))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3A3C))
            ) {
                Text("Chơi mới", textAlign = TextAlign.Center, color = Color.White)
            }
        }
    }
}
