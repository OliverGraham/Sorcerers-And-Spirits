package com.linklabs.sorcerersandspirits.ui.screens.tavern

import com.linklabs.sorcerersandspirits.domain.tavern.Challenge
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.SendChallengeResult
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendChallengeUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SetCurrentTravelerAvailableUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateChallengerStatusUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class ChallengeHandler(
    private val coroutineScope: CoroutineScope,
    private val sendChallengeUseCase: SendChallengeUseCase,
    private val updateChallengerStatusUseCase: UpdateChallengerStatusUseCase,
    private val setCurrentTravelerAvailableUseCase: SetCurrentTravelerAvailableUseCase,
    private val getCurrentTavernState: () -> TavernState?,
    private val updateTavernState: ((TavernState) -> TavernState) -> Unit,
    private val onEvent: (TavernEvent) -> Unit,
) {
    private var timerJob: Job? = null
    private var timerChallengeId: String? = null
    private var navigatingToArenaChallengeId: String? = null

    fun sendChallenges(userTraveler: TravelerState, selectedTravelerStates: Set<TravelerState>) {
        val instigator =
            userTraveler.toChallenger(isInstigator = true, status = ChallengerStatus.Accepted)

        val invitees =
            selectedTravelerStates.map { travelerState ->
                travelerState.toChallenger(isInstigator = false, status = ChallengerStatus.Pending)
            }

        coroutineScope.launch {
            runCatching { sendChallengeUseCase(instigator = instigator, invitees = invitees) }
                .onSuccess { result ->
                    when (result) {
                        is SendChallengeResult.Success -> Unit
                        is SendChallengeResult.NotEnoughChallengersAvailable -> {
                            onEvent(
                                TavernEvent.NotEnoughChallengersAvailable(
                                    challengerStates =
                                        result.unavailableChallengers.map { it.toChallengerState() }
                                )
                            )
                        }

                        is SendChallengeResult.Failure -> {
                            // TODO:
                        }
                    }
                }
                .onFailure { onEvent(TavernEvent.ShowChallengeSendFailed) }
        }
    }

    fun handleChallengeUpdate(challenge: Challenge?, userId: String) {
        if (challenge == null) {
            val shouldSetCurrentTravelerAvailable =
                shouldSetCurrentTravelerAvailableWhenChallengeClears()

            stopTimer()
            clearChallenge()
            navigatingToArenaChallengeId = null

            if (shouldSetCurrentTravelerAvailable) {
                coroutineScope.launch { setCurrentTravelerAvailableUseCase() }
            }

            return
        }

        val currentTavernState = getCurrentTavernState()

        val userChallenger =
            challenge.challengers[userId]
                ?: run {
                    stopTimer()
                    clearChallenge()
                    return
                }

        val currentChallengeState = currentTavernState?.challenge
        val isSameChallenge = currentChallengeState?.challengeId == challenge.challengeId
        val isPendingInvitee = userChallenger.status == ChallengerStatus.Pending

        val challengeState =
            challenge.toChallengeState(
                showChallengeDialog =
                    when {
                        isPendingInvitee -> true
                        isSameChallenge -> currentChallengeState.showChallengeDialog
                        else -> false
                    },
                secondsRemaining =
                    when {
                        !isPendingInvitee -> null
                        isSameChallenge ->
                            currentChallengeState.secondsRemaining ?: COUNTDOWN_SECONDS
                        else -> COUNTDOWN_SECONDS
                    },
            )

        updateTavernState { tavernState -> tavernState.copy(challenge = challengeState) }

        val shouldNavigateToArena =
            userChallenger.status == ChallengerStatus.Accepted &&
                challengeState.hasEveryoneAccepted()

        when {
            shouldNavigateToArena -> {
                navigatingToArenaChallengeId = challenge.challengeId
                stopTimer()
                updateDialogVisibility(isVisible = false)

                onEvent(TavernEvent.NavigateToArena(arenaId = challenge.challengeId))
            }

            isPendingInvitee && timerChallengeId != challenge.challengeId -> {
                startTimer(challengeId = challenge.challengeId, userId = userId)
            }

            !isPendingInvitee -> {
                stopTimer()
            }
        }
    }

    fun acceptChallenge(challengeState: ChallengeState, userId: String) {
        updateChallengerStatus(
            challengeId = challengeState.challengeId,
            userId = userId,
            status = ChallengerStatus.Accepted,
        )
    }

    fun declineChallenge(challengeState: ChallengeState, userId: String) {
        updateChallengerStatus(
            challengeId = challengeState.challengeId,
            userId = userId,
            status = ChallengerStatus.Declined,
        )
    }

    fun updateDialogVisibility(isVisible: Boolean) {
        updateTavernState { tavernState ->
            tavernState.updateChallengeDialogVisibility(isVisible = isVisible)
        }
    }

    fun clearChallenge() {
        updateTavernState { tavernState -> tavernState.copy(challenge = null) }
    }

    private fun startTimer(challengeId: String, userId: String) {
        stopTimer()

        timerChallengeId = challengeId
        timerJob =
            coroutineScope.launch {
                var secondsRemaining = COUNTDOWN_SECONDS

                while (secondsRemaining >= 0) {
                    updateTavernState { tavernState ->
                        tavernState.updateChallengeStateSeconds(secondsRemaining = secondsRemaining)
                    }

                    delay(timeMillis = 1000L)
                    secondsRemaining--
                }

                updateChallengerStatus(
                    challengeId = challengeId,
                    userId = userId,
                    status = ChallengerStatus.Declined,
                )
            }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        timerChallengeId = null

        updateTavernState { tavernState ->
            tavernState.updateChallengeStateSeconds(secondsRemaining = null)
        }
    }

    private fun updateChallengerStatus(
        challengeId: String,
        userId: String,
        status: ChallengerStatus,
    ) {
        stopTimer()

        coroutineScope.launch {
            updateChallengerStatusUseCase(
                challengeId = challengeId,
                userId = userId,
                status = status,
            )

            updateDialogVisibility(isVisible = false)
        }
    }

    private fun shouldSetCurrentTravelerAvailableWhenChallengeClears(): Boolean {
        val currentChallengeId = getCurrentTavernState()?.challenge?.challengeId ?: return false

        return currentChallengeId != navigatingToArenaChallengeId
    }

    private fun TavernState.updateChallengeStateSeconds(secondsRemaining: Int?): TavernState {
        val challenge = challenge ?: return this

        return copy(challenge = challenge.copy(secondsRemaining = secondsRemaining))
    }

    private fun TavernState.updateChallengeDialogVisibility(isVisible: Boolean): TavernState {
        val challenge = challenge ?: return this

        return copy(challenge = challenge.copy(showChallengeDialog = isVisible))
    }

    private fun ChallengeState.hasEveryoneAccepted(): Boolean =
        challengerStates
            .filterNot { challengerState -> challengerState.status == ChallengerStatus.Declined }
            .let { activeChallengerStates ->
                activeChallengerStates.size >= MINIMUM_CHALLENGERS &&
                    activeChallengerStates.all { challengerState ->
                        challengerState.status == ChallengerStatus.Accepted
                    }
            }

    private companion object {
        private const val COUNTDOWN_SECONDS = 60
        private const val MINIMUM_CHALLENGERS = 2
    }
}
