package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AbilityAnimationStage
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.IMPACT_BURST_TAIL_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.NormalAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_TARGET_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.WeaponStrikeGeometry
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.impactBurst
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.projectileStrikeGeometry
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberDuoSeats
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberReplayingCue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val ICE_CHARGE_MILLIS = 150
internal const val ICE_FLIGHT_MILLIS = 300L

private const val ICE_FADE_MILLIS = 160
private const val ICE_KNOCKOUT_EXTRA_MILLIS = 120L

/** How far behind the bolt head its trail streams, as a fraction of the whole flight line. */
internal const val BOLT_TRAIL_SPAN = 0.28f

/**
 * When the bolt reaches the defender, so the shared defender reaction (SS-191) flinches as it
 * lands: the bolt charges at the Mage's hand, then flies across the gap.
 */
internal val MAGE_BOLT_REACTION_DELAY_MILLIS: Long = ICE_CHARGE_MILLIS + ICE_FLIGHT_MILLIS

/**
 * The flight of one Mage cast bolt. The choreography drives these Animatables and the draw layer
 * reads them, so a cast redraws the canvas without recomposing anything.
 */
@Stable
internal class MageBoltState {
    val travel = Animatable(initialValue = 0f)
    val boltAlpha = Animatable(initialValue = 0f)
    val impactAlpha = Animatable(initialValue = 0f)
    val impactScale = Animatable(initialValue = 0f)

    var scene by mutableStateOf<WeaponStrikeGeometry?>(value = null)
    var element by mutableStateOf(value = MageSpellElement.Ice)
}

/**
 * The Mage's normal attack (SS-198): a focused elemental bolt cast at the target. The element is
 * picked per cast from the cue id, so the same spell resolves everywhere; the bolt charges at the
 * Mage, flies across on a snowflake trail, and shatters on the defender — or slips past into empty
 * air on a miss. Geometry comes from the measured avatar bounds via [avatarBoundsFor]. Sits out any
 * cue that is not a Mage normal attack; a doubles roll is Spell Potency (SS-210) instead.
 */
@Composable
internal fun MageNormalAttackOverlay(
    animatingMessage: ArenaMessageState?,
    attackerBounds: Rect,
    defenderBounds: Rect,
    modifier: Modifier = Modifier,
) {
    val state = remember { MageBoltState() }
    val density = LocalDensity.current

    NormalAttackOverlay(
        animatingMessage = animatingMessage,
        attackerClassType = CharacterClassType.Mage,
        avatarBoundsFor = avatarBoundsFor,
        onCancel = { state.scene = null },
        play = { cue, attackerBounds, defenderBounds ->
            val element = mageElementFor(cueId = cue.id)
            state.play(
                geometry =
                    projectileStrikeGeometry(
                        cue = cue,
                        attackerBounds = attackerBounds,
                        defenderBounds = defenderBounds,
                        accentColor = element.palette().glow,
                        density = density,
                    ),
                element = element,
                normalRollMessageType = cue.normalRollMessageType,
            )
        },
        modifier = modifier,
        draw = { drawMageCast(state = state) },
    )
}

/**
 * How long one Mage cast bolt runs, so the message sequencer can hold the event until the bolt
 * charges, flies, and shatters instead of cutting it off (SS-201). A knockout holds the shatter a
 * touch longer.
 */
internal fun mageNormalAttackDurationMillis(normalRollMessageType: NormalRollMessageType): Long =
    ICE_CHARGE_MILLIS +
        ICE_FLIGHT_MILLIS +
        IMPACT_BURST_TAIL_MILLIS +
        ICE_FADE_MILLIS +
        if (normalRollMessageType == NormalRollMessageType.Knockout) {
            ICE_KNOCKOUT_EXTRA_MILLIS
        } else {
            0L
        }

internal suspend fun MageBoltState.play(
    geometry: WeaponStrikeGeometry,
    element: MageSpellElement,
    normalRollMessageType: NormalRollMessageType,
) {
    travel.snapTo(targetValue = 0f)
    boltAlpha.snapTo(targetValue = 0f)
    impactAlpha.snapTo(targetValue = 0f)
    impactScale.snapTo(targetValue = 0f)
    this.scene = geometry
    this.element = element

    boltAlpha.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = ICE_CHARGE_MILLIS))
    travel.animateTo(
        targetValue = 1f,
        animationSpec =
            tween(durationMillis = ICE_FLIGHT_MILLIS.toInt(), easing = FastOutLinearInEasing),
    )

    if (normalRollMessageType == NormalRollMessageType.ArmorClassMiss) {
        boltAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = ICE_FADE_MILLIS),
        )
    } else {
        coroutineScope {
            launch {
                impactBurst(
                    impactAlpha = impactAlpha,
                    impactScale = impactScale,
                    peakScale = shatterPeakScale(normalRollMessageType = normalRollMessageType),
                )
            }
            launch {
                boltAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = ICE_FADE_MILLIS),
                )
            }
        }
    }

    if (normalRollMessageType == NormalRollMessageType.Knockout) {
        delay(timeMillis = ICE_KNOCKOUT_EXTRA_MILLIS)
    }
    this.scene = null
}

private fun shatterPeakScale(normalRollMessageType: NormalRollMessageType): Float =
    when (normalRollMessageType) {
        NormalRollMessageType.ArmorClassHit -> 0.8f
        NormalRollMessageType.Damage -> 1f
        NormalRollMessageType.Knockout -> 1.35f
        NormalRollMessageType.ArmorClassMiss -> 0f
    }

internal fun DrawScope.drawMageCast(state: MageBoltState) {
    val geometry = state.scene ?: return

    val boltAlpha = state.boltAlpha.value
    if (boltAlpha > 0f) {
        val position =
            lerp(start = geometry.start, stop = geometry.target, fraction = state.travel.value)
        val tail =
            lerp(
                start = geometry.start,
                stop = geometry.target,
                fraction = (state.travel.value - BOLT_TRAIL_SPAN).coerceAtLeast(minimumValue = 0f),
            )
        drawMageBolt(element = state.element, tail = tail, position = position, alpha = boltAlpha)
    }

    val impactAlpha = state.impactAlpha.value
    if (impactAlpha > 0f && state.impactScale.value > 0f) {
        drawMageImpact(
            element = state.element,
            center = geometry.target,
            scale = state.impactScale.value,
            alpha = impactAlpha,
        )
    }
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MageIceboltKnockoutPreview() {
    MageNormalAttackPreview(
        element = MageSpellElement.Ice,
        normalRollMessageType = NormalRollMessageType.Knockout,
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MageIceboltDamagePreview() {
    MageNormalAttackPreview(
        element = MageSpellElement.Ice,
        normalRollMessageType = NormalRollMessageType.Damage,
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MageIceboltArmorClassMissPreview() {
    MageNormalAttackPreview(
        element = MageSpellElement.Ice,
        normalRollMessageType = NormalRollMessageType.ArmorClassMiss,
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MageLightningboltDamagePreview() {
    MageNormalAttackPreview(
        element = MageSpellElement.Lightning,
        normalRollMessageType = NormalRollMessageType.Damage,
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MageLightningboltKnockoutPreview() {
    MageNormalAttackPreview(
        element = MageSpellElement.Lightning,
        normalRollMessageType = NormalRollMessageType.Knockout,
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MageFireballDamagePreview() {
    MageNormalAttackPreview(
        element = MageSpellElement.Fire,
        normalRollMessageType = NormalRollMessageType.Damage,
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MageFireballKnockoutPreview() {
    MageNormalAttackPreview(
        element = MageSpellElement.Fire,
        normalRollMessageType = NormalRollMessageType.Knockout,
    )
}

@Composable
private fun MageNormalAttackPreview(
    element: MageSpellElement,
    normalRollMessageType: NormalRollMessageType,
) {
    val seats = rememberDuoSeats()
    val cue =
        rememberReplayingCue(replayKey = element to normalRollMessageType) { iteration ->
            CombatAnimationCue.NormalAttack(
                id = mageElementPreviewCueId(element = element, iteration = iteration),
                attackerUserId = PREVIEW_ACTOR_ID,
                attackerClassType = CharacterClassType.Mage,
                defenderUserId = PREVIEW_TARGET_ID,
                normalRollMessageType = normalRollMessageType,
                impersonatedClassType = CharacterClassType.None,
            )
        }
    AbilityAnimationStage(accentClass = CharacterClassType.Mage, seats = seats) {
        MageNormalAttackOverlay(
            animationCue = cue,
            avatarBoundsFor = seats.boundsFor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
