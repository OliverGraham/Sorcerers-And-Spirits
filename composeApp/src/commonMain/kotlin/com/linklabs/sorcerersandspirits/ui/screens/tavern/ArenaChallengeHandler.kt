package com.linklabs.sorcerersandspirits.ui.screens.tavern

import com.linklabs.sorcerersandspirits.domain.tavern.ArenaChallenge
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.RemoveArenaChallengeUseCase
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
    private val removeArenaChallengeUseCase: RemoveArenaChallengeUseCase,
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

    fun declineGhostedParticipants(challenge: ArenaChallenge, ghostedIds: List<String>) {
        coroutineScope.launch {
            var needsUpdate = false
            val updatedTravelers =
                challenge.travelers.map { traveler ->
                    val currentStatus =
                        traveler.status as? TravelerStatus.InvitedToArena ?: return@map traveler

                    // If they are a ghost AND they haven't been marked as Declined yet
                    if (
                        traveler.id in ghostedIds &&
                            currentStatus.inviteStatus != InviteStatus.Declined
                    ) {
                        needsUpdate = true
                        traveler.copy(
                            status = currentStatus.copy(inviteStatus = InviteStatus.Declined)
                        )
                    } else {
                        traveler
                    }
                }

            if (needsUpdate) {
                // This updates Firebase. Once this hits the server,
                // the UseCase will see it's "Complete" and delete it.
                updateArenaChallengeStatusUseCase(challenge.copy(travelers = updatedTravelers))
            }
        }
    }

    fun getUpdatedArenaState(
        newArenaChallenge: ArenaChallenge,
        oldArenaChallengeState: ArenaChallengeState?,
        userId: String?,
    ): ArenaChallengeState? {
        val newArenaChallengeState = newArenaChallenge.toArenaChallengeState(userId = userId)
        val userTraveler = newArenaChallengeState.userTravelerState ?: return null

        // Preserve existing seconds remaining if timer is already running
        val isConsidering = userTraveler.inviteStatus == InviteStatus.Considering
        val isDialogAlreadyOpen = oldArenaChallengeState?.showArenaChallengeDialog ?: false

        return newArenaChallengeState.copy(
            showArenaChallengeDialog = isConsidering || isDialogAlreadyOpen,
            secondsRemaining = oldArenaChallengeState?.secondsRemaining,
        )
    }

    fun processArenaChallengeEffects(arenaChallengeState: ArenaChallengeState) {
        val userTraveler = arenaChallengeState.userTravelerState ?: return
        val isConsidering = userTraveler.inviteStatus == InviteStatus.Considering

        // Trigger Timer
        if (isConsidering && timerJob == null) {
            startTimer(challenge = arenaChallengeState)
        }

        // Trigger RSVPs / Navigation
        arenaChallengeState.handleRSVPs()
    }

    /**
     * Processes a new [ArenaChallenge]. Encapsulates state mapping, timer logic, and navigation
     * triggers.
     */
    fun handleArenaChallengeUpdate(arenaChallenge: ArenaChallenge, userId: String?) {
        /* val arenaChallengeState = arenaChallenge.toArenaChallengeState(userId = userId)
        val userTraveler = arenaChallengeState.userTravelerState ?: return
        val isConsidering = userTraveler.inviteStatus == InviteStatus.Considering

        updateTavernState { tavernState ->
            val isDialogAlreadyOpen = tavernState.arenaChallenge?.showArenaChallengeDialog ?: false
            tavernState.copy(
                arenaChallenge = arenaChallengeState.copy(
                    showArenaChallengeDialog = isConsidering || isDialogAlreadyOpen
                )
            )
        }

        if (isConsidering && timerJob == null) {
            startTimer(challenge = arenaChallengeState)
        }

        arenaChallengeState.handleRSVPs()*/
    }

    private fun ArenaChallengeState.handleRSVPs() {
        if (!isChallengeInvitationComplete) return

        terminateAndCleanup()

        when {
            canArenaCombatBegin && hasUserTravelerAccepted -> {
                onEvent(TavernEvent.NavigateToArena(arenaId = arenaId))
            }
            !canArenaCombatBegin -> {
                println("NOT ENOUGH PEOPLE YO $this")
                // TODO: show snack bar saying not enough people accepted
            }
        }
    }

    private fun ArenaChallengeState.terminateAndCleanup() {
        stopTimer()
        updateDialogVisibility(isVisible = false)

        //  removeArenaChallengeUseCase(arenaChallenge = toArenaChallenge())
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
