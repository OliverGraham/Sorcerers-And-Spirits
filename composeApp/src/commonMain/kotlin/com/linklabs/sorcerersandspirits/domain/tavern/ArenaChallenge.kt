package com.linklabs.sorcerersandspirits.domain.tavern

import com.linklabs.sorcerersandspirits.domain.traveler.Traveler

data class ArenaChallenge(
    val arenaId: String,
    val sender: Traveler,
    val receivers: List<Traveler>,
    val status: ChallengeStatus = ChallengeStatus.PENDING,
)

enum class ChallengeStatus { PENDING, ACCEPTED, DECLINED }
