package com.linklabs.sorcerersandspirits.ui.screens.tavern

import com.linklabs.sorcerersandspirits.domain.tavern.ArenaChallenge
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendArenaChallengesUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateArenaChallengeStatusUseCase
import com.linklabs.sorcerersandspirits.domain.traveler.InviteStatus
import com.linklabs.sorcerersandspirits.domain.traveler.TravelerStatus
import com.linklabs.sorcerersandspirits.domain.utilities.indexOfFirstOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class ArenaChallengeHandler(
    private val coroutineScope: CoroutineScope,
    private val updateArenaChallengeStatusUseCase: UpdateArenaChallengeStatusUseCase,
    private val sendArenaChallengesUseCase: SendArenaChallengesUseCase,
    private val updateTavernState: ((TavernState) -> TavernState) -> Unit,
    private val onEvent: (TavernEvent) -> Unit,
) {
    private var timerJob: Job? = null

    fun sendArenaChallenges(
        userTraveler: TravelerState,
        selectedTravelerStates: Set<TravelerState>,
    ) {
        val sender =
            userTraveler.toTraveler(
                status =
                    TravelerStatus.InvitedToArena(
                        isSenderOfInvite = true,
                        inviteStatus = InviteStatus.Accepted,
                    )
            )
        val selectedTravelers =
            selectedTravelerStates.map {
                it.toTraveler(
                    status =
                        TravelerStatus.InvitedToArena(
                            isSenderOfInvite = false,
                            inviteStatus = InviteStatus.Considering,
                        )
                )
            }

        coroutineScope.launch {
            sendArenaChallengesUseCase(travelers = listOf(sender) + selectedTravelers)
        }
    }

    /**
     * Processes a new [ArenaChallenge]. Encapsulates state mapping, timer logic, and navigation
     * triggers.
     */
    fun handleArenaChallengeUpdate(arenaChallenge: ArenaChallenge, userId: String?) {
        val userTraveler = arenaChallenge.getUserTraveler(userId = userId) ?: return
        val userStatus = userTraveler.status as? TravelerStatus.InvitedToArena ?: return

        val isConsidering = userStatus.inviteStatus == InviteStatus.Considering
        val challengeState =
            arenaChallenge.toArenaChallengeState(
                userId = userId,
                showArenaChallengeDialog = isConsidering,
                secondsRemaining = COUNTDOWN_SECONDS,
            )
        updateTavernState { it.copy(arenaChallenge = challengeState) }

        // 2. Manage Timer
        if (isConsidering && timerJob == null) {
            startTimer(challenge = challengeState)
        }

        // 3. Handle Navigation Event
        when {
            arenaChallenge.hasEveryoneAcceptedTheInvite -> {
                stopTimer()
                updateDialogVisibility(isVisible = false)
                onEvent(TavernEvent.NavigateToArena(arenaId = arenaChallenge.arenaId))
            }
        // TODO: Figure out
        // arenaChallenge.hasEveryoneRespondedToTheInvite -> {

        // }
        }
    }

    fun updateDialogVisibility(isVisible: Boolean) {
        updateTavernState { tavernState ->
            tavernState.updateArenaChallengeDialogVisibility(isVisible = isVisible)
        }
    }

    private fun startTimer(challenge: ArenaChallengeState) {
        stopTimer()

        timerJob =
            coroutineScope.launch {
                var secondsRemaining = COUNTDOWN_SECONDS
                while (secondsRemaining >= 0) {
                    updateTavernState {
                        it.updateArenaChallengeStateSeconds(secondsRemaining = secondsRemaining)
                    }
                    kotlinx.coroutines.delay(timeMillis = 1000L)
                    secondsRemaining--
                }
                declineArenaChallenge(challenge)
            }
    }

    fun acceptArenaChallenge(arenaChallengeState: ArenaChallengeState) {
        updateArenaChallengeStatus(
            arenaChallengeState = arenaChallengeState,
            inviteStatus = InviteStatus.Accepted,
        )
    }

    fun declineArenaChallenge(arenaChallengeState: ArenaChallengeState) {
        updateArenaChallengeStatus(
            arenaChallengeState = arenaChallengeState,
            inviteStatus = InviteStatus.Declined,
        )
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        updateTavernState { it.updateArenaChallengeStateSeconds(secondsRemaining = null) }
    }

    private fun updateArenaChallengeStatus(
        arenaChallengeState: ArenaChallengeState,
        inviteStatus: InviteStatus,
    ) {
        stopTimer()

        val updatedArenaChallengeState =
            arenaChallengeState.updateUserInviteStatus(inviteStatus = inviteStatus) ?: return

        updateArenaChallengeStatusUseCase(
            arenaChallenge = updatedArenaChallengeState.toArenaChallenge()
        )
        updateDialogVisibility(isVisible = false)
    }

    private fun ArenaChallengeState.updateUserInviteStatus(
        inviteStatus: InviteStatus
    ): ArenaChallengeState? {
        val userIndex =
            travelerStates.indexOfFirstOrNull { it.isThisTravelerTheUser } ?: return null

        val userState = travelerStates[userIndex]
        val currentStatus = userState.status as? TravelerStatus.InvitedToArena ?: return null

        val updatedUser = userState.copy(status = currentStatus.copy(inviteStatus = inviteStatus))

        return copy(
            travelerStates = travelerStates.toMutableList().apply { this[userIndex] = updatedUser }
        )
    }

    private fun TavernState.updateArenaChallengeStateSeconds(secondsRemaining: Int?): TavernState {
        val arenaChallenge = arenaChallenge ?: return this
        val updatedArenaChallengeState = arenaChallenge.copy(secondsRemaining = secondsRemaining)

        return copy(arenaChallenge = updatedArenaChallengeState)
    }

    private fun TavernState.updateArenaChallengeDialogVisibility(isVisible: Boolean): TavernState {
        val arenaChallenge = arenaChallenge ?: return this
        val updatedArenaChallengeState = arenaChallenge.copy(showArenaChallengeDialog = isVisible)

        return copy(arenaChallenge = updatedArenaChallengeState)
    }

    companion object {
        private const val COUNTDOWN_SECONDS = 60
    }
}
