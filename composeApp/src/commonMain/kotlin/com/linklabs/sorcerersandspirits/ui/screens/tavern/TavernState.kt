package com.linklabs.sorcerersandspirits.ui.screens.tavern

import com.linklabs.sorcerersandspirits.domain.tavern.Tavern
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler

data class TavernState(
    val userTraveler: TravelerState,
    val otherTravelers: List<TravelerState>,
)

data class TravelerState(
    val id: String,
    val displayName: String,
    val isThisTravelerTheUser: Boolean,
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
)

internal fun TravelerState.toTraveler() = Traveler(
    id = id,
    displayName = displayName,
)
