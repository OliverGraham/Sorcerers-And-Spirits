package com.linklabs.sorcerersandspirits.domain.game

/**
 * Supplies the currently-active ability roll thresholds. The values come from RemoteConfig (the
 * same fetch [CharacterClassProvider.refresh] activates), falling back to [AbilityConfig.DEFAULT]
 * when nothing has been fetched or the payload can't be parsed.
 */
interface AbilityConfigProvider {
    fun getAbilityConfig(): AbilityConfig
}
