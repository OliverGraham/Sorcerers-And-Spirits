package com.linklabs.sorcerersandspirits.domain.tavern

import com.linklabs.sorcerersandspirits.domain.traveler.GamePlayStatus
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler

data class ArenaChallenge(
    val arenaId: String,
    val sender: Traveler,
    val receivers: List<Traveler>,
) {
    val hasEveryoneAcceptedTheInvite =
        receivers.all { it.gamePlayStatus == GamePlayStatus.AcceptedInvite }

    val allParticipantIds = receivers.map { it.id } + sender.id

    fun getUserTraveler(userId: String?) = receivers.find { it.id == userId } ?: sender
}
