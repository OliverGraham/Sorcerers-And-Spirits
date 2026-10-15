package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.audio.SoundEffect

internal fun ArenaMessageState.soundEffect(): SoundEffect? = when (this) {
    is ArenaMessageState.NormalRoll.Attack -> SoundEffect.DiceSettle
    is ArenaMessageState.NormalRoll.Outcome -> normalRollMessageType.soundEffect()
    else -> null
}

private fun NormalRollMessageType.soundEffect(): SoundEffect = when (this) {
    NormalRollMessageType.ArmorClassHit -> SoundEffect.ArmorBreak
    NormalRollMessageType.ArmorClassMiss -> SoundEffect.ArmorHold
    NormalRollMessageType.Damage -> SoundEffect.Damage
    NormalRollMessageType.Knockout -> SoundEffect.Knockout
}
