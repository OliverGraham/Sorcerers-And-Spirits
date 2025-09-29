package com.linklabs.sorcerersandspirits.domain.tavern

class GetTavernUseCase(
    private val tavernRepository: TavernRepository,
) {
    operator fun invoke(): Tavern {
        return tavernRepository.getTavern()
    }
}
