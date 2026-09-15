package com.linklabs.sorcerersandspirits.data.arena.utilities

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.withActiveUserIds
import com.linklabs.sorcerersandspirits.nullOnTransactionDecodeCrashSerializer
import dev.gitlive.firebase.database.DatabaseReference

internal class ExitArena(private val userId: String) {
    suspend operator fun invoke(arenaReference: DatabaseReference) {
        val strategy =
            nullOnTransactionDecodeCrashSerializer(serializer = ArenaMatchEntity.serializer())

        arenaReference.runTransaction(strategy = strategy) { match ->
            match?.exitArenaTransaction()
        }
    }

    private fun ArenaMatchEntity.exitArenaTransaction(): ArenaMatchEntity? {
        val remainingPlayers = playerEntitiesMap - userId

        if (remainingPlayers.isEmpty()) return null

        return when (this) {
            is ArenaMatchEntity.WaitingForAllPlayers ->
                copy(playerEntitiesMap = playerEntitiesMap - userId, newMessages = emptyList())

            is ArenaMatchEntity.ChoosingClasses -> {
                // TODO: make function. add messages?
                val updatedPlayers = playerEntitiesMap - userId

                if (updatedPlayers.size < 2) {
                    ArenaMatchEntity.Finished(
                        newMessages = emptyList(),
                        playerEntitiesMap = updatedPlayers,
                        matchNumberInGauntlet = matchNumberInGauntlet,
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
                        newMessages = emptyList(),
                    )
                }
            }

            is ArenaMatchEntity.Combat -> {
                // TODO: make function. add messages?
                val updatedPlayers = playerEntitiesMap - userId

                if (updatedPlayers.size < 2) {
                    ArenaMatchEntity.Finished(
                        newMessages = emptyList(),
                        playerEntitiesMap = updatedPlayers,
                        matchNumberInGauntlet = matchNumberInGauntlet,
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
                        newMessages = emptyList(),
                    )
                }
            }

            is ArenaMatchEntity.Finished ->
                copy(playerEntitiesMap = playerEntitiesMap - userId, newMessages = emptyList())
        }
    }
}
