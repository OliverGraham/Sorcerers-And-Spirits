package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.tavern.ArenaChallenge
import com.linklabs.sorcerersandspirits.domain.tavern.Tavern
import com.linklabs.sorcerersandspirits.domain.traveler.GamePlayStatus
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler

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
    val gamePlayStatus: GamePlayStatus,
)

@Immutable
data class ArenaChallengeState(
    val arenaId: String,
    val sender: TravelerState,
    val receivers: List<TravelerState>,
)

// TODO: Reevaluate - what should the UI be?
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
    gamePlayStatus = gamePlayStatus,
)

internal fun ArenaChallenge.toArenaChallengeState() = ArenaChallengeState(
    arenaId = arenaId,
    sender = sender.toTravelerState(isThisTravelerTheUser = false),
    receivers = receivers.map { it.toTravelerState(isThisTravelerTheUser = false) },
)

internal fun ArenaChallengeState.toArenaChallenge() = ArenaChallenge(
    arenaId = arenaId,
    sender = sender.toTraveler(),
    receivers = receivers.map { it.toTraveler() },
)

internal fun TravelerState.toTraveler() = Traveler(
    id = id,
    displayName = displayName,
    gamePlayStatus = gamePlayStatus,
)
