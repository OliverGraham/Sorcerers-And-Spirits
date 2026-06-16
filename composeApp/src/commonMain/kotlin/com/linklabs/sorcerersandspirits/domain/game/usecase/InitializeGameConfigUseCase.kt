package com.linklabs.sorcerersandspirits.domain.game.usecase

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassProvider

class InitializeGameConfigUseCase(private val characterClassProvider: CharacterClassProvider) {
    suspend operator fun invoke() = characterClassProvider.refresh()
}
