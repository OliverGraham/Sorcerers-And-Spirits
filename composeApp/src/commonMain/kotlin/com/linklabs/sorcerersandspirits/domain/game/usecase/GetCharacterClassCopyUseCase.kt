package com.linklabs.sorcerersandspirits.domain.game.usecase

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassCopy
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassCopyProvider

class GetCharacterClassCopyUseCase(
    private val characterClassCopyProvider: CharacterClassCopyProvider,
) {
    operator fun invoke(): CharacterClassCopy = characterClassCopyProvider.getCharacterClassCopy()
}
