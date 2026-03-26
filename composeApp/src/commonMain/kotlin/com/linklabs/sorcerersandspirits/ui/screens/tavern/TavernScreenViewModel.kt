package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.EnterTavernUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetArenaChallengeFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetTavernFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.RemoveArenaChallengeUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendArenaChallengesUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateArenaChallengeStatusUseCase
import com.linklabs.sorcerersandspirits.domain.traveler.UpdateTravelerDisplayNameUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import com.linklabs.sorcerersandspirits.ui.uistate.updateLoaded
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
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
    private val removeArenaChallengeUseCase: RemoveArenaChallengeUseCase,
) : ViewModel() {

    private val _eventChannelFlow = Channel<TavernEvent>()
    val eventChannelFlow = _eventChannelFlow.receiveAsFlow()

    private val _uiStateFlow = MutableStateFlow<UiState<TavernState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val loadedTavernState
        get() = (_uiStateFlow.value as? UiState.Loaded)?.data

    // TODO: Determine if a flow should be made out of this
    private val userId = getUserIdUseCase()

    private val tavernStateFlow2 =
        combine(
            flow = getTavernFlowUseCase().filterNotNull(),
            flow2 = getArenaChallengeFlowUseCase().onStart { emit(value = null) },
        ) { tavern, arenaChallenge ->
            val updatedArenaState =
                if (arenaChallenge != null) {
                    arenaChallengeHandler.getUpdatedArenaState(
                        newArenaChallenge = arenaChallenge,
                        oldArenaChallengeState = loadedTavernState?.arenaChallenge,
                        userId = userId,
                    )
                } else {
                    null
                }

            val tavernState =
                tavern.toTavernState(userId = userId) ?: return@combine UiState.Loading
            UiState.Loaded(data = tavernState.copy(arenaChallenge = updatedArenaState))
        }

    private val tavernStateFlow =
        combine(
            getTavernFlowUseCase().filterNotNull(),
            getArenaChallengeFlowUseCase().onStart { emit(null) },
        ) { tavern, arenaChallenge ->
            val userId = userId
            val baseTavernState =
                tavern.toTavernState(userId = userId) ?: return@combine UiState.Loading

            val updatedArenaState =
                if (arenaChallenge != null) {
                    // Identify anyone in the challenge not physically in the Tavern list
                    val peopleInTavernIds = tavern.travelers.map { it.id }.toSet()
                    val ghostedIds =
                        arenaChallenge.travelers
                            .filter { it.id !in peopleInTavernIds }
                            .map { it.id }

                    // If ghosts exist, the handler will try to flip them to Declined in the DB
                    if (ghostedIds.isNotEmpty()) {
                        arenaChallengeHandler.declineGhostedParticipants(arenaChallenge, ghostedIds)
                    }

                    arenaChallengeHandler.getUpdatedArenaState(
                        newArenaChallenge = arenaChallenge,
                        oldArenaChallengeState = loadedTavernState?.arenaChallenge,
                        userId = userId,
                    )
                } else {
                    null
                }

            UiState.Loaded(data = baseTavernState.copy(arenaChallenge = updatedArenaState))
        }

    private val arenaChallengeHandler =
        ArenaChallengeHandler(
            coroutineScope = viewModelScope,
            updateArenaChallengeStatusUseCase = updateArenaChallengeStatusUseCase,
            sendArenaChallengesUseCase = sendArenaChallengesUseCase,
            removeArenaChallengeUseCase = removeArenaChallengeUseCase,
            updateTavernState = { transform -> _uiStateFlow.updateLoaded(transform = transform) },
            onEvent = { event -> viewModelScope.launch { _eventChannelFlow.send(element = event) } },
        )

    init {
        viewModelScope.launch {
            enterTavernUseCase()
            tavernStateFlow
                .catch { _uiStateFlow.value = UiState.Error }
                .collect { newState ->
                    _uiStateFlow.value = newState

                    val arenaChallengeState = loadedTavernState?.arenaChallenge
                    if (arenaChallengeState != null) {
                        arenaChallengeHandler.processArenaChallengeEffects(
                            arenaChallengeState = arenaChallengeState
                        )
                    }
                }
        }
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
}
