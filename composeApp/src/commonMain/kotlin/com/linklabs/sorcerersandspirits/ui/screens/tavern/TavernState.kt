package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.tavern.ArenaChallenge
import com.linklabs.sorcerersandspirits.domain.tavern.Tavern
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import com.linklabs.sorcerersandspirits.domain.traveler.TravelerStatus

@Immutable
data class TavernState(
    val userTraveler: TravelerState,
    val otherTravelers: List<TravelerState>,
    val arenaChallenge: ArenaChallengeState? = null,
    val showArenaChallengeDialog: Boolean = false,
)

@Immutable
data class TravelerState(
    val id: String,
    val displayName: String,
    val isThisTravelerTheUser: Boolean,
    val status: TravelerStatus,
)

@Immutable
data class ArenaChallengeState(
    val arenaId: String,
    val travelerStates: List<TravelerState>,
) {
    val sender = travelerStates.find { it.status is TravelerStatus.InvitedToArena && it.status.isSenderOfInvite }
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

private fun Traveler.toTravelerState(isThisTravelerTheUser: Boolean) = TravelerState(
    id = id,
    displayName = displayName,
    isThisTravelerTheUser = isThisTravelerTheUser,
    status = status,
)

internal fun TravelerState.toTraveler(status: TravelerStatus) = Traveler(
    id = id,
    displayName = displayName,
    status = status,
)

internal fun ArenaChallenge.toArenaChallengeState(userId: String?) = ArenaChallengeState(
    arenaId = arenaId,
    travelerStates = travelers.map { it.toTravelerState(isThisTravelerTheUser = it.id == userId) },
)

internal fun ArenaChallengeState.toArenaChallenge() = ArenaChallenge(
    arenaId = arenaId,
    travelers = travelerStates.map { it.toTraveler(status = it.status) },
)
