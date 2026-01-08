package com.example.master.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.master.auth.AuthManager
import com.example.master.core.network.NetworkMonitor
import com.example.master.data.local.GameStateStore
import com.example.master.data.local.PendingShopAction
import com.example.master.data.local.ShopSyncStore
import com.example.master.data.repository.LearningRepository
import com.example.master.network.ApiService
import com.example.master.network.UpdateBoosterRequest
import com.example.master.network.UpdateQuestRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
    val boosters: List<BoosterUi> = emptyList(),
    val quests: List<QuestUi> = emptyList(),
    val message: String? = null
)

data class BoosterUi(
    val key: String,
    val title: String,
    val description: String,
    val costCoins: Int,
    val isOwned: Boolean
)

data class QuestUi(
    val key: String,
    val title: String,
    val description: String,
    val rewardCoins: Int,
    val progress: Float,
    val stepsLabel: String,
    val type: String = "DAILY",
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false
)

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val repository: LearningRepository,
    private val authManager: AuthManager,
    private val gameStateStore: GameStateStore,
    private val api: ApiService,
    private val shopSyncStore: ShopSyncStore,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    private val baseBoosters = listOf(
        BoosterUi(
            key = "hint_vocab",
            title = "Hint từ vựng",
            description = "Hiện gợi ý nghĩa tiếng Việt cho 1 câu hỏi",
            costCoins = 30,
            isOwned = true
        ),
        BoosterUi(
            key = "xp_boost_2x",
            title = "XP Boost 2x",
            description = "Nhân đôi XP cho bài học kế tiếp",
            costCoins = 120,
            isOwned = false
        ),
        BoosterUi(
            key = "skip_question",
            title = "Bỏ qua câu",
            description = "Bỏ qua 1 câu hỏi khó",
            costCoins = 60,
            isOwned = false
        ),
        BoosterUi(
            key = "streak_freeze",
            title = "Streak Freeze",
            description = "Bảo vệ streak nếu bỏ lỡ 1 ngày",
            costCoins = 150,
            isOwned = false
        )
    )

    private val boosterKeyAliases = mapOf(
        "hint từ vựng" to "hint_vocab",
        "hint tu vung" to "hint_vocab",
        "xp boost 2x" to "xp_boost_2x",
        "bỏ qua câu" to "skip_question",
        "bo qua cau" to "skip_question",
        "streak freeze" to "streak_freeze"
    )

    private val baseQuests = listOf(
        QuestUi(
            key = "daily_goal",
            title = "Mục tiêu hôm nay",
            description = "Hoàn thành 1 bài học hôm nay",
            rewardCoins = 50,
            progress = 0.4f,
            stepsLabel = "2/5",
            type = "DAILY",
            isCompleted = false
        ),
        QuestUi(
            key = "flashcard_focus",
            title = "Ôn flashcard",
            description = "Đạt điểm tối thiểu 40/50",
            rewardCoins = 120,
            progress = 1f,
            stepsLabel = "4/4",
            type = "DAILY",
            isCompleted = true
        ),
        QuestUi(
            key = "weekly_streaker",
            title = "Streak tuần",
            description = "Duy trì streak 5 ngày trong tuần",
            rewardCoins = 200,
            progress = 0.6f,
            stepsLabel = "3/5",
            type = "WEEKLY",
            isCompleted = false
        )
    )

    private val questKeyAliases = mapOf(
        "mục tiêu hôm nay" to "daily_goal",
        "daily goal" to "daily_goal",
        "ôn flashcard" to "flashcard_focus",
        "on flashcard" to "flashcard_focus",
        "streak tuần" to "weekly_streaker",
        "weekly streaker" to "weekly_streaker"
    )

    init {
        observeUserCoins()
        observeState()
        syncFromRemote()
        flushPendingShopActions()
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
            combine(gameStateStore.ownedBoosters, gameStateStore.claimedQuests) { owned, claimed ->
                val normalizedOwned = owned.mapTo(mutableSetOf()) { normalizeBoosterKey(it) }
                val normalizedClaimed = claimed.mapTo(mutableSetOf()) { normalizeQuestKey(it) }
                val boosters = baseBoosters.map { b ->
                    b.copy(isOwned = normalizedOwned.contains(b.key) || b.isOwned)
                }
                val quests = baseQuests.map { q ->
                    q.copy(isClaimed = normalizedClaimed.contains(q.key))
                }
                _uiState.update {
                    it.copy(
                        boosters = boosters,
                        quests = quests
                    )
                }
            }.collect { }
        }
    }

    fun refreshFromRemote() {
        syncFromRemote()
        flushPendingShopActions()
    }

    private fun syncFromRemote() {
        viewModelScope.launch {
            val userId = authManager.getCurrentUserId() ?: return@launch
            if (!networkMonitor.isConnectedNow()) return@launch

            runCatching { api.getGameState(userId) }.onSuccess { resp ->
                resp.boosters.forEach {
                    if (it.isOwned) gameStateStore.setBoosterOwned(normalizeBoosterKey(it.boosterKey))
                }
                resp.quests.forEach {
                    if (it.isClaimed) gameStateStore.setQuestClaimed(normalizeQuestKey(it.questKey))
                }
                _uiState.update { it.copy(message = null) }
            }.onFailure {
                if (networkMonitor.isConnectedNow()) {
                    _uiState.update { it.copy(message = "Không đồng bộ được state backend") }
                }
            }
        }
    }

    private fun flushPendingShopActions() {
        viewModelScope.launch {
            if (!networkMonitor.isConnectedNow()) return@launch
            val queue = shopSyncStore.getQueue().toMutableList()
            if (queue.isEmpty()) return@launch

            _uiState.update { it.copy(isSyncing = true) }
            val remaining = mutableListOf<PendingShopAction>()

            queue.forEach { action ->
                val result = when (action.type) {
                    "BOOSTER" -> runCatching {
                        api.updateBooster(action.userId, UpdateBoosterRequest(action.key, action.value))
                    }
                    "QUEST" -> runCatching {
                        api.updateQuest(action.userId, UpdateQuestRequest(action.key, action.value))
                    }
                    else -> runCatching { Unit }
                }
                if (result.isFailure) {
                    remaining.add(action)
                }
            }

            shopSyncStore.saveQueue(remaining)
            _uiState.update { it.copy(isSyncing = false) }
        }
    }

    fun purchaseBooster(boosterKey: String) {
        val userId = authManager.getCurrentUserId() ?: run {
            _uiState.update { it.copy(message = "Chưa đăng nhập") }
            return
        }
        val state = _uiState.value
        val booster = state.boosters.find { it.key == boosterKey } ?: return

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
            gameStateStore.setBoosterOwned(booster.key)
            if (networkMonitor.isConnectedNow()) {
                runCatching {
                    api.updateBooster(userId, UpdateBoosterRequest(booster.key, true))
                }.onFailure {
                    shopSyncStore.enqueue(PendingShopAction(userId, "BOOSTER", booster.key, true))
                }
            } else {
                shopSyncStore.enqueue(PendingShopAction(userId, "BOOSTER", booster.key, true))
            }
            _uiState.update {
                it.copy(
                    boosters = it.boosters.map { b ->
                        if (b.key == booster.key) b.copy(isOwned = true) else b
                    },
                    message = "Đã mua ${booster.title}"
                )
            }
        }
    }

    fun claimQuest(questKey: String) {
        val userId = authManager.getCurrentUserId() ?: run {
            _uiState.update { it.copy(message = "Chưa đăng nhập") }
            return
        }
        val quest = _uiState.value.quests.find { it.key == questKey } ?: return

        if (!quest.isCompleted || quest.isClaimed) {
            _uiState.update { it.copy(message = "Quest chưa hoàn thành hoặc đã nhận thưởng") }
            return
        }

        viewModelScope.launch {
            repository.addCoins(userId, quest.rewardCoins)
            gameStateStore.setQuestClaimed(quest.key)
            if (networkMonitor.isConnectedNow()) {
                runCatching { api.updateQuest(userId, UpdateQuestRequest(quest.key, true)) }
                    .onFailure {
                        shopSyncStore.enqueue(PendingShopAction(userId, "QUEST", quest.key, true))
                    }
            } else {
                shopSyncStore.enqueue(PendingShopAction(userId, "QUEST", quest.key, true))
            }
            _uiState.update {
                it.copy(
                    quests = it.quests.map { q ->
                        if (q.key == quest.key) q.copy(isClaimed = true) else q
                    },
                    message = "Nhận ${quest.rewardCoins} coins từ quest"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun normalizeBoosterKey(rawKey: String): String {
        val key = rawKey.trim().lowercase()
        return boosterKeyAliases[key] ?: key
    }

    private fun normalizeQuestKey(rawKey: String): String {
        val key = rawKey.trim().lowercase()
        return questKeyAliases[key] ?: key
    }
}
