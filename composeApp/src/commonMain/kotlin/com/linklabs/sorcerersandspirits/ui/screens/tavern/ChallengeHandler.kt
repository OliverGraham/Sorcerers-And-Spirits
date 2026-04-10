package com.linklabs.sorcerersandspirits.ui.screens.tavern

import com.linklabs.sorcerersandspirits.domain.tavern.Challenge
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.SendChallengeResult
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendChallengeUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateChallengerStatusUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class HandleChallengeUpdateResult(
    val challengeState: ChallengeState,
    val tavernEvent: TavernEvent?,
)

internal class ChallengeHandler(
    private val coroutineScope: CoroutineScope,
    private val sendChallengeUseCase: SendChallengeUseCase,
    private val updateChallengerStatusUseCase: UpdateChallengerStatusUseCase,
    private val updateTavernState: ((TavernState) -> TavernState) -> Unit,
) {
    private var timerJob: Job? = null
    private var timerChallengeId: String? = null

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

        val isSameChallenge = previousChallengeState?.challengeId == updatedChallenge.challengeId
        val isPendingInvitee = userChallenger.status == ChallengerStatus.Pending

        val challengeState =
            updatedChallenge.toChallengeState(
                showChallengeDialog =
                    when {
                        isPendingInvitee -> true
                        isSameChallenge -> previousChallengeState.showChallengeDialog
                        else -> false
                    },
                secondsRemaining =
                    when {
                        !isPendingInvitee -> null
                        isSameChallenge ->
                            previousChallengeState.secondsRemaining ?: COUNTDOWN_SECONDS
                        else -> COUNTDOWN_SECONDS
                    },
            )

        val shouldNavigateToArena =
            userChallenger.status == ChallengerStatus.Accepted &&
                challengeState.hasEveryoneAccepted()

        val tavernEvent =
            when {
                shouldNavigateToArena -> {
                    stopTimer()
                    updateDialogVisibility(isVisible = false)

                    TavernEvent.NavigateToArena(arenaId = updatedChallenge.challengeId)
                }

                isPendingInvitee && timerChallengeId != updatedChallenge.challengeId -> {
                    startTimer(challengeId = updatedChallenge.challengeId, userId = userId)
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
