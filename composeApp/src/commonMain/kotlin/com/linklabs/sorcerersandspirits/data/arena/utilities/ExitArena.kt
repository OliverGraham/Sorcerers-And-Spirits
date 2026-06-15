package com.linklabs.sorcerersandspirits.data.arena.utilities

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.withActiveUserIds
import dev.gitlive.firebase.database.DatabaseReference
import kotlinx.serialization.builtins.nullable

// TODO(SS-045) Arena: When it's finished because someone left early, then make sure there's UI
// about this
internal class ExitArena(private val userId: String) {
    suspend operator fun invoke(arenaReference: DatabaseReference) {
        arenaReference.runTransaction(strategy = ArenaMatchEntity.serializer().nullable) { match ->
            match?.exitArenaTransaction()
        }
    }

    private fun ArenaMatchEntity.exitArenaTransaction(): ArenaMatchEntity? =
        when (this) {
            is ArenaMatchEntity.WaitingForAllPlayers -> {
                val updatedPlayers = playerEntitiesMap.toMutableMap()
                updatedPlayers.remove(key = userId)
                copy(playerEntitiesMap = updatedPlayers)
            }

            is ArenaMatchEntity.ChoosingClasses -> {
                val updatedPlayers = playerEntitiesMap.toMutableMap()
                updatedPlayers.remove(key = userId)

                if (updatedPlayers.size < 2) {
                    ArenaMatchEntity.Finished(
                        messages = messages,
                        numberOfNewMessages = 0,
                        playerEntitiesMap = updatedPlayers,
                    )
                } else {
                    val nextActivePlayerId =
                        if (activePlayerUserId == userId) {
                            updatedPlayers.determineNextChoosingPlayerId()
                        } else {
                            activePlayerUserId
                        }
                    copy(
                        playerEntitiesMap = updatedPlayers,
                        activePlayerUserId = nextActivePlayerId,
                    )
                }
            }

            is ArenaMatchEntity.Combat -> {
                val updatedPlayers = playerEntitiesMap.toMutableMap()
                updatedPlayers.remove(key = userId)

                if (updatedPlayers.size < 2) {
                    ArenaMatchEntity.Finished(
                        messages = messages,
                        numberOfNewMessages = 0,
                        playerEntitiesMap = updatedPlayers,
                    )
                } else {
                    val updatedNextCombatAction =
                        if (userId in nextCombatActionEntity.activeUserIds) {
                            val newActiveUserIds =
                                (nextCombatActionEntity.activeUserIds - userId).ifEmpty {
                                    setOf(updatedPlayers.getFastestPlayer().userId)
                                }
                            nextCombatActionEntity.withActiveUserIds(newActiveUserIds)
                        } else {
                            nextCombatActionEntity
                        }

                    copy(
                        playerEntitiesMap = updatedPlayers,
                        nextCombatActionEntity = updatedNextCombatAction,
                    )
                }
            }

            is ArenaMatchEntity.Finished -> {
                val updatedPlayers = playerEntitiesMap.toMutableMap()
                updatedPlayers.remove(key = userId)

                if (updatedPlayers.isEmpty()) {
                    null
                } else {
                    copy(playerEntitiesMap = updatedPlayers)
                }
            }
        }
}
