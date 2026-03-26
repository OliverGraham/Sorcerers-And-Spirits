package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.tavern.ArenaChallenge
import com.linklabs.sorcerersandspirits.domain.tavern.Tavern
import com.linklabs.sorcerersandspirits.domain.traveler.InviteStatus
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import com.linklabs.sorcerersandspirits.domain.traveler.TravelerStatus

@Immutable
data class TavernState(
    val userTraveler: TravelerState,
    val otherTravelers: List<TravelerState>,
    val arenaChallenge: ArenaChallengeState? = null,
)

@Immutable
data class TravelerState(
    val id: String,
    val displayName: String,
    val isThisTravelerTheUser: Boolean,
    val status: TravelerStatus,
) {
    val inviteStatus = (status as? TravelerStatus.InvitedToArena)?.inviteStatus
}

@Immutable
data class ArenaChallengeState(
    val arenaId: String,
    val travelerStates: List<TravelerState>,
    val isChallengeInvitationComplete: Boolean = false,
    val canArenaCombatBegin: Boolean,
    val hasUserTravelerAccepted: Boolean,
    val showArenaChallengeDialog: Boolean = false,
    val secondsRemaining: Int? = null,
) {
    val sender =
        travelerStates.find {
            it.status is TravelerStatus.InvitedToArena && it.status.isSenderOfInvite
        }

    val userTravelerState = travelerStates.find { it.isThisTravelerTheUser }
}

internal fun Tavern.toTavernState(userId: String?): TavernState? {
    val (userTravelers, otherTravelers) = travelers.partition { it.id == userId }
    val userTraveler =
        userTravelers.firstOrNull()?.toTravelerState(isThisTravelerTheUser = true) ?: return null

    return TavernState(
        userTraveler = userTraveler,
        otherTravelers = otherTravelers.map { it.toTravelerState(isThisTravelerTheUser = false) },
    )
}

private fun Traveler.toTravelerState(isThisTravelerTheUser: Boolean) =
    TravelerState(
        id = id,
        displayName = displayName,
        isThisTravelerTheUser = isThisTravelerTheUser,
        status = status,
    )

internal fun TravelerState.toTraveler(status: TravelerStatus) =
    Traveler(id = id, displayName = displayName, status = status)

internal fun ArenaChallenge.toArenaChallengeState(userId: String?): ArenaChallengeState {
    val travelerStates =
        travelers.map { it.toTravelerState(isThisTravelerTheUser = it.id == userId) }
    val userTravelerState = travelerStates.find { it.isThisTravelerTheUser }

    return ArenaChallengeState(
        arenaId = arenaId,
        travelerStates = travelerStates,
        isChallengeInvitationComplete = isChallengeInvitationComplete,
        canArenaCombatBegin = canArenaCombatBegin,
        hasUserTravelerAccepted = userTravelerState?.inviteStatus == InviteStatus.Accepted,
    )
}

internal fun ArenaChallengeState.toArenaChallenge() =
    ArenaChallenge(
        arenaId = arenaId,
        travelers = travelerStates.map { it.toTraveler(status = it.status) },
    )
