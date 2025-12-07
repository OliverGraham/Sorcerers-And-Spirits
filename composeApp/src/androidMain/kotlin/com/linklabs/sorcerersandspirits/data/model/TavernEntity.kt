package com.linklabs.sorcerersandspirits.data.model

import com.linklabs.sorcerersandspirits.domain.tavern.Tavern
import kotlinx.serialization.Serializable

@Serializable
data class TavernEntity(
    val travelerEntities: List<TravelerEntity>,
) {
    fun toTavern(): Tavern {
        val travelers = travelerEntities.mapNotNull { it.toTraveler() }
        return Tavern(travelers = travelers)
    }
}
