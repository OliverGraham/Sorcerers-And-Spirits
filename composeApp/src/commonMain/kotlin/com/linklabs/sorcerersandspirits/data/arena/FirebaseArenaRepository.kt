package com.linklabs.sorcerersandspirits.data.arena

import com.linklabs.sorcerersandspirits.data.FireStoreKeys.ARENA_MATCHES_PATH
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.toDomain
import com.linklabs.sorcerersandspirits.data.arena.model.toEntity
import com.linklabs.sorcerersandspirits.data.arena.model.toSelectedEntity
import com.linklabs.sorcerersandspirits.data.arena.utilities.ChooseCharacterClass
import com.linklabs.sorcerersandspirits.data.arena.utilities.ConnectToArena
import com.linklabs.sorcerersandspirits.data.arena.utilities.addNewKey
import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CombatResult
import com.linklabs.sorcerersandspirits.domain.arena.model.RollType
import dev.gitlive.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.nullable

class FirebaseArenaRepository(database: FirebaseDatabase) : ArenaRepository {

    private val arenaMatchesReference = database.reference(path = ARENA_MATCHES_PATH)

    override suspend fun chooseCharacterClass(
        arenaId: String,
        selectedArenaPlayer: ArenaPlayer.Selected,
        nextPlayerId: String,
    ) {
        ChooseCharacterClass(
                arenaReference = arenaMatchesReference.child(path = arenaId),
                selectedArenaPlayer = selectedArenaPlayer.toSelectedEntity(),
                nextPlayerId = nextPlayerId,
            )
            .invoke()
    }

    override suspend fun connectToArena(
        arenaId: String,
        expectedNumberOfPlayers: Int,
        userId: String,
        displayName: String,
    ) =
        ConnectToArena(
                arenaReference = arenaMatchesReference.child(path = arenaId),
                expectedNumberOfPlayers = expectedNumberOfPlayers,
                userId = userId,
                displayName = displayName,
            )
            .invoke()

    override suspend fun updateArenaPlayer(arenaId: String, arenaPlayer: ArenaPlayer) {
        arenaMatchesReference
            .child(path = arenaId)
            .child(path = ArenaMatchEntity.playerEntitiesMapName)
            .child(path = arenaPlayer.userId)
            .setValue(value = arenaPlayer.toEntity())
    }

    override suspend fun applyCombatResult(arenaId: String, result: CombatResult) {
        val arenaReference = arenaMatchesReference.child(path = arenaId)

        arenaReference.runTransaction(strategy = ArenaMatchEntity.serializer()) { existingMatch ->
            if (existingMatch !is ArenaMatchEntity.Combat) return@runTransaction existingMatch

            val messagesReference = arenaReference.child(path = ArenaMatchEntity.messagesName)
            val newMessages =
                result.messages.map { outgoingMessage ->
                    outgoingMessage.toEntity(id = messagesReference.addNewKey())
                }
            val allMessages = existingMatch.messages + newMessages

            when (result) {
                is CombatResult.Miss -> {
                    existingMatch.copy(
                        messages = allMessages,
                        numberOfNewMessages = newMessages.size,
                        activePlayerUserId = result.nextActivePlayerId,
                        pendingRollType = RollType.ArmorClass,
                        pendingTargetUserId = null,
                    )
                }

                is CombatResult.Hit -> {
                    existingMatch.copy(
                        messages = allMessages,
                        numberOfNewMessages = newMessages.size,
                        pendingRollType = RollType.Health,
                        pendingTargetUserId = result.targetId,
                    )
                }

                is CombatResult.DamageDealt -> {
                    val updatedPlayerMap = existingMatch.playerEntitiesMap.toMutableMap()
                    val target = updatedPlayerMap[result.targetId]!!
                    updatedPlayerMap[result.targetId] =
                        target.copy(
                            characterClassEntity =
                                target.characterClassEntity.copy(hitPoints = result.newHp)
                        )

                    existingMatch.copy(
                        messages = allMessages,
                        numberOfNewMessages = newMessages.size,
                        playerEntitiesMap = updatedPlayerMap,
                        activePlayerUserId = result.nextActivePlayerId,
                        pendingRollType = RollType.ArmorClass,
                        pendingTargetUserId = null,
                    )
                }

                is CombatResult.TargetDefeated -> {
                    val updatedPlayerMap = existingMatch.playerEntitiesMap.toMutableMap()
                    updatedPlayerMap.remove(result.defeatedPlayerId)

                    existingMatch.copy(
                        messages = allMessages,
                        numberOfNewMessages = newMessages.size,
                        playerEntitiesMap = updatedPlayerMap,
                        activePlayerUserId = result.nextActivePlayerId,
                        pendingRollType = RollType.ArmorClass,
                        pendingTargetUserId = null,
                    )
                }

                is CombatResult.Victory -> {
                    ArenaMatchEntity.Finished(
                        messages = allMessages,
                        numberOfNewMessages = newMessages.size,
                    )
                }
            }
        }
    }

    override fun getArenaMatchFlow(arenaId: String): Flow<ArenaMatch?> =
        arenaMatchesReference
            .child(path = arenaId)
            .valueEvents
            .map { snapshot ->
                snapshot.value(strategy = ArenaMatchEntity.serializer().nullable)?.toDomain()
            }
            .distinctUntilChanged()
}
