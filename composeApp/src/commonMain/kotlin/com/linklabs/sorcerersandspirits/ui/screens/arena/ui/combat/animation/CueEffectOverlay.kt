package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue

// TODO: Remove this? Definitely change with AnimationQueue
//       Another remember + doing something.
/**
 * The shared plumbing every cue-driven effect overlay repeats (SS-191): hold the id of the last cue
 * it played, and on each new [animationCue] either play the one it is responsible for or stand
 * down. [select] picks the cue this overlay acts on, returning null for every other cue; [onCancel]
 * clears any effect still on screen when the incoming cue is not ours; [play] runs the chosen cue's
 * choreography once; and [draw] paints the current frame. A cue already live at first composition
 * is skipped, and each cue plays at most once.
 */
@Composable
internal fun <T : CombatAnimationCue> CueEffectOverlay(
    animatingMessage: ArenaMessageState?,
    select: (CombatAnimationCue) -> T?,
    onCancel: () -> Unit,
    play: suspend (T) -> Unit,
    modifier: Modifier = Modifier,
    draw: DrawScope.() -> Unit,
) {
    var lastPlayedCueId by remember { mutableStateOf(value = animationCue?.id) }

    LaunchedEffect(key1 = animationCue) {
        val cue = animationCue?.let(select)
        if (cue == null) {
            onCancel()
            lastPlayedCueId = animationCue?.id
            return@LaunchedEffect
        }
        if (cue.id == lastPlayedCueId) return@LaunchedEffect
        lastPlayedCueId = cue.id
        play(cue)
    }

    Canvas(modifier = modifier) { draw() }
}
