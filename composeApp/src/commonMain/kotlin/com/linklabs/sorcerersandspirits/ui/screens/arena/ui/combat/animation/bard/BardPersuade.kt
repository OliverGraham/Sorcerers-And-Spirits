package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PersuadePhase
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AbilityAnimationStage
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.CueEffectOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberReplayingCues
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberSoloSeat
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val LUTE_APPEAR_MILLIS = 240
private const val LUTE_SUMMON_HOLD_MILLIS = 140L
private const val STRUM_RING_MILLIS = 540
private const val NOTE_ENTER_MILLIS = 380
private const val GRAY_MILLIS = 420
private const val NOTE_FALL_MILLIS = 640
private const val FLOURISH_MILLIS = 720
private const val FLUTE_CROSSFADE_MILLIS = 460
private const val AIR_FLOW_MILLIS = 1000
private const val SWIRL_MILLIS = 1200
private const val LUTE_FADE_MILLIS = 280

/** The scale a summoned lute pops in from. */
private const val LUTE_POP_SCALE = 0.72f

/**
 * The animation channels of one Persuade (SS-207): [luteAlpha] and [luteScale] pop the lute in and
 * fade it away; [strum] rings the strings; [notes] blooms the strummed notes to their hover; [fall]
 * and [grayness] tip a failed roll's notes over and drain its colour; [flourish] sweeps a stopped
 * roll's notes up bright; [fluteReveal] crossfades the lute into the charmer's flute; and [airFlow]
 * with [swirl] breathe air through that flute inside the hypnotic redirect coil.
 */
@Stable
internal class PersuadeEffectState {
    val luteAlpha = Animatable(initialValue = 0f)
    val luteScale = Animatable(initialValue = LUTE_POP_SCALE)
    val strum = Animatable(initialValue = 0f)
    val notes = Animatable(initialValue = 0f)
    val grayness = Animatable(initialValue = 0f)
    val fall = Animatable(initialValue = 0f)
    val flourish = Animatable(initialValue = 0f)
    val fluteReveal = Animatable(initialValue = 0f)
    val airFlow = Animatable(initialValue = 0f)
    val swirl = Animatable(initialValue = 0f)

    var scene by mutableStateOf<PersuadeScene?>(value = null)
}

/** The fixed geometry of one Persuade: the lute and flute both centre on [anchor] at the Bard. */
@Immutable internal data class PersuadeScene(val anchor: Offset, val accentColor: Color)

/**
 * The Bard's Persuade effect (SS-207). The instant the Bard's armor is passed a lute is conjured in
 * front of them and held while the roll is made. On the roll the strings ring and notes bloom off
 * the soundhole: a failed roll drains the whole instrument to gray and tips the notes over sideways
 * as the attack presses on, while a stopping roll holds its colour and sweeps the notes up in a
 * bright flourish. A charming six plays that same flourish, then crossfades the lute into a
 * charmer's flute that breathes air through a slow hypnotic swirl of notes as the attacker is bent
 * onto a new target. Anchored to the Bard's measured seat via [avatarBoundsFor]; sits out any cue
 * that is not a Persuade.
 */
@Composable
internal fun PersuadeOverlay(
    animationCue: CombatAnimationCue?,
    avatarBoundsFor: (String) -> Rect?,
    modifier: Modifier = Modifier,
) {
    val state = remember { PersuadeEffectState() }
    val density = LocalDensity.current

    CueEffectOverlay(
        animationCue = animationCue,
        select = { it as? CombatAnimationCue.Persuade },
        onCancel = { state.scene = null },
        play = playLabel@{ cue ->
                val bardBounds = avatarBoundsFor(cue.bardUserId) ?: return@playLabel
                val scene =
                    persuadeScene(
                        bardBounds = bardBounds,
                        accentColor = CharacterClassType.Bard.toColor(),
                        density = density,
                    )
                state.play(scene = scene, phase = cue.phase)
            },
        modifier = modifier,
        draw = { drawPersuade(state = state) },
    )
}

/**
 * How long each Persuade phase runs, so the message sequencer holds its callout until the lute has
 * finished ringing, graying, flourishing, or breathing out its charm rather than cutting it short.
 */
internal fun persuadeDurationMillis(phase: PersuadePhase): Long =
    when (phase) {
        PersuadePhase.Summon -> LUTE_APPEAR_MILLIS.toLong() + LUTE_SUMMON_HOLD_MILLIS

        PersuadePhase.StrumFailed ->
            STRUM_RING_MILLIS.toLong() + NOTE_FALL_MILLIS.toLong() + LUTE_FADE_MILLIS.toLong()

        PersuadePhase.StrumStopped ->
            STRUM_RING_MILLIS.toLong() + FLOURISH_MILLIS.toLong() + LUTE_FADE_MILLIS.toLong()

        PersuadePhase.CharmSummon ->
            STRUM_RING_MILLIS.toLong() + FLOURISH_MILLIS.toLong() + FLUTE_CROSSFADE_MILLIS.toLong()

        PersuadePhase.CharmComplete -> SWIRL_MILLIS.toLong() + LUTE_FADE_MILLIS.toLong()
    }

private fun persuadeScene(bardBounds: Rect, accentColor: Color, density: Density): PersuadeScene {
    val liftPx = with(density) { 6.dp.toPx() }
    return PersuadeScene(
        anchor = bardBounds.center - Offset(x = 0f, y = liftPx),
        accentColor = accentColor,
    )
}

private suspend fun PersuadeEffectState.play(scene: PersuadeScene, phase: PersuadePhase) {
    when (phase) {
        PersuadePhase.Summon -> summon(scene = scene)
        PersuadePhase.StrumFailed -> strumFailed(scene = scene)
        PersuadePhase.StrumStopped -> strumStopped(scene = scene)
        PersuadePhase.CharmSummon -> charmSummon(scene = scene)
        PersuadePhase.CharmComplete -> charmComplete(scene = scene)
    }
}

/** The lute is conjured the moment armor is passed and held, waiting out the Persuade roll. */
private suspend fun PersuadeEffectState.summon(scene: PersuadeScene) {
    resetChannels()
    luteAlpha.snapTo(targetValue = 0f)
    luteScale.snapTo(targetValue = LUTE_POP_SCALE)
    this.scene = scene

    coroutineScope {
        launch {
            luteAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = LUTE_APPEAR_MILLIS),
            )
        }
        launch {
            luteScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
            )
        }
    }
}

/** A failed roll: the strings ring, then the whole lute grays out and the notes tumble sideways. */
private suspend fun PersuadeEffectState.strumFailed(scene: PersuadeScene) {
    beginResolve(scene = scene)
    ringStrings()

    coroutineScope {
        launch {
            grayness.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = GRAY_MILLIS),
            )
        }
        launch {
            fall.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(durationMillis = NOTE_FALL_MILLIS, easing = FastOutSlowInEasing),
            )
        }
    }
    fadeOut()
}

/**
 * A stopping roll: the strings ring, the colour holds, and the notes sweep up in a bright flourish.
 */
private suspend fun PersuadeEffectState.strumStopped(scene: PersuadeScene) {
    beginResolve(scene = scene)
    ringStrings()
    flourish.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = FLOURISH_MILLIS, easing = FastOutSlowInEasing),
    )
    fadeOut()
}

/**
 * A charming six: the same stopping flourish, then the lute crossfades into the flute and is held
 * for the redirect that the following [charmComplete] cue plays out.
 */
private suspend fun PersuadeEffectState.charmSummon(scene: PersuadeScene) {
    beginResolve(scene = scene)
    ringStrings()
    flourish.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = FLOURISH_MILLIS, easing = FastOutSlowInEasing),
    )
    fluteReveal.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = FLUTE_CROSSFADE_MILLIS),
    )
}

/** The redirect resolves: the flute breathes air through the slow hypnotic swirl, then fades. */
private suspend fun PersuadeEffectState.charmComplete(scene: PersuadeScene) {
    this.scene = scene
    luteAlpha.snapTo(targetValue = 1f)
    fluteReveal.snapTo(targetValue = 1f)
    flourish.snapTo(targetValue = 1f)
    airFlow.snapTo(targetValue = 0f)
    swirl.snapTo(targetValue = 0f)

    coroutineScope {
        launch {
            airFlow.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = AIR_FLOW_MILLIS, easing = LinearEasing),
            )
        }
        launch {
            swirl.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = SWIRL_MILLIS, easing = LinearEasing),
            )
        }
    }
    fadeOut()
}

/** Plant the held lute and clear the resolution channels so a fresh strum reads from zero. */
private suspend fun PersuadeEffectState.beginResolve(scene: PersuadeScene) {
    this.scene = scene
    luteAlpha.snapTo(targetValue = 1f)
    luteScale.snapTo(targetValue = 1f)
    resetChannels()
}

/** The strings ring out in a decaying vibration while the strummed notes bloom to their hover. */
private suspend fun PersuadeEffectState.ringStrings() {
    coroutineScope {
        launch {
            strum.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = STRUM_RING_MILLIS, easing = LinearEasing),
            )
        }
        launch {
            notes.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(durationMillis = NOTE_ENTER_MILLIS, easing = FastOutSlowInEasing),
            )
        }
    }
}

private suspend fun PersuadeEffectState.fadeOut() {
    luteAlpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = LUTE_FADE_MILLIS))
    this.scene = null
}

private suspend fun PersuadeEffectState.resetChannels() {
    strum.snapTo(targetValue = 0f)
    notes.snapTo(targetValue = 0f)
    grayness.snapTo(targetValue = 0f)
    fall.snapTo(targetValue = 0f)
    flourish.snapTo(targetValue = 0f)
    fluteReveal.snapTo(targetValue = 0f)
    airFlow.snapTo(targetValue = 0f)
    swirl.snapTo(targetValue = 0f)
}

private fun DrawScope.drawPersuade(state: PersuadeEffectState) {
    val scene = state.scene ?: return
    val grayness = state.grayness.value
    val luteAlpha = state.luteAlpha.value
    val fluteReveal = state.fluteReveal.value
    val accent = scene.accentColor

    val isLuteShowing = luteAlpha > 0f && fluteReveal < 1f
    if (isLuteShowing) {
        drawLute(
            anchor = scene.anchor,
            accentColor = accent,
            alpha = luteAlpha * (1f - fluteReveal),
            scale = state.luteScale.value,
            strum = state.strum.value,
            grayness = grayness,
        )
    }
    if (fluteReveal > 0f) {
        drawFlute(
            anchor = scene.anchor,
            accentColor = accent,
            alpha = luteAlpha * fluteReveal,
            airFlow = state.airFlow.value,
        )
    }
    drawPersuadeNotes(
        anchor = scene.anchor,
        accentColor = accent,
        grayness = grayness,
        enter = state.notes.value,
        fall = state.fall.value,
        flourish = state.flourish.value,
    )
    drawSwirlNotes(
        anchor = scene.anchor,
        accentColor = accent,
        swirl = state.swirl.value,
        alpha = luteAlpha,
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun BardPersuadeFailedPreview() {
    BardPersuadePreview(resolution = PersuadePhase.StrumFailed)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun BardPersuadeStoppedPreview() {
    BardPersuadePreview(resolution = PersuadePhase.StrumStopped)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun BardPersuadeCharmPreview() {
    val seats = rememberSoloSeat()
    val cue =
        rememberReplayingCues(replayKey = "charm") { iteration ->
            listOf(
                persuadeCue(iteration = iteration, phase = PersuadePhase.Summon),
                persuadeCue(iteration = iteration, phase = PersuadePhase.CharmSummon),
                persuadeCue(iteration = iteration, phase = PersuadePhase.CharmComplete),
            )
        }
    AbilityAnimationStage(accentClass = CharacterClassType.Bard, seats = seats) {
        PersuadeOverlay(
            animationCue = cue,
            avatarBoundsFor = seats.boundsFor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun BardPersuadePreview(resolution: PersuadePhase) {
    val seats = rememberSoloSeat()
    val cue =
        rememberReplayingCues(replayKey = resolution) { iteration ->
            listOf(
                persuadeCue(iteration = iteration, phase = PersuadePhase.Summon),
                persuadeCue(iteration = iteration, phase = resolution),
            )
        }
    AbilityAnimationStage(accentClass = CharacterClassType.Bard, seats = seats) {
        PersuadeOverlay(
            animationCue = cue,
            avatarBoundsFor = seats.boundsFor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun persuadeCue(iteration: Int, phase: PersuadePhase): CombatAnimationCue.Persuade =
    CombatAnimationCue.Persuade(
        id = "persuade-$phase-$iteration",
        bardUserId = PREVIEW_ACTOR_ID,
        phase = phase,
    )
