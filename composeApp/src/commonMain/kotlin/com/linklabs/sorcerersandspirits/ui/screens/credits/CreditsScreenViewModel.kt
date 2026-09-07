package com.linklabs.sorcerersandspirits.ui.screens.credits

import androidx.lifecycle.ViewModel
import com.linklabs.sorcerersandspirits.domain.credits.usecase.GetCreditsUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class CreditsScreenViewModel(private val getCreditsUseCase: GetCreditsUseCase) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow<UiState<CreditsState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    init {
        loadCredits()
    }

    fun retry() {
        _uiStateFlow.value = UiState.Loading
        loadCredits()
    }

    private fun loadCredits() {
        _uiStateFlow.value = UiState.Loaded(data = getCreditsUseCase().toState())
    }
}
