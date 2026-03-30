package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.EnterTavernUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetActiveChallengeFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetTavernTravelersFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendChallengeUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SetCurrentTravelerAvailableUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateChallengerStatusUseCase
import com.linklabs.sorcerersandspirits.domain.traveler.UpdateTravelerDisplayNameUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import com.linklabs.sorcerersandspirits.ui.uistate.updateLoaded
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TavernScreenViewModel(
    private val enterTavernUseCase: EnterTavernUseCase,
    private val updateTravelerDisplayNameUseCase: UpdateTravelerDisplayNameUseCase,
    private val sendChallengeUseCase: SendChallengeUseCase,
    private val getActiveChallengeFlowUseCase: GetActiveChallengeFlowUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val getTavernTravelersFlowUseCase: GetTavernTravelersFlowUseCase,
    private val updateChallengerStatusUseCase: UpdateChallengerStatusUseCase,
    private val setCurrentTravelerAvailableUseCase: SetCurrentTravelerAvailableUseCase,
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
        viewModelScope.launch { enterTavern() }
        viewModelScope.launch { collectScreenState() }
    }

    fun onActionButtonClick() {
        viewModelScope.launch {
            updateTravelerDisplayNameUseCase(displayName = "A Traveler with an updated name")
        }
    }

    fun showChallengeDialog() {
        challengeHandler.updateDialogVisibility(isVisible = true)
    }

    fun hideChallengeDialog() {
        challengeHandler.updateDialogVisibility(isVisible = false)
    }

    fun onSendChallenge(userTraveler: TravelerState, selectedTravelerStates: Set<TravelerState>) {
        challengeHandler.sendChallenges(
            userTraveler = userTraveler,
            selectedTravelerStates = selectedTravelerStates,
        )
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

    private suspend fun enterTavern() {
        runCatching { enterTavernUseCase() }.onFailure { _uiStateFlow.value = UiState.Error }
    }

    private suspend fun collectScreenState() {
        val userId = getUserIdUseCase()
        if (userId == null) {
            _uiStateFlow.value = UiState.Error
            return
        }

        // TODO: Fix
        combine(flow = getTavernTravelersFlowUseCase(), flow2 = getActiveChallengeFlowUseCase()) {
                tavernTravelers,
                challenge ->
                tavernTravelers to challenge
            }
            .catch {
                challengeHandler.clearChallenge()
                _uiStateFlow.value = UiState.Error
            }
            .collect { (tavernTravelers, challenge) ->
                val newTavernState = tavernTravelers.toTavernState(userId = userId)

                if (newTavernState == null) {
                    _uiStateFlow.value = UiState.Loading
                    return@collect
                }

                _uiStateFlow.update { currentUiState ->
                    UiState.Loaded(
                        data =
                            newTavernState.copy(challenge = currentUiState.currentChallengeState())
                    )
                }

                challengeHandler.handleChallengeUpdate(challenge = challenge, userId = userId)
            }
    }

    private fun UiState<TavernState>.currentChallengeState(): ChallengeState? =
        (this as? UiState.Loaded)?.data?.challenge
}
