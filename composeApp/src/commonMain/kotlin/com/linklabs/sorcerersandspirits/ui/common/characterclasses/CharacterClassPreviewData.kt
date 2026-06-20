package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

private const val PREVIEW_STAT = 5

/**
 * A sample state for [type] for previews and design tooling only. Built with placeholder stats via
 * the production [CharacterClass.create] builder so ability wiring stays correct without
 * duplicating the RemoteConfig-owned stat values.
 */
internal fun previewCharacterClassState(type: CharacterClassType): CharacterClassState =
    CharacterClass.create(
            type = type,
            hitPoints = PREVIEW_STAT,
            maxHitPoints = PREVIEW_STAT,
            armorClass = PREVIEW_STAT,
            speed = PREVIEW_STAT,
        )
        .toState()

/** Sample states for every playable class, for previews and design tooling only. */
internal fun previewCharacterClassStates(): List<CharacterClassState> =
    CharacterClass.playableTypes().map { type -> previewCharacterClassState(type = type) }
