package com.linklabs.sorcerersandspirits.ui.screens.tavern.challenge

import com.linklabs.sorcerersandspirits.domain.tavern.Challenge
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.SendChallengeResult
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendChallengeUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateChallengerStatusUseCase
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ChallengeState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernEvent
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TravelerState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.toChallengeState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.toChallenger
import com.linklabs.sorcerersandspirits.ui.screens.tavern.toChallengerState

internal class ChallengeHandler(
    private val challengeTimer: ChallengeTimer,
    private val sendChallengeUseCase: SendChallengeUseCase,
    private val updateChallengerStatusUseCase: UpdateChallengerStatusUseCase,
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

    // TODO: Update comments
    fun handleChallengeUpdateAndGetState(
        previousChallengeState: ChallengeState?,
        updatedChallenge: Challenge?,
        userId: String,
    ): ChallengeState? =
        when {
            // if previous is null and new is not, (show , start timer)
            previousChallengeState == null && updatedChallenge != null -> {
                // TODO: Currently, this will open the dialog for the instigator - which is GOOD
                //  But the Dialog UI needs to be updated for instigator vs others

                val isUserInstigator =
                    updatedChallenge.getUserChallenger(userId = userId)?.isInstigator == true
                challengeTimer.startTimer(callOnTimerExpiration = !isUserInstigator)
                updatedChallenge.toChallengeState(
                    showChallengeDialog = true,
                    secondsRemaining = COUNTDOWN_SECONDS,
                )
            }
            // if previous is not null and new is null, (hide dialog, clear seconds if needed, etc.)
            previousChallengeState != null && updatedChallenge == null -> {
                challengeTimer.stopTimer()
                // clearChallenge()
                null
            }

            // if previous is not null and new is not null, (show dialog if old is showing it, copy
            // seconds)
            previousChallengeState != null && updatedChallenge != null -> {
                val isChallengeReadyToStart = updatedChallenge.isReadyToStart(userId = userId)

                if (isChallengeReadyToStart) {
                    updatedChallenge.toChallengeState(isReadyToStart = true)
                } else {
                    updatedChallenge.toChallengeState(
                        showChallengeDialog = previousChallengeState.showChallengeDialog,
                        secondsRemaining = previousChallengeState.secondsRemaining,
                    )
                }
            }
            else -> null
        }

    suspend fun updateChallengerStatus(challengeId: String, status: ChallengerStatus) {
        challengeTimer.stopTimer()
        updateChallengerStatusUseCase(challengeId = challengeId, status = status)
    }

    // TODO: Move seconds somewhere?
    private companion object {
        private const val COUNTDOWN_SECONDS = 60
    }
}
