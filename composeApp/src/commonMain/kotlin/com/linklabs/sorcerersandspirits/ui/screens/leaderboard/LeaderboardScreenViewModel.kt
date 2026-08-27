package com.linklabs.sorcerersandspirits.ui.screens.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.leaderboard.LeaderboardEntry
import com.linklabs.sorcerersandspirits.domain.leaderboard.usecase.GetLeaderboardUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class LeaderboardScreenViewModel(private val getLeaderboardUseCase: GetLeaderboardUseCase) :
    ViewModel() {

    private val _uiStateFlow = MutableStateFlow<UiState<LeaderboardState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private var fetchedEntries: List<LeaderboardEntry> = emptyList()
    private var selectedSort: LeaderboardSort = LeaderboardSort.GauntletsWon

    init {
        loadLeaderboard()
    }

    fun onRetry() {
        loadLeaderboard()
    }

    /** Re-orders the already-fetched rows client-side; no network read. */
    fun onSortSelected(sort: LeaderboardSort) {
        if (sort == selectedSort) return

        selectedSort = sort
        if (_uiStateFlow.value is UiState.Loaded) {
            _uiStateFlow.value = UiState.Loaded(data = buildState())
        }
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            _uiStateFlow.value = UiState.Loading

            val fetched = runCatching { getLeaderboardUseCase() }.getOrNull()

            _uiStateFlow.value =
                if (fetched != null) {
                    fetchedEntries = fetched
                    UiState.Loaded(data = buildState())
                } else {
                    UiState.Error
                }
        }
    }

    private fun buildState(): LeaderboardState = LeaderboardState(
        entries = fetchedEntries.toRowStates(sort = selectedSort),
        sort = selectedSort,
    )
}
