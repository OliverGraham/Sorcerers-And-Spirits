package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetTavernDataFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendChallengeUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SetCurrentTravelerAvailableUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateChallengerStatusUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import com.linklabs.sorcerersandspirits.ui.uistate.updateLoaded
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class TavernScreenViewModel(
    private val getTavernDataFlowUseCase: GetTavernDataFlowUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val sendChallengeUseCase: SendChallengeUseCase,
    private val setCurrentTravelerAvailableUseCase: SetCurrentTravelerAvailableUseCase,
    private val updateChallengerStatusUseCase: UpdateChallengerStatusUseCase,
) : ViewModel() {

    private val _eventChannelFlow = Channel<TavernEvent>()
    val eventChannelFlow = _eventChannelFlow.receiveAsFlow()

    private val _uiStateFlow = MutableStateFlow<UiState<TavernState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val challengeHandler =
        ChallengeHandler(
            coroutineScope = viewModelScope,
            sendChallengeUseCase = sendChallengeUseCase,
            updateChallengerStatusUseCase = updateChallengerStatusUseCase,
            setCurrentTravelerAvailableUseCase = setCurrentTravelerAvailableUseCase,
            getCurrentTavernState = { (_uiStateFlow.value as? UiState.Loaded)?.data },
            updateTavernState = { transform -> _uiStateFlow.updateLoaded(transform = transform) },
            onEvent = { event -> viewModelScope.launch { _eventChannelFlow.send(element = event) } },
        )

    init {
        viewModelScope.launch { collectScreenState() }
    }

    fun onActionButtonClick() {}

    fun showChallengeDialog() {
        challengeHandler.updateDialogVisibility(isVisible = true)
    }

    fun hideChallengeDialog() {
        challengeHandler.updateDialogVisibility(isVisible = false)
    }

    fun onSendChallenge(userTraveler: TravelerState, selectedTravelerStates: Set<TravelerState>) {
        viewModelScope.launch {
            challengeHandler.sendChallenges(
                userTraveler = userTraveler,
                selectedTravelerStates = selectedTravelerStates,
            )
        }
    }

    fun onAcceptChallenge(challengeState: ChallengeState) {
        viewModelScope.launch {
            val userId = getUserIdUseCase() ?: return@launch

            challengeHandler.acceptChallenge(challengeState = challengeState, userId = userId)
        }
    }

    fun onDeclineChallenge(challengeState: ChallengeState) {
        viewModelScope.launch {
            val userId = getUserIdUseCase() ?: return@launch

            challengeHandler.declineChallenge(challengeState = challengeState, userId = userId)
        }
    }

    private suspend fun collectScreenState() {
        getTavernDataFlowUseCase().filterNotNull().collect { data ->
            val tavernState =
                data.travelers.toTavernState(userId = data.profile.userId) ?: return@collect

            val result =
                challengeHandler.handleChallengeUpdate(
                    previousChallengeState = tavernState.challenge,
                    updatedChallenge = data.challenge,
                    userId = data.profile.userId,
                )

            val updatedTavernState =
                if (result != null) {
                    if (result.tavernEvent != null) {
                        _eventChannelFlow.send(element = result.tavernEvent)
                    }
                    tavernState.copy(challenge = result.challengeState)
                } else {
                    tavernState
                }

            _uiStateFlow.value = UiState.Loaded(data = updatedTavernState)
        }
    }
}
