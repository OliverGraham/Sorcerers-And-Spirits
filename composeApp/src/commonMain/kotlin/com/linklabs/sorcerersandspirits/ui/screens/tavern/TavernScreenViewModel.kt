package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.EnterTavernUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetArenaChallengeFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetTavernFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendArenaChallengesUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateArenaChallengeStatusUseCase
import com.linklabs.sorcerersandspirits.domain.traveler.GamePlayStatus
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

    init {
        viewModelScope.launch {
            enterTavernUseCase()
            collectTavernFlow()
        }

        viewModelScope.launch {
            collectArenaChallenges()
        }
    }

    fun onActionButtonClick() {
        viewModelScope.launch {
            updateTravelerDisplayNameUseCase(
                displayName = "A Traveler with an updated name",
            )
        }
    }

    fun onSendChallengerNotifications(
        userTraveler: TravelerState,
        selectedTravelerStates: Set<TravelerState>,
    ) {
        viewModelScope.launch {
            sendArenaChallengesUseCase(
                sender = userTraveler.toTraveler().copy(gamePlayStatus = GamePlayStatus.AcceptedInvite),
                receivers = selectedTravelerStates.map { it.toTraveler().copy(gamePlayStatus = GamePlayStatus.ConsideringInvite) },
            )
        }
    }

    fun onAcceptArenaChallenge(challenge: ArenaChallengeState, userTraveler: TravelerState) {
        updateArenaChallengeStatus(
            arenaChallengeState = challenge,
            userTravelerId = userTraveler.id,
            gamePlayStatus = GamePlayStatus.AcceptedInvite,
        )
    }

    fun onDeclineArenaChallenge(challenge: ArenaChallengeState, userTraveler: TravelerState) {
        updateArenaChallengeStatus(
            arenaChallengeState = challenge,
            userTravelerId = userTraveler.id,
            gamePlayStatus = GamePlayStatus.DeclinedInvite,
        )
    }

    private fun updateArenaChallengeStatus(
        arenaChallengeState: ArenaChallengeState,
        gamePlayStatus: GamePlayStatus,
        userTravelerId: String,
    ) {
        viewModelScope.launch {
            val updatedChallenge = arenaChallengeState.copy(
                receivers = arenaChallengeState.receivers.map { travelerState ->
                    if (travelerState.id == userTravelerId) {
                        travelerState.copy(gamePlayStatus = gamePlayStatus)
                    } else {
                        travelerState
                    }
                },
            )

            updateArenaChallengeStatusUseCase(
                arenaChallenge = updatedChallenge.toArenaChallenge(),
            )
        }
    }

    private suspend fun collectTavernFlow() {
        getTavernFlowUseCase().map { tavern ->
            val tavernState = tavern.toTavernState(userId = getUserIdUseCase())
            if (tavernState == null) {
                UiState.Loading
            } else {
                UiState.Loaded(data = tavernState)
            }
        }
            .catch { emit(UiState.Error) }
            .collect { newState ->
                _uiStateFlow.value = newState
            }
    }

    private suspend fun collectArenaChallenges() {
        val userId = getUserIdUseCase()

        getArenaChallengeFlowUseCase().filterNotNull().collect { arenaChallenge ->
            val allParticipants = arenaChallenge.receivers + arenaChallenge.sender
            val userGamePlayStatus =
                allParticipants.find { it.id == userId }?.gamePlayStatus ?: return@collect

            when (userGamePlayStatus) {
                GamePlayStatus.ConsideringInvite -> {
                    closeArenaChallengeDialog()

                    _uiStateFlow.updateLoaded { it.copy(arenaChallenge = arenaChallenge.toArenaChallengeState()) }
                }
                GamePlayStatus.AcceptedInvite -> {
                    // Only show dialog if I'm not the one who sent it
                    // if (arenaChallenge.sender.id == userId) return@collect

                    // TODO: Make UI to track Status of this. Maybe button to bring up dialog again?
                    closeArenaChallengeDialog()

                    // Navigation logic: If all receivers accepted, go to Arena
                    if (arenaChallenge.receivers.all { it.gamePlayStatus == GamePlayStatus.AcceptedInvite }) {
                        _eventChannelFlow.send(
                            element =
                            TavernEvent.NavigateToArena(arenaId = arenaChallenge.arenaId),
                        )
                    }
                }
                GamePlayStatus.DeclinedInvite -> {
                    closeArenaChallengeDialog()
                }
                GamePlayStatus.Available,
                GamePlayStatus.InArena,
                -> {
                    // TODO: Nothing here?
                }
            }
        }
    }

    private fun closeArenaChallengeDialog() {
        _uiStateFlow.updateLoaded { it.copy(arenaChallenge = null) }
    }
}
