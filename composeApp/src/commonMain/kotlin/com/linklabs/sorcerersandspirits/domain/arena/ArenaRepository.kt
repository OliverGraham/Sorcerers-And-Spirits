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

    // NOTE: this flow now emits an ArenaMatch whose `messages` = latest RTDB batch and `newMessages`
    // = same batch; the full accumulated log is layered on in CoordinateArenaMatchUseCase via the
    // on-device store (see ArenaMessageStore). This interface method is otherwise unchanged.
    // No, this should not happen, and is the type of thing I want to avoid. See my note in ArenaMatchEntity.
    fun getArenaMatchFlow(arenaId: String): Flow<ArenaMatch?>

    suspend fun exitArena(arenaId: String, userId: String)

    suspend fun stayForAnotherMatch(arenaId: String, userId: String)

    suspend fun finishMatch(arenaId: String)
}
