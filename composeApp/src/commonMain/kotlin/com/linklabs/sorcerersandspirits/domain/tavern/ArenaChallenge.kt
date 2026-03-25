package com.linklabs.sorcerersandspirits.domain.tavern

import com.linklabs.sorcerersandspirits.domain.traveler.InviteStatus
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import com.linklabs.sorcerersandspirits.domain.traveler.TravelerStatus

data class ArenaChallenge(
    val arenaId: String,
    val travelers: List<Traveler>,
) {
    val hasEveryoneAcceptedTheInvite =
        travelers.all {
            it.status is TravelerStatus.InvitedToArena && it.status.inviteStatus == InviteStatus.Accepted
        }

    fun getUserTraveler(userId: String?) = travelers.find { it.id == userId }
}
