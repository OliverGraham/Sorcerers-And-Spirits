package com.linklabs.sorcerersandspirits.data.arena

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.getValue
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.ARENA_MATCHES_PATH
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMessageEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMessageType
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaPlayerEntity
import com.linklabs.sorcerersandspirits.data.arena.model.toDomain
import com.linklabs.sorcerersandspirits.data.arena.model.toEntity
import com.linklabs.sorcerersandspirits.data.getValueFlow
import com.linklabs.sorcerersandspirits.data.runTransactionAwait
import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPhase
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// TODO Arena: Update the slop

class AndroidArenaRepository(database: FirebaseDatabase) : ArenaRepository {

    private val arenaMatchesReference = database.reference.child(ARENA_MATCHES_PATH)

    // TODO Arena: Cleanup, docstring
    // Determine the next phase and active player
    // If nextPlayerId is null, it means everyone has picked a class
    override suspend fun chooseCharacterClass(
        arenaId: String,
        arenaPlayer: ArenaPlayer,
        nextPlayerId: String?,
    ) {
        arenaMatchesReference.child(arenaId).runTransactionAwait { currentData ->
            currentData.chooseCharacterClassTransaction(
                arenaPlayer = arenaPlayer,
                nextPlayerId = nextPlayerId,
            )
        }
    }

    private fun MutableData.chooseCharacterClassTransaction(
        arenaPlayer: ArenaPlayer,
        nextPlayerId: String?,
    ): Transaction.Result {
        val match = getValue<ArenaMatchEntity>() ?: return Transaction.abort()

        val updatedPlayers = match.playerEntitiesMap.toMutableMap()
        updatedPlayers[arenaPlayer.userId] = arenaPlayer.toEntity()

        val nextPhase = if (nextPlayerId == null) ArenaPhase.Combat else ArenaPhase.ChoosingClasses

        val (finalActivePlayerUserId, updatedMessages) =
            generateCharacterSelectionMessages(
                match = match,
                updatedPlayers = updatedPlayers,
                arenaPlayer = arenaPlayer,
                nextPhase = nextPhase,
                nextPlayerId = nextPlayerId,
            )

        val updatedMatch =
            match.copy(
                playerEntitiesMap = updatedPlayers,
                activePlayerUserId = finalActivePlayerUserId,
                phase = nextPhase,
                messages = updatedMessages,
            )

        this.value = updatedMatch
        return Transaction.success(this)
    }

    private fun generateCharacterSelectionMessages(
        match: ArenaMatchEntity,
        updatedPlayers: Map<String, ArenaPlayerEntity>,
        arenaPlayer: ArenaPlayer,
        nextPhase: ArenaPhase,
        nextPlayerId: String?,
    ): Pair<String?, List<ArenaMessageEntity>> {
        val messagesReference =
            arenaMatchesReference.child(match.arenaId).child(ArenaMatchEntity.messagesName)

        val selectionMessage =
            ArenaMessageEntity(
                id = messagesReference.addNewKey(),
                type = ArenaMessageType.Generic,
                message =
                    "${arenaPlayer.displayName} selected ${arenaPlayer.characterClass?.className.orEmpty()}",
            )

        var finalActivePlayerUserId = nextPlayerId

        val transitionMessages =
            if (nextPhase == ArenaPhase.Combat) {
                val firstPlayer =
                    updatedPlayers.values.maxByOrNull { it.characterClassEntity?.speed ?: 0 }
                finalActivePlayerUserId = firstPlayer?.userId

                buildList {
                    if (firstPlayer != null) {
                        val className = firstPlayer.characterClassEntity?.className ?: "Player"
                        add(
                            ArenaMessageEntity(
                                id = messagesReference.addNewKey(),
                                type = ArenaMessageType.Generic,
                                message = "It is now $className's turn!",
                            )
                        )
                    }
                    add(
                        ArenaMessageEntity(
                            id = messagesReference.addNewKey(),
                            type = ArenaMessageType.Round,
                            roundNumber = 1,
                        )
                    )
                    add(
                        ArenaMessageEntity(
                            id = messagesReference.addNewKey(),
                            type = ArenaMessageType.Generic,
                            message = "Combat is now starting!",
                        )
                    )
                }
            } else {
                emptyList()
            }

        return finalActivePlayerUserId to (transitionMessages + selectionMessage + match.messages)
    }

    /**
     * Connects a user to the arena. Uses a transaction so the first player initializes the match,
     * and so gets the honor of being first in the game.
     */
    override suspend fun connectToArena(
        arenaId: String,
        expectedNumberOfPlayers: Int,
        userId: String,
        displayName: String,
    ) =
        arenaMatchesReference.child(arenaId).runTransactionAwait { currentData ->
            val match =
                currentData.getValue<ArenaMatchEntity>()
                    ?: ArenaMatchEntity(
                        arenaId = arenaId,
                        phase = ArenaPhase.WaitingForAllPlayers,
                        activePlayerUserId = userId,
                    )

            val updatedPlayers = match.playerEntitiesMap.toMutableMap()

            if (userId !in updatedPlayers) {
                updatedPlayers[userId] =
                    ArenaPlayerEntity(displayName = displayName, userId = userId)

                // Optional: Automatically log the joining event here if desired
                // match.logs.put(UUID.randomUUID(), ArenaLogEntity(...))
            }

            val arenaPhase =
                if (updatedPlayers.size == expectedNumberOfPlayers) {
                    ArenaPhase.ChoosingClasses
                } else {
                    ArenaPhase.WaitingForAllPlayers
                }

            currentData.value = match.copy(playerEntitiesMap = updatedPlayers, phase = arenaPhase)
            Transaction.success(currentData)
        }

    /** Updates the game phase using the Enum directly. */
    suspend fun updateArenaPhase(arenaId: String, phase: ArenaPhase) {
        arenaMatchesReference.child(arenaId).child("phase").setValue(phase)
    }

    /** Updates a player's specific state (e.g., picking a class or taking damage). */
    override suspend fun updateArenaPlayer(arenaId: String, arenaPlayer: ArenaPlayer) {
        arenaMatchesReference
            .child(arenaId)
            .child(ArenaMatchEntity.playerEntitiesMapName)
            .child(arenaPlayer.userId)
            .runTransactionAwait { currentData ->
                currentData.value = arenaPlayer.toEntity()
                Transaction.success(currentData)
            }
    }

    /** Streams the match document to all subscribers. */
    override fun getArenaMatchFlow(arenaId: String): Flow<ArenaMatch?> =
        arenaMatchesReference.child(arenaId).getValueFlow<ArenaMatchEntity>().map { entity ->
            entity?.toDomain()
        }

    private fun DatabaseReference.addNewKey() = push().key.orEmpty()
}
