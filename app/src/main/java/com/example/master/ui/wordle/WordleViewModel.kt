package com.example.master.ui.wordle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.master.data.repository.LearningRepository
import com.example.master.data.local.entity.WordEntity
import com.example.master.network.ApiService
import com.example.master.network.toEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

enum class WordleMode { PRACTICE }

enum class LetterState { UNKNOWN, ABSENT, PRESENT, CORRECT }

enum class WordleStatus { IN_PROGRESS, WON, LOST }

data class GuessResult(
    val word: String,
    val results: List<LetterState>
)

data class WordleUiState(
    val mode: WordleMode = WordleMode.PRACTICE,
    val wordLength: Int = WORDLE_LENGTH,
    val maxGuesses: Int = 5,
    val currentGuess: String = "",
    val guesses: List<GuessResult> = emptyList(),
    val keyboard: Map<Char, LetterState> = emptyMap(),
    val status: WordleStatus = WordleStatus.IN_PROGRESS,
    val message: String? = null,
    val isValidating: Boolean = false,
    val solution: String? = null,
    val solutionTranslation: String? = null,
    val solutionExample: String? = null,
    val solutionPronunciation: String? = null,
    val hintText: String? = null
)

private const val WORDLE_LENGTH = 5

@HiltViewModel
class WordleViewModel @Inject constructor(
    private val repository: LearningRepository,
    private val api: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordleUiState())
    val uiState: StateFlow<WordleUiState> = _uiState.asStateFlow()

    private var targetWord: String = ""
    private var targetEntity: WordEntity? = null
    private var validWords: Set<String> = emptySet()
    private var allWords: List<WordEntity> = emptyList()
    private val httpClient = OkHttpClient()
    private val gson = Gson()

    init {
        viewModelScope.launch {
            repository.getAllWords().collect { words ->
                allWords = words
                if (allWords.isEmpty()) {
                    allWords = defaultWordBank()
                }
                if (targetWord.isBlank()) {
                    startNewGame(force = true)
                }
            }
        }

        viewModelScope.launch {
            fetchRemoteWords()
        }
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
        if (state.isValidating) return
        val guess = state.currentGuess.trim().uppercase()
        if (guess.length != state.wordLength) {
            _uiState.update { it.copy(message = "Chưa đủ ký tự") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isValidating = true, message = null) }
            val isEnglish = checkEnglishWord(guess.lowercase())
            if (!isEnglish) {
                _uiState.update { it.copy(message = "Từ không hợp lệ", isValidating = false) }
                return@launch
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
                    isValidating = false,
                    solution = if (nextStatus != WordleStatus.IN_PROGRESS) targetWord else null,
                    solutionTranslation = if (nextStatus != WordleStatus.IN_PROGRESS) targetEntity?.translation else null,
                    solutionExample = if (nextStatus != WordleStatus.IN_PROGRESS) targetEntity?.exampleSentence else null,
                    solutionPronunciation = if (nextStatus != WordleStatus.IN_PROGRESS) targetEntity?.pronunciation else null,
                    hintText = if (nextStatus == WordleStatus.IN_PROGRESS) {
                        buildLetterHint(newGuesses)
                    } else {
                        null
                    }
                )
            }
        }
    }

    fun restartGame() {
        startNewGame(force = true)
    }

    private fun startNewGame(force: Boolean = false) {
        if (allWords.isEmpty()) {
            _uiState.update { it.copy(message = "Không có dữ liệu Wordle, hãy thử lại khi có mạng.") }
            return
        }
        if (!force) {
            val state = _uiState.value
            val inProgress = state.status == WordleStatus.IN_PROGRESS
            val hasGuesses = state.guesses.isNotEmpty() || state.currentGuess.isNotEmpty()
            if (inProgress && hasGuesses) {
                return
            }
        }
        val state = _uiState.value

        val selected = selectTarget(allWords)
        if (selected == null) {
            _uiState.update { it.copy(message = "Không tìm thấy từ phù hợp") }
            return
        }

        targetEntity = selected
        targetWord = selected.word.uppercase()

        validWords = buildValidSet(allWords, WORDLE_LENGTH)

        _uiState.update {
            it.copy(
                wordLength = WORDLE_LENGTH,
                maxGuesses = 5,
                currentGuess = "",
                guesses = emptyList(),
                keyboard = defaultKeyboard(),
                status = WordleStatus.IN_PROGRESS,
                message = null,
                isValidating = false,
                solution = null,
                solutionTranslation = null,
                solutionExample = null,
                solutionPronunciation = null,
                hintText = null
            )
        }
    }

    private fun buildLetterHint(guesses: List<GuessResult>): String? {
        if (targetWord.isBlank()) return null
        val revealed = BooleanArray(targetWord.length)
        guesses.forEach { guess ->
            guess.results.forEachIndexed { index, state ->
                if (state == LetterState.CORRECT) {
                    revealed[index] = true
                }
            }
        }
        val hiddenIndex = revealed.indexOfFirst { !it }
        if (hiddenIndex == -1 || hiddenIndex >= targetWord.length) return null
        val letter = targetWord[hiddenIndex]
        return "Gợi ý: chữ cái ở vị trí ${hiddenIndex + 1} là '$letter'"
    }

    private fun selectTarget(
        words: List<WordEntity>
    ): WordEntity? {
        val filtered = words.filter { w ->
            isValidWord(w.word) && w.word.length == WORDLE_LENGTH
        }
        if (filtered.isEmpty()) return null
        return filtered.random()
    }

    private fun buildValidSet(words: List<WordEntity>, wordLength: Int): Set<String> {
        return words.filter { w ->
            isValidWord(w.word) && w.word.length == wordLength
        }
            .map { it.word.uppercase() }
            .toSet()
    }

    private suspend fun fetchRemoteWords() {
        val remoteList = fetchRandomWordsFromApi()
        if (remoteList.isNotEmpty()) {
            allWords = remoteList
            startNewGame()
            return
        }

        val lessons = runCatching { api.getLessons() }.getOrNull().orEmpty()
        if (lessons.isEmpty()) return

        val remoteWords = lessons.flatMap { lesson ->
            runCatching { api.getWordsByLesson(lesson.id) }.getOrDefault(emptyList())
        }

        if (remoteWords.isNotEmpty()) {
            val mapped = remoteWords.map { it.toEntity() }
            allWords = mapped
            startNewGame()
        } else if (allWords.isEmpty()) {
            allWords = defaultWordBank()
            startNewGame()
        }
    }

    private suspend fun fetchRandomWordsFromApi(): List<WordEntity> = withContext(Dispatchers.IO) {
        val collected = mutableListOf<String>()
        val word = fetchValidRandomWord(WORDLE_LENGTH, maxAttempts = 10)
        if (!word.isNullOrBlank()) {
            collected.add(word)
        }
        return@withContext collected
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { it.length == WORDLE_LENGTH }
            .filter { it.all { ch -> ch.isLetter() } }
            .distinct()
            .map {
                WordEntity(
                    word = it.lowercase(),
                    translation = "",
                    pronunciation = "",
                    partOfSpeech = "",
                    exampleSentence = "",
                    exampleTranslation = "",
                    lessonId = 0,
                    difficulty = 1,
                    category = ""
                )
            }
            .toList()
    }

    private suspend fun fetchValidRandomWord(length: Int, maxAttempts: Int): String? {
        repeat(maxAttempts) {
            val request = Request.Builder()
                .url("https://random-word-api.herokuapp.com/word?length=$length")
                .get()
                .build()
            val response = runCatching { httpClient.newCall(request).execute() }.getOrNull() ?: return@repeat
            val word = response.use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string().orEmpty()
                val type = object : TypeToken<List<String>>() {}.type
                val words = runCatching { gson.fromJson<List<String>>(body, type) }.getOrDefault(emptyList())
                words.firstOrNull()
            }?.trim()
            if (!word.isNullOrBlank() && checkEnglishWord(word.lowercase())) {
                return word
            }
        }
        return null
    }

    private fun defaultWordBank(): List<WordEntity> = listOf(
        WordEntity("apple", "quả táo", "/ˈæpəl/", "noun", "I eat an apple", "Tôi ăn một quả táo", lessonId = 0, difficulty = 1, category = "food"),
        WordEntity("brain", "bộ não", "/bɹˈeɪn/", "noun", "Use your brain", "Dùng trí não của bạn", lessonId = 0, difficulty = 2, category = "body"),
        WordEntity("chair", "cái ghế", "/tʃˈɛɹ/", "noun", "Sit on the chair", "Ngồi trên ghế", lessonId = 0, difficulty = 1, category = "home"),
        WordEntity("drive", "lái xe", "/dɹˈaɪv/", "verb", "I drive to work", "Tôi lái xe đi làm", lessonId = 0, difficulty = 2, category = "travel"),
        WordEntity("earth", "trái đất", "/ˈɝθ/", "noun", "The earth is round", "Trái đất hình cầu", lessonId = 0, difficulty = 2, category = "nature"),
        WordEntity("flame", "ngọn lửa", "/flˈeɪm/", "noun", "The flame is bright", "Ngọn lửa sáng", lessonId = 0, difficulty = 3, category = "safety"),
        WordEntity("grape", "quả nho", "/gɹˈeɪp/", "noun", "Grapes are sweet", "Nho thì ngọt", lessonId = 0, difficulty = 1, category = "food"),
        WordEntity("happy", "hạnh phúc", "/hˈæpi/", "adjective", "I am happy", "Tôi hạnh phúc", lessonId = 0, difficulty = 1, category = "feelings"),
        WordEntity("island", "hòn đảo", "/ˈaɪlənd/", "noun", "It is an island", "Đó là một hòn đảo", lessonId = 0, difficulty = 2, category = "travel"),
        WordEntity("jelly", "thạch", "/dʒˈɛli/", "noun", "Jelly is soft", "Thạch mềm", lessonId = 0, difficulty = 1, category = "food"),
        WordEntity("knife", "con dao", "/nˈaɪf/", "noun", "Use a knife", "Dùng dao", lessonId = 0, difficulty = 2, category = "home"),
        WordEntity("lemon", "quả chanh", "/lˈɛmən/", "noun", "Lemon is sour", "Chanh chua", lessonId = 0, difficulty = 1, category = "food"),
        WordEntity("mount", "leo lên", "/mˈaʊnt/", "verb", "Mount the horse", "Cưỡi lên ngựa", lessonId = 0, difficulty = 3, category = "travel"),
        WordEntity("night", "ban đêm", "/nˈaɪt/", "noun", "Good night", "Chúc ngủ ngon", lessonId = 0, difficulty = 1, category = "daily"),
        WordEntity("ocean", "đại dương", "/ˈoʊʃən/", "noun", "The ocean is deep", "Đại dương sâu", lessonId = 0, difficulty = 2, category = "nature"),
        WordEntity("piano", "đàn piano", "/piˈænoʊ/", "noun", "Play the piano", "Chơi piano", lessonId = 0, difficulty = 2, category = "hobby"),
        WordEntity("queen", "nữ hoàng", "/kwˈin/", "noun", "The queen smiles", "Nữ hoàng mỉm cười", lessonId = 0, difficulty = 2, category = "people"),
        WordEntity("river", "dòng sông", "/ɹˈɪvɚ/", "noun", "Cross the river", "Băng qua sông", lessonId = 0, difficulty = 2, category = "nature"),
        WordEntity("smile", "nụ cười", "/smˈaɪl/", "noun", "She has a smile", "Cô ấy có nụ cười", lessonId = 0, difficulty = 1, category = "feelings"),
        WordEntity("table", "cái bàn", "/tˈeɪbəl/", "noun", "Put it on the table", "Đặt lên bàn", lessonId = 0, difficulty = 1, category = "home"),
        WordEntity("urban", "đô thị", "/ˈɝbən/", "adjective", "Urban area", "Khu vực đô thị", lessonId = 0, difficulty = 3, category = "daily"),
        WordEntity("voice", "giọng nói", "/vˈɔɪs/", "noun", "Her voice is calm", "Giọng cô ấy dịu", lessonId = 0, difficulty = 2, category = "people"),
        WordEntity("water", "nước", "/wˈɔtɚ/", "noun", "Drink water", "Uống nước", lessonId = 0, difficulty = 1, category = "daily"),
        WordEntity("xenon", "khí xenon", "/zˈinɑn/", "noun", "Xenon is a gas", "Xenon là khí", lessonId = 0, difficulty = 3, category = "science"),
        WordEntity("youth", "tuổi trẻ", "/jˈuθ/", "noun", "Enjoy your youth", "Hưởng tuổi trẻ", lessonId = 0, difficulty = 2, category = "people"),
        WordEntity("zebra", "ngựa vằn", "/zˈibɹə/", "noun", "Zebra has stripes", "Ngựa vằn có sọc", lessonId = 0, difficulty = 1, category = "animals")
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

    private suspend fun checkEnglishWord(word: String): Boolean = withContext(Dispatchers.IO) {
        if (word.length != WORDLE_LENGTH) return@withContext false
        val request = Request.Builder()
        val request = Request.Builder()
            .url("https://api.dictionaryapi.dev/api/v2/entries/en/$word")
            .get()
            .build()
        val response = runCatching { httpClient.newCall(request).execute() }.getOrNull() ?: return@withContext validWords.contains(word.uppercase())
        response.use { resp ->
            if (resp.isSuccessful) return@withContext true
            if (resp.code == 404) return@withContext false
            return@withContext validWords.contains(word.uppercase())
        }
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

}
