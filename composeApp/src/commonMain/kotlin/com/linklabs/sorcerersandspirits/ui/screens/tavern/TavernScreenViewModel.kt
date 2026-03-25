package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.EnterTavernUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetArenaChallengeFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetTavernFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendArenaChallengesUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateArenaChallengeStatusUseCase
import com.linklabs.sorcerersandspirits.domain.traveler.UpdateTravelerDisplayNameUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import com.linklabs.sorcerersandspirits.ui.uistate.updateLoaded
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class TavernScreenViewModel(
    private val enterTavernUseCase: EnterTavernUseCase,
    private val updateTravelerDisplayNameUseCase: UpdateTravelerDisplayNameUseCase,
    private val sendArenaChallengesUseCase: SendArenaChallengesUseCase,
    private val getArenaChallengeFlowUseCase: GetArenaChallengeFlowUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val getTavernFlowUseCase: GetTavernFlowUseCase,
    private val updateArenaChallengeStatusUseCase: UpdateArenaChallengeStatusUseCase,
) : ViewModel() {

    private val _eventChannelFlow = Channel<TavernEvent>()
    val eventChannelFlow = _eventChannelFlow.receiveAsFlow()

    private val _uiStateFlow = MutableStateFlow<UiState<TavernState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val arenaChallengeHandler =
        ArenaChallengeHandler(
            coroutineScope = viewModelScope,
            updateArenaChallengeStatusUseCase = updateArenaChallengeStatusUseCase,
            sendArenaChallengesUseCase = sendArenaChallengesUseCase,
            updateTavernState = { transform -> _uiStateFlow.updateLoaded(transform = transform) },
            onEvent = { event -> viewModelScope.launch { _eventChannelFlow.send(element = event) } },
        )

    init {
        viewModelScope.launch {
            enterTavernUseCase()
            collectTavernFlow()
        }

        viewModelScope.launch { collectArenaChallenges() }
    }

    fun onActionButtonClick() {
        viewModelScope.launch {
            updateTravelerDisplayNameUseCase(displayName = "A Traveler with an updated name")
        }
    }

    fun showArenaChallengeDialog() {
        arenaChallengeHandler.updateDialogVisibility(isVisible = true)
    }

    fun hideArenaChallengeDialog() {
        arenaChallengeHandler.updateDialogVisibility(isVisible = false)
    }

    fun onSendChallengerNotifications(
        userTraveler: TravelerState,
        selectedTravelerStates: Set<TravelerState>,
    ) {
        arenaChallengeHandler.sendArenaChallenges(
            userTraveler = userTraveler,
            selectedTravelerStates = selectedTravelerStates,
        )
    }

    fun onAcceptArenaChallenge(challenge: ArenaChallengeState) {
        arenaChallengeHandler.acceptArenaChallenge(arenaChallengeState = challenge)
    }

    fun onDeclineArenaChallenge(challenge: ArenaChallengeState) {
        arenaChallengeHandler.declineArenaChallenge(arenaChallengeState = challenge)
    }

    // TODO: this needs to be improved
    private suspend fun collectTavernFlow() {
        getTavernFlowUseCase()
            .filterNotNull()
            .map { tavern ->
                val tavernState = tavern.toTavernState(userId = getUserIdUseCase())
                if (tavernState == null) {
                    UiState.Loading
                } else {
                    UiState.Loaded(data = tavernState)
                }
            }
            .catch { emit(UiState.Error) }
            .collect { newState -> _uiStateFlow.value = newState }
    }

    private suspend fun collectArenaChallenges() {
        val userId = getUserIdUseCase()

        getArenaChallengeFlowUseCase().filterNotNull().collect { arenaChallenge ->
            arenaChallengeHandler.handleArenaChallengeUpdate(
                arenaChallenge = arenaChallenge,
                userId = userId,
            )
        }
    }
}
