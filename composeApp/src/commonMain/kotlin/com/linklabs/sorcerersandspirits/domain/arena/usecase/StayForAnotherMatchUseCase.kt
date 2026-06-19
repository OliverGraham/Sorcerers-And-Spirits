package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository

class StayForAnotherMatchUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val arenaRepository: ArenaRepository,
) {
    /**
     * Readies [playerUserId] up for the next match, defaulting to the signed-in user. A computer
     * match has no client for the bot, so the user's client readies it up by passing its id here.
     */
    suspend operator fun invoke(arenaId: String, playerUserId: String? = null) {
        val userId = playerUserId ?: authenticationRepository.getUserId() ?: return

        arenaRepository.stayForAnotherMatch(arenaId = arenaId, userId = userId)
    }
}
