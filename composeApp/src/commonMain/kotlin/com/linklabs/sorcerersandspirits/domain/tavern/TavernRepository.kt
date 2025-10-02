package com.linklabs.sorcerersandspirits.domain.tavern

import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import kotlinx.coroutines.flow.Flow

interface TavernRepository {
    fun getTavernFlow(): Flow<Tavern>

    suspend fun newTravelerEntersTavern(traveler: Traveler)

    suspend fun leaveTavern(id: String)
}
