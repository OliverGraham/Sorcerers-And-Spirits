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
        // QUESTION (central fork): lobby-phase messages are CROSS-CLIENT and currently accumulate
        // in RTDB (appendCapped), so every already-present player sees "X connected"/"chose Y".
        // If setup writes also become REPLACE (only latest batch) + per-device store, a player only
        // stores batches it witnessed live — so a player who connects 5th never gets "player 1..4
        // connected". "No mid-MATCH joins" holds, but the LOBBY fills at staggered times.
        // Options:
        //   A) Replace everywhere; accept lobby log starts from when each client joined (simplest).
        //   B) Keep append semantics for the lobby/choosing phase; only Combat uses replace.
        //   C) On connect, seed the joining client's store with the current RTDB message list once,
        //      then switch to replace+witnessed thereafter.
        // Which do you want? (Skeleton below is written assuming A / replace, matching combat.)
        // CHANGE: numberOfNewMessages removed; the batch written here is just [connectedMessage].
        // Option B seems like the best user experience. Let's go that route. If it's difficult or
        // very different than what you're doing here, make a ticket and we can take care of it later.
        val updatedMessages =
            messages.appendCapped(newMessages = listOf(element = connectedMessage))

        return if (updatedPlayers.size == expectedNumberOfPlayers) {
            ArenaMatchEntity.ChoosingClasses(
                playerEntitiesMap = updatedPlayers,
                activePlayerUserId = userId,
                messages = updatedMessages,
                matchNumberInGauntlet = matchNumberInGauntlet,
            )
        } else {
            copy(playerEntitiesMap = updatedPlayers, messages = updatedMessages)
        }
    }
}
