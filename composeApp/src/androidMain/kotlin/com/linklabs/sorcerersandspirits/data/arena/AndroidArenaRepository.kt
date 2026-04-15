package com.linklabs.sorcerersandspirits.data.arena

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Transaction
import com.google.firebase.database.getValue
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.ARENA_MATCHES_PATH
import com.linklabs.sorcerersandspirits.data.getValueFlow
import com.linklabs.sorcerersandspirits.data.runTransactionAwait
import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

// TODO: After implementing class definitions, add more logic here

class AndroidArenaRepository(database: FirebaseDatabase) : ArenaRepository {

    private val arenaMatchesReference = database.reference.child(ARENA_MATCHES_PATH)

    /**
     * Connects a user to the arena. Uses a transaction so the first player initializes the match.
     */
    override suspend fun connectToArena(arenaId: String, userId: String) {
        arenaMatchesReference.child(arenaId).runTransactionAwait { currentData ->
            val match =
                currentData.getValue<ArenaMatchEntity>()
                    ?: ArenaMatchEntity(arenaId = arenaId, phase = ArenaPhase.WaitingForAllPlayers)

            val updatedPlayers = match.playerEntitiesMap.toMutableMap()

            if (!updatedPlayers.containsKey(userId)) {
                updatedPlayers[userId] = ArenaPlayerEntity(userId = userId)

                // Optional: Automatically log the joining event here if desired
                // match.logs.put(UUID.randomUUID(), ArenaLogEntity(...))
            }

            currentData.value = match.copy(playerEntitiesMap = updatedPlayers)
            Transaction.success(currentData)
        }
    }

    /** Updates the game phase using the Enum directly. */
    suspend fun updateArenaPhase(arenaId: String, phase: ArenaPhase) {
        arenaMatchesReference.child(arenaId).child("phase").setValue(phase)
    }

    /** Updates a player's specific state (e.g., picking a class or taking damage). */
    suspend fun updatePlayerState(
        arenaId: String,
        userId: String,
        update: (ArenaPlayerEntity) -> ArenaPlayerEntity,
    ) {
        arenaMatchesReference
            .child(arenaId)
            .child("playerEntitiesMap")
            .child(userId)
            .runTransactionAwait { currentData ->
                val player =
                    currentData.getValue<ArenaPlayerEntity>()
                        ?: return@runTransactionAwait Transaction.abort()

                currentData.value = update(player)
                Transaction.success(currentData)
            }
    }

    /** Adds a log entry (Attack, Class chosen, etc.) to the match. */
    suspend fun addLogEntry(arenaId: String, log: ArenaLogEntity) {
        arenaMatchesReference.child(arenaId).child("logs").push().setValue(log)
    }

    /** Streams the match document to all subscribers. */
    fun getArenaMatchFlow(arenaId: String): Flow<ArenaMatchEntity?> =
        arenaMatchesReference.child(arenaId).getValueFlow()
}

@Serializable
data class ArenaMatchEntity(
    val arenaId: String = "",
    val phase: ArenaPhase = ArenaPhase.WaitingForAllPlayers,
    val activePlayerId: String? = null,
    val playerEntitiesMap: Map<String, ArenaPlayerEntity> = emptyMap(),
    val logs: Map<String, ArenaLogEntity> = emptyMap(),
)

@Serializable data class ArenaPlayerEntity(val userId: String = "", val displayName: String = "")

@Serializable
data class ArenaLogEntity(
    val type: String = "", // Matches the sealed interface types in ViewModel
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)

enum class ArenaPhase {
    WaitingForAllPlayers,
    ChoosingClasses,
    Combat,
    Finished,
}
