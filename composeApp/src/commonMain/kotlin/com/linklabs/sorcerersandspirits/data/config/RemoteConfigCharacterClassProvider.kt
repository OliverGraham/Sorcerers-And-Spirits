package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassProvider

class RemoteConfigCharacterClassProvider(
    private val remoteConfigDataSource: RemoteConfigDataSource
) : CharacterClassProvider {

    override suspend fun refresh() = remoteConfigDataSource.initialize()

    override fun getAllClasses(): List<CharacterClass> {
        val statsByType = remoteConfigDataSource.characterClassStats()

        return CharacterClass.getAllClasses().map { characterClass ->
            val stats = statsByType[characterClass.type] ?: return@map characterClass

            characterClass.copy(
                hitPoints = stats.hitPoints,
                maxHitPoints = stats.maxHitPoints,
                armorClass = stats.armorClass,
                speed = stats.speed,
            )
        }
    }
}
