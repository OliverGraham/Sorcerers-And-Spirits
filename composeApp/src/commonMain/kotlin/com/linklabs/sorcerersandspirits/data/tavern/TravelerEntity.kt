package com.linklabs.sorcerersandspirits.data.tavern

import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import kotlinx.serialization.Serializable

@Serializable
data class TravelerEntity(
    val userId: String = "",
    val displayName: String = "",
    val tavernStatus: TavernStatus = TavernStatus.Available,
    val rating: Long = 0,
)

fun TravelerEntity.toDomain(): Traveler = Traveler(
    userId = userId,
    displayName = displayName,
    tavernStatus = tavernStatus,
    rating = rating,
)
