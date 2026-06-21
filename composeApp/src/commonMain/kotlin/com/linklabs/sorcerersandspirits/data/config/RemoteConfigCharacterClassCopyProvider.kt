package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassCopy
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassCopyProvider

class RemoteConfigCharacterClassCopyProvider(
    private val remoteConfigDataSource: RemoteConfigDataSource
) : CharacterClassCopyProvider {

    override fun getCharacterClassCopy(): CharacterClassCopy =
        remoteConfigDataSource.characterClassCopy().toCharacterClassCopy()
}
