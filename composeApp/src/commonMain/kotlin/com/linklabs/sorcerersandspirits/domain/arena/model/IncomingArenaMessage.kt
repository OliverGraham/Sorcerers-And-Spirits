package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

data class IncomingArenaMessage(val id: String, val roundNumber: Int, val message: ArenaMessage)

data class CharacterClassMessageInfo(val className: String, val type: CharacterClassType)
