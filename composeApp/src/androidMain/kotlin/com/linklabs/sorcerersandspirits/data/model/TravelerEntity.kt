package com.linklabs.sorcerersandspirits.data.model

import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import kotlinx.serialization.Serializable

@Serializable
data class TravelerEntity(
    val id: String = "",
    val displayName: String = "",
) {
    fun toTraveler(): Traveler? {
        val id = id
        if (id.isEmpty()) return null

        val displayName = displayName
        if (displayName.isEmpty()) return null

        return Traveler(
            id = id,
            displayName = displayName,
        )
    }
}

internal fun Traveler.toTravelerEntity() = TravelerEntity(
    id = id,
    displayName = displayName,
)
