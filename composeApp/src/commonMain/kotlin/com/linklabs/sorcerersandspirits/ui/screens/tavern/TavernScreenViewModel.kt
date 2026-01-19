package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.EnterTavernUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetTavernFlowUseCase
import com.linklabs.sorcerersandspirits.domain.traveler.UpdateTravelerDisplayNameUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TavernScreenViewModel(
    private val enterTavernUseCase: EnterTavernUseCase,
    private val updateTravelerDisplayNameUseCase: UpdateTravelerDisplayNameUseCase,
    getTavernFlowUseCase: GetTavernFlowUseCase,
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<TavernEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val uiStateFlow = getTavernFlowUseCase()
        .map { tavern -> UiState.Loaded(data = tavern) }
        .catch { UiState.Error }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    init {
        enterTavern()
    }

    fun onActionButtonClick() {
        viewModelScope.launch {
            updateTravelerDisplayNameUseCase(
                displayName = "A Traveler with an updated name",
            )
        }
    }

    fun enterTavern() {
        viewModelScope.launch {
            enterTavernUseCase()
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
