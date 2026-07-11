package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.paladin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.HolyShieldPhase
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AbilityAnimationStage
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.CueEffectOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_TARGET_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberDuoSeats
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberReplayingCues
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.visibleAvatarWidth
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val RAISE_MILLIS = 620

internal const val HOLY_SHIELD_PRAY_GLOW_MILLIS = 560

private const val BEAM_DESCEND_MILLIS = 560
private const val HEART_FLOAT_MILLIS = 1000
private const val SHIMMER_PERIOD_MILLIS = 2200

private const val SHIELD_FALL_MILLIS = 720

private const val WRATH_ROLLUP_MILLIS = 640
private const val WRATH_GONE_MILLIS = 200
private const val WRATH_BLAST_RISE_MILLIS = 240
private const val WRATH_BLAST_HOLD_MILLIS = 220
private const val WRATH_BLAST_FADE_MILLIS = 380

/** When in the Wrath choreography the blast lands, so the struck attacker shakes on impact. */
internal const val HOLY_SHIELD_WRATH_IMPACT_DELAY_MILLIS =
    WRATH_ROLLUP_MILLIS + WRATH_GONE_MILLIS + WRATH_BLAST_RISE_MILLIS

/**
 * The held shield's placement off the Paladin: a compact heater carried to one side rather than
 * covering the whole body, sized off the visible avatar width and its bounds height.
 */
private const val SHIELD_HALF_WIDTH_FRACTION = 0.3f
private const val SHIELD_HEIGHT_FRACTION = 0.46f
private const val SHIELD_SIDE_OFFSET_FRACTION = 0.42f
private const val SHIELD_TOP_FRACTION = 0.24f

/** How far the failed shield topples off its bottom point, in degrees. */
private const val TOPPLE_DEGREES = 84f

/** The holy aura's peak opacity as it bathes the Paladin the instant the knockout would land. */
private const val SANCTIFY_GLOW_ALPHA = 0.85f

/** The lowest Wrath blast force even a 1 keeps, so the weakest roll still lands with intent. */
private const val WRATH_MIN_INTENSITY = 0.45f

private val stoneGray = Color(color = 0xFF74777C)
private val stoneGrayDeep = Color(color = 0xFF5C5F63)
private val shieldGreen = paladinEssenceGreen
private val shieldGreenDeep = Color(color = 0xFF2C6E1F)
private val holyCore = Color(color = 0xFFFFFDF0)
private val gemEmerald = Color(color = 0xFF7BF04A)
private val gemRuby = Color(color = 0xFFE23A5A)

/**
 * The Paladin's Holy Shield effect (SS-212). The instant a knockout would land the Paladin grays to
 * stone and an ornate mace-gold shield rises to cover them; the survival roll sets the shield
 * glowing, and then either saves the Paladin under a living beam of holy light with a +1 heart
 * floating up, or topples the shield to gray as the Paladin falls to the normal knockout. A
 * survived shield rolls its light up to the heavens and blasts it back down on the attacker, scaled
 * by the damage roll. Anchored to the Paladin's — and, for the blast, the attacker's — measured
 * seats; sits out any cue that is not a Holy Shield.
 */
@Composable
internal fun PaladinHolyShieldOverlay(
    animationCue: CombatAnimationCue?,
    avatarBoundsFor: (String) -> Rect?,
    modifier: Modifier = Modifier,
) {
    val state = remember { HolyShieldState() }
    val textMeasurer = rememberTextMeasurer()
    val plusOneLayout =
        remember(textMeasurer) {
            textMeasurer.measure(
                text = "+1",
                style = TextStyle(fontSize = PLUS_ONE_MEASURE_SP.sp, fontWeight = FontWeight.Black),
            )
        }

    CueEffectOverlay(
        animationCue = animationCue,
        select = { it as? CombatAnimationCue.HolyShield },
        onCancel = {},
        play = playLabel@{ cue ->
                val paladinBounds = avatarBoundsFor(cue.paladinUserId) ?: return@playLabel
                val attackerBounds = cue.attackerUserId?.let(avatarBoundsFor)
                state.play(cue = cue, paladin = paladinBounds, attacker = attackerBounds)
            },
        modifier = modifier,
        draw = { drawHolyShield(state = state, plusOneLayout = plusOneLayout) },
    )
}

private const val PLUS_ONE_MEASURE_SP = 64

/**
 * How long each Holy Shield phase runs, so the message sequencer holds its callout until the shield
 * has risen, the prayer has saved or toppled it, or the Wrath light has rolled up and blasted down
 * — rather than cutting the effect off. A saved prayer's living beam keeps shimmering past this,
 * held alive until the Wrath cue takes over.
 */
internal fun paladinHolyShieldOverlayDurationMillis(
    phase: HolyShieldPhase,
    didSurvive: Boolean?,
): Long =
    when (phase) {
        HolyShieldPhase.Raise -> RAISE_MILLIS.toLong()

        HolyShieldPhase.Pray ->
            if (didSurvive == true) {
                (HOLY_SHIELD_PRAY_GLOW_MILLIS + BEAM_DESCEND_MILLIS).toLong()
            } else {
                (HOLY_SHIELD_PRAY_GLOW_MILLIS + SHIELD_FALL_MILLIS).toLong()
            }

        HolyShieldPhase.Wrath ->
            (WRATH_ROLLUP_MILLIS +
                    WRATH_GONE_MILLIS +
                    WRATH_BLAST_RISE_MILLIS +
                    WRATH_BLAST_HOLD_MILLIS +
                    WRATH_BLAST_FADE_MILLIS)
                .toLong()
    }

/**
 * The state of one Holy Shield (SS-212). The choreography drives these Animatables and the draw
 * layer reads them, so the shield rises, glows, saves or topples, and blasts without recomposing.
 * [sanctify] bathes the Paladin in holy light; [shieldRaise] scales the shield in; [shieldGlow] is
 * the roll's holy light; [holyBeam] is the saving beam of light shining down; [shimmer] winds
 * forever so that beam reads as endlessly alive; [heart] floats the +1 up; [shieldFall] and
 * [shieldDrain] topple and gray a failed shield; [beamRollUp] rolls the saved light up to the
 * heavens; [dissolve] fades the spent shield; and [blast] slams the Wrath light down on the
 * attacker.
 */
@Stable
internal class HolyShieldState {
    val sanctify = Animatable(initialValue = 0f)
    val shieldRaise = Animatable(initialValue = 0f)
    val shieldGlow = Animatable(initialValue = 0f)
    val holyBeam = Animatable(initialValue = 0f)
    val shimmer = Animatable(initialValue = 0f)
    val heart = Animatable(initialValue = 0f)
    val shieldFall = Animatable(initialValue = 0f)
    val shieldDrain = Animatable(initialValue = 0f)
    val beamRollUp = Animatable(initialValue = 0f)
    val dissolve = Animatable(initialValue = 0f)
    val blast = Animatable(initialValue = 0f)

    var scene by mutableStateOf<HolyShieldScene?>(value = null)
    var wrathIntensity by mutableStateOf(value = WRATH_MIN_INTENSITY)
}

/**
 * The seats a Holy Shield aims at: the Paladin it covers and, for Wrath, the attacker it strikes.
 */
@Immutable internal data class HolyShieldScene(val paladin: Rect, val attacker: Rect?)

private suspend fun HolyShieldState.play(
    cue: CombatAnimationCue.HolyShield,
    paladin: Rect,
    attacker: Rect?,
) {
    when (cue.phase) {
        HolyShieldPhase.Raise -> raise(paladin = paladin)

        HolyShieldPhase.Pray ->
            if (cue.didSurvive == true) {
                praySuccess(paladin = paladin)
            } else {
                prayFail(paladin = paladin)
            }

        HolyShieldPhase.Wrath ->
            wrath(
                paladin = paladin,
                attacker = attacker,
                intensity = wrathIntensity(die = cue.wrathIntensity),
            )
    }
}

/**
 * The shield rises to cover the Paladin as holy light gathers over them, the moment the knockout
 * would land.
 */
private suspend fun HolyShieldState.raise(paladin: Rect) {
    resetAll()
    scene = HolyShieldScene(paladin = paladin, attacker = null)
    coroutineScope {
        launch {
            sanctify.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = RAISE_MILLIS, easing = FastOutSlowInEasing),
            )
        }
        launch {
            shieldRaise.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = RAISE_MILLIS, easing = FastOutSlowInEasing),
            )
        }
    }
}

/**
 * A saved prayer: the shield glows through the roll, then a beam of holy light shines down on the
 * Paladin with a +1 heart floating up, the sanctifying aura held bright around them. The beam
 * shimmers forever, held alive until Wrath.
 */
private suspend fun HolyShieldState.praySuccess(paladin: Rect) {
    scene = HolyShieldScene(paladin = paladin, attacker = null)
    if (shieldRaise.value == 0f) shieldRaise.snapTo(targetValue = 1f)

    shieldGlow.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = HOLY_SHIELD_PRAY_GLOW_MILLIS),
    )

    coroutineScope {
        launch {
            holyBeam.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(durationMillis = BEAM_DESCEND_MILLIS, easing = FastOutSlowInEasing),
            )
        }
        launch {
            heart.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(durationMillis = HEART_FLOAT_MILLIS, easing = LinearOutSlowInEasing),
            )
        }
        launch { shimmerForever() }
    }
}

/** A failed prayer: the shield's glow dies as it topples off its point and grays to spent stone. */
private suspend fun HolyShieldState.prayFail(paladin: Rect) {
    scene = HolyShieldScene(paladin = paladin, attacker = null)
    if (shieldRaise.value == 0f) shieldRaise.snapTo(targetValue = 1f)

    shieldGlow.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = HOLY_SHIELD_PRAY_GLOW_MILLIS),
    )

    coroutineScope {
        launch {
            shieldGlow.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = SHIELD_FALL_MILLIS),
            )
        }
        launch {
            shieldFall.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(durationMillis = SHIELD_FALL_MILLIS, easing = FastOutLinearInEasing),
            )
        }
        launch {
            shieldDrain.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = SHIELD_FALL_MILLIS),
            )
        }
        launch {
            sanctify.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = SHIELD_FALL_MILLIS),
            )
        }
    }
    dissolve.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = WRATH_GONE_MILLIS))

    scene = null
}

/**
 * Wrath: the saved beam rolls up to the heavens as the shield dissolves behind it, and after a beat
 * of dark the light blasts back down on the attacker, its force scaled by the damage roll.
 */
private suspend fun HolyShieldState.wrath(paladin: Rect, attacker: Rect?, intensity: Float) {
    scene = HolyShieldScene(paladin = paladin, attacker = attacker)
    wrathIntensity = intensity
    sanctify.snapTo(targetValue = 1f)
    if (holyBeam.value == 0f) holyBeam.snapTo(targetValue = 1f)
    if (shieldRaise.value == 0f) shieldRaise.snapTo(targetValue = 1f)

    coroutineScope {
        launch {
            beamRollUp.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(durationMillis = WRATH_ROLLUP_MILLIS, easing = FastOutSlowInEasing),
            )
        }
        launch {
            dissolve.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = WRATH_ROLLUP_MILLIS),
            )
        }
    }
    holyBeam.snapTo(targetValue = 0f)
    delay(timeMillis = WRATH_GONE_MILLIS.toLong())

    blast.animateTo(
        targetValue = 1f,
        animationSpec =
            tween(durationMillis = WRATH_BLAST_RISE_MILLIS, easing = FastOutSlowInEasing),
    )
    delay(timeMillis = WRATH_BLAST_HOLD_MILLIS.toLong())
    blast.animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = WRATH_BLAST_FADE_MILLIS),
    )

    scene = null
}

/** The beam's endless shimmer: winds one turn at a time so its living glow never snaps back. */
private suspend fun HolyShieldState.shimmerForever() {
    while (true) {
        shimmer.animateTo(
            targetValue = shimmer.value + 1f,
            animationSpec = tween(durationMillis = SHIMMER_PERIOD_MILLIS, easing = LinearEasing),
        )
    }
}

private suspend fun HolyShieldState.resetAll() {
    sanctify.snapTo(targetValue = 0f)
    shieldRaise.snapTo(targetValue = 0f)
    shieldGlow.snapTo(targetValue = 0f)
    holyBeam.snapTo(targetValue = 0f)
    shimmer.snapTo(targetValue = 0f)
    heart.snapTo(targetValue = 0f)
    shieldFall.snapTo(targetValue = 0f)
    shieldDrain.snapTo(targetValue = 0f)
    beamRollUp.snapTo(targetValue = 0f)
    dissolve.snapTo(targetValue = 0f)
    blast.snapTo(targetValue = 0f)
}

/** The 1–6 damage die mapped to a blast force, floored so even a 1 lands with intent. */
private fun wrathIntensity(die: Int?): Float {
    val face = (die ?: 1).coerceIn(minimumValue = 1, maximumValue = 6)
    return lerp(start = WRATH_MIN_INTENSITY, stop = 1f, fraction = (face - 1) / 5f)
}

private fun DrawScope.drawHolyShield(state: HolyShieldState, plusOneLayout: TextLayoutResult) {
    val scene = state.scene ?: return
    val geometry = shieldGeometry(paladin = scene.paladin)

    val sanctify = state.sanctify.value
    if (sanctify > 0f) drawSanctifyGlow(paladin = scene.paladin, amount = sanctify)

    val beam = state.holyBeam.value
    if (beam > 0f) {
        drawHolyBeam(
            geometry = beamGeometry(paladin = scene.paladin),
            beam = beam,
            rollUp = state.beamRollUp.value,
            shimmer = state.shimmer.value,
        )
    }

    val raise = state.shieldRaise.value
    val dissolve = state.dissolve.value
    if (raise > 0f && dissolve < 1f) {
        drawShield(
            geometry = geometry,
            raise = raise,
            glow = state.shieldGlow.value,
            fall = state.shieldFall.value,
            drain = state.shieldDrain.value,
            dissolve = dissolve,
            holyBeam = beam,
        )
    }

    val heart = state.heart.value
    if (heart > 0f) drawHeart(geometry = geometry, progress = heart, plusOneLayout = plusOneLayout)

    val blast = state.blast.value
    if (blast > 0f && scene.attacker != null) {
        drawWrathBlast(attacker = scene.attacker, blast = blast, intensity = state.wrathIntensity)
    }
}

/**
 * The fixed geometry of one shield: a compact ornate heater held to one side of the Paladin seat,
 * as a shield carried on the arm rather than a wall drawn over the whole body.
 */
private fun shieldGeometry(paladin: Rect): ShieldGeometry {
    val avatarWidth = paladin.visibleAvatarWidth()
    val halfWidth = avatarWidth * SHIELD_HALF_WIDTH_FRACTION
    val top = paladin.top + paladin.height * SHIELD_TOP_FRACTION
    val height = paladin.height * SHIELD_HEIGHT_FRACTION
    val centerX = paladin.center.x - avatarWidth * SHIELD_SIDE_OFFSET_FRACTION
    return ShieldGeometry(
        centerX = centerX,
        top = top,
        height = height,
        halfWidth = halfWidth,
        center = Offset(x = centerX, y = top + height * 0.42f),
        tip = Offset(x = centerX, y = top + height),
    )
}

private data class ShieldGeometry(
    val centerX: Float,
    val top: Float,
    val height: Float,
    val halfWidth: Float,
    val center: Offset,
    val tip: Offset,
)

/**
 * The saving beam's column, centered on the Paladin rather than the side-held shield, so the holy
 * light lands on the character it revives.
 */
private fun beamGeometry(paladin: Rect): ShieldGeometry {
    val centerX = paladin.center.x
    return ShieldGeometry(
        centerX = centerX,
        top = paladin.top,
        height = paladin.height,
        halfWidth = paladin.visibleAvatarWidth() * 0.5f,
        center = Offset(x = centerX, y = paladin.top + paladin.height * 0.42f),
        tip = Offset(x = centerX, y = paladin.top + paladin.height),
    )
}

/**
 * The sanctifying aura that bathes the Paladin in holy light the instant a knockout would land: a
 * bright radial glow of essence-green and gold blooming over the whole avatar rather than a dull
 * stone wash, so the divine save reads as light gathering around them.
 */
private fun DrawScope.drawSanctifyGlow(paladin: Rect, amount: Float) {
    val center = Offset(x = paladin.center.x, y = paladin.top + paladin.height * 0.5f)
    val radius = paladin.height * 0.62f
    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        paladinEssenceCore.copy(alpha = SANCTIFY_GLOW_ALPHA * amount),
                        paladinEssenceGreen.copy(alpha = 0.5f * amount),
                        paladinGold.copy(alpha = 0.28f * amount),
                        Color.Transparent,
                    ),
                center = center,
                radius = radius,
            ),
        radius = radius,
        center = center,
    )
}

/**
 * The beam of holy light shining down on the Paladin: a widening column from the heavens, a bright
 * pool where it meets them, drifting motes, and shimmering rays — all winding off [shimmer] so it
 * reads as endlessly alive. [rollUp] lifts the beam's foot toward the top, rolling it up to the
 * heavens during Wrath.
 */
private fun DrawScope.drawHolyBeam(
    geometry: ShieldGeometry,
    beam: Float,
    rollUp: Float,
    shimmer: Float,
) {
    val topY = -geometry.height * 0.5f
    val footY = lerp(start = geometry.center.y, stop = topY, fraction = rollUp)
    if (footY <= topY) return

    val breathe = 0.82f + 0.18f * sin(x = shimmer * 2f * PI.toFloat())
    val columnHalfWidth = geometry.halfWidth * 1.35f
    val topHalfWidth = geometry.halfWidth * 0.34f

    val columnPath =
        Path().apply {
            moveTo(x = geometry.centerX - topHalfWidth, y = topY)
            lineTo(x = geometry.centerX + topHalfWidth, y = topY)
            lineTo(x = geometry.centerX + columnHalfWidth, y = footY)
            lineTo(x = geometry.centerX - columnHalfWidth, y = footY)
            close()
        }
    drawPath(
        path = columnPath,
        brush =
            Brush.verticalGradient(
                colors =
                    listOf(
                        holyCore.copy(alpha = 0.0f),
                        holyCore.copy(alpha = 0.30f * beam * breathe),
                        paladinGold.copy(alpha = 0.42f * beam * breathe),
                    ),
                startY = topY,
                endY = footY,
            ),
    )

    for (ray in 0 until 5) {
        val phase = ray / 5f
        val sway = sin(x = (shimmer + phase) * 2f * PI.toFloat()) * geometry.halfWidth * 0.5f
        val footX = geometry.centerX + (phase - 0.5f) * columnHalfWidth * 1.6f + sway
        drawLine(
            color = holyCore.copy(alpha = 0.28f * beam * breathe),
            start = Offset(x = geometry.centerX + (phase - 0.5f) * topHalfWidth * 1.4f, y = topY),
            end = Offset(x = footX, y = footY),
            strokeWidth = geometry.halfWidth * 0.06f,
        )
    }

    val poolRollOff = 1f - rollUp
    if (poolRollOff > 0f) {
        val poolRadius = geometry.halfWidth * (1.5f + 0.15f * sin(x = shimmer * 4f * PI.toFloat()))
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors =
                        listOf(
                            holyCore.copy(alpha = 0.55f * beam * poolRollOff),
                            paladinEssenceGreen.copy(alpha = 0.28f * beam * poolRollOff),
                            Color.Transparent,
                        ),
                    center = geometry.center,
                    radius = poolRadius,
                ),
            radius = poolRadius,
            center = geometry.center,
        )
        for (mote in 0 until 7) {
            val motePhase = (shimmer * 0.6f + mote / 7f) % 1f
            val rise = 1f - motePhase
            val moteX =
                geometry.centerX +
                    sin(x = (mote + shimmer) * 2f * PI.toFloat()) * geometry.halfWidth * 0.7f
            val moteY = lerp(start = geometry.center.y, stop = topY * 0.4f, fraction = rise)
            drawCircle(
                color = holyCore.copy(alpha = 0.5f * beam * poolRollOff * motePhase),
                radius = geometry.halfWidth * 0.05f * (0.5f + motePhase),
                center = Offset(x = moteX, y = moteY),
            )
        }
    }
}

/**
 * The ornate heater shield: a green field ringed in gold with a jeweled boss and studs, matching
 * the Paladin's mace (SS-199). [raise] scales it in, [glow] adds the roll's holy light, [holyBeam]
 * brightens its face as the saving light lands, and [fall]/[drain] topple and gray a spent shield
 * while [dissolve] fades it away as its light leaves for Wrath.
 */
private fun DrawScope.drawShield(
    geometry: ShieldGeometry,
    raise: Float,
    glow: Float,
    fall: Float,
    drain: Float,
    dissolve: Float,
    holyBeam: Float,
) {
    val alpha = raise * (1f - dissolve)
    val face = lerpColor(start = shieldGreen, stop = stoneGray, fraction = drain)
    val faceDeep = lerpColor(start = shieldGreenDeep, stop = stoneGrayDeep, fraction = drain)
    val goldFace = lerpColor(start = paladinGold, stop = stoneGray, fraction = drain)
    val goldDeep = lerpColor(start = paladinGoldDeep, stop = stoneGray, fraction = drain)

    rotate(degrees = fall * TOPPLE_DEGREES, pivot = geometry.tip) {
        scale(scale = lerp(start = 0.7f, stop = 1f, fraction = raise), pivot = geometry.center) {
            val outline = shieldPath(geometry = geometry, inset = 0f)

            translate(left = geometry.halfWidth * 0.16f, top = geometry.height * 0.05f) {
                drawPath(path = outline, color = Color.Black.copy(alpha = 0.34f * alpha))
            }

            if (glow > 0f || holyBeam > 0f) {
                val glowStrength = maxOf(glow, holyBeam)
                drawCircle(
                    brush =
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    paladinEssenceGreen.copy(alpha = 0.5f * glowStrength * alpha),
                                    paladinGold.copy(alpha = 0.3f * glowStrength * alpha),
                                    Color.Transparent,
                                ),
                            center = geometry.center,
                            radius = geometry.height * 0.7f,
                        ),
                    radius = geometry.height * 0.7f,
                    center = geometry.center,
                )
            }

            drawPath(
                path = outline,
                brush =
                    Brush.verticalGradient(
                        colors = listOf(face, faceDeep),
                        startY = geometry.top,
                        endY = geometry.tip.y,
                    ),
                alpha = alpha,
            )

            drawPath(
                path = outline,
                brush =
                    Brush.radialGradient(
                        colors =
                            listOf(holyCore.copy(alpha = 0.55f * (1f - drain)), Color.Transparent),
                        center =
                            Offset(
                                x = geometry.centerX - geometry.halfWidth * 0.42f,
                                y = geometry.top + geometry.height * 0.16f,
                            ),
                        radius = geometry.height * 0.55f,
                    ),
                alpha = alpha,
            )

            drawPath(
                path = outline,
                brush = Brush.verticalGradient(colors = listOf(goldFace, goldDeep)),
                style = Stroke(width = geometry.halfWidth * 0.16f),
                alpha = alpha,
            )
            drawPath(
                path = shieldPath(geometry = geometry, inset = geometry.halfWidth * 0.24f),
                color = goldDeep,
                style = Stroke(width = geometry.halfWidth * 0.05f),
                alpha = alpha * 0.8f,
            )

            drawGoldCross(geometry = geometry, gold = goldFace, alpha = alpha)
            drawShieldJewels(
                geometry = geometry,
                drain = drain,
                glow = maxOf(glow, holyBeam),
                alpha = alpha,
            )

            val faceLight = holyBeam * (1f - drain)
            if (faceLight > 0f) {
                drawPath(
                    path = outline,
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(holyCore.copy(alpha = 0.5f * faceLight), Color.Transparent),
                            startY = geometry.top,
                            endY = geometry.center.y,
                        ),
                    alpha = alpha,
                )
            }
        }
    }
}

/** The gold cross banding the shield face, echoing the mace's gold reinforcement. */
private fun DrawScope.drawGoldCross(geometry: ShieldGeometry, gold: Color, alpha: Float) {
    val barHalf = geometry.halfWidth * 0.1f
    drawRoundRect(
        color = gold,
        topLeft =
            Offset(x = geometry.centerX - barHalf, y = geometry.top + geometry.height * 0.14f),
        size = Size(width = barHalf * 2f, height = geometry.height * 0.7f),
        cornerRadius = CornerRadius(x = barHalf),
        alpha = alpha * 0.85f,
    )
    drawRoundRect(
        color = gold,
        topLeft =
            Offset(
                x = geometry.centerX - geometry.halfWidth * 0.62f,
                y = geometry.center.y - barHalf,
            ),
        size = Size(width = geometry.halfWidth * 1.24f, height = barHalf * 2f),
        cornerRadius = CornerRadius(x = barHalf),
        alpha = alpha * 0.85f,
    )
}

/** The central emerald boss and the ruby studs that make the shield read as ornate. */
private fun DrawScope.drawShieldJewels(
    geometry: ShieldGeometry,
    drain: Float,
    glow: Float,
    alpha: Float,
) {
    val emerald = lerpColor(start = gemEmerald, stop = stoneGray, fraction = drain)
    val ruby = lerpColor(start = gemRuby, stop = stoneGray, fraction = drain)
    val bossRadius = geometry.halfWidth * 0.24f

    drawCircle(
        color = paladinGoldDeep,
        radius = bossRadius * 1.35f,
        center = geometry.center,
        alpha = alpha,
    )
    drawCircle(
        brush =
            Brush.radialGradient(
                colors = listOf(holyCore.copy(alpha = 0.6f + 0.4f * glow), emerald),
                center =
                    Offset(
                        x = geometry.center.x - bossRadius * 0.3f,
                        y = geometry.center.y - bossRadius * 0.3f,
                    ),
                radius = bossRadius,
            ),
        radius = bossRadius,
        center = geometry.center,
        alpha = alpha,
    )

    val studRadius = geometry.halfWidth * 0.09f
    for (stud in studPositions(geometry = geometry)) {
        drawCircle(
            color = paladinGoldDeep,
            radius = studRadius * 1.4f,
            center = stud,
            alpha = alpha,
        )
        drawCircle(color = ruby, radius = studRadius, center = stud, alpha = alpha)
        drawCircle(
            color = holyCore.copy(alpha = 0.7f),
            radius = studRadius * 0.35f,
            center = Offset(x = stud.x - studRadius * 0.3f, y = stud.y - studRadius * 0.3f),
            alpha = alpha,
        )
    }
}

private fun studPositions(geometry: ShieldGeometry): List<Offset> {
    val topY = geometry.top + geometry.height * 0.14f
    val sideY = geometry.center.y
    return listOf(
        Offset(x = geometry.centerX - geometry.halfWidth * 0.54f, y = topY),
        Offset(x = geometry.centerX + geometry.halfWidth * 0.54f, y = topY),
        Offset(x = geometry.centerX - geometry.halfWidth * 0.5f, y = sideY),
        Offset(x = geometry.centerX + geometry.halfWidth * 0.5f, y = sideY),
    )
}

/** A heater shield outline, [inset] shrunk toward its center for the inner banding. */
private fun shieldPath(geometry: ShieldGeometry, inset: Float): Path {
    val halfWidth = geometry.halfWidth - inset
    val top = geometry.top + inset
    val height = geometry.height - inset * 2f
    val shoulderY = top + height * 0.07f
    val straightY = top + height * 0.5f
    val tipY = top + height
    val centerX = geometry.centerX

    return Path().apply {
        moveTo(x = centerX - halfWidth, y = shoulderY)
        quadraticTo(
            x1 = centerX,
            y1 = top - height * 0.03f,
            x2 = centerX + halfWidth,
            y2 = shoulderY,
        )
        lineTo(x = centerX + halfWidth, y = straightY)
        quadraticTo(x1 = centerX + halfWidth, y1 = tipY - height * 0.08f, x2 = centerX, y2 = tipY)
        quadraticTo(
            x1 = centerX - halfWidth,
            y1 = tipY - height * 0.08f,
            x2 = centerX - halfWidth,
            y2 = straightY,
        )
        close()
    }
}

/** The +1 heart that floats up off a saved shield, rising and fading as [progress] runs. */
private fun DrawScope.drawHeart(
    geometry: ShieldGeometry,
    progress: Float,
    plusOneLayout: TextLayoutResult,
) {
    val fade = 1f - ((progress - 0.7f) / 0.3f).coerceIn(minimumValue = 0f, maximumValue = 1f)
    val appear = (progress / 0.2f).coerceAtMost(maximumValue = 1f)
    val alpha = fade * appear
    if (alpha <= 0.01f) return

    val heartSize = geometry.halfWidth * 0.7f
    val rise = geometry.height * 0.55f * progress
    val center =
        Offset(x = geometry.centerX + geometry.halfWidth * 0.5f, y = geometry.center.y - rise)

    val heart = heartPath(center = center, size = heartSize)
    drawCircle(
        brush =
            Brush.radialGradient(
                colors = listOf(holyCore.copy(alpha = 0.5f), Color.Transparent),
                center = center,
                radius = heartSize,
            ),
        radius = heartSize,
        center = center,
        alpha = alpha,
    )
    drawPath(path = heart, color = gemRuby, alpha = alpha)
    drawPath(
        path = heart,
        color = holyCore.copy(alpha = 0.9f),
        style = Stroke(width = heartSize * 0.08f),
        alpha = alpha,
    )

    val glyphScale = (heartSize * 1.4f) / plusOneLayout.size.height
    val textCenter = Offset(x = center.x + heartSize * 1.1f, y = center.y)
    scale(scale = glyphScale, pivot = textCenter) {
        drawText(
            textLayoutResult = plusOneLayout,
            color = paladinGold,
            topLeft =
                Offset(
                    x = textCenter.x - plusOneLayout.size.width / 2f,
                    y = textCenter.y - plusOneLayout.size.height / 2f,
                ),
            alpha = alpha,
        )
    }
}

private fun heartPath(center: Offset, size: Float): Path =
    Path().apply {
        moveTo(x = center.x, y = center.y + size * 0.4f)
        cubicTo(
            x1 = center.x - size * 0.6f,
            y1 = center.y + size * 0.05f,
            x2 = center.x - size * 0.55f,
            y2 = center.y - size * 0.38f,
            x3 = center.x,
            y3 = center.y - size * 0.18f,
        )
        cubicTo(
            x1 = center.x + size * 0.55f,
            y1 = center.y - size * 0.38f,
            x2 = center.x + size * 0.6f,
            y2 = center.y + size * 0.05f,
            x3 = center.x,
            y3 = center.y + size * 0.4f,
        )
        close()
    }

/**
 * The Wrath blast: the rolled-up holy light slams back down on the attacker as a bright descending
 * column and a burst at impact, its width and reach scaled by [intensity] so a big roll floods the
 * seat while even a 1 still lands a fierce shaft.
 */
private fun DrawScope.drawWrathBlast(attacker: Rect, blast: Float, intensity: Float) {
    val impact = attacker.center
    val avatarWidth = attacker.visibleAvatarWidth()
    val topY = -attacker.height * 0.4f
    val columnHalfWidth =
        avatarWidth * lerp(start = 0.3f, stop = 0.7f, fraction = intensity) * blast
    val reach = lerp(start = 0.7f, stop = 1f, fraction = intensity)

    val columnPath =
        Path().apply {
            moveTo(x = impact.x - columnHalfWidth * 0.35f, y = topY)
            lineTo(x = impact.x + columnHalfWidth * 0.35f, y = topY)
            lineTo(x = impact.x + columnHalfWidth, y = impact.y)
            lineTo(x = impact.x - columnHalfWidth, y = impact.y)
            close()
        }
    drawPath(
        path = columnPath,
        brush =
            Brush.verticalGradient(
                colors =
                    listOf(
                        holyCore.copy(alpha = 0.85f * blast * reach),
                        paladinGold.copy(alpha = 0.7f * blast * reach),
                        paladinEssenceGreen.copy(alpha = 0.55f * blast),
                    ),
                startY = topY,
                endY = impact.y,
            ),
    )

    val burstRadius = avatarWidth * lerp(start = 0.55f, stop = 1.1f, fraction = intensity) * blast
    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        holyCore.copy(alpha = 0.9f * blast),
                        paladinGold.copy(alpha = 0.6f * blast),
                        paladinEssenceGreen.copy(alpha = 0.3f * blast),
                        Color.Transparent,
                    ),
                center = impact,
                radius = burstRadius,
            ),
        radius = burstRadius,
        center = impact,
    )

    val rayCount = 8
    for (ray in 0 until rayCount) {
        val angle = ray / rayCount.toFloat() * 2f * PI.toFloat()
        val length = burstRadius * (0.7f + 0.3f * intensity)
        drawLine(
            color = holyCore.copy(alpha = 0.6f * blast * intensity),
            start = impact,
            end = impact + Offset(x = cos(x = angle) * length, y = sin(x = angle) * length),
            strokeWidth = avatarWidth * 0.03f,
        )
    }
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PaladinHolyShieldSavedPreview() {
    PaladinHolyShieldPreview(didSurvive = true)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PaladinHolyShieldStruckDownPreview() {
    PaladinHolyShieldPreview(didSurvive = false)
}

/**
 * The whole Holy Shield played over the Paladin seat: the shield rises over the stoned Paladin, the
 * prayer roll glows, and then either the beam saves them and Wrath blasts the attacker, or the
 * shield topples and grays — all driven off the one replaying cue sequence.
 */
@Composable
private fun PaladinHolyShieldPreview(didSurvive: Boolean) {
    val seats = rememberDuoSeats()
    val cue =
        rememberReplayingCues(replayKey = didSurvive) { iteration ->
            buildList {
                add(
                    element =
                        holyShieldPreviewCue(iteration = iteration, phase = HolyShieldPhase.Raise)
                )
                add(
                    element =
                        holyShieldPreviewCue(
                            iteration = iteration,
                            phase = HolyShieldPhase.Pray,
                            didSurvive = didSurvive,
                        )
                )
                if (didSurvive) {
                    add(
                        element =
                            holyShieldPreviewCue(
                                iteration = iteration,
                                phase = HolyShieldPhase.Wrath,
                                didSurvive = true,
                                attackerUserId = PREVIEW_TARGET_ID,
                                wrathIntensity = iteration % 6 + 1,
                            )
                    )
                }
            }
        }
    AbilityAnimationStage(accentClass = CharacterClassType.Paladin, seats = seats) {
        PaladinHolyShieldOverlay(
            animationCue = cue,
            avatarBoundsFor = seats.boundsFor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun holyShieldPreviewCue(
    iteration: Int,
    phase: HolyShieldPhase,
    didSurvive: Boolean? = null,
    attackerUserId: String? = null,
    wrathIntensity: Int? = null,
): CombatAnimationCue.HolyShield =
    CombatAnimationCue.HolyShield(
        id = "holy-shield-$phase-$didSurvive-$iteration",
        paladinUserId = PREVIEW_ACTOR_ID,
        phase = phase,
        didSurvive = didSurvive,
        attackerUserId = attackerUserId,
        wrathIntensity = wrathIntensity,
    )
