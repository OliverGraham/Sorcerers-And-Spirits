package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatWeapon
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.combatWeapon
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val meleeDashDistance = 84.dp
internal const val MELEE_DASH_MILLIS = 200
internal const val MELEE_THRUST_MILLIS = 100
internal const val MELEE_EMBED_MILLIS = 160L
internal const val MELEE_REACTION_DELAY_MILLIS = 280L

internal const val THROW_FLIGHT_MILLIS = 440
internal const val THROW_REACTION_DELAY_MILLIS = 400L
private const val CARD_SPIN_ROTATIONS = 4f

internal const val SLASH_SWEEP_MILLIS = 300
internal const val TWIN_SLASH_STAGGER_MILLIS = 500
internal const val TWIN_SLASH_RAISE_MILLIS = 180
internal const val TWIN_SLASH_READY_HOLD_MILLIS = 220
private const val SLASH_CONNECT_MILLIS = 240L
private const val SLASH_FADE_MILLIS = 200
private const val SLASH_END_HOLD_MILLIS = 120L

/** The tail an [impactBurst] adds after it fires: its 60ms lead plus its 240ms fade-out. */
private const val IMPACT_BURST_TAIL_MILLIS = 300L
private const val SLASH_TRAIL_SAMPLES = 12
private const val SLASH_ENGAGE_FRACTION = 0.22f
private const val SWORD_ERECT_DEGREES = 180f

/** When the follow blade lands, so the shared defender reaction (SS-191) meets its cut. */
internal const val TWIN_SWORD_REACTION_DELAY_MILLIS =
    TWIN_SLASH_RAISE_MILLIS +
        TWIN_SLASH_READY_HOLD_MILLIS +
        TWIN_SLASH_STAGGER_MILLIS +
        SLASH_CONNECT_MILLIS

private const val WITHDRAW_MILLIS = 220

private val steelBright = Color(color = 0xFFF2F6FA)
private val steelShade = Color(color = 0xFFAEB9C4)
private val cardFace = Color(color = 0xFFFBF6EC)
private val cardEdge = Color(color = 0xFFD8CFBC)

/**
 * The flight of one weapon strike. The choreography drives these Animatables and the draw layer
 * reads them, so a strike redraws the canvas without recomposing anything.
 */
@Stable
internal class WeaponStrikeState {
    val travel = Animatable(initialValue = 0f)
    val weaponAlpha = Animatable(initialValue = 0f)
    val spin = Animatable(initialValue = 0f)
    val impactAlpha = Animatable(initialValue = 0f)
    val impactScale = Animatable(initialValue = 0f)
    val leadSlash = BladeSlashState()
    val followSlash = BladeSlashState()

    var scene by mutableStateOf<WeaponStrikeScene?>(value = null)
}

/**
 * One blade of the Duelist's twin-sword slash (SS-194): [sweep] runs 0-to-1 along the blade's arc
 * across the defender, and [alpha] fades the blade in as the cut begins and out once it lands.
 */
@Stable
internal class BladeSlashState {
    val sweep = Animatable(initialValue = 0f)
    val alpha = Animatable(initialValue = 0f)
}

/**
 * The fixed geometry of one melee strike, measured before it launches. [travel] runs along the
 * [start]-to-[target] line: the approach tracks the attacker's dash up to [dashFraction], and the
 * thrust covers the rest.
 */
@Immutable
internal data class WeaponStrikeScene(
    val weapon: CombatWeapon,
    val start: Offset,
    val target: Offset,
    val dashFraction: Float,
    val accentColor: Color,
)

/**
 * The weapon layer of a normal attack (SS-192): a close-combat strike. The attacker's class weapon
 * appears in front of the avatar as it dashes into range, thrusts into the defender avatar — or
 * past it on a miss — flares a burst in the attacker's class color, and withdraws as the avatar
 * settles back. Geometry comes from the measured avatar bounds via [avatarBoundsFor], so the pierce
 * always lands on the defender art regardless of layout. Classes without a [combatWeapon] play no
 * strike here and keep their SS-191 body lunges.
 */
@Composable
internal fun WeaponStrikeOverlay(
    animationCue: CombatAnimationCue?,
    avatarBoundsFor: (String) -> Rect?,
    modifier: Modifier = Modifier,
) {
    val state = remember { WeaponStrikeState() }
    val density = LocalDensity.current
    var lastPlayedCueId by remember { mutableStateOf(value = animationCue?.id) }

    LaunchedEffect(key1 = animationCue) {
        val cue = animationCue
        val weapon = (cue as? CombatAnimationCue.NormalAttack)?.attackerClassType?.combatWeapon
        if (cue == null || weapon == null) {
            state.scene = null
            lastPlayedCueId = cue?.id
            return@LaunchedEffect
        }
        if (cue.id == lastPlayedCueId) return@LaunchedEffect
        lastPlayedCueId = cue.id

        val attackerBounds = avatarBoundsFor(cue.attackerUserId) ?: return@LaunchedEffect
        val defenderBounds = avatarBoundsFor(cue.defenderUserId) ?: return@LaunchedEffect

        val scene =
            strikeScene(
                weapon = weapon,
                outcome = cue.outcome,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
                accentColor = cue.attackerClassType.toColor(),
                density = density,
            )
        state.play(scene = scene, outcome = cue.outcome)
    }

    Canvas(modifier = modifier) { drawWeaponStrike(state = state) }
}

/**
 * How long a class's attacker-side normal-attack animation runs, so the message sequencer can hold
 * the event until its strike finishes instead of cutting it off (SS-194 / SS-201). Kept in step
 * with the play* choreography above. Strikes shorter than the flash window don't change pacing —
 * the sequencer already holds a message at least that long — so only the longer twin-sword combo
 * actually extends its message's dwell.
 */
internal fun CombatWeapon?.strikeDurationMillis(outcome: NormalRollMessageType): Long =
    when (this) {
        CombatWeapon.TwinSwords -> {
            val followSweepMillis =
                maxOf(SLASH_SWEEP_MILLIS.toLong(), SLASH_CONNECT_MILLIS + IMPACT_BURST_TAIL_MILLIS)
            (TWIN_SLASH_RAISE_MILLIS + TWIN_SLASH_READY_HOLD_MILLIS + TWIN_SLASH_STAGGER_MILLIS)
                .toLong() + followSweepMillis + SLASH_END_HOLD_MILLIS + SLASH_FADE_MILLIS
        }

        CombatWeapon.Dagger ->
            MELEE_DASH_MILLIS +
                MELEE_THRUST_MILLIS +
                MELEE_EMBED_MILLIS +
                WITHDRAW_MILLIS +
                if (outcome == NormalRollMessageType.Knockout) 120L else 0L

        CombatWeapon.PlayingCard -> THROW_FLIGHT_MILLIS + 200L

        null -> 600L
    }

private fun strikeScene(
    weapon: CombatWeapon,
    outcome: NormalRollMessageType,
    attackerBounds: Rect,
    defenderBounds: Rect,
    accentColor: Color,
    density: Density,
): WeaponStrikeScene {
    val isThrustMiss =
        outcome == NormalRollMessageType.ArmorClassMiss && weapon != CombatWeapon.TwinSwords
    val target =
        if (isThrustMiss) {
            missThrustTarget(defenderBounds = defenderBounds, origin = attackerBounds.center)
        } else {
            defenderBounds.center
        }

    val approach = target - attackerBounds.center
    val approachDistance = approach.getDistance()
    val handLeadPx = with(density) { 40.dp.toPx() }
    val dashPx = with(density) { meleeDashDistance.toPx() }

    val start = attackerBounds.center + approach * (handLeadPx / approachDistance)
    val dashFraction =
        (dashPx / (target - start).getDistance()).coerceIn(
            minimumValue = 0.1f,
            maximumValue = 0.75f,
        )

    return WeaponStrikeScene(
        weapon = weapon,
        start = start,
        target = target,
        dashFraction = dashFraction,
        accentColor = accentColor,
    )
}

/**
 * Where a missed thrust ends up: slipped past the defender's left while they sidestep right, and
 * carried beyond them so the blade reads as stabbing empty air.
 */
private fun missThrustTarget(defenderBounds: Rect, origin: Offset): Offset {
    val strikeDirection = if (defenderBounds.center.y > origin.y) 1f else -1f
    return Offset(
        x = defenderBounds.center.x - defenderBounds.width * 0.35f,
        y = defenderBounds.center.y + strikeDirection * defenderBounds.height * 0.3f,
    )
}

private suspend fun WeaponStrikeState.play(
    scene: WeaponStrikeScene,
    outcome: NormalRollMessageType,
) {
    travel.snapTo(targetValue = 0f)
    weaponAlpha.snapTo(targetValue = 0f)
    spin.snapTo(targetValue = 0f)
    impactAlpha.snapTo(targetValue = 0f)
    impactScale.snapTo(targetValue = 0f)
    leadSlash.sweep.snapTo(targetValue = 0f)
    leadSlash.alpha.snapTo(targetValue = 0f)
    followSlash.sweep.snapTo(targetValue = 0f)
    followSlash.alpha.snapTo(targetValue = 0f)
    this.scene = scene

    when (scene.weapon) {
        CombatWeapon.Dagger -> playMeleeStrike(scene = scene, outcome = outcome)
        CombatWeapon.PlayingCard -> playThrownStrike(outcome = outcome)
        CombatWeapon.TwinSwords -> playTwinSwordSlash(outcome = outcome)
    }

    this.scene = null
}

/**
 * The melee weapon strike (SS-192): dash into range, pierce, and withdraw as the avatar settles.
 */
private suspend fun WeaponStrikeState.playMeleeStrike(
    scene: WeaponStrikeScene,
    outcome: NormalRollMessageType,
) {
    when (outcome) {
        NormalRollMessageType.ArmorClassHit ->
            pierce(dashFraction = scene.dashFraction, burstScale = 0.8f)

        NormalRollMessageType.ArmorClassMiss -> whiff(dashFraction = scene.dashFraction)

        NormalRollMessageType.Damage -> pierce(dashFraction = scene.dashFraction, burstScale = 1f)

        NormalRollMessageType.Knockout -> pierceDeep(dashFraction = scene.dashFraction)
    }
}

/**
 * The thrown weapon strike (SS-193): the card spins the full span to the defender and flares a
 * burst on landing, or spins past into empty air on a miss. Nothing withdraws — a thrown card lands
 * and is gone, so the weapon fades into the burst rather than being pulled back.
 */
private suspend fun WeaponStrikeState.playThrownStrike(outcome: NormalRollMessageType) {
    when (outcome) {
        NormalRollMessageType.ArmorClassHit -> throwAcross(burstScale = 0.8f)
        NormalRollMessageType.ArmorClassMiss -> throwPast()
        NormalRollMessageType.Damage -> throwAcross(burstScale = 1f)
        NormalRollMessageType.Knockout -> throwAcross(burstScale = 1.3f)
    }
}

/**
 * The Duelist's twin-sword slash (SS-194): both blades rise into an erect ready pose at the
 * Duelist's sides and hold, then the lead blade slashes across to the defender and roughly half a
 * second later the follow blade cuts from the other side — one-two, both on screen the whole time.
 * A landed cut flares a burst as each blade reaches the defender; a parried one (armor holds)
 * sweeps in cleanly with no flare while the defender sidesteps. Both blades fade out together once
 * the combo lands.
 */
private suspend fun WeaponStrikeState.playTwinSwordSlash(outcome: NormalRollMessageType) {
    val connects = outcome != NormalRollMessageType.ArmorClassMiss
    val followBurst =
        when (outcome) {
            NormalRollMessageType.Knockout -> 1.3f
            NormalRollMessageType.Damage -> 1f
            NormalRollMessageType.ArmorClassHit -> 0.8f
            NormalRollMessageType.ArmorClassMiss -> 0f
        }

    coroutineScope {
        launch { leadSlash.raise() }
        launch { followSlash.raise() }
    }
    delay(timeMillis = TWIN_SLASH_READY_HOLD_MILLIS.toLong())

    coroutineScope {
        launch {
            sweepBlade(blade = leadSlash, connects = connects, burstScale = followBurst * 0.7f)
        }
        launch {
            delay(timeMillis = TWIN_SLASH_STAGGER_MILLIS.toLong())
            sweepBlade(blade = followSlash, connects = connects, burstScale = followBurst)
        }
    }

    delay(timeMillis = SLASH_END_HOLD_MILLIS)
    coroutineScope {
        launch {
            leadSlash.alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = SLASH_FADE_MILLIS),
            )
        }
        launch {
            followSlash.alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = SLASH_FADE_MILLIS),
            )
        }
    }
}

/** The blade fades into its erect ready pose, held upright at the Duelist's side before the cut. */
private suspend fun BladeSlashState.raise() {
    sweep.snapTo(targetValue = 0f)
    alpha.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = TWIN_SLASH_RAISE_MILLIS),
    )
}

/** One blade's cut: sweep the arc from the ready pose to the defender, flaring on contact. */
private suspend fun WeaponStrikeState.sweepBlade(
    blade: BladeSlashState,
    connects: Boolean,
    burstScale: Float,
) {
    coroutineScope {
        launch {
            blade.sweep.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(durationMillis = SLASH_SWEEP_MILLIS, easing = FastOutSlowInEasing),
            )
        }
        launch {
            if (connects) {
                delay(timeMillis = SLASH_CONNECT_MILLIS)
                impactBurst(peakScale = burstScale)
            }
        }
    }
}

/** The card's flight to its target: it appears at the hand and spins across the gap. */
private suspend fun WeaponStrikeState.flyAcross(durationMillis: Int) {
    weaponAlpha.snapTo(targetValue = 1f)
    coroutineScope {
        launch {
            travel.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(durationMillis = durationMillis, easing = FastOutLinearInEasing),
            )
        }
        launch {
            spin.animateTo(
                targetValue = CARD_SPIN_ROTATIONS * 360f,
                animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing),
            )
        }
    }
}

/** A landed throw: the card spins to the defender, then vanishes into a class-color burst. */
private suspend fun WeaponStrikeState.throwAcross(burstScale: Float) {
    flyAcross(durationMillis = THROW_FLIGHT_MILLIS)
    coroutineScope {
        launch { impactBurst(peakScale = burstScale) }
        launch {
            weaponAlpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 110))
        }
    }
}

/** The missed throw: the card spins past the sidestepping defender and fades into empty air. */
private suspend fun WeaponStrikeState.throwPast() {
    flyAcross(durationMillis = THROW_FLIGHT_MILLIS)
    coroutineScope {
        launch { travel.animateTo(targetValue = 1.3f, animationSpec = tween(durationMillis = 180)) }
        launch {
            spin.animateTo(
                targetValue = (CARD_SPIN_ROTATIONS + 0.5f) * 360f,
                animationSpec = tween(durationMillis = 180),
            )
        }
        launch {
            weaponAlpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 180))
        }
    }
}

/** The blade fades in at the attacker's hands and rides the avatar's dash into melee range. */
private suspend fun WeaponStrikeState.closeIn(dashFraction: Float) {
    coroutineScope {
        launch {
            weaponAlpha.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 120))
        }
        launch {
            travel.animateTo(
                targetValue = dashFraction,
                animationSpec = tween(durationMillis = MELEE_DASH_MILLIS),
            )
        }
    }
}

/**
 * The close-combat pierce: dash into range, thrust the blade into the defender, hold it embedded
 * while the burst flares, then withdraw as the attacker settles back.
 */
private suspend fun WeaponStrikeState.pierce(dashFraction: Float, burstScale: Float) {
    closeIn(dashFraction = dashFraction)
    travel.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = MELEE_THRUST_MILLIS, easing = FastOutLinearInEasing),
    )
    coroutineScope {
        launch { impactBurst(peakScale = burstScale) }
        launch {
            delay(timeMillis = MELEE_EMBED_MILLIS)
            withdraw(dashFraction = dashFraction)
        }
    }
}

/**
 * The knockout: the pierce, then the blade driven deeper with a bigger burst before withdrawing.
 */
private suspend fun WeaponStrikeState.pierceDeep(dashFraction: Float) {
    closeIn(dashFraction = dashFraction)
    travel.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = MELEE_THRUST_MILLIS, easing = FastOutLinearInEasing),
    )
    coroutineScope {
        launch {
            impactAlpha.snapTo(targetValue = 1f)
            impactScale.snapTo(targetValue = 0f)
            impactScale.animateTo(targetValue = 0.7f, animationSpec = tween(durationMillis = 100))
            impactAlpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 100))
        }
        launch {
            delay(timeMillis = 80L)
            travel.animateTo(targetValue = 1.1f, animationSpec = tween(durationMillis = 120))
        }
    }
    coroutineScope {
        launch { impactBurst(peakScale = 1.3f) }
        launch {
            delay(timeMillis = MELEE_EMBED_MILLIS)
            withdraw(dashFraction = dashFraction)
        }
    }
}

/** The miss: the thrust carries past the defender into empty air, hangs, and is pulled back. */
private suspend fun WeaponStrikeState.whiff(dashFraction: Float) {
    closeIn(dashFraction = dashFraction)
    travel.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing),
    )
    delay(timeMillis = 120L)
    withdraw(dashFraction = dashFraction)
}

private suspend fun WeaponStrikeState.withdraw(dashFraction: Float) {
    coroutineScope {
        launch {
            travel.animateTo(
                targetValue = 1f - (1f - dashFraction) * 0.6f,
                animationSpec = tween(durationMillis = WITHDRAW_MILLIS),
            )
        }
        launch {
            weaponAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = WITHDRAW_MILLIS),
            )
        }
    }
}

private suspend fun WeaponStrikeState.impactBurst(peakScale: Float) {
    impactAlpha.snapTo(targetValue = 1f)
    impactScale.snapTo(targetValue = 0f)
    coroutineScope {
        launch {
            impactScale.animateTo(
                targetValue = peakScale,
                animationSpec = tween(durationMillis = 160),
            )
        }
        launch {
            delay(timeMillis = 60L)
            impactAlpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 240))
        }
    }
}

private fun DrawScope.drawWeaponStrike(state: WeaponStrikeState) {
    val scene = state.scene ?: return

    when (scene.weapon) {
        CombatWeapon.Dagger,
        CombatWeapon.PlayingCard -> drawProjectileWeapon(scene = scene, state = state)

        CombatWeapon.TwinSwords -> drawTwinSwordSlash(scene = scene, state = state)
    }

    val impactAlpha = state.impactAlpha.value
    val isBursting = impactAlpha > 0f && state.impactScale.value > 0f
    if (isBursting) {
        drawImpactBurst(scene = scene, scale = state.impactScale.value, alpha = impactAlpha)
    }
}

private fun DrawScope.drawProjectileWeapon(scene: WeaponStrikeScene, state: WeaponStrikeState) {
    val weaponAlpha = state.weaponAlpha.value
    if (weaponAlpha <= 0f) return

    val position = lerp(start = scene.start, stop = scene.target, fraction = state.travel.value)
    when (scene.weapon) {
        CombatWeapon.Dagger ->
            drawBladeThrust(
                scene = scene,
                travel = state.travel.value,
                position = position,
                alpha = weaponAlpha,
            )

        CombatWeapon.PlayingCard ->
            drawCardThrow(
                scene = scene,
                position = position,
                spinDegrees = state.spin.value,
                alpha = weaponAlpha,
            )

        CombatWeapon.TwinSwords -> Unit
    }
}

/**
 * The Duelist's twin blades (SS-194): the lead blade slashes in from the defender's left, the
 * follow blade from the right, so the two cuts cross over the defender.
 */
private fun DrawScope.drawTwinSwordSlash(scene: WeaponStrikeScene, state: WeaponStrikeState) {
    drawBladeSlash(scene = scene, blade = state.leadSlash, fromSide = -1f)
    drawBladeSlash(scene = scene, blade = state.followSlash, fromSide = 1f)
}

/** The flash of steel: a class-color streak trailing the blade, brightening to white at its tip. */
private fun DrawScope.drawBladeThrust(
    scene: WeaponStrikeScene,
    travel: Float,
    position: Offset,
    alpha: Float,
) {
    val trailStart =
        lerp(
            start = scene.start,
            stop = scene.target,
            fraction = (travel - 0.18f).coerceAtLeast(minimumValue = 0f),
        )

    drawLine(
        brush =
            Brush.linearGradient(
                colors =
                    listOf(Color.Transparent, scene.accentColor.copy(alpha = 0.6f), steelBright),
                start = trailStart,
                end = position,
            ),
        start = trailStart,
        end = position,
        strokeWidth = 8.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha * 0.7f,
    )

    drawDagger(scene = scene, position = position, alpha = alpha)
}

/**
 * The thrown playing card (SS-193): a soft class-color trail marks its arc, and the card itself
 * spins in-plane about its center as it flies — a spade pip in the class color on a poker-card
 * face.
 */
private fun DrawScope.drawCardThrow(
    scene: WeaponStrikeScene,
    position: Offset,
    spinDegrees: Float,
    alpha: Float,
) {
    drawLine(
        brush =
            Brush.linearGradient(
                colors = listOf(Color.Transparent, scene.accentColor.copy(alpha = 0.35f)),
                start = scene.start,
                end = position,
            ),
        start = scene.start,
        end = position,
        strokeWidth = 5.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha * 0.5f,
    )

    rotate(degrees = spinDegrees, pivot = position) {
        translate(left = position.x, top = position.y) {
            drawPlayingCard(accentColor = scene.accentColor, alpha = alpha)
        }
    }
}

/**
 * One slashing blade (SS-194): it launches from the attacker's hand — offset to their left or right
 * so the two swords come from either side — and sweeps a bowed arc across to the defender, crossing
 * the attack line so the paired cuts form an X. A class-color trail brightens to steel at the
 * leading edge, with the sword riding the tip of the cut, oriented along the sweep.
 */
private fun DrawScope.drawBladeSlash(
    scene: WeaponStrikeScene,
    blade: BladeSlashState,
    fromSide: Float,
) {
    val alpha = blade.alpha.value
    if (alpha <= 0f) return

    val axisVector = scene.target - scene.start
    val axisLength = axisVector.getDistance()
    if (axisLength == 0f) return
    val axis = axisVector / axisLength
    val perpAxis = Offset(x = -axis.y, y = axis.x)

    val start = scene.start + perpAxis * (fromSide * 26.dp.toPx())
    val end = scene.target - perpAxis * (fromSide * 22.dp.toPx())
    val bowNormal = perpAxis * fromSide
    val bow = 26.dp.toPx()

    val sweep = blade.sweep.value
    val engage = smoothstep(edge0 = 0f, edge1 = SLASH_ENGAGE_FRACTION, value = sweep)
    val erectTip = start + Offset(x = 0f, y = -64.dp.toPx())
    val arcPoint =
        slashPoint(start = start, end = end, perp = bowNormal, bow = bow, fraction = sweep)
    val position = lerp(start = erectTip, stop = arcPoint, fraction = engage)

    if (engage > 0f) {
        drawPath(
            path =
                slashTrailPath(
                    start = start,
                    end = end,
                    perp = bowNormal,
                    bow = bow,
                    progress = sweep,
                ),
            brush =
                Brush.linearGradient(
                    colors =
                        listOf(
                            Color.Transparent,
                            scene.accentColor.copy(alpha = 0.55f),
                            steelBright,
                        ),
                    start = start,
                    end = arcPoint,
                ),
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
            alpha = alpha * 0.8f * engage,
        )
    }

    val tangent =
        slashTangent(start = start, end = end, perp = bowNormal, bow = bow, fraction = sweep)
    val slashDegrees = atan2(y = tangent.y, x = tangent.x) * 180f / PI.toFloat() - 90f
    val degrees =
        lerpAngle(fromDegrees = SWORD_ERECT_DEGREES, toDegrees = slashDegrees, fraction = engage)
    rotate(degrees = degrees, pivot = position) {
        translate(left = position.x, top = position.y) {
            drawSwordShape(accentColor = scene.accentColor, alpha = alpha)
        }
    }
}

/** Smooth 0-to-1 ramp between [edge0] and [edge1], easing the blade out of its erect pose. */
private fun smoothstep(edge0: Float, edge1: Float, value: Float): Float {
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(minimumValue = 0f, maximumValue = 1f)
    return t * t * (3f - 2f * t)
}

/**
 * Interpolate an angle along the shorter arc, so the erect blade rolls the near way into the cut.
 */
private fun lerpAngle(fromDegrees: Float, toDegrees: Float, fraction: Float): Float {
    val delta = ((toDegrees - fromDegrees + 540f) % 360f) - 180f
    return fromDegrees + delta * fraction
}

/** A point along the bowed slash arc: the straight span pushed out by a sine-peaked bow. */
private fun slashPoint(
    start: Offset,
    end: Offset,
    perp: Offset,
    bow: Float,
    fraction: Float,
): Offset {
    val base = lerp(start = start, stop = end, fraction = fraction)
    val bowAmount = sin(x = fraction.toDouble() * PI).toFloat() * bow
    return base + perp * bowAmount
}

/** The arc's local direction at [fraction], sampled either side so the blade rides tip-first. */
private fun slashTangent(
    start: Offset,
    end: Offset,
    perp: Offset,
    bow: Float,
    fraction: Float,
): Offset {
    val ahead =
        slashPoint(start, end, perp, bow, (fraction + 0.02f).coerceAtMost(maximumValue = 1f))
    val behind =
        slashPoint(start, end, perp, bow, (fraction - 0.02f).coerceAtLeast(minimumValue = 0f))
    return ahead - behind
}

private fun slashTrailPath(
    start: Offset,
    end: Offset,
    perp: Offset,
    bow: Float,
    progress: Float,
): Path =
    Path().apply {
        val origin = slashPoint(start = start, end = end, perp = perp, bow = bow, fraction = 0f)
        moveTo(x = origin.x, y = origin.y)
        for (sample in 1..SLASH_TRAIL_SAMPLES) {
            val fraction = progress * sample / SLASH_TRAIL_SAMPLES
            val point =
                slashPoint(start = start, end = end, perp = perp, bow = bow, fraction = fraction)
            lineTo(x = point.x, y = point.y)
        }
    }

/**
 * A slender rapier in local coordinates: tip at the origin leading the cut, a thin fencing blade
 * holding its width then tapering to a sharp point, in bright steel with a specular glint down its
 * length and a white flash at the tip. The hilt carries the class-color accent — curved quillons, a
 * swept knuckle bow curling around the hand, the grip, and the pommel.
 */
private fun DrawScope.drawSwordShape(accentColor: Color, alpha: Float) {
    val bladeLength = 88.dp.toPx()
    val bladeHalfWidth = 2.6.dp.toPx()
    val taperStart = -bladeLength * 0.8f
    val guardY = -bladeLength
    val quillonHalfWidth = 15.dp.toPx()
    val gripHalfWidth = 3.dp.toPx()
    val gripLength = 17.dp.toPx()
    val gripTopY = guardY - gripLength
    val pommelY = gripTopY - 3.dp.toPx()

    val blade =
        Path().apply {
            moveTo(x = 0f, y = 0f)
            lineTo(x = -bladeHalfWidth, y = taperStart)
            lineTo(x = -bladeHalfWidth, y = guardY)
            lineTo(x = bladeHalfWidth, y = guardY)
            lineTo(x = bladeHalfWidth, y = taperStart)
            close()
        }
    drawPath(
        path = blade,
        brush =
            Brush.linearGradient(
                colors = listOf(Color.White, steelBright, steelShade),
                start = Offset(x = 0f, y = 0f),
                end = Offset(x = 0f, y = guardY),
            ),
        alpha = alpha,
    )

    drawLine(
        color = Color.White,
        start = Offset(x = 0f, y = taperStart),
        end = Offset(x = 0f, y = guardY * 0.12f),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha * 0.85f,
    )

    drawCircle(
        color = Color.White,
        radius = 2.dp.toPx(),
        center = Offset(x = 0f, y = 0f),
        alpha = alpha * 0.9f,
    )

    val quillons =
        Path().apply {
            moveTo(x = -quillonHalfWidth, y = guardY - 5.dp.toPx())
            cubicTo(
                x1 = -quillonHalfWidth * 0.3f,
                y1 = guardY + 5.dp.toPx(),
                x2 = quillonHalfWidth * 0.3f,
                y2 = guardY + 5.dp.toPx(),
                x3 = quillonHalfWidth,
                y3 = guardY - 5.dp.toPx(),
            )
        }
    drawPath(
        path = quillons,
        color = accentColor,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        alpha = alpha,
    )

    val knuckleBow =
        Path().apply {
            moveTo(x = quillonHalfWidth, y = guardY - 5.dp.toPx())
            cubicTo(
                x1 = quillonHalfWidth + 7.dp.toPx(),
                y1 = guardY - gripLength * 0.4f,
                x2 = gripHalfWidth + 5.dp.toPx(),
                y2 = pommelY + 3.dp.toPx(),
                x3 = 0f,
                y3 = pommelY,
            )
        }
    drawPath(
        path = knuckleBow,
        color = accentColor,
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
        alpha = alpha,
    )

    drawRoundRect(
        color = accentColor,
        topLeft = Offset(x = -gripHalfWidth, y = gripTopY),
        size = Size(width = gripHalfWidth * 2, height = gripLength),
        cornerRadius = CornerRadius(x = gripHalfWidth),
        alpha = alpha,
    )

    drawCircle(
        color = accentColor,
        radius = 4.dp.toPx(),
        center = Offset(x = 0f, y = pommelY),
        alpha = alpha,
    )
}

private fun DrawScope.drawDagger(scene: WeaponStrikeScene, position: Offset, alpha: Float) {
    val thrustVector = scene.target - scene.start
    val degreesToTarget = atan2(y = thrustVector.y, x = thrustVector.x) * 180f / PI.toFloat() - 90f

    rotate(degrees = degreesToTarget, pivot = position) {
        translate(left = position.x, top = position.y) {
            drawDaggerShape(accentColor = scene.accentColor, alpha = alpha)
        }
    }
}

/**
 * The dagger in local coordinates: tip at the origin leading the thrust, blade trailing behind it,
 * with the guard, hilt, and pommel bringing up the rear. The blade is a steel gradient; the guard
 * and hilt carry the class-color accent.
 */
private fun DrawScope.drawDaggerShape(accentColor: Color, alpha: Float) {
    val bladeLength = 52.dp.toPx()
    val bladeHalfWidth = 6.5.dp.toPx()
    val guardHalfWidth = 13.dp.toPx()
    val guardHeight = 5.dp.toPx()
    val hiltHalfWidth = 3.dp.toPx()
    val hiltLength = 16.dp.toPx()

    val blade =
        Path().apply {
            moveTo(x = 0f, y = 0f)
            lineTo(x = -bladeHalfWidth, y = -bladeLength * 0.85f)
            lineTo(x = -bladeHalfWidth * 0.55f, y = -bladeLength)
            lineTo(x = bladeHalfWidth * 0.55f, y = -bladeLength)
            lineTo(x = bladeHalfWidth, y = -bladeLength * 0.85f)
            close()
        }
    drawPath(
        path = blade,
        brush =
            Brush.linearGradient(
                colors = listOf(steelBright, steelShade),
                start = Offset(x = 0f, y = 0f),
                end = Offset(x = 0f, y = -bladeLength),
            ),
        alpha = alpha,
    )

    drawRoundRect(
        color = accentColor,
        topLeft = Offset(x = -guardHalfWidth, y = -bladeLength - guardHeight),
        size = Size(width = guardHalfWidth * 2, height = guardHeight),
        cornerRadius = CornerRadius(x = guardHeight / 2),
        alpha = alpha,
    )

    drawRoundRect(
        color = accentColor,
        topLeft = Offset(x = -hiltHalfWidth, y = -bladeLength - guardHeight - hiltLength),
        size = Size(width = hiltHalfWidth * 2, height = hiltLength),
        cornerRadius = CornerRadius(x = hiltHalfWidth),
        alpha = alpha,
    )

    drawCircle(
        color = steelShade,
        radius = 4.5.dp.toPx(),
        center = Offset(x = 0f, y = -bladeLength - guardHeight - hiltLength - 3.dp.toPx()),
        alpha = alpha,
    )
}

/**
 * The playing card in local coordinates, centered on the origin so it spins about its own middle: a
 * pale poker-card face rimmed in the class color, with a single spade pip in the class color at its
 * heart.
 */
private fun DrawScope.drawPlayingCard(accentColor: Color, alpha: Float) {
    val halfWidth = 15.dp.toPx()
    val halfHeight = 21.dp.toPx()
    val corner = CornerRadius(x = 3.5.dp.toPx())
    val topLeft = Offset(x = -halfWidth, y = -halfHeight)
    val cardSize = Size(width = halfWidth * 2, height = halfHeight * 2)

    drawRoundRect(
        color = cardFace,
        topLeft = topLeft,
        size = cardSize,
        cornerRadius = corner,
        alpha = alpha,
    )
    drawRoundRect(
        color = cardEdge,
        topLeft = topLeft,
        size = cardSize,
        cornerRadius = corner,
        style = Stroke(width = 1.5.dp.toPx()),
        alpha = alpha,
    )

    drawPath(
        path = spadePath(halfWidth = 7.dp.toPx(), halfHeight = 9.dp.toPx()),
        color = accentColor,
        alpha = alpha,
    )
}

/**
 * A spade centered on the origin, point up over a short stem: two lobes swept out with mirrored
 * cubics that curl into the base, then a trapezoidal stem closing it off.
 */
private fun spadePath(halfWidth: Float, halfHeight: Float): Path =
    Path().apply {
        moveTo(x = 0f, y = -halfHeight)
        cubicTo(
            x1 = halfWidth * 0.35f,
            y1 = -halfHeight * 0.5f,
            x2 = halfWidth,
            y2 = -halfHeight * 0.05f,
            x3 = halfWidth,
            y3 = halfHeight * 0.35f,
        )
        cubicTo(
            x1 = halfWidth,
            y1 = halfHeight * 0.8f,
            x2 = halfWidth * 0.4f,
            y2 = halfHeight * 0.72f,
            x3 = halfWidth * 0.14f,
            y3 = halfHeight * 0.42f,
        )
        lineTo(x = halfWidth * 0.32f, y = halfHeight)
        lineTo(x = -halfWidth * 0.32f, y = halfHeight)
        lineTo(x = -halfWidth * 0.14f, y = halfHeight * 0.42f)
        cubicTo(
            x1 = -halfWidth * 0.4f,
            y1 = halfHeight * 0.72f,
            x2 = -halfWidth,
            y2 = halfHeight * 0.8f,
            x3 = -halfWidth,
            y3 = halfHeight * 0.35f,
        )
        cubicTo(
            x1 = -halfWidth,
            y1 = -halfHeight * 0.05f,
            x2 = -halfWidth * 0.35f,
            y2 = -halfHeight * 0.5f,
            x3 = 0f,
            y3 = -halfHeight,
        )
        close()
    }

/** The pierce flare: a white-cored burst rimmed in the class color at the point of impact. */
private fun DrawScope.drawImpactBurst(scene: WeaponStrikeScene, scale: Float, alpha: Float) {
    val radius = 38.dp.toPx() * scale
    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(steelBright, scene.accentColor.copy(alpha = 0.8f), Color.Transparent),
                center = scene.target,
                radius = radius,
            ),
        radius = radius,
        center = scene.target,
        alpha = alpha,
    )
}
