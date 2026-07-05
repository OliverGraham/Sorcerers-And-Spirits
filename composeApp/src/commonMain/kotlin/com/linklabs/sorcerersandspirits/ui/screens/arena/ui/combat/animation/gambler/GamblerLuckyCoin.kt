package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AbilityAnimationStage
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_TARGET_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberDuoSeats
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberReplayingCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberReplayingCues
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberSoloSeat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val COIN_TILT_DEGREES = 34f
private const val FLIP_TURNS = 3
private const val TOSS_MILLIS = 850
private const val COIN_APPEAR_MILLIS = 100
private const val ARMOR_PULSE_MILLIS = 500
private const val FLIP_REST_HOLD_MILLIS = 1500L
private const val COIN_FADE_MILLIS = 300

private const val COIN_THROW_FLIGHT_MILLIS = 480
private const val THROW_SPIN_TURNS = 4
private const val THROW_IMPACT_MILLIS = 220

/**
 * How long a defensive Lucky Coin flip runs — the toss, the armor pulse when the bonus holds, the
 * rest, and the fade — so the message sequencer holds the event until the coin settles (SS-201).
 */
internal fun gamblerLuckyCoinFlipDurationMillis(didStopAttack: Boolean): Long {
    val armorPulseMillis = if (didStopAttack) ARMOR_PULSE_MILLIS.toLong() else 0L
    return COIN_APPEAR_MILLIS +
        TOSS_MILLIS +
        armorPulseMillis +
        FLIP_REST_HOLD_MILLIS +
        COIN_FADE_MILLIS
}

/** How long the winning High Roller coin lofts up before it rests, awaiting the throw (SS-201). */
internal fun gamblerLuckyCoinHighRollerDurationMillis(): Long =
    (COIN_APPEAR_MILLIS + TOSS_MILLIS).toLong()

/** How long the High Roller throw flies into the attacker and bursts on landing (SS-201). */
internal fun gamblerLuckyCoinThrowDurationMillis(): Long =
    (COIN_THROW_FLIGHT_MILLIS + THROW_IMPACT_MILLIS).toLong()

private const val LUCKY_FACE = 6
private const val PIP_OFFSET = 0.42f
private const val COIN_THICKNESS_FRACTION = 0.17f
private const val RIM_SEGMENTS = 40
private const val SIDE_SHADE_MIN = 0.5f
private const val DEGREES_TO_RADIANS = PI.toFloat() / 180f

private val coinFace = Color(color = 0xFFF4C752)
private val coinRim = Color(color = 0xFF9A6B1E)
private val coinShine = Color(color = 0xFFFBE9B0)
private val coinPip = Color(color = 0xFF5A3E12)
private val luckyFace = Color(color = 0xFFFFD24A)
private val luckyPip = Color(color = 0xFF7A4E12)
private val luckyGlow = Color(color = 0xFFFFF3D0)
private val impactCore = Color(color = 0xFFFFF3D0)
private val groundShadow = Color(color = 0x55000000)

/**
 * Which lucky-coin figure is on screen: a defensive flip, the winning High Roller flip, or the
 * throw.
 */
private enum class LuckyCoinMode {
    Flip,
    HighRoller,
    Throw,
}

/**
 * The measured geometry of one lucky-coin figure (SS-193), laid out from the Gambler's avatar
 * bounds — and the attacker's, for the throw. [restCenter] is the Gambler's feet, where the coin
 * lands and rests; the throw lofts from there to [throwTarget].
 */
@Immutable
private data class LuckyCoinScene(
    val mode: LuckyCoinMode,
    val faceValue: Int,
    val didStopAttack: Boolean,
    val isKnockout: Boolean,
    val radiusPx: Float,
    val thicknessPx: Float,
    val restCenter: Offset,
    val apexRisePx: Float,
    val throwTarget: Offset,
    val throwLobPx: Float,
    val accentColor: Color,
)

/**
 * The animation channels of one lucky-coin figure (SS-193): [arc] lofts the tossed coin and
 * [tumble] spins it end over end onto its face, [armorPulse] rings out when the added armor holds,
 * and [travel]/[throwSpin]/[impact] carry the High Roller throw into the attacker. [coinAlpha]
 * fades the coin in and away.
 */
@Stable
private class LuckyCoinEffectState {
    val coinAlpha = Animatable(initialValue = 0f)
    val arc = Animatable(initialValue = 0f)
    val tumble = Animatable(initialValue = 0f)
    val armorPulse = Animatable(initialValue = 0f)
    val travel = Animatable(initialValue = 0f)
    val throwSpin = Animatable(initialValue = 0f)
    val impactScale = Animatable(initialValue = 0f)
    val impactAlpha = Animatable(initialValue = 0f)

    var scene by mutableStateOf<LuckyCoinScene?>(value = null)
}

/**
 * The Gambler's Lucky Coin effect (SS-193): when the Gambler's armor is broken, a golden coin is
 * tossed up over their seat and tumbles end over end in three dimensions before landing on its face
 * at the Gambler's feet, sitting on the ground as a solid coin you could pick up — ringing out a
 * shield pulse when the added armor turns the attack away. On a High Roller the coin lands on its
 * lucky face and stays put until the Gambler rolls, then is flung across into the attacker for
 * payback damage. Anchored to the measured avatar bounds via [avatarBoundsFor], and sits out any
 * cue that is not a lucky-coin cue.
 */
@Composable
internal fun LuckyCoinOverlay(
    animationCue: CombatAnimationCue?,
    avatarBoundsFor: (String) -> Rect?,
    modifier: Modifier = Modifier,
) {
    val state = remember { LuckyCoinEffectState() }
    val density = LocalDensity.current
    val latestCue by rememberUpdatedState(newValue = animationCue)

    LaunchedEffect(key1 = Unit) {
        var lastPlayedCueId = latestCue?.id
        var playJob: Job? = null
        snapshotFlow { latestCue }
            .collect { cue ->
                if (cue == null) return@collect
                if (cue.id == lastPlayedCueId) return@collect
                val scene =
                    luckyCoinScene(cue = cue, avatarBoundsFor = avatarBoundsFor, density = density)
                        ?: return@collect
                lastPlayedCueId = cue.id
                playJob?.cancel()
                playJob = launch { state.play(scene = scene) }
            }
    }

    Canvas(modifier = modifier) { drawLuckyCoinScene(state = state) }
}

private fun luckyCoinScene(
    cue: CombatAnimationCue,
    avatarBoundsFor: (String) -> Rect?,
    density: Density,
): LuckyCoinScene? =
    when (cue) {
        is CombatAnimationCue.LuckyCoinFlip -> {
            val bounds = avatarBoundsFor(cue.gamblerUserId) ?: return null
            flipScene(
                bounds = bounds,
                mode = LuckyCoinMode.Flip,
                faceValue = cue.faceValue,
                didStopAttack = cue.didStopAttack,
            )
        }

        is CombatAnimationCue.LuckyCoinHighRoller -> {
            val bounds = avatarBoundsFor(cue.gamblerUserId) ?: return null
            flipScene(
                bounds = bounds,
                mode = LuckyCoinMode.HighRoller,
                faceValue = LUCKY_FACE,
                didStopAttack = false,
            )
        }

        is CombatAnimationCue.LuckyCoinHighRollerThrow -> {
            val gamblerBounds = avatarBoundsFor(cue.gamblerUserId) ?: return null
            val attackerBounds = avatarBoundsFor(cue.attackerUserId) ?: return null
            throwScene(
                gamblerBounds = gamblerBounds,
                attackerBounds = attackerBounds,
                isKnockout = cue.isKnockout,
                density = density,
            )
        }

        else -> null
    }

private fun flipScene(
    bounds: Rect,
    mode: LuckyCoinMode,
    faceValue: Int,
    didStopAttack: Boolean,
): LuckyCoinScene {
    val radius = coinRadiusFor(bounds = bounds)
    return LuckyCoinScene(
        mode = mode,
        faceValue = faceValue,
        didStopAttack = didStopAttack,
        isKnockout = false,
        radiusPx = radius,
        thicknessPx = radius * COIN_THICKNESS_FRACTION,
        restCenter = coinRestCenter(bounds = bounds),
        apexRisePx = bounds.height * 0.5f,
        throwTarget = Offset.Zero,
        throwLobPx = 0f,
        accentColor = CharacterClassType.Gambler.toColor(),
    )
}

private fun throwScene(
    gamblerBounds: Rect,
    attackerBounds: Rect,
    isKnockout: Boolean,
    density: Density,
): LuckyCoinScene {
    val radius = coinRadiusFor(bounds = gamblerBounds) * 0.85f
    return LuckyCoinScene(
        mode = LuckyCoinMode.Throw,
        faceValue = LUCKY_FACE,
        didStopAttack = false,
        isKnockout = isKnockout,
        radiusPx = radius,
        thicknessPx = radius * COIN_THICKNESS_FRACTION,
        restCenter = coinRestCenter(bounds = gamblerBounds),
        apexRisePx = 0f,
        throwTarget = attackerBounds.center,
        throwLobPx = with(density) { 48.dp.toPx() },
        accentColor = CharacterClassType.Gambler.toColor(),
    )
}

private fun coinRestCenter(bounds: Rect): Offset =
    Offset(x = bounds.center.x, y = bounds.bottom - bounds.height * 0.05f)

private fun coinRadiusFor(bounds: Rect): Float = min(bounds.width, bounds.height) * 0.16f

private suspend fun LuckyCoinEffectState.play(scene: LuckyCoinScene) {
    coinAlpha.snapTo(targetValue = 0f)
    arc.snapTo(targetValue = 0f)
    tumble.snapTo(targetValue = 0f)
    armorPulse.snapTo(targetValue = 0f)
    travel.snapTo(targetValue = 0f)
    throwSpin.snapTo(targetValue = 0f)
    impactScale.snapTo(targetValue = 0f)
    impactAlpha.snapTo(targetValue = 0f)
    this.scene = scene

    when (scene.mode) {
        LuckyCoinMode.Flip -> {
            toss()
            if (scene.didStopAttack) {
                armorPulse.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = ARMOR_PULSE_MILLIS),
                )
            }
            delay(timeMillis = FLIP_REST_HOLD_MILLIS)
            coinAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = COIN_FADE_MILLIS),
            )
            this.scene = null
        }

        LuckyCoinMode.HighRoller -> toss()

        LuckyCoinMode.Throw -> {
            throwCoin(isKnockout = scene.isKnockout)
            this.scene = null
        }
    }
}

/**
 * The toss: the coin fades in, lofts up and back down while tumbling end over end onto its face.
 */
private suspend fun LuckyCoinEffectState.toss() {
    coinAlpha.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = COIN_APPEAR_MILLIS),
    )
    coroutineScope {
        launch {
            arc.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = TOSS_MILLIS, easing = LinearEasing),
            )
        }
        launch {
            tumble.animateTo(
                targetValue = FLIP_TURNS * 360f,
                animationSpec = tween(durationMillis = TOSS_MILLIS, easing = LinearOutSlowInEasing),
            )
        }
    }
}

/** The High Roller throw: the resting coin lofts across into the attacker and bursts on landing. */
private suspend fun LuckyCoinEffectState.throwCoin(isKnockout: Boolean) {
    coinAlpha.snapTo(targetValue = 1f)
    coroutineScope {
        launch {
            travel.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(durationMillis = COIN_THROW_FLIGHT_MILLIS, easing = FastOutLinearInEasing),
            )
        }
        launch {
            throwSpin.animateTo(
                targetValue = THROW_SPIN_TURNS * 360f,
                animationSpec =
                    tween(durationMillis = COIN_THROW_FLIGHT_MILLIS, easing = LinearEasing),
            )
        }
    }
    coroutineScope {
        launch { impactBurst(peakScale = if (isKnockout) 1.4f else 1f) }
        launch {
            coinAlpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 110))
        }
    }
}

private suspend fun LuckyCoinEffectState.impactBurst(peakScale: Float) {
    impactAlpha.snapTo(targetValue = 1f)
    impactScale.snapTo(targetValue = 0f)
    coroutineScope {
        launch {
            impactScale.animateTo(
                targetValue = peakScale,
                animationSpec = tween(durationMillis = THROW_IMPACT_MILLIS),
            )
        }
        launch {
            delay(timeMillis = 70L)
            impactAlpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 200))
        }
    }
}

private fun DrawScope.drawLuckyCoinScene(state: LuckyCoinEffectState) {
    val scene = state.scene ?: return
    when (scene.mode) {
        LuckyCoinMode.Flip,
        LuckyCoinMode.HighRoller -> drawTossedCoin(scene = scene, state = state)

        LuckyCoinMode.Throw -> drawThrownCoin(scene = scene, state = state)
    }
}

private fun DrawScope.drawTossedCoin(scene: LuckyCoinScene, state: LuckyCoinEffectState) {
    val arc = state.arc.value
    val alpha = state.coinAlpha.value
    if (alpha <= 0.01f) return

    val liftPx = scene.apexRisePx * sin(PI.toFloat() * arc)
    val center = Offset(x = scene.restCenter.x, y = scene.restCenter.y - liftPx)
    val isGold = scene.mode == LuckyCoinMode.HighRoller

    val grounded = 1f - sin(PI.toFloat() * arc)
    drawGroundShadow(
        center = scene.restCenter,
        radius = scene.radiusPx,
        grounded = grounded,
        alpha = alpha,
    )

    if (isGold) {
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors = listOf(luckyGlow.copy(alpha = 0.45f), Color.Transparent),
                    center = center,
                    radius = scene.radiusPx * 2f,
                ),
            radius = scene.radiusPx * 2f,
            center = center,
            alpha = alpha,
        )
    }

    if (state.armorPulse.value > 0f) {
        drawArmorPulse(
            center = center,
            radius = scene.radiusPx,
            pulse = state.armorPulse.value,
            accentColor = scene.accentColor,
        )
    }

    drawCoin3d(
        center = center,
        radius = scene.radiusPx,
        thickness = scene.thicknessPx,
        tumbleDegrees = state.tumble.value,
        faceValue = scene.faceValue,
        isGold = isGold,
        alpha = alpha,
    )
}

private fun DrawScope.drawThrownCoin(scene: LuckyCoinScene, state: LuckyCoinEffectState) {
    val alpha = state.coinAlpha.value
    if (alpha > 0.01f) {
        val travel = state.travel.value
        val flat = lerp(start = scene.restCenter, stop = scene.throwTarget, fraction = travel)
        val center = Offset(x = flat.x, y = flat.y - scene.throwLobPx * sin(PI.toFloat() * travel))
        drawLine(
            brush =
                Brush.linearGradient(
                    colors = listOf(Color.Transparent, scene.accentColor.copy(alpha = 0.4f)),
                    start = scene.restCenter,
                    end = center,
                ),
            start = scene.restCenter,
            end = center,
            strokeWidth = 5.dp.toPx(),
            alpha = alpha * 0.5f,
        )
        drawCoin3d(
            center = center,
            radius = scene.radiusPx,
            thickness = scene.thicknessPx,
            tumbleDegrees = state.throwSpin.value,
            faceValue = scene.faceValue,
            isGold = true,
            alpha = alpha,
        )
    }

    val impactAlpha = state.impactAlpha.value
    val isBursting = impactAlpha > 0f && state.impactScale.value > 0f
    if (isBursting) {
        drawImpactBurst(
            center = scene.throwTarget,
            scale = state.impactScale.value,
            alpha = impactAlpha,
            accentColor = scene.accentColor,
        )
    }
}

/**
 * A solid coin drawn as a true cylinder (SS-193): viewed from a fixed three-quarter tilt like the
 * dice, its rolled face reads on top as a foreshortened ellipse with the milled rim wrapping the
 * front below it, so it reads as a physical coin in space. [tumbleDegrees] pitches it end over end;
 * as it turns past edge-on the lit rim shows and then the minted reverse.
 */
private fun DrawScope.drawCoin3d(
    center: Offset,
    radius: Float,
    thickness: Float,
    tumbleDegrees: Float,
    faceValue: Int,
    isGold: Boolean,
    alpha: Float,
) {
    val pitch = (COIN_TILT_DEGREES + tumbleDegrees) * DEGREES_TO_RADIANS
    val cosPitch = cos(pitch)
    val sinPitch = sin(pitch)

    val topRim =
        rimPoints(
            center = center,
            radius = radius,
            y = thickness,
            cosPitch = cosPitch,
            sinPitch = sinPitch,
        )
    val bottomRim =
        rimPoints(
            center = center,
            radius = radius,
            y = -thickness,
            cosPitch = cosPitch,
            sinPitch = sinPitch,
        )

    drawCoinEdge(topRim = topRim, bottomRim = bottomRim, cosPitch = cosPitch, alpha = alpha)

    val isTopFacing = sinPitch >= 0f
    val face = if (isGold) luckyFace else coinFace
    val visibleRim = if (isTopFacing) topRim else bottomRim
    val capPath = rimPath(rim = visibleRim)

    drawPath(path = capPath, color = face, alpha = alpha)
    drawPath(path = capPath, color = coinRim, style = Stroke(width = radius * 0.08f), alpha = alpha)

    if (isTopFacing) {
        drawCoinPips(
            center = center,
            radius = radius,
            thickness = thickness,
            cosPitch = cosPitch,
            sinPitch = sinPitch,
            faceValue = faceValue,
            color = if (isGold) luckyPip else coinPip,
            alpha = alpha,
        )
    } else {
        drawCoinBack(
            center = center,
            radius = radius,
            thickness = thickness,
            cosPitch = cosPitch,
            sinPitch = sinPitch,
            alpha = alpha,
        )
    }
}

/** The projected rim of one cap: [y] is the cap's height along the coin's thickness axis. */
private fun rimPoints(
    center: Offset,
    radius: Float,
    y: Float,
    cosPitch: Float,
    sinPitch: Float,
): List<Offset> =
    (0..RIM_SEGMENTS).map { index ->
        val theta = (index.toFloat() / RIM_SEGMENTS) * 2f * PI.toFloat()
        projectCoinPoint(
            center = center,
            localX = radius * cos(theta),
            localY = y,
            localZ = radius * sin(theta),
            cosPitch = cosPitch,
            sinPitch = sinPitch,
        )
    }

/** Orthographic projection of a coin-local point, pitched about the horizontal screen axis. */
private fun projectCoinPoint(
    center: Offset,
    localX: Float,
    localY: Float,
    localZ: Float,
    cosPitch: Float,
    sinPitch: Float,
): Offset {
    val rotatedY = localY * cosPitch - localZ * sinPitch
    return Offset(x = center.x + localX, y = center.y - rotatedY)
}

private fun rimPath(rim: List<Offset>): Path =
    Path().apply {
        moveTo(x = rim.first().x, y = rim.first().y)
        rim.drop(n = 1).forEach { lineTo(x = it.x, y = it.y) }
        close()
    }

/** The milled edge: the front-facing band of quads between the two rims, shaded by their facing. */
private fun DrawScope.drawCoinEdge(
    topRim: List<Offset>,
    bottomRim: List<Offset>,
    cosPitch: Float,
    alpha: Float,
) {
    repeat(times = RIM_SEGMENTS) { index ->
        val theta = ((index + 0.5f) / RIM_SEGMENTS) * 2f * PI.toFloat()
        val facing = sin(theta) * cosPitch
        if (facing <= 0f) return@repeat

        val quad =
            Path().apply {
                moveTo(x = topRim[index].x, y = topRim[index].y)
                lineTo(x = topRim[index + 1].x, y = topRim[index + 1].y)
                lineTo(x = bottomRim[index + 1].x, y = bottomRim[index + 1].y)
                lineTo(x = bottomRim[index].x, y = bottomRim[index].y)
                close()
            }
        val brightness = (SIDE_SHADE_MIN + (1f - SIDE_SHADE_MIN) * facing).coerceIn(0f, 1f)
        drawPath(
            path = quad,
            color = colorLerp(start = coinRim, stop = coinShine, fraction = brightness),
            alpha = alpha,
        )
    }
}

private fun DrawScope.drawCoinPips(
    center: Offset,
    radius: Float,
    thickness: Float,
    cosPitch: Float,
    sinPitch: Float,
    faceValue: Int,
    color: Color,
    alpha: Float,
) {
    val pipRadius = radius * 0.11f
    pipLayout(faceValue = faceValue).forEach { unit ->
        val position =
            projectCoinPoint(
                center = center,
                localX = unit.x * radius,
                localY = thickness,
                localZ = unit.y * radius,
                cosPitch = cosPitch,
                sinPitch = sinPitch,
            )
        drawCircle(color = color, radius = pipRadius, center = position, alpha = alpha)
    }
}

/** The minted reverse: a ring inset from the rim with a small central boss, projected in place. */
private fun DrawScope.drawCoinBack(
    center: Offset,
    radius: Float,
    thickness: Float,
    cosPitch: Float,
    sinPitch: Float,
    alpha: Float,
) {
    val ring =
        (0..RIM_SEGMENTS).map { index ->
            val theta = (index.toFloat() / RIM_SEGMENTS) * 2f * PI.toFloat()
            projectCoinPoint(
                center = center,
                localX = radius * 0.6f * cos(theta),
                localY = -thickness,
                localZ = radius * 0.6f * sin(theta),
                cosPitch = cosPitch,
                sinPitch = sinPitch,
            )
        }
    drawPath(
        path = rimPath(rim = ring),
        color = coinRim,
        style = Stroke(width = radius * 0.09f),
        alpha = alpha * 0.85f,
    )
    drawCircle(color = coinRim, radius = radius * 0.14f, center = center, alpha = alpha * 0.85f)
}

/** The die-face dot pattern for a value, in unit coordinates about the coin center. */
private fun pipLayout(faceValue: Int): List<Offset> {
    val near = PIP_OFFSET
    return when (faceValue) {
        1 -> listOf(Offset.Zero)

        2 -> listOf(Offset(x = -near, y = -near), Offset(x = near, y = near))

        3 -> listOf(Offset(x = -near, y = -near), Offset.Zero, Offset(x = near, y = near))

        4 ->
            listOf(
                Offset(x = -near, y = -near),
                Offset(x = near, y = -near),
                Offset(x = -near, y = near),
                Offset(x = near, y = near),
            )

        5 ->
            listOf(
                Offset(x = -near, y = -near),
                Offset(x = near, y = -near),
                Offset.Zero,
                Offset(x = -near, y = near),
                Offset(x = near, y = near),
            )

        else ->
            listOf(
                Offset(x = -near, y = -near),
                Offset(x = -near, y = 0f),
                Offset(x = -near, y = near),
                Offset(x = near, y = -near),
                Offset(x = near, y = 0f),
                Offset(x = near, y = near),
            )
    }
}

/** The soft contact shadow that grounds the coin, strongest when it is resting at the feet. */
private fun DrawScope.drawGroundShadow(
    center: Offset,
    radius: Float,
    grounded: Float,
    alpha: Float,
) {
    val shadowWidth = radius * (1.5f + grounded * 0.6f)
    val shadowHeight = radius * 0.42f
    drawOval(
        color = groundShadow,
        topLeft =
            Offset(x = center.x - shadowWidth, y = center.y + radius * 0.3f - shadowHeight / 2f),
        size = Size(width = shadowWidth * 2f, height = shadowHeight),
        alpha = alpha * (0.25f + grounded * 0.4f),
    )
}

/** The shield pulse when the added armor holds: an expanding ring in the Gambler's gold. */
private fun DrawScope.drawArmorPulse(
    center: Offset,
    radius: Float,
    pulse: Float,
    accentColor: Color,
) {
    val ringRadius = radius * (1.2f + pulse * 1.8f)
    drawCircle(
        color = accentColor,
        radius = ringRadius,
        center = center,
        style = Stroke(width = radius * 0.16f * (1f - pulse)),
        alpha = (1f - pulse) * 0.9f,
    )
}

/** The payback flare: a white-cored burst rimmed in the Gambler's gold at the point of impact. */
private fun DrawScope.drawImpactBurst(
    center: Offset,
    scale: Float,
    alpha: Float,
    accentColor: Color,
) {
    val radius = 40.dp.toPx() * scale
    drawCircle(
        brush =
            Brush.radialGradient(
                colors = listOf(impactCore, accentColor.copy(alpha = 0.8f), Color.Transparent),
                center = center,
                radius = radius,
            ),
        radius = radius,
        center = center,
        alpha = alpha,
    )
}

private const val PREVIEW_FLIP_FACE = 4

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun LuckyCoinFlipArmorHeldPreview() {
    LuckyCoinFlipPreview(didStopAttack = true)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun LuckyCoinFlipArmorBrokenPreview() {
    LuckyCoinFlipPreview(didStopAttack = false)
}

@Composable
private fun LuckyCoinFlipPreview(didStopAttack: Boolean) {
    val seats = rememberSoloSeat()
    val cue =
        rememberReplayingCue(replayKey = didStopAttack) { iteration ->
            CombatAnimationCue.LuckyCoinFlip(
                id = "lucky-coin-flip-$iteration",
                gamblerUserId = PREVIEW_ACTOR_ID,
                faceValue = PREVIEW_FLIP_FACE,
                didStopAttack = didStopAttack,
            )
        }
    AbilityAnimationStage(accentClass = CharacterClassType.Gambler, seats = seats) {
        LuckyCoinOverlay(
            animationCue = cue,
            avatarBoundsFor = seats.boundsFor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun LuckyCoinHighRollerPaybackPreview() {
    val seats = rememberDuoSeats()
    val cue =
        rememberReplayingCues(replayKey = "high-roller") { iteration ->
            listOf(
                CombatAnimationCue.LuckyCoinHighRoller(
                    id = "high-roller-$iteration",
                    gamblerUserId = PREVIEW_ACTOR_ID,
                ),
                CombatAnimationCue.LuckyCoinHighRollerThrow(
                    id = "high-roller-throw-$iteration",
                    gamblerUserId = PREVIEW_ACTOR_ID,
                    attackerUserId = PREVIEW_TARGET_ID,
                    damage = LUCKY_FACE,
                    isKnockout = true,
                ),
            )
        }
    AbilityAnimationStage(accentClass = CharacterClassType.Gambler, seats = seats) {
        LuckyCoinOverlay(
            animationCue = cue,
            avatarBoundsFor = seats.boundsFor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
