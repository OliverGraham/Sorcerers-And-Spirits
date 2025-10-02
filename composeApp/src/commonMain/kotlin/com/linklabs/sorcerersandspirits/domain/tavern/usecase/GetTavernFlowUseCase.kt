package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import com.linklabs.sorcerersandspirits.domain.tavern.Tavern
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import kotlinx.coroutines.flow.Flow

class GetTavernFlowUseCase(
    private val tavernRepository: TavernRepository,
) {
    operator fun invoke(): Flow<Tavern> {
        return tavernRepository.getTavernFlow()
    }
}
