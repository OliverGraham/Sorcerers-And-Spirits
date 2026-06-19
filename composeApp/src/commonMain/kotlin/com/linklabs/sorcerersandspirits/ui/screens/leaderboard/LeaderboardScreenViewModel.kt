package com.linklabs.sorcerersandspirits.ui.screens.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    init {
        loadLeaderboard()
    }

    fun onRetry() {
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            _uiStateFlow.value = UiState.Loading

            val result = runCatching { getLeaderboardUseCase() }

            val loadedEntries = result.getOrNull()
            _uiStateFlow.value =
                if (loadedEntries != null) {
                    UiState.Loaded(
                        data = LeaderboardState(entries = loadedEntries.map { it.toRowState() })
                    )
                } else {
                    UiState.Error
                }
        }
    }
}
