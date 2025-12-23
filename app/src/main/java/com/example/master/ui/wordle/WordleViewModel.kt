package com.example.master.ui.wordle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.master.data.local.WordleStateStore
import com.example.master.data.repository.LearningRepository
import com.example.master.data.local.entity.WordEntity
import com.example.master.network.ApiService
import com.example.master.network.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WordleMode { DAILY, PRACTICE, TOPIC }

enum class WordleDifficulty(
    val lengthRange: IntRange,
    val maxGuesses: Int,
    val hintsAllowed: Int
) {
    EASY(4..5, 7, 2),
    MEDIUM(5..6, 6, 1),
    HARD(6..7, 5, 0)
}

enum class LetterState { UNKNOWN, ABSENT, PRESENT, CORRECT }

enum class WordleStatus { IN_PROGRESS, WON, LOST }

data class GuessResult(
    val word: String,
    val results: List<LetterState>
)

data class HintEntry(
    val level: Int,
    val text: String,
    val imageUrl: String? = null,
    val cost: Int = 20
)

data class WordleUiState(
    val mode: WordleMode = WordleMode.PRACTICE,
    val difficulty: WordleDifficulty = WordleDifficulty.MEDIUM,
    val availableTopics: List<String> = emptyList(),
    val selectedTopic: String? = null,
    val wordLength: Int = 5,
    val maxGuesses: Int = 6,
    val currentGuess: String = "",
    val guesses: List<GuessResult> = emptyList(),
    val keyboard: Map<Char, LetterState> = emptyMap(),
    val status: WordleStatus = WordleStatus.IN_PROGRESS,
    val message: String? = null,
    val hints: List<HintEntry> = emptyList(),
    val hintsAllowed: Int = 1,
    val score: Int? = null,
    val solution: String? = null,
    val solutionTranslation: String? = null,
    val solutionExample: String? = null,
    val solutionPronunciation: String? = null,
    val dailyStreak: Int = 0,
    val dailyCompleted: Boolean = false
)

@HiltViewModel
class WordleViewModel @Inject constructor(
    private val repository: LearningRepository,
    private val wordleStateStore: WordleStateStore,
    private val api: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordleUiState())
    val uiState: StateFlow<WordleUiState> = _uiState.asStateFlow()

    private var targetWord: String = ""
    private var targetEntity: WordEntity? = null
    private var validWords: Set<String> = emptySet()
    private var allWords: List<WordEntity> = emptyList()

    init {
        viewModelScope.launch {
            repository.getAllWords().collect { words ->
                allWords = words
                if (allWords.isEmpty()) {
                    allWords = defaultWordBank()
                }
                val topics = words.mapNotNull { it.category.takeIf { c -> c.isNotBlank() } }
                    .distinct()
                    .sorted()
                _uiState.update { it.copy(availableTopics = topics) }
                if (targetWord.isBlank()) {
                    startNewGame()
                }
            }
        }

        viewModelScope.launch {
            wordleStateStore.dailyState.collect { daily ->
                val today = currentEpochDay()
                _uiState.update {
                    it.copy(
                        dailyStreak = daily.streak,
                        dailyCompleted = daily.lastPlayedDay == today
                    )
                }
            }
        }

        viewModelScope.launch {
            fetchRemoteWords()
        }
    }

    fun onModeChange(mode: WordleMode) {
        _uiState.update { it.copy(mode = mode, selectedTopic = if (mode == WordleMode.TOPIC) it.selectedTopic else null) }
        startNewGame()
    }

    fun onDifficultyChange(difficulty: WordleDifficulty) {
        _uiState.update {
            it.copy(
                difficulty = difficulty,
                maxGuesses = difficulty.maxGuesses,
                hintsAllowed = difficulty.hintsAllowed
            )
        }
        startNewGame()
    }

    fun onTopicChange(topic: String?) {
        _uiState.update { it.copy(selectedTopic = topic) }
        startNewGame()
    }

    fun onKeyPress(letter: Char) {
        val state = _uiState.value
        if (state.status != WordleStatus.IN_PROGRESS) return
        if (state.currentGuess.length >= state.wordLength) return
        if (!letter.isLetter()) return
        _uiState.update { it.copy(currentGuess = it.currentGuess + letter.uppercaseChar()) }
    }

    fun onBackspace() {
        val state = _uiState.value
        if (state.status != WordleStatus.IN_PROGRESS) return
        if (state.currentGuess.isEmpty()) return
        _uiState.update { it.copy(currentGuess = it.currentGuess.dropLast(1)) }
    }

    fun onSubmitGuess() {
        val state = _uiState.value
        if (state.status != WordleStatus.IN_PROGRESS) return
        val guess = state.currentGuess.trim().uppercase()
        if (guess.length != state.wordLength) {
            _uiState.update { it.copy(message = "Chưa đủ ký tự") }
            return
        }
        if (!validWords.contains(guess)) {
            _uiState.update { it.copy(message = "Từ không hợp lệ") }
            return
        }

        val results = evaluateGuess(guess, targetWord)
        val updatedKeyboard = updateKeyboard(state.keyboard, guess, results)
        val newGuesses = state.guesses + GuessResult(guess, results)

        val won = guess == targetWord
        val lost = !won && newGuesses.size >= state.maxGuesses

        val nextStatus = when {
            won -> WordleStatus.WON
            lost -> WordleStatus.LOST
            else -> WordleStatus.IN_PROGRESS
        }

        _uiState.update {
            it.copy(
                guesses = newGuesses,
                currentGuess = "",
                keyboard = updatedKeyboard,
                status = nextStatus,
                message = null,
                solution = if (nextStatus != WordleStatus.IN_PROGRESS) targetWord else null,
                solutionTranslation = if (nextStatus != WordleStatus.IN_PROGRESS) targetEntity?.translation else null,
                solutionExample = if (nextStatus != WordleStatus.IN_PROGRESS) targetEntity?.exampleSentence else null,
                solutionPronunciation = if (nextStatus != WordleStatus.IN_PROGRESS) targetEntity?.pronunciation else null
            )
        }

        if (nextStatus != WordleStatus.IN_PROGRESS && state.mode == WordleMode.DAILY) {
            viewModelScope.launch {
                wordleStateStore.updateDailyResult(currentEpochDay(), nextStatus == WordleStatus.WON)
            }
        }

        if (nextStatus == WordleStatus.WON) {
            val score = calculateScore(newGuesses.size, state.hints.size, state.mode, state.dailyStreak)
            _uiState.update { it.copy(score = score) }
        }
    }

    fun useHint() {
        val state = _uiState.value
        if (state.status != WordleStatus.IN_PROGRESS) return
        if (state.hints.size >= state.hintsAllowed) {
            _uiState.update { it.copy(message = "Bạn đã dùng hết hint") }
            return
        }
        val hint = buildHint(state.hints.size + 1, targetEntity)
        _uiState.update { it.copy(hints = it.hints + hint, message = null) }
    }

    fun restartGame() {
        startNewGame()
    }

    private fun startNewGame() {
        if (allWords.isEmpty()) {
            _uiState.update { it.copy(message = "Không có dữ liệu Wordle, hãy thử lại khi có mạng.") }
            return
        }
        val state = _uiState.value
        val difficulty = state.difficulty
        val mode = state.mode
        val topic = state.selectedTopic

        val selected = selectTarget(allWords, mode, difficulty, topic)
        if (selected == null) {
            _uiState.update { it.copy(message = "Không tìm thấy từ phù hợp") }
            return
        }

        targetEntity = selected
        targetWord = selected.word.uppercase()

        val wordLength = targetWord.length
        validWords = buildValidSet(allWords, mode, difficulty, topic, wordLength)

        _uiState.update {
            it.copy(
                wordLength = wordLength,
                maxGuesses = difficulty.maxGuesses,
                currentGuess = "",
                guesses = emptyList(),
                keyboard = defaultKeyboard(),
                status = WordleStatus.IN_PROGRESS,
                message = null,
                hints = emptyList(),
                hintsAllowed = difficulty.hintsAllowed,
                score = null,
                solution = null,
                solutionTranslation = null,
                solutionExample = null,
                solutionPronunciation = null
            )
        }
    }

    private fun selectTarget(
        words: List<WordEntity>,
        mode: WordleMode,
        difficulty: WordleDifficulty,
        topic: String?
    ): WordEntity? {
        val filtered = words.filter { w ->
            isValidWord(w.word) &&
                w.word.length in difficulty.lengthRange &&
                (topic == null || w.category == topic)
        }
        if (filtered.isEmpty()) return null

        val lengths = filtered.map { it.word.length }.distinct().sorted()
        val chosenLength = when (mode) {
            WordleMode.DAILY -> {
                val seed = buildDailySeed(difficulty, topic)
                lengths[abs(seed) % lengths.size]
            }
            else -> lengths.random()
        }

        val candidates = filtered.filter { it.word.length == chosenLength }
        return when (mode) {
            WordleMode.DAILY -> {
                val seed = buildDailySeed(difficulty, topic)
                val index = abs(seed / 7) % candidates.size
                candidates[index]
            }
            else -> candidates.random()
        }
    }

    private fun buildValidSet(
        words: List<WordEntity>,
        mode: WordleMode,
        difficulty: WordleDifficulty,
        topic: String?,
        wordLength: Int
    ): Set<String> {
        return words.filter { w ->
            isValidWord(w.word) &&
                w.word.length == wordLength &&
                w.word.length in difficulty.lengthRange &&
                (mode != WordleMode.TOPIC || topic == null || w.category == topic)
        }.map { it.word.uppercase() }.toSet()
    }

    private suspend fun fetchRemoteWords() {
        val lessons = runCatching { api.getLessons() }.getOrNull().orEmpty()
        if (lessons.isEmpty()) return

        val remoteWords = lessons.flatMap { lesson ->
            runCatching { api.getWordsByLesson(lesson.id) }.getOrDefault(emptyList())
        }

        if (remoteWords.isNotEmpty()) {
            val mapped = remoteWords.map { it.toEntity() }
            allWords = mapped
            val topics = mapped.mapNotNull { it.category.takeIf { c -> c.isNotBlank() } }
                .distinct()
                .sorted()
            _uiState.update { it.copy(availableTopics = topics) }
            startNewGame()
        } else if (allWords.isEmpty()) {
            allWords = defaultWordBank()
            _uiState.update { it.copy(availableTopics = allWords.mapNotNull { it.category }.distinct()) }
            startNewGame()
        }
    }

    private fun defaultWordBank(): List<WordEntity> = listOf(
        WordEntity("apple", "quả táo", "APPLE", "noun", "I eat an apple", "Tôi ăn một quả táo", lessonId = 0, difficulty = 1, category = "food"),
        WordEntity("brain", "bộ não", "BRAIN", "noun", "Use your brain", "Dùng trí não của bạn", lessonId = 0, difficulty = 2, category = "body"),
        WordEntity("chair", "cái ghế", "CHAIR", "noun", "Sit on the chair", "Ngồi trên ghế", lessonId = 0, difficulty = 1, category = "home"),
        WordEntity("drive", "lái xe", "DRIVE", "verb", "I drive to work", "Tôi lái xe đi làm", lessonId = 0, difficulty = 2, category = "travel"),
        WordEntity("earth", "trái đất", "EARTH", "noun", "The earth is round", "Trái đất hình cầu", lessonId = 0, difficulty = 2, category = "nature"),
        WordEntity("flame", "ngọn lửa", "FLAME", "noun", "The flame is bright", "Ngọn lửa sáng", lessonId = 0, difficulty = 3, category = "safety"),
        WordEntity("grape", "quả nho", "GRAPE", "noun", "Grapes are sweet", "Nho thì ngọt", lessonId = 0, difficulty = 1, category = "food"),
        WordEntity("happy", "hạnh phúc", "HAPPY", "adjective", "I am happy", "Tôi hạnh phúc", lessonId = 0, difficulty = 1, category = "feelings"),
        WordEntity("island", "hòn đảo", "ISLAND", "noun", "It is an island", "Đó là một hòn đảo", lessonId = 0, difficulty = 2, category = "travel"),
        WordEntity("jelly", "thạch", "JELLY", "noun", "Jelly is soft", "Thạch mềm", lessonId = 0, difficulty = 1, category = "food"),
        WordEntity("knife", "con dao", "KNIFE", "noun", "Use a knife", "Dùng dao", lessonId = 0, difficulty = 2, category = "home"),
        WordEntity("lemon", "quả chanh", "LEMON", "noun", "Lemon is sour", "Chanh chua", lessonId = 0, difficulty = 1, category = "food"),
        WordEntity("mount", "leo lên", "MOUNT", "verb", "Mount the horse", "Cưỡi lên ngựa", lessonId = 0, difficulty = 3, category = "travel"),
        WordEntity("night", "ban đêm", "NIGHT", "noun", "Good night", "Chúc ngủ ngon", lessonId = 0, difficulty = 1, category = "daily"),
        WordEntity("ocean", "đại dương", "OCEAN", "noun", "The ocean is deep", "Đại dương sâu", lessonId = 0, difficulty = 2, category = "nature"),
        WordEntity("piano", "đàn piano", "PIANO", "noun", "Play the piano", "Chơi piano", lessonId = 0, difficulty = 2, category = "hobby"),
        WordEntity("queen", "nữ hoàng", "QUEEN", "noun", "The queen smiles", "Nữ hoàng mỉm cười", lessonId = 0, difficulty = 2, category = "people"),
        WordEntity("river", "dòng sông", "RIVER", "noun", "Cross the river", "Băng qua sông", lessonId = 0, difficulty = 2, category = "nature"),
        WordEntity("smile", "nụ cười", "SMILE", "noun", "She has a smile", "Cô ấy có nụ cười", lessonId = 0, difficulty = 1, category = "feelings"),
        WordEntity("table", "cái bàn", "TABLE", "noun", "Put it on the table", "Đặt lên bàn", lessonId = 0, difficulty = 1, category = "home"),
        WordEntity("urban", "đô thị", "URBAN", "adjective", "Urban area", "Khu vực đô thị", lessonId = 0, difficulty = 3, category = "daily"),
        WordEntity("voice", "giọng nói", "VOICE", "noun", "Her voice is calm", "Giọng cô ấy dịu", lessonId = 0, difficulty = 2, category = "people"),
        WordEntity("water", "nước", "WATER", "noun", "Drink water", "Uống nước", lessonId = 0, difficulty = 1, category = "daily"),
        WordEntity("xenon", "khí xenon", "XENON", "noun", "Xenon is a gas", "Xenon là khí", lessonId = 0, difficulty = 3, category = "science"),
        WordEntity("youth", "tuổi trẻ", "YOUTH", "noun", "Enjoy your youth", "Hưởng tuổi trẻ", lessonId = 0, difficulty = 2, category = "people"),
        WordEntity("zebra", "ngựa vằn", "ZEBRA", "noun", "Zebra has stripes", "Ngựa vằn có sọc", lessonId = 0, difficulty = 1, category = "animals")
    )

    private fun isValidWord(word: String): Boolean = word.all { it.isLetter() }

    private fun defaultKeyboard(): Map<Char, LetterState> {
        val map = LinkedHashMap<Char, LetterState>()
        for (c in 'A'..'Z') {
            map[c] = LetterState.UNKNOWN
        }
        return map
    }

    private fun evaluateGuess(guess: String, target: String): List<LetterState> {
        val results = MutableList(guess.length) { LetterState.ABSENT }
        val counts = mutableMapOf<Char, Int>()

        target.forEach { c -> counts[c] = (counts[c] ?: 0) + 1 }

        for (i in guess.indices) {
            val c = guess[i]
            if (c == target[i]) {
                results[i] = LetterState.CORRECT
                counts[c] = (counts[c] ?: 0) - 1
            }
        }

        for (i in guess.indices) {
            val c = guess[i]
            if (results[i] == LetterState.CORRECT) continue
            val remaining = counts[c] ?: 0
            if (remaining > 0) {
                results[i] = LetterState.PRESENT
                counts[c] = remaining - 1
            } else {
                results[i] = LetterState.ABSENT
            }
        }

        return results
    }

    private fun updateKeyboard(
        keyboard: Map<Char, LetterState>,
        guess: String,
        results: List<LetterState>
    ): Map<Char, LetterState> {
        val updated = keyboard.toMutableMap()
        for (i in guess.indices) {
            val c = guess[i]
            val current = updated[c] ?: LetterState.UNKNOWN
            val next = results[i]
            updated[c] = pickHigherState(current, next)
        }
        return updated
    }

    private fun pickHigherState(current: LetterState, next: LetterState): LetterState {
        val order = mapOf(
            LetterState.UNKNOWN to 0,
            LetterState.ABSENT to 1,
            LetterState.PRESENT to 2,
            LetterState.CORRECT to 3
        )
        return if ((order[next] ?: 0) > (order[current] ?: 0)) next else current
    }

    private fun buildHint(level: Int, word: WordEntity?): HintEntry {
        if (word == null) return HintEntry(level, "Không có gợi ý")
        return when (level) {
            1 -> HintEntry(level, "Nghĩa: ${word.translation}")
            2 -> {
                val example = word.exampleSentence.takeIf { it.isNotBlank() }
                val masked = example?.replace(word.word, "_____", ignoreCase = true) ?: "Không có ví dụ"
                HintEntry(level, "Ví dụ: $masked")
            }
            3 -> {
                val hint = "Chữ cái: ${word.word.first().uppercaseChar()} _ _ ${word.word.last().uppercaseChar()}"
                HintEntry(level, hint)
            }
            4 -> {
                val pron = word.pronunciation.takeIf { it.isNotBlank() } ?: "Không có phát âm"
                HintEntry(level, "Phát âm: $pron")
            }
            else -> {
                val image = word.imageUrl
                if (image.isNullOrBlank()) {
                    HintEntry(level, "Không có hình ảnh")
                } else {
                    HintEntry(level, "Hình ảnh minh họa", imageUrl = image)
                }
            }
        }
    }

    private fun calculateScore(
        guessCount: Int,
        hintsUsed: Int,
        mode: WordleMode,
        currentStreak: Int
    ): Int {
        val base = 100
        val penalty = ((guessCount - 1).coerceAtLeast(0) * 10) + (hintsUsed * 20)
        val multiplier = if (mode == WordleMode.DAILY && currentStreak >= 1) 1.5 else 1.0
        val raw = (base - penalty).coerceAtLeast(0)
        return (raw * multiplier).toInt()
    }

    private fun buildDailySeed(difficulty: WordleDifficulty, topic: String?): Int {
        val day = currentEpochDay()
        return day * 31 + difficulty.ordinal * 7 + (topic?.hashCode() ?: 0)
    }

    private fun currentEpochDay(): Int {
        return LocalDate.now(ZoneId.systemDefault()).toEpochDay().toInt()
    }
}
