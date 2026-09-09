package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val DEGREES_PER_CLASS = 45f
private const val QUARTER_TURN_DEGREES = 90f
private const val HALF_TURN_DEGREES = 180f
private const val LABEL_FLIP_LABEL = "class_label_flip"
private const val LABEL_FLIP_DURATION_MILLIS = 300
private const val DISABLED_ALPHA = 0.35f

private const val AURA_TRANSITION_LABEL = "class_aura"
private const val AURA_SCALE_LABEL = "class_aura_scale"
private const val AURA_ALPHA_LABEL = "class_aura_alpha"
private const val AURA_SIZE_FRACTION = 2f
private const val AURA_GLOW_LIGHTEN = 0.5f
private const val AURA_SOLID_STOP = 0.55f
private const val AURA_MIN_SCALE = 0.82f
private const val AURA_MAX_SCALE = 1.18f
private const val AURA_MIN_ALPHA = 0.45f
private const val AURA_MAX_ALPHA = 0.95f
private const val AURA_PULSE_DURATION_MILLIS = 1200

private const val SPOKE_LENGTH_FRACTION = 0.34f
private const val SPOKE_THICKNESS_FRACTION = 0.06f
private const val HUB_RADIUS_FRACTION = 0.06f
private const val AVATAR_SIZE_FRACTION = 0.24f
private const val AVATAR_RADIUS_FRACTION = 0.38f
private const val LABEL_RADIUS_FRACTION = 0.2f
private const val LABEL_NUMBER_RADIUS_FRACTION = 0.08f

// TODO: Refactor this BAD

@Composable
fun CharacterClassWheel(
    characterClasses: List<CharacterClassState>,
    selectedClass: CharacterClassState?,
    unavailableClasses: Set<CharacterClassType>,
    isItThisUsersTurn: Boolean,
    pinnedAuraClassType: CharacterClassType?,
    modifier: Modifier = Modifier,
) {
    val count = characterClasses.size
    if (count == 0) return

    val scope = rememberCoroutineScope()
    val initialIndex =
        remember(key1 = count) {
            characterClasses.indexOf(selectedClass).coerceAtLeast(minimumValue = 0)
        }

    var rotationDegrees by
        remember(key1 = count) { mutableFloatStateOf(rotationForIndex(index = initialIndex)) }
    var animationJob by remember { mutableStateOf<Job?>(null) }

    val centeredIndex by
        remember(key1 = count) {
            derivedStateOf { centeredIndex(rotationDegrees = rotationDegrees, count = count) }
        }

    var statsIndex by remember(key1 = count) { mutableIntStateOf(value = initialIndex) }
    val statsClass = characterClasses[statsIndex]

    fun settleStatsToCenter() {
        statsIndex = centeredIndex(rotationDegrees = rotationDegrees, count = count)
    }

    fun animateRotationTo(targetDegrees: Float) {
        animationJob?.cancel()
        animationJob =
            scope.launch {
                val animatable = Animatable(initialValue = rotationDegrees)
                animatable.animateTo(targetValue = targetDegrees, animationSpec = WHEEL_SNAP_SPEC) {
                    rotationDegrees = value
                }
                settleStatsToCenter()
            }
    }

    val onClassSelected = ArenaActionsProvider.current.onSelectCharacterClass
    fun select(index: Int) {
        animateRotationTo(
            targetDegrees = nearestRotationForIndex(index = index, fromDegrees = rotationDegrees),
        )
        val targetClass = characterClasses[index]
        if (targetClass.type !in unavailableClasses) {
            if (isItThisUsersTurn) onClassSelected(targetClass)
        }
    }

    LaunchedEffect(key1 = selectedClass) {
        val targetIndex = characterClasses.indexOf(selectedClass)
        val isElsewhere =
            targetIndex >= 0 &&
                targetIndex != centeredIndex(rotationDegrees = rotationDegrees, count = count)
        if (isElsewhere) {
            animateRotationTo(
                targetDegrees =
                nearestRotationForIndex(index = targetIndex, fromDegrees = rotationDegrees),
            )
        }
    }

    var inspectedClass by remember { mutableStateOf<CharacterClassState?>(value = null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(weight = 1f, fill = false),
                contentAlignment = Alignment.Center,
            ) {
                val wheelSize =
                    if (constraints.hasBoundedHeight) minOf(maxWidth, maxHeight) else maxWidth

                Box(
                    modifier =
                    Modifier.size(size = wheelSize).pointerInput(key1 = count) {
                        detectDragGestures(
                            onDragStart = { animationJob?.cancel() },
                            onDragEnd = {
                                select(
                                    index =
                                    centeredIndex(rotationDegrees = rotationDegrees, count = count),
                                )
                            },
                            onDragCancel = {
                                select(
                                    index =
                                    centeredIndex(rotationDegrees = rotationDegrees, count = count),
                                )
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val previousAngle =
                                    atan2(
                                        y = change.previousPosition.y - centerY,
                                        x = change.previousPosition.x - centerX,
                                    )
                                val currentAngle =
                                    atan2(
                                        y = change.position.y - centerY,
                                        x = change.position.x - centerX,
                                    )
                                var sweptDegrees =
                                    (currentAngle - previousAngle) * 180f / PI.toFloat()
                                if (sweptDegrees > 180f) sweptDegrees -= 360f
                                if (sweptDegrees < -180f) sweptDegrees += 360f
                                rotationDegrees += sweptDegrees
                            },
                        )
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    RotatingWheel(
                        characterClasses = characterClasses,
                        rotationDegrees = { rotationDegrees },
                        unavailableClasses = unavailableClasses,
                        selectedIndex =
                        when {
                            pinnedAuraClassType != null ->
                                characterClasses.indexOfFirst { it.type == pinnedAuraClassType }

                            isItThisUsersTurn -> centeredIndex

                            else -> -1
                        },
                        wheelSize = wheelSize,
                        onClassClick = { index -> select(index = index) },
                    )
                }
            }

            ClassPageStats(
                characterClass = statsClass,
                onInspect = { inspectedClass = statsClass },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        InspectDrawer(
            characterClass = inspectedClass,
            onClose = { inspectedClass = null },
            isDetailed = true,
        )
    }
}

private val WHEEL_SNAP_SPEC =
    spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

private val LABEL_FLIP_SPEC = tween<Float>(durationMillis = LABEL_FLIP_DURATION_MILLIS)

@Composable
private fun RotatingWheel(
    characterClasses: List<CharacterClassState>,
    rotationDegrees: () -> Float,
    unavailableClasses: Set<CharacterClassType>,
    selectedIndex: Int,
    wheelSize: Dp,
    onClassClick: (Int) -> Unit,
) {
    val hubColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier.size(size = wheelSize).graphicsLayer { rotationZ = rotationDegrees() },
        contentAlignment = Alignment.Center,
    ) {
        WheelSpokes(characterClasses = characterClasses, hubColor = hubColor)

        characterClasses.forEachIndexed { index, characterClass ->
            val baseAngle = index * DEGREES_PER_CLASS
            val isAvailable = characterClass.type !in unavailableClasses

            ClassLabel(
                name = characterClass.className,
                turnOrder = index + 1,
                baseAngle = baseAngle,
                wheelRotationDegrees = rotationDegrees,
                wheelSize = wheelSize,
                isAvailable = isAvailable,
            )
            ClassAvatar(
                type = characterClass.type,
                baseAngle = baseAngle,
                wheelRotationDegrees = rotationDegrees,
                wheelSize = wheelSize,
                isAvailable = isAvailable,
                isSelected = index == selectedIndex,
                onClick = { onClassClick(index) },
            )
        }
    }
}

@Composable
private fun WheelSpokes(characterClasses: List<CharacterClassState>, hubColor: Color) {
    Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(ratio = 1f)) {
        val center = Offset(x = size.width / 2f, y = size.height / 2f)
        val spokeLength = size.minDimension * SPOKE_LENGTH_FRACTION
        val spokeThickness = size.minDimension * SPOKE_THICKNESS_FRACTION
        val spokeCorner = CornerRadius(x = spokeThickness / 2f, y = spokeThickness / 2f)

        characterClasses.forEachIndexed { index, characterClass ->
            rotate(degrees = index * DEGREES_PER_CLASS, pivot = center) {
                drawRoundRect(
                    color = characterClass.type.toColor(),
                    topLeft =
                    Offset(x = center.x - spokeThickness / 2f, y = center.y - spokeLength),
                    size = Size(width = spokeThickness, height = spokeLength),
                    cornerRadius = spokeCorner,
                )
            }
        }

        drawCircle(
            color = hubColor,
            radius = size.minDimension * HUB_RADIUS_FRACTION,
            center = center,
        )
    }
}

@Composable
private fun BoxScope.ClassAvatar(
    type: CharacterClassType,
    baseAngle: Float,
    wheelRotationDegrees: () -> Float,
    wheelSize: Dp,
    isAvailable: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val angleRadians = baseAngle.toDouble() * PI / 180.0
    val sinAngle = sin(angleRadians).toFloat()
    val cosAngle = cos(angleRadians).toFloat()
    val avatarSize = wheelSize * AVATAR_SIZE_FRACTION

    Box(
        modifier =
        Modifier.size(size = avatarSize)
            .align(Alignment.Center)
            .offset {
                val radius = (wheelSize * AVATAR_RADIUS_FRACTION).toPx()
                IntOffset(
                    x = (radius * sinAngle).roundToInt(),
                    y = (-radius * cosAngle).roundToInt(),
                )
            }
            .graphicsLayer { rotationZ = -wheelRotationDegrees() }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication =
                ripple(bounded = false, radius = avatarSize * AURA_SIZE_FRACTION / 2f),
                onClickLabel = type.name,
            ) {
                onClick()
            }
            .alpha(alpha = if (isAvailable) 1f else DISABLED_ALPHA),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            ClassAura(color = type.toColor(), diameter = avatarSize * AURA_SIZE_FRACTION)
        }
        Image(
            painter = painterResource(resource = type.toBustResource()),
            contentDescription = type.name,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ClassAura(color: Color, diameter: Dp, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = AURA_TRANSITION_LABEL)
    val breathSpec =
        infiniteRepeatable<Float>(
            animation = tween(durationMillis = AURA_PULSE_DURATION_MILLIS),
            repeatMode = RepeatMode.Reverse,
        )
    val scale by
        transition.animateFloat(
            initialValue = AURA_MIN_SCALE,
            targetValue = AURA_MAX_SCALE,
            animationSpec = breathSpec,
            label = AURA_SCALE_LABEL,
        )
    val glowAlpha by
        transition.animateFloat(
            initialValue = AURA_MIN_ALPHA,
            targetValue = AURA_MAX_ALPHA,
            animationSpec = breathSpec,
            label = AURA_ALPHA_LABEL,
        )

    val glowColor = lerp(start = color, stop = Color.White, fraction = AURA_GLOW_LIGHTEN)

    Box(
        modifier =
        modifier
            .size(size = diameter)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = glowAlpha
            }
            .background(
                brush =
                Brush.radialGradient(
                    colorStops =
                    arrayOf(
                        0f to glowColor,
                        AURA_SOLID_STOP to glowColor,
                        1f to Color.Transparent,
                    ),
                ),
                shape = CircleShape,
            ),
    )
}

@Composable
private fun BoxScope.ClassLabel(
    name: String,
    turnOrder: Int,
    baseAngle: Float,
    wheelRotationDegrees: () -> Float,
    wheelSize: Dp,
    isAvailable: Boolean,
) {
    val flipTurns by
        remember(key1 = baseAngle) {
            derivedStateOf {
                val settledRotation =
                    (wheelRotationDegrees() / DEGREES_PER_CLASS).roundToInt() * DEGREES_PER_CLASS
                flipTurnsFor(effectiveAngle = baseAngle + settledRotation)
            }
        }
    val labelRotation by
        animateFloatAsState(
            targetValue = baseAngle - QUARTER_TURN_DEGREES - HALF_TURN_DEGREES * flipTurns,
            animationSpec = LABEL_FLIP_SPEC,
            label = LABEL_FLIP_LABEL,
        )

    LabelText(
        text = name,
        radiusFraction = LABEL_RADIUS_FRACTION,
        baseAngle = baseAngle,
        rotation = { labelRotation },
        wheelSize = wheelSize,
        isAvailable = isAvailable,
    )
    LabelText(
        text = turnOrder.toString(),
        radiusFraction = LABEL_NUMBER_RADIUS_FRACTION,
        baseAngle = baseAngle,
        rotation = { labelRotation },
        wheelSize = wheelSize,
        isAvailable = isAvailable,
    )
}

@Composable
private fun BoxScope.LabelText(
    text: String,
    radiusFraction: Float,
    baseAngle: Float,
    rotation: () -> Float,
    wheelSize: Dp,
    isAvailable: Boolean,
) {
    val angleRadians = baseAngle.toDouble() * PI / 180.0
    val sinAngle = sin(x = angleRadians).toFloat()
    val cosAngle = cos(x = angleRadians).toFloat()

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier =
        Modifier.align(Alignment.Center)
            .offset {
                val radius = (wheelSize * radiusFraction).toPx()
                IntOffset(
                    x = (radius * sinAngle).roundToInt(),
                    y = (-radius * cosAngle).roundToInt(),
                )
            }
            .graphicsLayer { rotationZ = rotation() }
            .alpha(alpha = if (isAvailable) 1f else DISABLED_ALPHA),
    )
}

private fun flipTurnsFor(effectiveAngle: Float): Int = ceil((effectiveAngle - HALF_TURN_DEGREES) / HALF_TURN_DEGREES).toInt()

private fun rotationForIndex(index: Int) = -index * DEGREES_PER_CLASS

private fun centeredIndex(rotationDegrees: Float, count: Int): Int {
    val rawIndex = (-rotationDegrees / DEGREES_PER_CLASS).roundToInt()
    return ((rawIndex % count) + count) % count
}

private fun nearestRotationForIndex(index: Int, fromDegrees: Float): Float {
    val base = rotationForIndex(index = index)
    val turns = ((fromDegrees - base) / 360f).roundToInt()
    return base + turns * 360f
}

@Preview
@Composable
private fun CharacterClassWheelPreview() {
    val characterClasses = previewCharacterClassStates()
    SorcerersAndSpiritsTheme {
        CompositionLocalProvider(LocalCharacterClassCopy provides previewCharacterClassCopy()) {
            CharacterClassWheel(
                characterClasses = characterClasses,
                selectedClass = characterClasses.first(),
                unavailableClasses = emptySet(),
                isItThisUsersTurn = true,
                pinnedAuraClassType = null,
            )
        }
    }
}
