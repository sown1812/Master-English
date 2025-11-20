package com.example.master.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val title: String = "Thử thách hằng ngày",
    val rewardCoins: Int = 120,
    val status: ChallengeStatus = ChallengeStatus.READY,
    val progress: Int = 0,
    val target: Int = 5
)

class StoreViewModel(
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
            title = "Hint từ vựng",
            description = "Hiển thị gợi ý nghĩa tiếng Việt cho 1 câu hỏi",
            costCoins = 30,
            isOwned = true
        ),
        BoosterItem(
            title = "Double XP",
            description = "Nhận gấp đôi điểm cho level kế tiếp",
            costCoins = 120,
            isOwned = false
        ),
        BoosterItem(
            title = "Skip câu",
            description = "Bỏ qua 1 câu hỏi khó",
            costCoins = 60,
            isOwned = false
        )
    )

    private val baseQuests = listOf(
        QuestUi(
            title = "Ăn liền 15 tiếng tắc quái",
            description = "Hoàn thành 3 level độ khó Medium",
            rewardCoins = 80,
            progress = 0.6f,
            stepsLabel = "3/5",
            isCompleted = false
        ),
        QuestUi(
            title = "Làm thử thách từ vựng hôm nay",
            description = "Đạt điểm tối thiểu 40/50",
            rewardCoins = 120,
            progress = 1f,
            stepsLabel = "4/4",
            isCompleted = true
        ),
        QuestUi(
            title = "Chia sẻ streak",
            description = "Chia sẻ kết quả streak lên mạng xã hội",
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
                _uiState.update { it.copy(isLoading = false, message = "Chưa đăng nhập") }
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
                _uiState.update { it.copy(message = "Không đồng bộ được state backend (offline?)") }
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
                        // giữ lại action để retry lần sau
                    }
            }
            _uiState.update { it.copy(isSyncing = false) }
        }
    }

    fun purchaseBooster(title: String) {
        val userId = authManager.getCurrentUserId() ?: run {
            _uiState.update { it.copy(message = "Chưa đăng nhập") }
            return
        }
        val state = _uiState.value
        val booster = state.boosters.find { it.title == title } ?: return
        if (booster.isOwned) {
            _uiState.update { it.copy(message = "Bạn đã sở hữu ${booster.title}") }
            return
        }
        if (state.coins < booster.costCoins) {
            _uiState.update { it.copy(message = "Không đủ coins") }
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
                    message = "Đã mua ${booster.title}"
                )
            }
        }
    }

    fun claimQuest(title: String) {
        val userId = authManager.getCurrentUserId() ?: run {
            _uiState.update { it.copy(message = "Chưa đăng nhập") }
            return
        }
        val quest = _uiState.value.quests.find { it.title == title } ?: return
        if (!quest.isCompleted || quest.isClaimed) {
            _uiState.update { it.copy(message = "Quest chưa hoàn thành hoặc đã nhận thưởng") }
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
                    message = "Nhận ${quest.rewardCoins} coins từ quest"
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
            _uiState.update { it.copy(message = "Chưa đăng nhập") }
            return
        }
        val dc = _uiState.value.dailyChallenge
        if (dc.status != ChallengeStatus.IN_PROGRESS) {
            _uiState.update { it.copy(message = "Chưa bắt đầu thử thách") }
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
                    message = "Nhận ${dc.rewardCoins} coins từ thử thách"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

class StoreViewModelFactory(
    private val repository: LearningRepository,
    private val authManager: AuthManager,
    private val gameStateStore: GameStateStore,
    private val api: ApiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoreViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StoreViewModel(repository, authManager, gameStateStore, api) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
