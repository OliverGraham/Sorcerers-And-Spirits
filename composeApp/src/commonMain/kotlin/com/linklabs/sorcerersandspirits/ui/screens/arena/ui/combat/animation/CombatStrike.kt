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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The bright and shaded steel a drawn blade is lit with, shared across the weapon strikes. */
internal val steelBright = Color(color = 0xFFF2F6FA)
internal val steelShade = Color(color = 0xFFAEB9C4)

internal val meleeDashDistance = 84.dp

/** The tail an [impactBurst] adds after it fires: its 60ms lead plus its 240ms fade-out. */
internal const val IMPACT_BURST_TAIL_MILLIS = 300L

@Immutable
internal data class WeaponStrikeGeometry(
    val start: Offset,
    val target: Offset,
    val dashFraction: Float,
    val accentColor: Color,
)

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

internal fun projectileStrikeGeometry(
    messageType: NormalRollMessageType,
    attackerBounds: Rect,
    defenderBounds: Rect,
    accentColor: Color,
    density: Density,
): WeaponStrikeGeometry {
    val target =
        if (messageType == NormalRollMessageType.ArmorClassMiss) {
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

internal fun missThrustTarget(defenderBounds: Rect, origin: Offset): Offset {
    val strikeDirection = if (defenderBounds.center.y > origin.y) 1f else -1f
    return Offset(
        x = defenderBounds.center.x - defenderBounds.width * 0.35f,
        y = defenderBounds.center.y + strikeDirection * defenderBounds.height * 0.3f,
    )
}

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
