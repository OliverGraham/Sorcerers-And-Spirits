package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import kotlin.coroutines.cancellation.CancellationException

class RemoveDisconnectedPlayerUseCase(private val arenaRepository: ArenaRepository) {
    suspend operator fun invoke(arenaId: String, userId: String) {
        try {
            arenaRepository.removeDisconnectedPlayer(arenaId = arenaId, userId = userId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
        }
    }
}
