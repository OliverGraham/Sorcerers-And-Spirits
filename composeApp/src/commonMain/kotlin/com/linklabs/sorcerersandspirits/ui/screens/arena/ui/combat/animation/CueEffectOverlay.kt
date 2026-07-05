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
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue

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
    animationCue: CombatAnimationCue?,
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

/**
 * The normal-attack flavor of [CueEffectOverlay]: it acts only on the normal attack of
 * [attackerClassType] and resolves the attacker and defender seats before handing off, so each
 * class's overlay supplies only what makes its weapon its own — the strike line it measures, the
 * choreography it plays, and the frame it draws.
 */
@Composable
internal fun NormalAttackOverlay(
    animationCue: CombatAnimationCue?,
    attackerClassType: CharacterClassType,
    avatarBoundsFor: (String) -> Rect?,
    onCancel: () -> Unit,
    play:
        suspend (
            cue: CombatAnimationCue.NormalAttack, attackerBounds: Rect, defenderBounds: Rect,
        ) -> Unit,
    modifier: Modifier = Modifier,
    draw: DrawScope.() -> Unit,
) {
    CueEffectOverlay(
        animationCue = animationCue,
        select = { cue ->
            (cue as? CombatAnimationCue.NormalAttack)?.takeIf {
                it.attackerClassType == attackerClassType
            }
        },
        onCancel = onCancel,
        play = playLabel@{ cue ->
                val attackerBounds = avatarBoundsFor(cue.attackerUserId) ?: return@playLabel
                val defenderBounds = avatarBoundsFor(cue.defenderUserId) ?: return@playLabel
                play(cue, attackerBounds, defenderBounds)
            },
        modifier = modifier,
        draw = draw,
    )
}
