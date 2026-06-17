package com.linklabs.sorcerersandspirits.data.arena.utilities

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaPlayerEntity
import com.linklabs.sorcerersandspirits.domain.arena.model.Gauntlet
import com.linklabs.sorcerersandspirits.nullOnTransactionDecodeCrashSerializer
import dev.gitlive.firebase.database.DatabaseReference

internal class StayForAnotherMatch(private val userId: String) {
    suspend operator fun invoke(arenaReference: DatabaseReference) {
        val strategy =
            nullOnTransactionDecodeCrashSerializer(serializer = ArenaMatchEntity.serializer())

        arenaReference.runTransaction(strategy = strategy) { match ->
            match.stayForAnotherMatchTransaction()
        }
    }

    private fun ArenaMatchEntity?.stayForAnotherMatchTransaction(): ArenaMatchEntity? {
        val match = this ?: return null

        val updatedPlayers = match.playerEntitiesMap.toMutableMap()
        val currentPlayer = updatedPlayers[userId] ?: return match

        updatedPlayers[userId] =
            when (currentPlayer) {
                is ArenaPlayerEntity.Selected -> currentPlayer.copy(isReadyForNextMatch = true)
                is ArenaPlayerEntity.Unselected -> currentPlayer.copy(isReadyForNextMatch = true)
            }

        val allReady = updatedPlayers.values.all { it.isReadyForNextMatch }

        return if (allReady) {
            // Reset match
            val resetPlayers =
                updatedPlayers.mapValues { (_, player) ->
                    ArenaPlayerEntity.Unselected(
                        userId = player.userId,
                        displayName = player.displayName,
                    )
                }

            if (resetPlayers.size < 2) {
                null
            } else {
                ArenaMatchEntity.ChoosingClasses(
                    playerEntitiesMap = resetPlayers,
                    activePlayerUserId = resetPlayers.keys.random(), // Or some other logic
                    messages = emptyList(),
                    numberOfNewMessages = 0,
                    matchNumberInGauntlet = match.nextMatchNumberInGauntlet(),
                )
            }
        } else {
            match.withUpdatedPlayers(updatedPlayers)
        }
    }

    // Advancing within the Gauntlet (1 -> 2 -> 3); once the final match is done, "Go Again" wraps
    // back to 1 to start a fresh Gauntlet.
    private fun ArenaMatchEntity.nextMatchNumberInGauntlet(): Int =
        if (matchNumberInGauntlet >= Gauntlet.MATCHES_PER_GAUNTLET) {
            1
        } else {
            matchNumberInGauntlet + 1
        }

    private fun ArenaMatchEntity.withUpdatedPlayers(
        updatedPlayers: Map<String, ArenaPlayerEntity>
    ): ArenaMatchEntity =
        when (this) {
            is ArenaMatchEntity.WaitingForAllPlayers -> copy(playerEntitiesMap = updatedPlayers)

            is ArenaMatchEntity.ChoosingClasses -> copy(playerEntitiesMap = updatedPlayers)

            is ArenaMatchEntity.Combat ->
                copy(
                    playerEntitiesMap =
                        updatedPlayers.mapValues { it.value as ArenaPlayerEntity.Selected }
                )

            is ArenaMatchEntity.Finished -> copy(playerEntitiesMap = updatedPlayers)
        }
}
