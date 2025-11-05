package com.example.master.ui.home

sealed class HomeNavigationEvent {
    data class NavigateToPlay(val level: Int) : HomeNavigationEvent()
    data class NavigateToDailyChallenge(val challengeTitle: String) : HomeNavigationEvent()
    object NavigateToAchievements : HomeNavigationEvent()
    object NavigateToStore : HomeNavigationEvent()
    data class NavigateToQuest(val quest: Quest) : HomeNavigationEvent()
    data class NavigateToBooster(val booster: BoosterItem) : HomeNavigationEvent()
    data class ThemeApplied(val themeName: String) : HomeNavigationEvent()
    data class ShowMessage(val message: String) : HomeNavigationEvent()
}
