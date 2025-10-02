package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository

class LeaveTavernUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val tavernRepository: TavernRepository,
) {
    suspend operator fun invoke() {
        val userId = authenticationRepository.getUserId() ?: return

        return tavernRepository.leaveTavern(id = userId)
    }
}
