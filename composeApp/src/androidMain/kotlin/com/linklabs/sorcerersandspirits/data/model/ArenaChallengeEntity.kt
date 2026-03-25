package com.linklabs.sorcerersandspirits.data.model

import com.linklabs.sorcerersandspirits.domain.tavern.ArenaChallenge
import kotlinx.serialization.Serializable

@Serializable
internal data class ArenaChallengeEntity(
    val arenaId: String = "",
    val travelerEntityMap: Map<String, TravelerEntity> = emptyMap(),
) {
    fun toArenaChallenge(): ArenaChallenge? {
        if (arenaId.isEmpty()) return null
        if (travelerEntityMap.isEmpty()) return null

        val travelers = travelerEntityMap.values.map { travelerEntity ->
            travelerEntity.toTraveler() ?: return null
        }

        return ArenaChallenge(
            arenaId = arenaId,
            travelers = travelers,
        )
    }

    companion object {
        fun getTravelerEntityMapName() = ArenaChallengeEntity::travelerEntityMap.name
    }
}

internal fun ArenaChallenge.toArenaChallengeEntity() = ArenaChallengeEntity(
    arenaId = arenaId,
    travelerEntityMap = travelers.associate { traveler -> traveler.id to traveler.toTravelerEntity() },
)
