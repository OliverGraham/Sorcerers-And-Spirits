package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.domain.game.ability.AbilityConfig
import com.linklabs.sorcerersandspirits.domain.game.ability.AbilityConfigProvider

class RemoteConfigAbilityConfigProvider(
    private val remoteConfigDataSource: RemoteConfigDataSource,
) : AbilityConfigProvider {

    override fun getAbilityConfig(): AbilityConfig = remoteConfigDataSource.abilityConfig().toDomain()
}
