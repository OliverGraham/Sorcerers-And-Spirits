package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.EnterTavernUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetArenaChallengeFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetTavernFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendArenaChallengesUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateArenaChallengeStatusUseCase
import com.linklabs.sorcerersandspirits.domain.traveler.InviteStatus
import com.linklabs.sorcerersandspirits.domain.traveler.TravelerStatus
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

        viewModelScope.launch { collectArenaChallenges() }
    }

    fun onActionButtonClick() {
        viewModelScope.launch {
            updateTravelerDisplayNameUseCase(displayName = "A Traveler with an updated name")
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
        viewModelScope.launch {
            // TODO move this logic
            val sender =
                userTraveler.toTraveler(
                    status =
                        TravelerStatus.InvitedToArena(
                            isSenderOfInvite = true,
                            inviteStatus = InviteStatus.Accepted,
                        )
                )
            val selectedTravelers =
                selectedTravelerStates.map {
                    it.toTraveler(
                        status =
                            TravelerStatus.InvitedToArena(
                                isSenderOfInvite = false,
                                inviteStatus = InviteStatus.Considering,
                            )
                    )
                }

            sendArenaChallengesUseCase(travelers = listOf(sender) + selectedTravelers)
        }
    }

    fun onAcceptArenaChallenge(challenge: ArenaChallengeState, userTraveler: TravelerState) {
        updateArenaChallengeStatus(
            arenaChallengeState = challenge,
            userTravelerId = userTraveler.id,
            inviteStatus = InviteStatus.Accepted,
        )
    }

    fun onDeclineArenaChallenge(challenge: ArenaChallengeState, userTraveler: TravelerState) {
        updateArenaChallengeStatus(
            arenaChallengeState = challenge,
            userTravelerId = userTraveler.id,
            inviteStatus = InviteStatus.Declined,
        )
    }

    private fun updateArenaChallengeStatus(
        arenaChallengeState: ArenaChallengeState,
        inviteStatus: InviteStatus,
        userTravelerId: String,
    ) {
        viewModelScope.launch {
            // TODO: Move this logic
            val userTravelerToUpdate =
                arenaChallengeState.travelerStates.find { it.isThisTravelerTheUser }
            if (userTravelerToUpdate == null) return@launch

            val updatedStatus =
                when (val status = userTravelerToUpdate.status) {
                    is TravelerStatus.InvitedToArena -> status.copy(inviteStatus = inviteStatus)
                    else -> return@launch
                }

            val updatedUserTraveler = userTravelerToUpdate.copy(status = updatedStatus)
            val updatedChallengeState =
                arenaChallengeState.copy(
                    travelerStates =
                        arenaChallengeState.travelerStates.map { travelerState ->
                            if (travelerState.id == userTravelerId) {
                                updatedUserTraveler
                            } else {
                                travelerState
                            }
                        }
                )

            updateArenaChallengeStatusUseCase(
                arenaChallenge = updatedChallengeState.toArenaChallenge()
            )
            updateArenaChallengeDialogVisibility(isVisible = false)
        }
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
            val userTraveler = arenaChallenge.getUserTraveler(userId = userId) ?: return@collect
            val userTravelerStatus = userTraveler.status
            if (userTravelerStatus !is TravelerStatus.InvitedToArena) return@collect

            _uiStateFlow.updateLoaded { tavernState ->
                tavernState.copy(
                    arenaChallenge = arenaChallenge.toArenaChallengeState(userId = userId),
                    showArenaChallengeDialog =
                        userTravelerStatus.inviteStatus == InviteStatus.Considering,
                )
            }

            if (arenaChallenge.hasEveryoneAcceptedTheInvite) {
                updateArenaChallengeDialogVisibility(isVisible = false)
                _eventChannelFlow.send(
                    element = TavernEvent.NavigateToArena(arenaId = arenaChallenge.arenaId)
                )
            }
        }
    }

    private fun updateArenaChallengeDialogVisibility(isVisible: Boolean) {
        _uiStateFlow.updateLoaded { it.copy(showArenaChallengeDialog = isVisible) }
    }
}
