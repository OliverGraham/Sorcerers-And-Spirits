package com.linklabs.sorcerersandspirits.data.arena

import com.linklabs.sorcerersandspirits.data.FireStoreKeys.ARENA_MATCHES_PATH
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMessageEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMessageType
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
        selectedArenaPlayer: ArenaPlayer.Selected,
        nextPlayerId: String,
    ) {
        arenaMatchesReference.child(path = arenaId).runTransaction(
            strategy = ArenaMatchEntity.serializer()
        ) { match ->
            val playerEntitiesMap =
                when (match) {
                    is ArenaMatchEntity.WaitingForAllPlayers -> match.playerEntitiesMap
                    is ArenaMatchEntity.ChoosingClasses -> match.playerEntitiesMap
                    is ArenaMatchEntity.Combat -> match.playerEntitiesMap
                    is ArenaMatchEntity.Finished -> emptyMap()
                }

            val updatedPlayers = playerEntitiesMap.toMutableMap()
            updatedPlayers[selectedArenaPlayer.userId] = selectedArenaPlayer.toEntity()

            val (updatedMessages, numberOfNewMessages) =
                generateCharacterSelectionMessages(
                    arenaId = arenaId,
                    match = match,
                    updatedPlayers = updatedPlayers,
                    selectedArenaPlayer = selectedArenaPlayer,
                )

            val areAllSelected = updatedPlayers.values.all { it is ArenaPlayerEntity.Selected }

            when {
                match is ArenaMatchEntity.Finished ->
                    ArenaMatchEntity.Finished(
                        messages = updatedMessages,
                        numberOfNewMessages = numberOfNewMessages,
                    )

                areAllSelected ->
                    ArenaMatchEntity.Combat(
                        playerEntitiesMap =
                            updatedPlayers.mapValues { it.value as ArenaPlayerEntity.Selected },
                        activePlayerUserId = nextPlayerId,
                        messages = updatedMessages,
                        numberOfNewMessages = numberOfNewMessages,
                    )

                else ->
                    ArenaMatchEntity.ChoosingClasses(
                        playerEntitiesMap = updatedPlayers,
                        activePlayerUserId = nextPlayerId,
                        messages = updatedMessages,
                        numberOfNewMessages = numberOfNewMessages,
                    )
            }
        }
    }

    // TODO Arena: return data class
    private fun generateCharacterSelectionMessages(
        arenaId: String,
        match: ArenaMatchEntity,
        updatedPlayers: Map<String, ArenaPlayerEntity>,
        selectedArenaPlayer: ArenaPlayer.Selected,
    ): Pair<List<ArenaMessageEntity>, Int> {
        val messagesReference =
            arenaMatchesReference.child(path = arenaId).child(path = ArenaMatchEntity.messagesName)

        val selectionMessage =
            ArenaMessageEntity(
                id = messagesReference.addNewKey(),
                type = ArenaMessageType.Generic,
                message =
                    "${selectedArenaPlayer.displayName} selected ${selectedArenaPlayer.characterClass.className}",
            )

        val combatStartingMessages =
            messagesReference.getCombatStartingMessages(playerEntities = updatedPlayers.values)

        val newMessages = listOf(selectionMessage) + combatStartingMessages
        return Pair(first = match.messages + newMessages, second = newMessages.size)
    }

    private fun DatabaseReference.getCombatStartingMessages(
        playerEntities: Collection<ArenaPlayerEntity>
    ): List<ArenaMessageEntity> {
        val selectedPlayers = playerEntities.filterIsInstance<ArenaPlayerEntity.Selected>()
        val areAllSelected = selectedPlayers.size == playerEntities.size

        return if (areAllSelected) {
            getAllSelectedMessages(selectedPlayers = selectedPlayers)
        } else {
            emptyList()
        }
    }

    private fun DatabaseReference.getAllSelectedMessages(
        selectedPlayers: List<ArenaPlayerEntity.Selected>
    ) = buildList {
        add(
            ArenaMessageEntity(
                id = addNewKey(),
                type = ArenaMessageType.Generic,
                message = "Combat is now starting!",
            )
        )
        add(ArenaMessageEntity(id = addNewKey(), type = ArenaMessageType.Round, roundNumber = 1))

        val firstPlayer = selectedPlayers.maxBy { it.characterClassEntity.speed }
        add(
            ArenaMessageEntity(
                id = addNewKey(),
                type = ArenaMessageType.Generic,
                message = "It is now ${firstPlayer.characterClassEntity.className}'s turn!",
            )
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
            val serializer = ArenaMatchEntity.serializer()

            if (!snapshot.exists) {
                arenaReference.setValue(
                    strategy = serializer,
                    value = ArenaMatchEntity.WaitingForAllPlayers(),
                )
            }

            arenaReference.runTransaction(strategy = serializer) { existingMatch ->
                val playerEntitiesMap =
                    when (existingMatch) {
                        is ArenaMatchEntity.WaitingForAllPlayers -> existingMatch.playerEntitiesMap
                        is ArenaMatchEntity.ChoosingClasses -> existingMatch.playerEntitiesMap
                        is ArenaMatchEntity.Combat -> existingMatch.playerEntitiesMap
                        is ArenaMatchEntity.Finished -> emptyMap()
                    }

                val updatedPlayers = playerEntitiesMap.toMutableMap()

                if (userId !in updatedPlayers) {
                    updatedPlayers[userId] =
                        ArenaPlayerEntity.Unselected(displayName = displayName, userId = userId)
                }

                if (updatedPlayers.size == expectedNumberOfPlayers) {
                    ArenaMatchEntity.ChoosingClasses(
                        playerEntitiesMap = updatedPlayers,
                        activePlayerUserId = userId,
                        messages = existingMatch.messages,
                        numberOfNewMessages = 0,
                    )
                } else {
                    ArenaMatchEntity.WaitingForAllPlayers(
                        playerEntitiesMap = updatedPlayers,
                        messages = existingMatch.messages,
                        numberOfNewMessages = 0,
                    )
                }
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
            .map { snapshot ->
                snapshot.value(strategy = ArenaMatchEntity.serializer().nullable)?.toDomain()
            }
            .distinctUntilChanged()

    private fun DatabaseReference.addNewKey(): String =
        push().key ?: error("Failed to generate Firebase push key")
}
