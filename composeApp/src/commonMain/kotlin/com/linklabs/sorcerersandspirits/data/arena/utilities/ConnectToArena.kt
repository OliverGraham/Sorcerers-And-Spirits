package com.linklabs.sorcerersandspirits.data.arena.utilities

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaPlayerEntity
import com.linklabs.sorcerersandspirits.nullOnTransactionDecodeCrashSerializer
import dev.gitlive.firebase.database.DatabaseReference

/**
 * Connects a user to the arena. Uses a transaction so the first player initializes the match, and
 * so gets the honor of being first in the game.
 */
class ConnectToArena(
    private val arenaReference: DatabaseReference,
    private val expectedNumberOfPlayers: Int,
    private val userId: String,
    private val displayName: String,
) {
    suspend operator fun invoke(): Boolean =
        try {
            val strategy =
                nullOnTransactionDecodeCrashSerializer(serializer = ArenaMatchEntity.serializer())

            arenaReference.runTransaction(strategy = strategy) { existingMatch ->
                existingMatch.connectToArenaTransaction()
            }
            true
        } catch (_: Exception) {
            false
        }

    private fun ArenaMatchEntity?.connectToArenaTransaction(): ArenaMatchEntity {
        val arenaMatchEntity = this ?: ArenaMatchEntity.WaitingForAllPlayers()

        return when (arenaMatchEntity) {
            is ArenaMatchEntity.WaitingForAllPlayers -> arenaMatchEntity.connectToArenaTransaction()

            is ArenaMatchEntity.ChoosingClasses,
            is ArenaMatchEntity.Combat,
            is ArenaMatchEntity.Finished -> arenaMatchEntity
        }
    }

    private fun ArenaMatchEntity.WaitingForAllPlayers.connectToArenaTransaction():
        ArenaMatchEntity {
        val updatedPlayers = playerEntitiesMap.toMutableMap()

        if (userId !in updatedPlayers) {
            updatedPlayers[userId] =
                ArenaPlayerEntity.Unselected(displayName = displayName, userId = userId)
        }

        // TODO Arena: Add messages for connecting here?
        return if (updatedPlayers.size == expectedNumberOfPlayers) {
            ArenaMatchEntity.ChoosingClasses(
                playerEntitiesMap = updatedPlayers,
                activePlayerUserId = userId,
                messages = messages,
                numberOfNewMessages = 0,
            )
        } else {
            copy(playerEntitiesMap = updatedPlayers)
        }
    }
}
