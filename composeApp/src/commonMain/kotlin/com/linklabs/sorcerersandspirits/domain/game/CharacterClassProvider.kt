package com.linklabs.sorcerersandspirits.domain.game

interface CharacterClassProvider {
    suspend fun refresh()

    fun getAllClasses(): List<CharacterClass>
}
