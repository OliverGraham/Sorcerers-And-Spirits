package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetProfileFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.EnterTavernUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetActiveChallengeFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetTavernTravelersFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendChallengeUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SetCurrentTravelerAvailableUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateChallengerStatusUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import com.linklabs.sorcerersandspirits.ui.uistate.updateLoaded
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class TavernScreenViewModel(
    private val enterTavernUseCase: EnterTavernUseCase,
    private val sendChallengeUseCase: SendChallengeUseCase,
    private val getActiveChallengeFlowUseCase: GetActiveChallengeFlowUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val getProfileFlowUseCase: GetProfileFlowUseCase,
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
            sendChallengeUseCase = sendChallengeUseCase,
            updateChallengerStatusUseCase = updateChallengerStatusUseCase,
            setCurrentTravelerAvailableUseCase = setCurrentTravelerAvailableUseCase,
        )

    init {
        viewModelScope.launch { collectScreenState() }
    }

    fun onActionButtonClick() {}

    fun showChallengeDialog() {
        updateChallengeDialogVisibility(isVisible = true)
    }

    fun hideChallengeDialog() {
        updateChallengeDialogVisibility(isVisible = false)
    }

    fun onSendChallenge(userTraveler: TravelerState, selectedTravelerStates: Set<TravelerState>) {
        viewModelScope.launch {
            val tavernEvent =
                challengeHandler.sendChallenges(
                    userTraveler = userTraveler,
                    selectedTravelerStates = selectedTravelerStates,
                ) ?: return@launch

            _eventChannelFlow.send(element = tavernEvent)
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

    // TODO: Improve
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun collectScreenState() {
        getProfileFlowUseCase()
            .filterNotNull()
            .distinctUntilChanged { oldProfile, newProfile ->
                isSameSession(old = oldProfile, new = newProfile)
            }
            .flatMapLatest { profile ->
                enterTavernUseCase(profile = profile)

                combine(
                    flow =
                        getTavernTravelersFlowUseCase().filterNot { travelers ->
                            travelers.none { it.userId == profile.userId }
                        },
                    flow2 = getActiveChallengeFlowUseCase(),
                ) { travelers, challenge ->
                    travelers.toTavernState(
                        userId = profile.userId,
                        updatedChallenge = challenge,
                        challengeBeforeUpdate = getChallengeFromCurrentState(),
                    )
                }
            }
            .collect { newTavernState ->
                _uiStateFlow.value = UiState.Loaded(data = newTavernState)

                challengeHandler.handleChallengeUpdate(
                    newTavernState = newTavernState,
                    onTimerTick = ::onChallengeTimerTick,
                )
            }
    }

    private fun isSameSession(old: Profile, new: Profile) =
        old.userId == new.userId && old.displayName == new.displayName

    private fun updateChallengeDialogVisibility(isVisible: Boolean) {
        val challengeState = getChallengeFromCurrentState() ?: return
        _uiStateFlow.updateLoaded {
            it.copy(challenge = challengeState.copy(showChallengeDialog = isVisible))
        }
    }

    private fun onChallengeTimerTick(secondsRemaining: Int) {
        val challengeState = getChallengeFromCurrentState() ?: return
        _uiStateFlow.updateLoaded {
            it.copy(challenge = challengeState.copy(secondsRemaining = secondsRemaining))
        }
    }

    private fun getChallengeFromCurrentState() =
        (_uiStateFlow.value as? UiState.Loaded)?.data?.challenge
}
