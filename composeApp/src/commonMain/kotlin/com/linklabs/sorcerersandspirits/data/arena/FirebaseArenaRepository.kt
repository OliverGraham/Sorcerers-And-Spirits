package com.linklabs.sorcerersandspirits.data.arena

import com.linklabs.sorcerersandspirits.data.FireStoreKeys.ARENA_MATCHES_PATH
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMessageEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMessageType
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaPhaseEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaPlayerEntity
import com.linklabs.sorcerersandspirits.data.arena.model.toDomain
import com.linklabs.sorcerersandspirits.data.arena.model.toEntity
import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import dev.gitlive.firebase.database.DatabaseReference
import dev.gitlive.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.nullable

// TODO Arena: Update the slop. Paginate messages and/or have a separate node for them to make more
//  efficient. Could also save all messages and get on demand, and only have the latest 30 or
// something

class FirebaseArenaRepository(database: FirebaseDatabase) : ArenaRepository {

    private val arenaMatchesReference = database.reference(path = ARENA_MATCHES_PATH)

    // TODO Arena: Cleanup, docstring
    override suspend fun chooseCharacterClass(
        arenaId: String,
        arenaPlayer: ArenaPlayer,
        nextPlayerId: String?,
    ) {
        arenaMatchesReference.child(path = arenaId).runTransaction(
            strategy = ArenaMatchEntity.serializer()
        ) { match ->
            val updatedPlayers = match.playerEntitiesMap.toMutableMap()
            updatedPlayers[arenaPlayer.userId] = arenaPlayer.toEntity()

            val nextPhase =
                if (nextPlayerId == null) {
                    ArenaPhaseEntity.Combat
                } else {
                    ArenaPhaseEntity.ChoosingClasses
                }

            val (finalActivePlayerUserId, updatedMessages, numberOfNewMessages) =
                generateCharacterSelectionMessages(
                    match = match,
                    updatedPlayers = updatedPlayers,
                    arenaPlayer = arenaPlayer,
                    nextPhase = nextPhase,
                    nextPlayerId = nextPlayerId,
                )

            match.copy(
                playerEntitiesMap = updatedPlayers,
                activePlayerUserId = finalActivePlayerUserId,
                phase = nextPhase,
                messages = updatedMessages,
                numberOfNewMessages = numberOfNewMessages,
            )
        }
    }

    // TODO Arena: return data class
    private fun generateCharacterSelectionMessages(
        match: ArenaMatchEntity,
        updatedPlayers: Map<String, ArenaPlayerEntity>,
        arenaPlayer: ArenaPlayer,
        nextPhase: ArenaPhaseEntity,
        nextPlayerId: String?,
    ): Triple<String?, List<ArenaMessageEntity>, Int> {
        val messagesReference =
            arenaMatchesReference
                .child(path = match.arenaId)
                .child(path = ArenaMatchEntity.messagesName)

        val selectionMessage =
            ArenaMessageEntity(
                id = messagesReference.addNewKey(),
                type = ArenaMessageType.Generic,
                message =
                    "${arenaPlayer.displayName} selected ${arenaPlayer.characterClass?.className.orEmpty()}",
            )

        var finalActivePlayerUserId = nextPlayerId

        val transitionMessages =
            if (nextPhase == ArenaPhaseEntity.Combat) {
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

        val newMessages = listOf(selectionMessage) + transitionMessages

        return Triple(
            first = finalActivePlayerUserId,
            second = match.messages + newMessages,
            third = newMessages.size,
        )
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
        try {
            val arenaReference = arenaMatchesReference.child(path = arenaId)
            val snapshot = arenaReference.valueEvents.first()
            if (!snapshot.exists) {
                arenaReference.setValue(
                    strategy = ArenaMatchEntity.serializer().nullable,
                    value =
                        ArenaMatchEntity(
                            arenaId = arenaId,
                            phase = ArenaPhaseEntity.WaitingForAllPlayers,
                            activePlayerUserId = userId,
                        ),
                )
            }

            arenaReference.runTransaction(strategy = ArenaMatchEntity.serializer()) { existingMatch
                ->
                val updatedPlayers = existingMatch.playerEntitiesMap.toMutableMap()

                if (userId !in updatedPlayers) {
                    updatedPlayers[userId] =
                        ArenaPlayerEntity(displayName = displayName, userId = userId)
                }

                val arenaPhase =
                    if (updatedPlayers.size == expectedNumberOfPlayers) {
                        ArenaPhaseEntity.ChoosingClasses
                    } else {
                        ArenaPhaseEntity.WaitingForAllPlayers
                    }

                existingMatch.copy(playerEntitiesMap = updatedPlayers, phase = arenaPhase)
            }
            true
        } catch (_: Exception) {
            false
        }

    override suspend fun updateArenaPlayer(arenaId: String, arenaPlayer: ArenaPlayer) {
        arenaMatchesReference
            .child(path = arenaId)
            .child(path = ArenaMatchEntity.playerEntitiesMapName)
            .child(path = arenaPlayer.userId)
            .setValue(value = arenaPlayer.toEntity())
    }

    override fun getArenaMatchFlow(arenaId: String): Flow<ArenaMatch?> =
        arenaMatchesReference
            .child(path = arenaId)
            .valueEvents
            .map { snapshot -> snapshot.value(ArenaMatchEntity.serializer().nullable)?.toDomain() }
            .distinctUntilChanged()

    private fun DatabaseReference.addNewKey(): String =
        push().key ?: error("Failed to generate Firebase push key")
}
