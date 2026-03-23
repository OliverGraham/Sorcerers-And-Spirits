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

    fun showArenaChallengeDialog() {
        updateArenaChallengeDialogVisibility(isVisible = true)
    }

    fun hideArenaChallengeDialog() {
        updateArenaChallengeDialogVisibility(isVisible = false)
    }

    fun onSendChallengerNotifications(
        userTraveler: TravelerState,
        selectedTravelerStates: Set<TravelerState>,
    ) {
        // TODO: Check GamePlayStatus before this, so to make sure what's legal and what isn't
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
            updateArenaChallengeDialogVisibility(isVisible = false)
        }
    }

    private suspend fun collectTavernFlow() {
        getTavernFlowUseCase().filterNotNull().map { tavern ->
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

    // TODO: What about if user logs in, and userId would change? Much later problem
    private suspend fun collectArenaChallenges() {
        val userId = getUserIdUseCase()

        // TODO: Figure out test cases, start writing tests here I suppose
        getArenaChallengeFlowUseCase().filterNotNull().collect { arenaChallenge ->
            _uiStateFlow.updateLoaded { tavernState ->
                val userGamePlayStatus =
                    arenaChallenge.getUserTraveler(userId = userId).gamePlayStatus

                // TODO: Could have the button for the dialog update with some animation to show that
                //  there's been new activity
                tavernState.copy(
                    arenaChallenge = arenaChallenge.toArenaChallengeState(userId = userId),
                    showArenaChallengeDialog = userGamePlayStatus == GamePlayStatus.ConsideringInvite,
                )
            }

            if (arenaChallenge.hasEveryoneAcceptedTheInvite) {
                updateArenaChallengeDialogVisibility(isVisible = false)
                _eventChannelFlow.send(
                    element =
                    TavernEvent.NavigateToArena(arenaId = arenaChallenge.arenaId),
                )
            }
        }
    }

    private fun updateArenaChallengeDialogVisibility(isVisible: Boolean) {
        _uiStateFlow.updateLoaded { it.copy(showArenaChallengeDialog = isVisible) }
    }
}
