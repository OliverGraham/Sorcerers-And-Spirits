package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AbilityAnimationStage
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.NormalAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_TARGET_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.WeaponStrikeGeometry
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberDuoSeats
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberReplayingCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.weaponStrikeGeometry
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DRUM_APPEAR_MILLIS = 220
private const val STICK_APPEAR_MILLIS = 180
private const val STICK_READY_HOLD_MILLIS = 90L
private const val STICK_STRIKE_MILLIS = 120
private const val STICK_LIFT_MILLIS = 240
private const val SOUND_WAVE_MILLIS = 470
private const val MUSIC_NOTE_MILLIS = 720
private const val DRUM_FADE_MILLIS = 220

/** The drum has to appear, the stick has to appear and settle, then it drives down to the head. */
private const val BARD_STRIKE_CONTACT_MILLIS: Long =
    DRUM_APPEAR_MILLIS.toLong() +
        STICK_APPEAR_MILLIS.toLong() +
        STICK_READY_HOLD_MILLIS +
        STICK_STRIKE_MILLIS.toLong()

/** The sound rings reach the opponent late in their travel, so the flinch lands as they arrive. */
internal val BARD_WAVE_REACTION_DELAY_MILLIS: Long =
    BARD_STRIKE_CONTACT_MILLIS + SOUND_WAVE_MILLIS * 80L / 100L

/** The stick's raised rest and the scale a drum pops in from. */
private const val DRUM_POP_SCALE = 0.72f

/**
 * The play-out of the Bard's snare strike. The choreography drives these Animatables and the draw
 * layer reads them, so a strike redraws the canvas without recomposing anything.
 */
@Stable
internal class BardStrikeState {
    val drumAlpha = Animatable(initialValue = 0f)
    val drumScale = Animatable(initialValue = DRUM_POP_SCALE)
    val stickAlpha = Animatable(initialValue = 0f)
    val stickSwing = Animatable(initialValue = 0f)
    val headDepress = Animatable(initialValue = 0f)
    val waves = Animatable(initialValue = 0f)
    val waveAlpha = Animatable(initialValue = 0f)
    val notes = Animatable(initialValue = 0f)

    var scene by mutableStateOf<WeaponStrikeGeometry?>(value = null)
    var showNotes by mutableStateOf(value = false)
}

/**
 * The Bard's normal attack (SS-196): a wooden snare drum pops up in front of the Bard, a drumstick
 * appears above it, then strikes down — the batter head dimples and bounces back — and sound rings
 * ripple toward the opponent. A landed strike answers with bard-colored music notes rising off the
 * drum; a whiffed one leaves the air silent. Sits out any cue that is not a Bard normal attack.
 */
@Composable
internal fun BardNormalAttackOverlay(
    animatingMessage: ArenaMessageState?,
    avatarBoundsFor: (String) -> Rect?,
    modifier: Modifier = Modifier,
) {
    val state = remember { BardStrikeState() }
    val density = LocalDensity.current

    NormalAttackOverlay(
        animatingMessage = animatingMessage,
        attackerClassType = CharacterClassType.Bard,
        avatarBoundsFor = avatarBoundsFor,
        onCancel = { state.scene = null },
        play = { cue, attackerBounds, defenderBounds ->
            state.play(
                geometry =
                    weaponStrikeGeometry(
                        attackerBounds = attackerBounds,
                        target = defenderBounds.center,
                        accentColor = CharacterClassType.Bard.toColor(),
                        density = density,
                    ),
                normalRollMessageType = cue.normalRollMessageType,
            )
        },
        modifier = modifier,
        draw = { drawBardDrumStrike(state = state) },
    )
}

/**
 * How long the Bard's snare strike runs, so the message sequencer holds the event until the drum
 * has popped in, been struck, rung out its sound and notes, and faded, instead of cutting it off.
 */
internal fun bardNormalAttackDurationMillis(): Long =
    BARD_STRIKE_CONTACT_MILLIS + MUSIC_NOTE_MILLIS.toLong() + DRUM_FADE_MILLIS.toLong()

/**
 * The drum pops in, the stick drops in above it and settles, then drives down onto the head. At
 * contact the head dimples and springs back, sound rings ripple toward the opponent, and — only on
 * a hit — music notes rise off the drum. The whole kit fades once the sound has rung out.
 */
internal suspend fun BardStrikeState.play(
    geometry: WeaponStrikeGeometry,
    normalRollMessageType: NormalRollMessageType,
) {
    drumAlpha.snapTo(targetValue = 0f)
    drumScale.snapTo(targetValue = DRUM_POP_SCALE)
    stickAlpha.snapTo(targetValue = 0f)
    stickSwing.snapTo(targetValue = 0f)
    headDepress.snapTo(targetValue = 0f)
    waves.snapTo(targetValue = 0f)
    waveAlpha.snapTo(targetValue = 0f)
    notes.snapTo(targetValue = 0f)

    val connects = normalRollMessageType != NormalRollMessageType.ArmorClassMiss
    this.scene = geometry
    this.showNotes = connects

    coroutineScope {
        launch {
            drumAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = DRUM_APPEAR_MILLIS),
            )
        }
        launch {
            drumScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
            )
        }
    }

    stickAlpha.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = STICK_APPEAR_MILLIS),
    )
    delay(timeMillis = STICK_READY_HOLD_MILLIS)

    stickSwing.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = STICK_STRIKE_MILLIS, easing = LinearEasing),
    )

    coroutineScope {
        launch {
            headDepress.snapTo(targetValue = 1f)
            headDepress.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.32f, stiffness = Spring.StiffnessMedium),
            )
        }
        launch {
            waveAlpha.snapTo(targetValue = 1f)
            waves.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = SOUND_WAVE_MILLIS, easing = LinearEasing),
            )
            waveAlpha.snapTo(targetValue = 0f)
        }
        launch {
            if (connects) {
                notes.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = MUSIC_NOTE_MILLIS, easing = LinearEasing),
                )
            }
        }
        launch {
            stickSwing.animateTo(
                targetValue = 0f,
                animationSpec =
                    tween(durationMillis = STICK_LIFT_MILLIS, easing = FastOutSlowInEasing),
            )
        }
    }

    coroutineScope {
        launch {
            drumAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = DRUM_FADE_MILLIS),
            )
        }
        launch {
            stickAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = DRUM_FADE_MILLIS),
            )
        }
    }

    this.scene = null
}

internal fun DrawScope.drawBardDrumStrike(state: BardStrikeState) {
    val geometry = state.scene ?: return
    val drumCenter = geometry.start
    val axisVector = geometry.target - drumCenter
    val axisLength = axisVector.getDistance()
    val direction = if (axisLength == 0f) Offset(x = 0f, y = -1f) else axisVector / axisLength

    val isRinging = state.waveAlpha.value > 0f && state.waves.value > 0f
    if (isRinging) {
        drawSoundWaves(
            origin = drumCenter,
            direction = direction,
            reach = axisLength,
            progress = state.waves.value,
            color = geometry.accentColor,
            alpha = state.waveAlpha.value * 0.9f,
        )
    }

    drawSnareDrum(
        center = drumCenter,
        depress = state.headDepress.value,
        scale = state.drumScale.value,
        alpha = state.drumAlpha.value,
        rimColor = geometry.accentColor,
    )

    if (state.stickAlpha.value > 0f) {
        drawDrumstick(
            fulcrum = drumstickFulcrum(drumCenter = drumCenter),
            angleRadians =
                drumstickAngle(swing = state.stickSwing.value, depress = state.headDepress.value),
            alpha = state.stickAlpha.value,
        )
    }

    if (state.showNotes && state.notes.value > 0f) {
        drawMusicNotes(
            anchor = Offset(x = drumCenter.x, y = drumCenter.y - 22.dp.toPx()),
            progress = state.notes.value,
            color = lerp(start = geometry.accentColor, stop = Color.White, fraction = 0.25f),
            alpha = 1f,
        )
    }
}

/** The stick's ready angle just below level, the strike angle that drops the bead onto the head. */
private const val STICK_REST_ANGLE = 0.14f
private const val STICK_STRIKE_ANGLE = 0.62f

/** A touch more travel as the head gives under the bead, so the stick presses in with the dip. */
private const val STICK_PRESS_ANGLE = 0.05f

/** The grip sits up and back from the head so that at [STICK_STRIKE_ANGLE] the bead lands on it. */
private fun DrawScope.drumstickFulcrum(drumCenter: Offset): Offset =
    drumCenter -
        Offset(x = cos(x = STICK_STRIKE_ANGLE), y = sin(x = STICK_STRIKE_ANGLE)) *
            drumstickLength.toPx()

/**
 * The stick swings from its ready angle down to the strike, pressing a hair further as it lands.
 */
private fun drumstickAngle(swing: Float, depress: Float): Float =
    STICK_REST_ANGLE + (STICK_STRIKE_ANGLE - STICK_REST_ANGLE) * swing + depress * STICK_PRESS_ANGLE

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun BardNormalAttackKnockoutPreview() {
    BardNormalAttackPreview(normalRollMessageType = NormalRollMessageType.Knockout)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun BardNormalAttackDamagePreview() {
    BardNormalAttackPreview(normalRollMessageType = NormalRollMessageType.Damage)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun BardNormalAttackArmorClassHitPreview() {
    BardNormalAttackPreview(normalRollMessageType = NormalRollMessageType.ArmorClassHit)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun BardNormalAttackArmorClassMissPreview() {
    BardNormalAttackPreview(normalRollMessageType = NormalRollMessageType.ArmorClassMiss)
}

@Composable
private fun BardNormalAttackPreview(normalRollMessageType: NormalRollMessageType) {
    val seats = rememberDuoSeats()
    val cue =
        rememberReplayingCue(replayKey = normalRollMessageType) { iteration ->
            CombatAnimationCue.NormalAttack(
                id = "bard-attack-$iteration",
                attackerUserId = PREVIEW_ACTOR_ID,
                attackerClassType = CharacterClassType.Bard,
                defenderUserId = PREVIEW_TARGET_ID,
                normalRollMessageType = normalRollMessageType,
                impersonatedClassType = CharacterClassType.None,
            )
        }
    AbilityAnimationStage(accentClass = CharacterClassType.Bard, seats = seats) {
        BardNormalAttackOverlay(
            animationCue = cue,
            avatarBoundsFor = seats.boundsFor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
