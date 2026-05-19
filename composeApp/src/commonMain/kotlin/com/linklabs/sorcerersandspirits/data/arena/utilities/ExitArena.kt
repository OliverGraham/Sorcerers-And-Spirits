package com.linklabs.sorcerersandspirits.data.arena.utilities

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import dev.gitlive.firebase.database.DatabaseReference

// TODO Arena: When it's finished because someone left early, then make sure there's UI about this
internal class ExitArena(private val userId: String) {
    suspend operator fun invoke(arenaReference: DatabaseReference) {
        arenaReference.runTransaction(strategy = ArenaMatchEntity.serializer()) { match ->
            match.exitArenaTransaction()
        }
    }

    private fun ArenaMatchEntity.exitArenaTransaction(): ArenaMatchEntity =
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
                    ArenaMatchEntity.Finished(messages = messages, numberOfNewMessages = 0)
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
                    ArenaMatchEntity.Finished(messages = messages, numberOfNewMessages = 0)
                } else {
                    val nextActivePlayerId =
                        if (activePlayerUserId == userId) {
                            updatedPlayers.getFastestPlayer().userId
                        } else {
                            activePlayerUserId
                        }
                    copy(
                        playerEntitiesMap = updatedPlayers,
                        activePlayerUserId = nextActivePlayerId,
                    )
                }
            }

            is ArenaMatchEntity.Finished -> this
        }
}
