package com.example.master.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.master.auth.AuthManager
import com.example.master.data.local.ChallengeStatus
import com.example.master.data.local.GameStateStore
import com.example.master.data.repository.LearningRepository
import com.example.master.network.ApiService
import com.example.master.network.UpdateBoosterRequest
import com.example.master.network.UpdateDailyRequest
import com.example.master.network.UpdateQuestRequest
import com.example.master.ui.home.BoosterItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoreUiState(
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val coins: Int = 0,
    val boosters: List<BoosterItem> = emptyList(),
    val quests: List<QuestUi> = emptyList(),
    val dailyChallenge: DailyChallengeUi = DailyChallengeUi(),
    val message: String? = null
)

data class QuestUi(
    val title: String,
    val description: String,
    val rewardCoins: Int,
    val progress: Float,
    val stepsLabel: String,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false
)

data class DailyChallengeUi(
    val title: String = "Th? thách h?ng ngày",
    val rewardCoins: Int = 120,
    val status: ChallengeStatus = ChallengeStatus.READY,
    val progress: Int = 0,
    val target: Int = 5
)

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val repository: LearningRepository,
    private val authManager: AuthManager,
    private val gameStateStore: GameStateStore,
    private val api: ApiService
) : ViewModel() {

    private val pendingActions = mutableListOf<suspend () -> Unit>()
    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    private val baseBoosters = listOf(
        BoosterItem(
            title = "Hint t? v?ng",
            description = "Hi?n th? g?i ý nghia ti?ng Vi?t cho 1 câu h?i",
            costCoins = 30,
            isOwned = true
        ),
        BoosterItem(
            title = "Double XP",
            description = "Nh?n g?p dôi di?m cho level k? ti?p",
            costCoins = 120,
            isOwned = false
        ),
        BoosterItem(
            title = "Skip câu",
            description = "B? qua 1 câu h?i khó",
            costCoins = 60,
            isOwned = false
        )
    )

    private val baseQuests = listOf(
        QuestUi(
            title = "An li?n 15 ti?ng t?c quái",
            description = "Hoàn thành 3 level d? khó Medium",
            rewardCoins = 80,
            progress = 0.6f,
            stepsLabel = "3/5",
            isCompleted = false
        ),
        QuestUi(
            title = "Làm th? thách t? v?ng hôm nay",
            description = "Ð?t di?m t?i thi?u 40/50",
            rewardCoins = 120,
            progress = 1f,
            stepsLabel = "4/4",
            isCompleted = true
        ),
        QuestUi(
            title = "Chia s? streak",
            description = "Chia s? k?t qu? streak lên m?ng xã h?i",
            rewardCoins = 40,
            progress = 1f,
            stepsLabel = "1/1",
            isCompleted = true
        )
    )

    init {
        observeUserCoins()
        observeState()
        syncFromRemote()
    }

    private fun observeUserCoins() {
        viewModelScope.launch {
            val userId = authManager.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, message = "Chua dang nh?p") }
                return@launch
            }
            repository.getUserProfile(userId).collect { profile ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        coins = profile?.coins ?: 0,
                        message = null
                    )
                }
            }
        }
    }

    private fun observeState() {
        viewModelScope.launch {
            combine(
                gameStateStore.ownedBoosters,
                gameStateStore.claimedQuests,
                gameStateStore.dailyState
            ) { owned, claimed, daily ->
                Triple(owned, claimed, daily)
            }.collect { (owned, claimed, daily) ->
                val boosters = baseBoosters.map { b ->
                    b.copy(isOwned = owned.contains(b.title) || b.isOwned)
                }
                val quests = baseQuests.map { q ->
                    q.copy(isClaimed = claimed.contains(q.title))
                }
                val dcState = _uiState.value.dailyChallenge.copy(
                    status = runCatching { ChallengeStatus.valueOf(daily.status) }.getOrDefault(ChallengeStatus.READY),
                    progress = daily.progress
                )
                _uiState.update {
                    it.copy(
                        boosters = boosters,
                        quests = quests,
                        dailyChallenge = dcState
                    )
                }
            }
        }
    }

    fun refreshFromRemote() {
        syncFromRemote()
        flushPending()
    }

    private fun syncFromRemote() {
        viewModelScope.launch {
            val userId = authManager.getCurrentUserId() ?: return@launch
            runCatching { api.getGameState(userId) }.onSuccess { resp ->
                gameStateStore.setDailyStatus(
                    runCatching { ChallengeStatus.valueOf(resp.daily?.status ?: "READY") }.getOrDefault(ChallengeStatus.READY),
                    resp.daily?.progress ?: 0
                )
                resp.boosters.forEach { if (it.isOwned) gameStateStore.setBoosterOwned(it.boosterKey) }
                resp.quests.forEach { if (it.isClaimed) gameStateStore.setQuestClaimed(it.questKey) }
                _uiState.update { it.copy(message = null) }
            }.onFailure {
                _uiState.update { it.copy(message = "Không d?ng b? du?c state backend (offline?)") }
            }
        }
    }

    private fun flushPending() {
        if (pendingActions.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val iterator = pendingActions.iterator()
            while (iterator.hasNext()) {
                val action = iterator.next()
                runCatching { action() }
                    .onSuccess { iterator.remove() }
                    .onFailure {
                        // gi? l?i action d? retry l?n sau
                    }
            }
            _uiState.update { it.copy(isSyncing = false) }
        }
    }

    fun purchaseBooster(title: String) {
        val userId = authManager.getCurrentUserId() ?: run {
            _uiState.update { it.copy(message = "Chua dang nh?p") }
            return
        }
        val state = _uiState.value
        val booster = state.boosters.find { it.title == title } ?: return
        if (booster.isOwned) {
            _uiState.update { it.copy(message = "B?n dã s? h?u ${booster.title}") }
            return
        }
        if (state.coins < booster.costCoins) {
            _uiState.update { it.copy(message = "Không d? coins") }
            return
        }

        viewModelScope.launch {
            repository.addCoins(userId, -booster.costCoins)
            gameStateStore.setBoosterOwned(title)
            val action: suspend () -> Unit = { api.updateBooster(userId, UpdateBoosterRequest(title, true)) }
            runCatching { action() }.onFailure { pendingActions.add(action) }
            _uiState.update {
                it.copy(
                    boosters = it.boosters.map { b ->
                        if (b.title == title) b.copy(isOwned = true) else b
                    },
                    message = "Ðã mua ${booster.title}"
                )
            }
        }
    }

    fun claimQuest(title: String) {
        val userId = authManager.getCurrentUserId() ?: run {
            _uiState.update { it.copy(message = "Chua dang nh?p") }
            return
        }
        val quest = _uiState.value.quests.find { it.title == title } ?: return
        if (!quest.isCompleted || quest.isClaimed) {
            _uiState.update { it.copy(message = "Quest chua hoàn thành ho?c dã nh?n thu?ng") }
            return
        }
        viewModelScope.launch {
            repository.addCoins(userId, quest.rewardCoins)
            gameStateStore.setQuestClaimed(title)
            val action: suspend () -> Unit = { api.updateQuest(userId, UpdateQuestRequest(title, true)) }
            runCatching { action() }.onFailure { pendingActions.add(action) }
            _uiState.update {
                it.copy(
                    quests = it.quests.map { q ->
                        if (q.title == title) q.copy(isClaimed = true) else q
                    },
                    message = "Nh?n ${quest.rewardCoins} coins t? quest"
                )
            }
        }
    }

    fun startDailyChallenge() {
        val target = _uiState.value.dailyChallenge.target
        _uiState.update {
            it.copy(
                dailyChallenge = it.dailyChallenge.copy(status = ChallengeStatus.IN_PROGRESS, progress = 0),
                message = null
            )
        }
        viewModelScope.launch {
            gameStateStore.setDailyStatus(ChallengeStatus.IN_PROGRESS, 0)
            authManager.getCurrentUserId()?.let { id ->
                val action: suspend () -> Unit = {
                    api.updateDaily(id, UpdateDailyRequest(status = "IN_PROGRESS", progress = 0, target = target))
                }
                runCatching { action() }.onFailure { pendingActions.add(action) }
            }
        }
    }

    fun submitDailyChallenge(score: Int) {
        val userId = authManager.getCurrentUserId() ?: run {
            _uiState.update { it.copy(message = "Chua dang nh?p") }
            return
        }
        val dc = _uiState.value.dailyChallenge
        if (dc.status != ChallengeStatus.IN_PROGRESS) {
            _uiState.update { it.copy(message = "Chua b?t d?u th? thách") }
            return
        }
        viewModelScope.launch {
            repository.addCoins(userId, dc.rewardCoins)
            gameStateStore.setDailyStatus(ChallengeStatus.CLAIMED, dc.target)
            val action: suspend () -> Unit = {
                api.updateDaily(userId, UpdateDailyRequest(status = "CLAIMED", progress = dc.target, target = dc.target))
            }
            runCatching { action() }.onFailure { pendingActions.add(action) }
            _uiState.update {
                it.copy(
                    dailyChallenge = dc.copy(status = ChallengeStatus.CLAIMED, progress = dc.target),
                    message = "Nh?n ${dc.rewardCoins} coins t? th? thách"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
