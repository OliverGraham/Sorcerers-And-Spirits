package com.linklabs.sorcerersandspirits.domain.game

/**
 * Supplies the playable classes with their currently-active stats. Numeric stats always come from
 * RemoteConfig (the single source of truth); ability wiring is resolved in code by
 * [CharacterClass.create] from the class type.
 */
interface CharacterClassProvider {

    /** Fetches and activates the latest remote stats. Call once at app start. */
    suspend fun refresh()

    fun getAllClasses(): List<CharacterClass>
}
