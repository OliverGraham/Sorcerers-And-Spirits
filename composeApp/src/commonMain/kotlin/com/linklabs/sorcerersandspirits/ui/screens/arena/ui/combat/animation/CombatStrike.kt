package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The bright and shaded steel a drawn blade is lit with, shared across the weapon strikes. */
internal val steelBright = Color(color = 0xFFF2F6FA)
internal val steelShade = Color(color = 0xFFAEB9C4)

/** How far a melee attacker dashes toward the defender before the thrust finishes the strike. */
internal val meleeDashDistance = 84.dp

/** The tail an [impactBurst] adds after it fires: its 60ms lead plus its 240ms fade-out. */
internal const val IMPACT_BURST_TAIL_MILLIS = 300L

/**
 * The fixed geometry of one weapon strike, measured before it launches. [travel] runs along the
 * [start]-to-[target] line: for a melee weapon the approach tracks the attacker's dash up to
 * [dashFraction] and the thrust covers the rest; a thrown or slashing weapon reads the same line to
 * its own choreography. The weapon originates from the attacker and travels to the [target]
 * (SS-192).
 */
@Immutable
internal data class WeaponStrikeGeometry(
    val start: Offset,
    val target: Offset,
    val dashFraction: Float,
    val accentColor: Color,
)

/**
 * Measures the strike line from the attacker's hand to [target]: [start] leads a touch out of the
 * attacker's center so the weapon reads as held ahead of the body, and [dashFraction] is how far
 * along the line the melee dash carries before the thrust finishes it.
 */
internal fun weaponStrikeGeometry(
    attackerBounds: Rect,
    target: Offset,
    accentColor: Color,
    density: Density,
): WeaponStrikeGeometry {
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

    return WeaponStrikeGeometry(
        start = start,
        target = target,
        dashFraction = dashFraction,
        accentColor = accentColor,
    )
}

/**
 * The strike line for a projectile weapon (SS-192/193): the dagger or card lands on the defender's
 * center, or slips past into empty air on a miss. Shared by every class whose weapon flies at the
 * defender rather than sweeping an arc. [accentColor] lets an Illusionist borrow the weapon in its
 * own blue (SS-197) rather than the attacker's class color.
 */
internal fun projectileStrikeGeometry(
    cue: CombatAnimationCue.NormalAttack,
    attackerBounds: Rect,
    defenderBounds: Rect,
    accentColor: Color,
    density: Density,
): WeaponStrikeGeometry {
    val target =
        if (cue.outcome == NormalRollMessageType.ArmorClassMiss) {
            missThrustTarget(defenderBounds = defenderBounds, origin = attackerBounds.center)
        } else {
            defenderBounds.center
        }
    return weaponStrikeGeometry(
        attackerBounds = attackerBounds,
        target = target,
        accentColor = accentColor,
        density = density,
    )
}

/**
 * Where a missed thrust ends up: slipped past the defender's left while they sidestep right, and
 * carried beyond them so the blade reads as stabbing empty air.
 */
internal fun missThrustTarget(defenderBounds: Rect, origin: Offset): Offset {
    val strikeDirection = if (defenderBounds.center.y > origin.y) 1f else -1f
    return Offset(
        x = defenderBounds.center.x - defenderBounds.width * 0.35f,
        y = defenderBounds.center.y + strikeDirection * defenderBounds.height * 0.3f,
    )
}

/**
 * The shared pierce flare choreography: the burst snaps in at full alpha and zero scale, swells to
 * [peakScale], and fades out on a short lead. Every weapon strike flares the same way on contact.
 */
internal suspend fun impactBurst(
    impactAlpha: Animatable<Float, *>,
    impactScale: Animatable<Float, *>,
    peakScale: Float,
) {
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

/** The pierce flare: a white-cored burst rimmed in the class color at the point of impact. */
internal fun DrawScope.drawImpactBurst(
    center: Offset,
    scale: Float,
    alpha: Float,
    accentColor: Color,
) {
    val radius = 38.dp.toPx() * scale
    drawCircle(
        brush =
            Brush.radialGradient(
                colors = listOf(steelBright, accentColor.copy(alpha = 0.8f), Color.Transparent),
                center = center,
                radius = radius,
            ),
        radius = radius,
        center = center,
        alpha = alpha,
    )
}
