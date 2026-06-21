package com.linklabs.sorcerersandspirits.data.arena.utilities

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.withActiveUserIds
import com.linklabs.sorcerersandspirits.domain.computer.ComputerOpponents
import dev.gitlive.firebase.database.DatabaseReference
import kotlinx.serialization.builtins.nullable

internal class ExitArena(private val userId: String) {
    suspend operator fun invoke(arenaReference: DatabaseReference) {
        arenaReference.runTransaction(strategy = ArenaMatchEntity.serializer().nullable) { match ->
            match?.exitArenaTransaction()
        }
    }

    private fun ArenaMatchEntity.exitArenaTransaction(): ArenaMatchEntity? {
        val remainingPlayers = playerEntitiesMap - userId

        // A match with no human players left is dead: the computer opponents have no client to play
        // or leave, so delete the whole match rather than orphaning it.
        val hasRemainingHumanPlayer =
            remainingPlayers.keys.any { id -> !ComputerOpponents.isComputer(userId = id) }
        if (!hasRemainingHumanPlayer) return null

        return when (this) {
            is ArenaMatchEntity.WaitingForAllPlayers ->
                copy(playerEntitiesMap = playerEntitiesMap - userId)

            is ArenaMatchEntity.ChoosingClasses -> {
                val updatedPlayers = playerEntitiesMap - userId

                if (updatedPlayers.size < 2) {
                    ArenaMatchEntity.Finished(
                        messages = messages,
                        numberOfNewMessages = 0,
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
                    )
                }
            }

            is ArenaMatchEntity.Combat -> {
                val updatedPlayers = playerEntitiesMap - userId

                if (updatedPlayers.size < 2) {
                    ArenaMatchEntity.Finished(
                        messages = messages,
                        numberOfNewMessages = 0,
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
                    )
                }
            }

            is ArenaMatchEntity.Finished -> copy(playerEntitiesMap = playerEntitiesMap - userId)
        }
    }
}
