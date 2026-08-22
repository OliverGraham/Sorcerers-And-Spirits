package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.paladin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaAnimationState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AnimationPreview
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.AvatarBoundsStateHolder
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.visibleAvatarWidth
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberSoloAvatar
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
internal const val HOLY_SHIELD_WRATH_IMPACT_DELAY_MILLIS =
    WRATH_ROLLUP_MILLIS + WRATH_GONE_MILLIS + WRATH_BLAST_RISE_MILLIS

private const val SHIELD_HALF_WIDTH_FRACTION = 0.3f
private const val SHIELD_HEIGHT_FRACTION = 0.46f
private const val SHIELD_SIDE_OFFSET_FRACTION = 0.42f
private const val SHIELD_TOP_FRACTION = 0.24f
private const val TOPPLE_DEGREES = 84f
private const val SANCTIFY_GLOW_ALPHA = 0.85f
private const val WRATH_MIN_INTENSITY = 0.45f

private val stoneGray = Color(color = 0xFF74777C)
private val stoneGrayDeep = Color(color = 0xFF5C5F63)
private val shieldGreen = paladinEssenceGreen
private val shieldGreenDeep = Color(color = 0xFF2C6E1F)
private val holyCore = Color(color = 0xFFFFFDF0)
private val gemEmerald = Color(color = 0xFF7BF04A)
private val gemRuby = Color(color = 0xFFE23A5A)

@Composable
internal fun PaladinHolyShieldOverlay(
    animatingMessage: ArenaMessageState.HolyShield,
    avatarBoundsStateHolder: AvatarBoundsStateHolder,
    modifier: Modifier = Modifier,
) {
    val state = remember { HolyShieldState() }
    val onAvatarAnimationComplete = ArenaActionsProvider.current.onAvatarComplete

    LaunchedEffect(key1 = animatingMessage.id) {
        state.play(message = animatingMessage, avatarBoundsStateHolder = avatarBoundsStateHolder)
        onAvatarAnimationComplete()
    }

    val textMeasurer = rememberTextMeasurer()
    val plusOneLayout =
        remember(key1 = textMeasurer) {
            textMeasurer.measure(
                text = "+1",
                style = TextStyle(fontSize = PLUS_ONE_MEASURE_SP.sp, fontWeight = FontWeight.Black),
            )
        }
    Canvas(modifier = modifier) { drawHolyShield(state = state, plusOneLayout = plusOneLayout) }
}

private const val PLUS_ONE_MEASURE_SP = 64

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

@Immutable internal data class HolyShieldScene(val paladin: Rect, val attacker: Rect?)

private suspend fun HolyShieldState.play(
    message: ArenaMessageState.HolyShield,
    avatarBoundsStateHolder: AvatarBoundsStateHolder,
) {
    val paladinBounds = avatarBoundsStateHolder.boundsFor(userId = message.paladinUserId) ?: return

    when (message) {
        is ArenaMessageState.HolyShield.Trigger -> raise(paladin = paladinBounds)

        is ArenaMessageState.HolyShield.Prayer ->
            if (message.didSurvive) {
                praySuccess(paladin = paladinBounds)
            } else {
                prayFail(paladin = paladinBounds)
            }

        is ArenaMessageState.HolyShield.Wrath -> {
            val attackerBounds =
                avatarBoundsStateHolder.boundsFor(userId = message.attackerUserId) ?: return
            wrath(
                paladin = paladinBounds,
                attacker = attackerBounds,
                intensity = wrathIntensity(die = message.singleDie),
            )
        }
    }
}

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

private fun wrathIntensity(die: Int): Float =
    lerp(start = WRATH_MIN_INTENSITY, stop = 1f, fraction = (die - 1) / 5f)

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

@Composable
private fun PaladinHolyShieldPreview(didSurvive: Boolean) {
    val avatars = rememberSoloAvatar()
    AnimationPreview(
        accentClass = CharacterClassType.Paladin,
        avatars = avatars,
        buildMessage = { id ->
            ArenaMessageState.HolyShield.Prayer(
                id = id,
                paladinUserId = PREVIEW_ACTOR_ID,
                firstDie = if (didSurvive) 6 else 2,
                secondDie = if (didSurvive) 5 else 2,
                didSurvive = didSurvive,
                animation =
                    if (didSurvive) {
                        ArenaAnimationState.Flash
                    } else {
                        ArenaAnimationState.FlashAndAvatar()
                    },
                gestures = emptyMap(),
            )
        },
    ) { message ->
        PaladinHolyShieldOverlay(
            animatingMessage = message,
            avatarBoundsStateHolder = avatars.boundsStateHolder,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
