package com.linklabs.sorcerersandspirits.ui.screens.tavern

import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.SendChallengeResult
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendChallengeUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SetCurrentTravelerAvailableUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateChallengerStatusUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class ChallengeHandler(
    private val sendChallengeUseCase: SendChallengeUseCase,
    private val updateChallengerStatusUseCase: UpdateChallengerStatusUseCase,
    private val setCurrentTravelerAvailableUseCase: SetCurrentTravelerAvailableUseCase,
) {
    private var timerJob: Job? = null

    suspend fun sendChallenges(
        userTraveler: TravelerState,
        selectedTravelerStates: Set<TravelerState>,
    ): TavernEvent? {
        val instigator =
            userTraveler.toChallenger(isInstigator = true, status = ChallengerStatus.Accepted)

        val invitees =
            selectedTravelerStates.map { travelerState ->
                travelerState.toChallenger(isInstigator = false, status = ChallengerStatus.Pending)
            }

        val result = sendChallengeUseCase(instigator = instigator, invitees = invitees)
        return when (result) {
            is SendChallengeResult.Success -> null
            is SendChallengeResult.NotEnoughChallengersAvailable ->
                TavernEvent.NotEnoughChallengersAvailable(
                    challengerStates = result.unavailableChallengers.map { it.toChallengerState() }
                )
            is SendChallengeResult.Failure -> TavernEvent.SendChallengeFailed
        }
    }

    /**
     * Coordinates timer and side-effects related to challenge updates and returns [TavernEvent] if
     * applicable
     */
    suspend fun handleChallengeUpdate(
        newTavernState: TavernState,
        onTimerTick: (secondsRemaining: Int) -> Unit,
    ): TavernEvent? {
        val challengeState = newTavernState.challenge
        // if newChallengeState is null and oldChallengeState is something
        if (challengeState == null) {
            releaseUserFromChallenge(tavernStatus = newTavernState.userTraveler.tavernStatus)
            return null
        }

        // TODO: This could be in use case, right?
        val userChallenger = newTavernState.userChallenger
        return when (userChallenger?.status) {
            ChallengerStatus.Accepted -> {
                stopTimer()
                if (challengeState.hasEveryoneAccepted()) {
                    TavernEvent.NavigateToArena(arenaId = challengeState.challengeId)
                } else {
                    null
                }
            }
            ChallengerStatus.Declined -> {
                stopTimer()
                null // todo: show snackbar?
            }
            ChallengerStatus.Pending -> {
                if (!isTimerGoing()) {
                    startTimer(
                        challengeId = challengeState.challengeId,
                        userId = newTavernState.userId,
                        onTimerTick = onTimerTick,
                    )
                }
                null
            }
            null -> null
        }
    }

    private suspend fun releaseUserFromChallenge(tavernStatus: TavernStatus?) {
        stopTimer()
    }

    suspend fun acceptChallenge(challengeState: ChallengeState, userId: String) {
        updateChallengerStatus(
            challengeId = challengeState.challengeId,
            userId = userId,
            status = ChallengerStatus.Accepted,
        )
    }

    suspend fun declineChallenge(challengeState: ChallengeState, userId: String) {
        updateChallengerStatus(
            challengeId = challengeState.challengeId,
            userId = userId,
            status = ChallengerStatus.Declined,
        )
    }

    private suspend fun updateChallengerStatus(
        challengeId: String,
        userId: String,
        status: ChallengerStatus,
    ) {
        stopTimer()
        updateChallengerStatusUseCase(challengeId = challengeId, userId = userId, status = status)
    }

    private suspend fun startTimer(
        challengeId: String,
        userId: String,
        onTimerTick: (secondsRemaining: Int) -> Unit,
    ) = coroutineScope {
        stopTimer()

        timerJob = launch {
            var secondsRemaining = COUNTDOWN_SECONDS
            while (secondsRemaining >= 0) {
                onTimerTick(secondsRemaining)
                delay(timeMillis = ONE_SECOND)
                secondsRemaining--
            }

            updateChallengerStatus(
                challengeId = challengeId,
                userId = userId,
                status = ChallengerStatus.Declined,
            )
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null

        /*updateTavernState { tavernState ->
            tavernState.updateChallengeStateSeconds(secondsRemaining = null)
        }*/
    }

    // TODO: Why is there navigatingToArenaChallengeId
    /*private fun shouldSetCurrentTravelerAvailableWhenChallengeClears(): Boolean {
        val currentChallengeId = getCurrentTavernState()?.challenge?.challengeId ?: return false

        return currentChallengeId != navigatingToArenaChallengeId
    }*/

    private fun TavernState.updateChallengeStateSeconds(secondsRemaining: Int?): TavernState {
        val challenge = challenge ?: return this

        return copy(challenge = challenge.copy(secondsRemaining = secondsRemaining))
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

    private fun isTimerGoing() = timerJob != null

    companion object {
        const val COUNTDOWN_SECONDS = 60
        private const val ONE_SECOND = 1000L
        private const val MINIMUM_CHALLENGERS = 2
    }
}
