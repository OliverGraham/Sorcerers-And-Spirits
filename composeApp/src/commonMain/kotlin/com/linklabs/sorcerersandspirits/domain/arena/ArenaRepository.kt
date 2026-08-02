package com.linklabs.sorcerersandspirits.domain.arena

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ActionOutcome
import kotlinx.coroutines.flow.Flow

interface ArenaRepository {

    suspend fun chooseCharacterClass(
        arenaId: String,
        selectedArenaPlayer: ArenaPlayer.Selected,
        nextPlayerId: String,
    )

    suspend fun updateArenaPlayer(arenaId: String, arenaPlayer: ArenaPlayer)

    suspend fun applyCombatResult(arenaId: String, result: ActionOutcome)

    suspend fun connectToArena(
        arenaId: String,
        expectedNumberOfPlayers: Int,
        userId: String,
        displayName: String,
    ): Boolean

    fun getArenaMatchFlow(arenaId: String): Flow<ArenaMatch?>

    suspend fun exitArena(arenaId: String, userId: String)

    suspend fun stayForAnotherMatch(arenaId: String, userId: String)

    suspend fun finishMatch(arenaId: String)
}
