package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.floor
import kotlin.math.sin

/**
 * The bolt itself, drawn at [position] along its flight with a trail streaming back to [tail]. One
 * of the element-agnostic draw seams the Mage overlays paint through — alongside [drawMageImpact]
 * and [drawMageAtmosphere] — each dispatching over [MageSpellElement] to that element's own art, so
 * a new element is one branch per seam that both the normal attack (SS-198) and Spell Potency
 * (SS-210) pick up at once.
 */
internal fun DrawScope.drawMageBolt(
    element: MageSpellElement,
    tail: Offset,
    position: Offset,
    alpha: Float,
) {
    when (element) {
        MageSpellElement.Ice -> drawIcebolt(tail = tail, position = position, alpha = alpha)

        MageSpellElement.Lightning ->
            drawLightningBolt(tail = tail, position = position, alpha = alpha)

        MageSpellElement.Fire -> drawFireball(tail = tail, position = position, alpha = alpha)
    }
}

/** The flare where a bolt lands: an ice shatter, an electric burst, a fire explosion. */
internal fun DrawScope.drawMageImpact(
    element: MageSpellElement,
    center: Offset,
    scale: Float,
    alpha: Float,
) {
    when (element) {
        MageSpellElement.Ice -> drawIceShatter(center = center, scale = scale, alpha = alpha)

        MageSpellElement.Lightning ->
            drawLightningBurst(center = center, scale = scale, alpha = alpha)

        MageSpellElement.Fire -> drawFireBurst(center = center, scale = scale, alpha = alpha)
    }
}

/**
 * The amplified area-of-effect atmosphere for Spell Potency, filling the whole battlefield and
 * focused on [epicenter] (the opponent, never the casting Mage): a swirling Blizzard dome of icy
 * wind for Ice, a lightning-lit Thunderstorm for Lightning, an Inferno of fire pillars for Fire.
 * [swirl] advances the churn and [alpha] fades the whole effect in and out.
 */
internal fun DrawScope.drawMageAtmosphere(
    element: MageSpellElement,
    epicenter: Offset,
    swirl: Float,
    alpha: Float,
) {
    when (element) {
        MageSpellElement.Ice ->
            drawBlizzardDome(epicenter = epicenter, swirl = swirl, alpha = alpha)

        MageSpellElement.Lightning ->
            drawThunderstorm(epicenter = epicenter, swirl = swirl, alpha = alpha)

        MageSpellElement.Fire -> drawInferno(epicenter = epicenter, swirl = swirl, alpha = alpha)
    }
}

/**
 * A cheap deterministic hash in [0, 1) for jitter, flicker, and scatter — the atmospheres and bolts
 * seed it from geometry so a given frame always draws the same, yet varies as the scene advances.
 */
internal fun mageNoise(seed: Float): Float {
    val scaled = sin(x = seed) * 43758.5453f
    return scaled - floor(x = scaled)
}
