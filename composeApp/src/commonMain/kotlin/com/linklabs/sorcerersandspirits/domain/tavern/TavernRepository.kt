package com.linklabs.sorcerersandspirits.domain.tavern

import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import kotlinx.coroutines.flow.Flow

interface TavernRepository {
    fun getTavernFlow(): Flow<Tavern>

    fun aTravelerEntersTavern(traveler: Traveler)

    suspend fun sendArenaChallenges(sender: Traveler, receivers: List<Traveler>)

    fun getArenaChallengeFlow(userId: String): Flow<ArenaChallenge>

    fun updateArenaChallengeStatus(arenaChallenge: ArenaChallenge)
}

/**
 * Copy of [TavernRepository], excluding the Flow and suspend functions (which iOS can't handle).
 */
interface TavernRepositoryCopyForIOS {
    fun aTravelerEntersTavern(traveler: Traveler)
}
