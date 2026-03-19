package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengeStatus
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.EnterTavernUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetArenaChallengeFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetTavernFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendArenaChallengesUseCase
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
    private val sendArenaChallengesUseCase: SendArenaChallengesUseCase,
    private val getArenaChallengeFlowUseCase: GetArenaChallengeFlowUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    getTavernFlowUseCase: GetTavernFlowUseCase,
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<TavernEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val uiStateFlow = getTavernFlowUseCase()
        .map { tavern ->
            val tavernState = tavern.toTavernState(userId = getUserIdUseCase())
            if (tavernState == null) {
                UiState.Loading
            } else {
                UiState.Loaded(data = tavernState)
            }
        }
        .catch { UiState.Error }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    init {
        viewModelScope.launch {
            enterTavernUseCase()
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
                sender = userTraveler.toTraveler(),
                receivers = selectedTravelerStates.map { it.toTraveler() },
            )
        }
    }

    private suspend fun collectArenaChallenges() {
        getArenaChallengeFlowUseCase().collect { arenaChallenge ->
            when {
                // Case A: Someone accepted a challenge I'm part of
                arenaChallenge.status == ChallengeStatus.ACCEPTED -> {
                    _eventFlow.emit(value = TavernEvent.NavigateToArena(arenaChallenge.arenaId))
                }

                // Case B: I am a receiver and it's still pending
                arenaChallenge.receivers.any { it.id == getUserIdUseCase() } &&
                    arenaChallenge.status == ChallengeStatus.PENDING -> {
                    _eventFlow.emit(value = TavernEvent.ChallengeReceived(arenaChallenge))
                }
            }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
