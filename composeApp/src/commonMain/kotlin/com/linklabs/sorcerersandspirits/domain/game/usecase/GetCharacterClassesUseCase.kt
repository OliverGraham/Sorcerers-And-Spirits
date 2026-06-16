package com.linklabs.sorcerersandspirits.domain.game.usecase

import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassProvider

class GetCharacterClassesUseCase(private val characterClassProvider: CharacterClassProvider) {
    operator fun invoke(): List<CharacterClass> = characterClassProvider.getAllClasses()
}
