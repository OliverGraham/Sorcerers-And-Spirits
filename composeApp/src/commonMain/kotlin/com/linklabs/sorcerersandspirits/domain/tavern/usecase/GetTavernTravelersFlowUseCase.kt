package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository

class GetTavernTravelersFlowUseCase(private val tavernRepository: TavernRepository) {
    operator fun invoke() = tavernRepository.getTavernTravelersFlow()
}
