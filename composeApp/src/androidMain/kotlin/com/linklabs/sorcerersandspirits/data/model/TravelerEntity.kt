package com.linklabs.sorcerersandspirits.data.model

import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import kotlinx.serialization.Serializable

@Serializable
data class TravelerEntity(
    val userId: String = "",
    val displayName: String = "",
    val tavernStatus: TavernStatus = TavernStatus.Available,
)

fun TravelerEntity.toDomain(): Traveler =
    Traveler(userId = userId, displayName = displayName, tavernStatus = tavernStatus)
