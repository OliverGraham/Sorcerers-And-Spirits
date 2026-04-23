package com.linklabs.sorcerersandspirits.domain.arena

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import kotlinx.coroutines.flow.Flow

interface ArenaRepository {

    suspend fun chooseCharacterClass(
        arenaId: String,
        arenaPlayer: ArenaPlayer,
        nextPlayerId: String?,
    )

    suspend fun updateArenaPlayer(arenaId: String, arenaPlayer: ArenaPlayer)

    suspend fun connectToArena(
        arenaId: String,
        expectedNumberOfPlayers: Int,
        userId: String,
    ): Boolean

    fun getArenaMatchFlow(arenaId: String): Flow<ArenaMatch?>
}
