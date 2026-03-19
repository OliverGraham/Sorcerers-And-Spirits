package com.linklabs.sorcerersandspirits.data.model

import com.linklabs.sorcerersandspirits.domain.tavern.ArenaChallenge
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengeStatus
import kotlinx.serialization.Serializable

@Serializable
data class ArenaChallengeEntity(
    val arenaId: String = "",
    val sender: TravelerEntity = TravelerEntity(),
    val receivers: List<TravelerEntity> = emptyList(),
    val status: ChallengeStatus = ChallengeStatus.PENDING,
) {
    fun toArenaChallenge(): ArenaChallenge? {
        if (arenaId.isEmpty()) return null

        val senderDomain = sender.toTraveler() ?: return null

        val receiversDomain = receivers.mapNotNull { it.toTraveler() }
        if (receiversDomain.isEmpty()) return null

        return ArenaChallenge(
            arenaId = arenaId,
            sender = senderDomain,
            receivers = receiversDomain,
            status = status,
        )
    }
}
