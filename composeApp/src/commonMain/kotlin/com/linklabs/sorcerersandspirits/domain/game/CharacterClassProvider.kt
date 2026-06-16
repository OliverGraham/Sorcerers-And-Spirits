package com.linklabs.sorcerersandspirits.domain.game

/**
 * Supplies the playable classes with their currently-active stats. Ability wiring always comes from
 * [CharacterClass]'s factories; the numeric stats may be overlaid from RemoteConfig.
 */
interface CharacterClassProvider {

    /** Fetches and activates the latest remote stats. Call once at app start. */
    suspend fun refresh()

    fun getAllClasses(): List<CharacterClass>
}
