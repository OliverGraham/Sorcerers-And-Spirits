package com.linklabs.sorcerersandspirits.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.tavern.ActiveChallenge
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetActiveChallengeFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateChallengerStatusUseCase
import com.linklabs.sorcerersandspirits.domain.utilities.Timer
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaScreenViewModelArguments
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ChallengeState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.challenge.ChallengeHandler
import com.linklabs.sorcerersandspirits.ui.screens.tavern.challenge.ChallengeUpdate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ChallengeViewModel(
    private val getActiveChallengeFlowUseCase: GetActiveChallengeFlowUseCase,
    private val updateChallengerStatusUseCase: UpdateChallengerStatusUseCase,
) : ViewModel() {

    private val _challengeStateFlow = MutableStateFlow<ChallengeState?>(value = null)
    val challengeStateFlow = _challengeStateFlow.asStateFlow()

    private val _navigateToArenaFlow = Channel<ArenaScreenViewModelArguments>()
    val navigateToArenaFlow = _navigateToArenaFlow.receiveAsFlow()

    private val challengeHandler =
        ChallengeHandler(
            challengeTimer =
                Timer(
                    coroutineScope = viewModelScope,
                    onTimerTick = ::updateChallengeStateSeconds,
                    onTimerExpiration = ::declineChallenge,
                ),
            updateChallengerStatusUseCase = updateChallengerStatusUseCase,
        )

    init {
        observeActiveChallenge()
    }

    fun onAcceptChallenge(challengeId: String) {
        updateChallengeStatus(challengeId = challengeId, status = ChallengerStatus.Accepted)
    }

    fun onDeclineChallenge(challengeId: String) {
        updateChallengeStatus(challengeId = challengeId, status = ChallengerStatus.Declined)
    }

    fun showChallengeDialog() {
        updateChallengeDialogVisibility(isVisible = true)
    }

    fun hideChallengeDialog() {
        updateChallengeDialogVisibility(isVisible = false)
    }

    private fun observeActiveChallenge() {
        viewModelScope.launch {
            getActiveChallengeFlowUseCase().collect { activeChallenge ->
                if (activeChallenge == null) {
                    _challengeStateFlow.value = null
                } else {
                    handleChallengeUpdate(activeChallenge = activeChallenge)
                }
            }
        }
    }

    private suspend fun handleChallengeUpdate(activeChallenge: ActiveChallenge) {
        val challengeUpdate =
            challengeHandler.handleChallengeUpdate(
                previousChallengeState = _challengeStateFlow.value,
                updatedChallenge = activeChallenge.challenge,
                userId = activeChallenge.userId,
            )

        when (challengeUpdate) {
            is ChallengeUpdate.ReadyToStart ->
                _navigateToArenaFlow.send(element = challengeUpdate.arguments)

            is ChallengeUpdate.Ongoing -> _challengeStateFlow.value = challengeUpdate.challengeState
        }
    }

    private fun declineChallenge() {
        val challengeId = _challengeStateFlow.value?.challengeId ?: return

        onDeclineChallenge(challengeId = challengeId)
    }

    private fun updateChallengeStatus(challengeId: String, status: ChallengerStatus) {
        viewModelScope.launch {
            challengeHandler.updateChallengerStatus(challengeId = challengeId, status = status)
            updateChallengeDialogVisibility(isVisible = false)
        }
    }

    private fun updateChallengeDialogVisibility(isVisible: Boolean) {
        val challenge = _challengeStateFlow.value ?: return

        _challengeStateFlow.value = challenge.copy(showChallengeDialog = isVisible)
    }

    private fun updateChallengeStateSeconds(secondsRemaining: Int) {
        val challenge = _challengeStateFlow.value ?: return

        _challengeStateFlow.value = challenge.copy(secondsRemaining = secondsRemaining)
    }
}
