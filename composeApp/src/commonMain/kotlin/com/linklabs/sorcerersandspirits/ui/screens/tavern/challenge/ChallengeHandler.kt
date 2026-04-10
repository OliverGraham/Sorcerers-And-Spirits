package com.linklabs.sorcerersandspirits.ui.screens.tavern.challenge

import com.linklabs.sorcerersandspirits.domain.tavern.Challenge
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.SendChallengeResult
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendChallengeUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateChallengerStatusUseCase
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ChallengeState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernEvent
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TravelerState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.toChallengeState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.toChallenger
import com.linklabs.sorcerersandspirits.ui.screens.tavern.toChallengerState

internal class ChallengeHandler(
    private val challengeTimer: ChallengeTimer,
    private val sendChallengeUseCase: SendChallengeUseCase,
    private val updateChallengerStatusUseCase: UpdateChallengerStatusUseCase,
    private val updateTavernState: ((TavernState) -> TavernState) -> Unit,
) {

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
            is SendChallengeResult.Failure -> TavernEvent.ShowChallengeSendFailed
        }
    }

    fun handleChallengeUpdate(
        previousChallengeState: ChallengeState?,
        updatedChallenge: Challenge?,
        userId: String,
    ): HandleChallengeUpdateResult? {
        if (updatedChallenge == null) return null

        val userChallenger =
            updatedChallenge.challengers[userId]
                ?: run {
                    stopTimer()
                    clearChallenge()
                    return null
                }

        val isPendingInvitee = userChallenger.status == ChallengerStatus.Pending

        val challengeState =
            updatedChallenge.toChallengeState(
                showChallengeDialog =
                    when {
                        isPendingInvitee -> true
                        previousChallengeState?.showChallengeDialog == true -> true
                        else -> false
                    },
                secondsRemaining =
                    when {
                        !isPendingInvitee -> null
                        challengeTimer.isTimerCurrentlyRunning() ->
                            previousChallengeState?.secondsRemaining
                        else -> COUNTDOWN_SECONDS
                    },
            )

        val shouldNavigateToArena =
            userChallenger.status == ChallengerStatus.Accepted && updatedChallenge.isReadyToStart()

        val tavernEvent =
            when {
                shouldNavigateToArena -> {
                    stopTimer()
                    updateDialogVisibility(isVisible = false)

                    TavernEvent.NavigateToArena(arenaId = updatedChallenge.challengeId)
                }

                isPendingInvitee && !challengeTimer.isTimerCurrentlyRunning() -> {
                    startTimer()
                    null
                }

                !isPendingInvitee -> {
                    stopTimer()
                    null
                }
                else -> null
            }

        return HandleChallengeUpdateResult(
            challengeState = challengeState,
            tavernEvent = tavernEvent,
        )
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

    fun updateDialogVisibility(isVisible: Boolean) {
        updateTavernState { tavernState ->
            tavernState.updateChallengeDialogVisibility(isVisible = isVisible)
        }
    }

    fun clearChallenge() {
        updateTavernState { tavernState -> tavernState.copy(challenge = null) }
    }

    private fun startTimer() {
        challengeTimer.startTimer()
    }

    private fun stopTimer() {
        challengeTimer.stopTimer()
    }

    private suspend fun updateChallengerStatus(
        challengeId: String,
        userId: String,
        status: ChallengerStatus,
    ) {
        stopTimer()

        updateChallengerStatusUseCase(challengeId = challengeId, userId = userId, status = status)

        updateDialogVisibility(isVisible = false)
    }

    private fun TavernState.updateChallengeDialogVisibility(isVisible: Boolean): TavernState {
        val challenge = challenge ?: return this

        return copy(challenge = challenge.copy(showChallengeDialog = isVisible))
    }

    private companion object {
        private const val COUNTDOWN_SECONDS = 60
    }
}
