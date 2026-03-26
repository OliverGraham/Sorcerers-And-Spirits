package com.linklabs.sorcerersandspirits.domain.tavern

import com.linklabs.sorcerersandspirits.domain.traveler.Traveler

data class ArenaChallenge(val arenaId: String, val travelers: List<Traveler>) {
    val totalParticipants = travelers.size
    val acceptedCount = travelers.count { it.hasAcceptedInviteToArena }
    val declinedCount = travelers.count { it.hasDeclinedInviteToArena }

    val canArenaCombatBegin =
        (acceptedCount + declinedCount == totalParticipants) && acceptedCount >= 2

    val notEnoughPlayers = (totalParticipants - declinedCount) < 2

    val isChallengeInvitationComplete = canArenaCombatBegin || notEnoughPlayers
}
