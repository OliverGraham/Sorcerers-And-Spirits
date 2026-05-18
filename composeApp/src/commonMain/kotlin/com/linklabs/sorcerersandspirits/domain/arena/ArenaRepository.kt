package com.linklabs.sorcerersandspirits.domain.arena

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CombatResult
import kotlinx.coroutines.flow.Flow

interface ArenaRepository {

    suspend fun chooseCharacterClass(
        arenaId: String,
        selectedArenaPlayer: ArenaPlayer.Selected,
        nextPlayerId: String,
    )

    suspend fun updateArenaPlayer(arenaId: String, arenaPlayer: ArenaPlayer)

    suspend fun applyCombatResult(arenaId: String, result: CombatResult)

    suspend fun connectToArena(
        arenaId: String,
        expectedNumberOfPlayers: Int,
        userId: String,
        displayName: String,
    ): Boolean

    fun getArenaMatchFlow(arenaId: String): Flow<ArenaMatch?>
}
