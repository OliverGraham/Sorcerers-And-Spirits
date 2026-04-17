package com.linklabs.sorcerersandspirits.domain.arena

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import kotlinx.coroutines.flow.Flow

interface ArenaRepository {
    suspend fun connectToArena(
        arenaId: String,
        expectedNumberOfPlayers: Int,
        userId: String,
    ): Boolean

    fun getArenaMatchFlow(arenaId: String): Flow<ArenaMatch?>
}
