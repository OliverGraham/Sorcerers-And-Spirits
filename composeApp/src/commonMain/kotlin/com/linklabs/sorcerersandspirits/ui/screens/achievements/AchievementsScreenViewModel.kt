package com.linklabs.sorcerersandspirits.ui.screens.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetProfileFlowUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class AchievementsScreenViewModel(private val getProfileFlowUseCase: GetProfileFlowUseCase) :
    ViewModel() {

    private val _uiStateFlow = MutableStateFlow<UiState<AchievementsState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    init {
        collectProfile()
    }

    private fun collectProfile() {
        viewModelScope.launch {
            getProfileFlowUseCase()
                .filterNotNull()
                .catch { _uiStateFlow.value = UiState.Error }
                .collect { profile ->
                    _uiStateFlow.value = UiState.Loaded(data = profile.toAchievementsState())
                }
        }
    }
}
