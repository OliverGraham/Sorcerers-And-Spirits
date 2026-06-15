package com.linklabs.sorcerersandspirits.ui.screens.tavern.challenge

import com.linklabs.sorcerersandspirits.domain.tavern.Challenge
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.SendChallengeResult
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendChallengeUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateChallengerStatusUseCase
import com.linklabs.sorcerersandspirits.domain.utilities.Timer
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ChallengeState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernEvent
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TravelerState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.toChallengeState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.toChallenger
import com.linklabs.sorcerersandspirits.ui.screens.tavern.toChallengerState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class ChallengeHandler(
    private val challengeTimer: Timer,
    private val sendChallengeUseCase: SendChallengeUseCase,
    private val updateChallengerStatusUseCase: UpdateChallengerStatusUseCase,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
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

    fun handleChallengeUpdateAndGetState(
        previousChallengeState: ChallengeState?,
        updatedChallenge: Challenge?,
        userId: String,
    ): ChallengeState? =
        when {
            previousChallengeState == null && updatedChallenge != null ->
                handleChallengeStart(userId = userId, updatedChallenge = updatedChallenge)

            previousChallengeState != null && updatedChallenge != null ->
                handleChallengeUpdate(
                    previousChallengeState = previousChallengeState,
                    updatedChallenge = updatedChallenge,
                    userId = userId,
                )

            previousChallengeState != null && updatedChallenge == null -> handleChallengeFinish()

            else -> null
        }

    suspend fun updateChallengerStatus(challengeId: String, status: ChallengerStatus) {
        challengeTimer.stopTimer()
        updateChallengerStatusUseCase(challengeId = challengeId, status = status)
    }

    private fun handleChallengeStart(userId: String, updatedChallenge: Challenge): ChallengeState {
        val isUserInstigator = updatedChallenge.isUserInstigator(userId = userId)
        val secondsRemaining = updatedChallenge.secondsRemaining(nowMillis = now())

        challengeTimer.startTimer(
            timerDuration = secondsRemaining,
            callOnTimerExpiration = !isUserInstigator,
        )

        return updatedChallenge.toChallengeState(
            isUserInstigator = isUserInstigator,
            secondsRemaining = secondsRemaining,
            showChallengeDialog = true,
        )
    }

    private fun handleChallengeUpdate(
        previousChallengeState: ChallengeState,
        updatedChallenge: Challenge,
        userId: String,
    ): ChallengeState {
        val isUserInstigator = updatedChallenge.isUserInstigator(userId = userId)
        val isChallengeReadyToStart = updatedChallenge.isReadyToStart(userId = userId)

        return if (isChallengeReadyToStart) {
            updatedChallenge.toChallengeState(
                isUserInstigator = isUserInstigator,
                isReadyToStart = true,
            )
        } else {
            updatedChallenge.toChallengeState(
                isUserInstigator = isUserInstigator,
                showChallengeDialog = previousChallengeState.showChallengeDialog,
                secondsRemaining = previousChallengeState.secondsRemaining,
            )
        }
    }

    private fun handleChallengeFinish(): ChallengeState? {
        challengeTimer.stopTimer()
        return null
    }
}
