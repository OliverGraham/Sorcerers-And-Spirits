package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassProvider

class RemoteConfigCharacterClassProvider(
    private val remoteConfigDataSource: RemoteConfigDataSource,
) : CharacterClassProvider {

    override suspend fun refresh() = remoteConfigDataSource.initialize()

    override fun getAllClasses(): List<CharacterClass> {
        val statsByType = remoteConfigDataSource.characterClassStats()

        return CharacterClass.playableTypes().mapNotNull { type ->
            val stats = statsByType[type] ?: return@mapNotNull null

            CharacterClass.create(
                type = type,
                hitPoints = stats.hitPoints,
                maxHitPoints = stats.maxHitPoints,
                armorClass = stats.armorClass,
                speed = stats.speed,
            )
        }
    }
}
