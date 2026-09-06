package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.AnimationCompleteArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaAnimationState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AnimationKey
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AnimationPreview
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.IMPACT_BURST_TAIL_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_TARGET_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.WeaponStrikeGeometry
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.impactBurst
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberDuoAvatars
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.weaponStrikeGeometry
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private const val ATMOSPHERE_IN_MILLIS = 420
private const val ATMOSPHERE_OUT_MILLIS = 420
private const val BOLT_FLIGHT_MILLIS = 320
private const val PUFF_FADE_MILLIS = 220
private const val BOLTS_PER_WAVE = 4
private const val WAVE_STAGGER_MILLIS = 190
private const val INTRA_WAVE_STAGGER_MILLIS = 48
private const val MAX_BOLTS = 12
private const val SWIRL_TURNS = 2.4f
private const val PUFF_PEAK_SCALE = 0.55f
private const val FINALE_PEAK_SCALE = 1.8f
private const val GATHER_BAND_FRACTION = 0.36f
private const val IMPACT_BAND_FRACTION = 0.24f
private const val BOLT_BOW_FRACTION = 0.05f

internal fun spellPotencyBoltCount(mainDamage: Int): Int = mainDamage.coerceIn(minimumValue = 1, maximumValue = MAX_BOLTS)

private fun waveCount(boltCount: Int): Int = (boltCount + BOLTS_PER_WAVE - 1) / BOLTS_PER_WAVE

private fun waveSize(boltCount: Int, wave: Int): Int = minOf(BOLTS_PER_WAVE, boltCount - wave * BOLTS_PER_WAVE)

private fun barrageWindowMillis(boltCount: Int): Int {
    val lastWave = waveCount(boltCount = boltCount) - 1
    val lastLaunch =
        lastWave * WAVE_STAGGER_MILLIS +
            (waveSize(boltCount = boltCount, wave = lastWave) - 1) * INTRA_WAVE_STAGGER_MILLIS
    return lastLaunch + BOLT_FLIGHT_MILLIS + PUFF_FADE_MILLIS
}

internal const val MAGE_SPELL_POTENCY_REACTION_DELAY_MILLIS =
    (ATMOSPHERE_IN_MILLIS + BOLT_FLIGHT_MILLIS).toLong()

@Stable
internal class SpellPotencyState {
    val atmosphereAlpha = Animatable(initialValue = 0f)
    val swirl = Animatable(initialValue = 0f)
    val clock = Animatable(initialValue = 0f)
    val finaleAlpha = Animatable(initialValue = 0f)
    val finaleScale = Animatable(initialValue = 0f)

    var scene by mutableStateOf<WeaponStrikeGeometry?>(value = null)
    var element by mutableStateOf(value = MageSpellElement.Ice)
    var boltCount by mutableStateOf(value = 1)
}

@Composable
internal fun MageSpellPotencyOverlay(
    animatingMessage: ArenaMessageState.SpellPotency,
    mageBounds: Rect,
    defenderBounds: Rect,
    modifier: Modifier = Modifier,
) {
    val state = remember { SpellPotencyState() }
    val density = LocalDensity.current
    val onAnimationComplete = ArenaActionsProvider.current.onAnimationComplete

    LaunchedEffect(key1 = animatingMessage.id) {
        val element = mageElementFor(messageId = animatingMessage.id)
        state.play(
            geometry =
            weaponStrikeGeometry(
                attackerBounds = mageBounds,
                target = defenderBounds.center,
                accentColor = element.palette().glow,
                density = density,
            ),
            element = element,
            boltCount = spellPotencyBoltCount(mainDamage = animatingMessage.mainDamage),
        )

        onAnimationComplete(
            AnimationCompleteArguments(
                messageId = animatingMessage.id,
                animationKey = AnimationKey.AvatarOverlay,
            ),
        )
    }

    Canvas(modifier = modifier) { drawSpellPotency(state = state) }
}

internal suspend fun SpellPotencyState.play(
    geometry: WeaponStrikeGeometry,
    element: MageSpellElement,
    boltCount: Int,
) {
    atmosphereAlpha.snapTo(targetValue = 0f)
    swirl.snapTo(targetValue = 0f)
    clock.snapTo(targetValue = 0f)
    finaleAlpha.snapTo(targetValue = 0f)
    finaleScale.snapTo(targetValue = 0f)
    this.scene = geometry
    this.element = element
    this.boltCount = boltCount

    val windowMillis = barrageWindowMillis(boltCount = boltCount)
    val totalMillis = mageSpellPotencyDurationMillis(mainDamage = boltCount).toInt()

    coroutineScope {
        launch {
            swirl.animateTo(
                targetValue = SWIRL_TURNS,
                animationSpec = tween(durationMillis = totalMillis, easing = LinearEasing),
            )
        }
        launch {
            atmosphereAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = ATMOSPHERE_IN_MILLIS),
            )
            clock.animateTo(
                targetValue = windowMillis.toFloat(),
                animationSpec = tween(durationMillis = windowMillis, easing = LinearEasing),
            )
            impactBurst(
                impactAlpha = finaleAlpha,
                impactScale = finaleScale,
                peakScale = FINALE_PEAK_SCALE,
            )
            atmosphereAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = ATMOSPHERE_OUT_MILLIS),
            )
        }
    }

    this.scene = null
}

private fun mageSpellPotencyDurationMillis(mainDamage: Int): Long = ATMOSPHERE_IN_MILLIS +
    barrageWindowMillis(boltCount = spellPotencyBoltCount(mainDamage = mainDamage)) +
    IMPACT_BURST_TAIL_MILLIS +
    ATMOSPHERE_OUT_MILLIS

internal fun DrawScope.drawSpellPotency(state: SpellPotencyState) {
    val geometry = state.scene ?: return

    drawMageAtmosphere(
        element = state.element,
        epicenter = geometry.target,
        swirl = state.swirl.value,
        alpha = state.atmosphereAlpha.value,
    )

    val clockMillis = state.clock.value
    var boltPhase = 0
    for (wave in 0 until waveCount(boltCount = state.boltCount)) {
        val size = waveSize(boltCount = state.boltCount, wave = wave)
        for (indexInWave in 0 until size) {
            val slot = if (size == 1) 0f else (indexInWave / (size - 1f)) * 2f - 1f
            val launchMillis = wave * WAVE_STAGGER_MILLIS + indexInWave * INTRA_WAVE_STAGGER_MILLIS
            drawBarrageBolt(
                geometry = geometry,
                element = state.element,
                slot = slot,
                phase = boltPhase,
                launchMillis = launchMillis,
                clockMillis = clockMillis,
            )
            boltPhase++
        }
    }

    val finaleAlpha = state.finaleAlpha.value
    if (finaleAlpha > 0f && state.finaleScale.value > 0f) {
        drawMageImpact(
            element = state.element,
            center = geometry.target,
            scale = state.finaleScale.value,
            alpha = finaleAlpha,
        )
    }
}

private fun DrawScope.drawBarrageBolt(
    geometry: WeaponStrikeGeometry,
    element: MageSpellElement,
    slot: Float,
    phase: Int,
    launchMillis: Int,
    clockMillis: Float,
) {
    val elapsed = clockMillis - launchMillis
    if (elapsed < 0f) return

    val origin = geometry.start + Offset(x = slot * size.width * GATHER_BAND_FRACTION / 2f, y = 0f)
    val impact = geometry.target + Offset(x = slot * size.width * IMPACT_BAND_FRACTION / 2f, y = 0f)
    val travel = (elapsed / BOLT_FLIGHT_MILLIS).coerceIn(minimumValue = 0f, maximumValue = 1f)

    if (travel < 1f) {
        val flight = impact - origin
        val length = flight.getDistance()
        val perpendicular =
            if (length > 0f) Offset(x = -flight.y / length, y = flight.x / length) else Offset.Zero
        val bowSign = if (phase % 2 == 0) 1f else -1f
        val bowAmount = size.width * BOLT_BOW_FRACTION * bowSign
        val tailFraction = (travel - BOLT_TRAIL_SPAN).coerceAtLeast(minimumValue = 0f)

        val position =
            lerp(start = origin, stop = impact, fraction = travel) +
                perpendicular * (sin(x = travel * PI.toFloat()) * bowAmount)
        val tail =
            lerp(start = origin, stop = impact, fraction = tailFraction) +
                perpendicular * (sin(x = tailFraction * PI.toFloat()) * bowAmount)
        drawMageBolt(element = element, tail = tail, position = position, alpha = 1f)
        return
    }

    val sinceLand = elapsed - BOLT_FLIGHT_MILLIS
    val puffAlpha =
        (1f - sinceLand / PUFF_FADE_MILLIS).coerceIn(minimumValue = 0f, maximumValue = 1f)
    if (puffAlpha > 0f) {
        drawMageImpact(
            element = element,
            center = impact,
            scale = PUFF_PEAK_SCALE,
            alpha = puffAlpha,
        )
    }
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MageSpellPotencyIcePreview() {
    MageSpellPotencyPreview(element = MageSpellElement.Ice, mainDamage = 8)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MageSpellPotencyLightningPreview() {
    MageSpellPotencyPreview(element = MageSpellElement.Lightning, mainDamage = 8)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MageSpellPotencyFirePreview() {
    MageSpellPotencyPreview(element = MageSpellElement.Fire, mainDamage = 8)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MageSpellPotencyLargeVolleyPreview() {
    MageSpellPotencyPreview(element = MageSpellElement.Ice, mainDamage = 12)
}

@Composable
private fun MageSpellPotencyPreview(element: MageSpellElement, mainDamage: Int) {
    val avatars = rememberDuoAvatars()
    AnimationPreview(
        accentClass = CharacterClassType.Mage,
        avatars = avatars,
        buildMessage = { id ->
            ArenaMessageState.SpellPotency(
                id = mageElementPreviewMessageId(element = element, seedId = id),
                mageUserId = PREVIEW_ACTOR_ID,
                mageName = "Mage",
                mainTargetUserId = PREVIEW_TARGET_ID,
                mainTargetClassName = "Rogue",
                mainTargetClassType = CharacterClassType.Rogue,
                firstDie = 6,
                secondDie = 6,
                mainDamage = mainDamage,
                splashDamage = mainDamage / 2,
                splashTargetUserIds = emptyList(),
                isMainTargetKnockedOut = false,
                animation = ArenaAnimationState.FlashAndAvatar(),
                gestures = emptyMap(),
            )
        },
    ) { message ->
        MageSpellPotencyOverlay(
            animatingMessage = message,
            mageBounds = avatars.boundsFor(PREVIEW_ACTOR_ID),
            defenderBounds = avatars.boundsFor(PREVIEW_TARGET_ID),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
