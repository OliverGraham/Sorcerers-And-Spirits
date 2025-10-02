package com.linklabs.sorcerersandspirits.domain.traveler

interface TravelerRepository {

    suspend fun saveTraveler(traveler: Traveler)

    suspend fun getTraveler(id: String): Traveler?

    suspend fun deleteTraveler(id: String)

    suspend fun updateDisplayName(id: String, displayName: String)
}
