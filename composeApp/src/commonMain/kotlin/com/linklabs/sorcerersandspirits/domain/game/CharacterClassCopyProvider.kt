package com.linklabs.sorcerersandspirits.domain.game

/**
 * Supplies the currently-active class stat + ability copy. Values come from RemoteConfig (the same
 * fetch [CharacterClassProvider.refresh] activates), falling back to the bundled defaults when
 * nothing has been fetched or the payload can't be parsed.
 */
interface CharacterClassCopyProvider {
    fun getCharacterClassCopy(): CharacterClassCopy
}
