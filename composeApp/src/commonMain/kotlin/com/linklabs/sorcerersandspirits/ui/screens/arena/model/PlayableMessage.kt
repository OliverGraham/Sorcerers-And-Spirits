package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable

// TODO: Why are there so many different types here? Can't the ArenaMessageState simply be used?
@Immutable
data class PlayableMessage(
    val message: ArenaMessageState,
    val cue: CombatAnimationCue?,
    val callout: FlashCallout?,
    val anchor: FlashAnchor,
) {
    val id: String = message.id
}

fun ArenaMessageState.toPlayable(): PlayableMessage =
    PlayableMessage(
        message = this,
        cue = combatAnimationCue,
        callout = toFlashCallout(),
        anchor = toFlashAnchor(),
    )
