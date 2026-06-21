package com.linklabs.sorcerersandspirits.data.arena.utilities

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMessageEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaPlayerEntity
import com.linklabs.sorcerersandspirits.nullOnTransactionDecodeCrashSerializer
import dev.gitlive.firebase.database.DatabaseReference

/**
 * Connects a user to the arena. Uses a transaction so the first player initializes the match, and
 * so gets the honor of being first in the game.
 */
internal class ConnectToArena(
    private val expectedNumberOfPlayers: Int,
    private val userId: String,
    private val displayName: String,
) {
    suspend operator fun invoke(arenaReference: DatabaseReference): Boolean =
        try {
            val strategy =
                nullOnTransactionDecodeCrashSerializer(serializer = ArenaMatchEntity.serializer())
            val messagesReference = arenaReference.child(path = ArenaMatchEntity.messagesName)

            arenaReference.runTransaction(strategy = strategy) { existingMatch ->
                existingMatch.connectToArenaTransaction(messagesReference = messagesReference)
            }
            true
        } catch (_: Exception) {
            false
        }

    private fun ArenaMatchEntity?.connectToArenaTransaction(
        messagesReference: DatabaseReference
    ): ArenaMatchEntity {
        val arenaMatchEntity = this ?: ArenaMatchEntity.WaitingForAllPlayers()

        return when (arenaMatchEntity) {
            is ArenaMatchEntity.WaitingForAllPlayers ->
                arenaMatchEntity.connectToArenaTransaction(messagesReference = messagesReference)

            is ArenaMatchEntity.ChoosingClasses,
            is ArenaMatchEntity.Combat,
            is ArenaMatchEntity.Finished -> arenaMatchEntity
        }
    }

    private fun ArenaMatchEntity.WaitingForAllPlayers.connectToArenaTransaction(
        messagesReference: DatabaseReference
    ): ArenaMatchEntity {
        val isAlreadyConnected = userId in playerEntitiesMap
        if (isAlreadyConnected) return this

        val updatedPlayers = playerEntitiesMap.toMutableMap()
        updatedPlayers[userId] =
            ArenaPlayerEntity.Unselected(displayName = displayName, userId = userId)

        val connectedMessage =
            ArenaMessageEntity.ConnectedToArena(
                id = messagesReference.addNewKey(),
                displayName = displayName,
            )
        val updatedMessages =
            messages.appendCapped(newMessages = listOf(element = connectedMessage))

        return if (updatedPlayers.size == expectedNumberOfPlayers) {
            ArenaMatchEntity.ChoosingClasses(
                playerEntitiesMap = updatedPlayers,
                activePlayerUserId = userId,
                messages = updatedMessages,
                numberOfNewMessages = 1,
                matchNumberInGauntlet = matchNumberInGauntlet,
            )
        } else {
            copy(
                playerEntitiesMap = updatedPlayers,
                messages = updatedMessages,
                numberOfNewMessages = numberOfNewMessages + 1,
            )
        }
    }
}
