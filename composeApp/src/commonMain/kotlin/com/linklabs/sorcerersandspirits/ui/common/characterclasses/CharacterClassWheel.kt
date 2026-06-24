package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassCopy
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.ClassCopyKeys
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.class_bard
import sorcerersandspirits.composeapp.generated.resources.class_duelist
import sorcerersandspirits.composeapp.generated.resources.class_gambler
import sorcerersandspirits.composeapp.generated.resources.class_illusionist
import sorcerersandspirits.composeapp.generated.resources.class_mage
import sorcerersandspirits.composeapp.generated.resources.class_necromancer
import sorcerersandspirits.composeapp.generated.resources.class_paladin
import sorcerersandspirits.composeapp.generated.resources.class_rogue
import sorcerersandspirits.composeapp.generated.resources.ic_armor
import sorcerersandspirits.composeapp.generated.resources.ic_health
import sorcerersandspirits.composeapp.generated.resources.ic_spc
import sorcerersandspirits.composeapp.generated.resources.ic_speed

private const val DEGREES_PER_CLASS = 45f
private const val QUARTER_TURN_DEGREES = 90f
private const val DISABLED_ALPHA = 0.35f

private const val CLASS_NAME_ANIM_LABEL = "stat_class_name"
private const val STAT_VALUE_ANIM_LABEL = "stat_value"
private const val ABILITY_NAME_ANIM_LABEL = "stat_ability_name"

private const val SPOKE_LENGTH_FRACTION = 0.34f
private const val SPOKE_THICKNESS_FRACTION = 0.06f
private const val HUB_RADIUS_FRACTION = 0.10f
private const val AVATAR_SIZE_FRACTION = 0.24f
private const val AVATAR_RADIUS_FRACTION = 0.38f
private const val LABEL_RADIUS_FRACTION = 0.22f

@Composable
fun CharacterClassWheel(
    characterClasses: List<CharacterClassState>,
    modifier: Modifier = Modifier,
    selectedClass: CharacterClassState? = null,
    unavailableClasses: Set<CharacterClassType> = emptySet(),
    classSelections: Map<CharacterClassType, String> = emptyMap(),
    onClassSelected: (CharacterClassState) -> Unit = {},
) {
    val count = characterClasses.size
    if (count == 0) return

    val scope = rememberCoroutineScope()
    val initialIndex =
        remember(key1 = count) {
            characterClasses.indexOf(selectedClass).coerceAtLeast(minimumValue = 0)
        }

    // Live rotation is a plain float updated synchronously on every drag frame; the Animatable
    // is used only for the settle/tap/selection animations so dragging never hops through a
    // coroutine.
    var rotationDegrees by
        remember(key1 = count) { mutableFloatStateOf(rotationForIndex(index = initialIndex)) }
    var animationJob by remember { mutableStateOf<Job?>(null) }

    // Derived so the wheel subtree (avatars, labels, stat bar) only recomposes when the centred
    // class actually changes — not on every drag frame. The live rotation reaches the GPU purely
    // through graphicsLayer reads, never through recomposition.
    val centeredIndex by
        remember(key1 = count) {
            derivedStateOf { centeredIndex(rotationDegrees = rotationDegrees, count = count) }
        }
    val centeredClass = characterClasses[centeredIndex]

    fun animateRotationTo(targetDegrees: Float) {
        animationJob?.cancel()
        animationJob =
            scope.launch {
                val animatable = Animatable(initialValue = rotationDegrees)
                animatable.animateTo(targetValue = targetDegrees, animationSpec = WHEEL_SNAP_SPEC) {
                    rotationDegrees = value
                }
            }
    }

    fun select(index: Int) {
        animateRotationTo(
            targetDegrees = nearestRotationForIndex(index = index, fromDegrees = rotationDegrees)
        )
        val targetClass = characterClasses[index]
        if (targetClass.type !in unavailableClasses) onClassSelected(targetClass)
    }

    LaunchedEffect(key1 = selectedClass) {
        val targetIndex = characterClasses.indexOf(selectedClass)
        val isElsewhere =
            targetIndex >= 0 &&
                targetIndex != centeredIndex(rotationDegrees = rotationDegrees, count = count)
        if (isElsewhere) {
            animateRotationTo(
                targetDegrees =
                    nearestRotationForIndex(index = targetIndex, fromDegrees = rotationDegrees)
            )
        }
    }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(
            modifier =
                Modifier.fillMaxWidth().widthIn(max = WHEEL_MAX_SIZE).aspectRatio(ratio = 1f),
            contentAlignment = Alignment.Center,
        ) {
            val wheelSize = maxWidth

            Box(
                modifier =
                    Modifier.size(size = wheelSize)
                        // Angular drag: rotate by the angle the finger sweeps around the centre so
                        // the grabbed point always tracks the thumb, whatever the clock position.
                        .pointerInput(key1 = count) {
                            detectDragGestures(
                                onDragStart = { animationJob?.cancel() },
                                onDragEnd = {
                                    select(
                                        index =
                                            centeredIndex(
                                                rotationDegrees = rotationDegrees,
                                                count = count,
                                            )
                                    )
                                },
                                onDragCancel = {
                                    select(
                                        index =
                                            centeredIndex(
                                                rotationDegrees = rotationDegrees,
                                                count = count,
                                            )
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
                    wheelSize = wheelSize,
                    onClassClick = { index -> select(index = index) },
                )
            }
        }

        ClassStatBar(
            characterClass = centeredClass,
            selectedBy = classSelections[centeredClass.type],
        )
    }
}

private val WHEEL_MAX_SIZE = 320.dp
private val WHEEL_SNAP_SPEC =
    spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

@Composable
private fun RotatingWheel(
    characterClasses: List<CharacterClassState>,
    rotationDegrees: () -> Float,
    unavailableClasses: Set<CharacterClassType>,
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
                baseAngle = baseAngle,
                wheelSize = wheelSize,
                isAvailable = isAvailable,
            )
            ClassAvatar(
                type = characterClass.type,
                baseAngle = baseAngle,
                wheelRotationDegrees = rotationDegrees,
                wheelSize = wheelSize,
                isAvailable = isAvailable,
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
                    color = spokeColor(type = characterClass.type),
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
    onClick: () -> Unit,
) {
    val angleRadians = baseAngle.toDouble() * PI / 180.0
    val sinAngle = sin(angleRadians).toFloat()
    val cosAngle = cos(angleRadians).toFloat()

    Image(
        painter = painterResource(resource = classArtwork(type = type)),
        contentDescription = type.name,
        modifier =
            Modifier.size(size = wheelSize * AVATAR_SIZE_FRACTION)
                .align(Alignment.Center)
                .offset {
                    val radius = (wheelSize * AVATAR_RADIUS_FRACTION).toPx()
                    IntOffset(
                        x = (radius * sinAngle).roundToInt(),
                        y = (-radius * cosAngle).roundToInt(),
                    )
                }
                .graphicsLayer { rotationZ = -wheelRotationDegrees() }
                .clickable(onClickLabel = type.name) { onClick() }
                .alpha(alpha = if (isAvailable) 1f else DISABLED_ALPHA),
    )
}

@Composable
private fun BoxScope.ClassLabel(
    name: String,
    baseAngle: Float,
    wheelSize: Dp,
    isAvailable: Boolean,
) {
    val angleRadians = baseAngle.toDouble() * PI / 180.0
    val sinAngle = sin(angleRadians).toFloat()
    val cosAngle = cos(angleRadians).toFloat()

    Text(
        text = name,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier =
            Modifier.align(Alignment.Center)
                .offset {
                    val radius = (wheelSize * LABEL_RADIUS_FRACTION).toPx()
                    IntOffset(
                        x = (radius * sinAngle).roundToInt(),
                        y = (-radius * cosAngle).roundToInt(),
                    )
                }
                .graphicsLayer { rotationZ = baseAngle - QUARTER_TURN_DEGREES }
                .alpha(alpha = if (isAvailable) 1f else DISABLED_ALPHA),
    )
}

@Composable
private fun ClassStatBar(
    characterClass: CharacterClassState,
    selectedBy: String?,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(value = false) }
    val classCopy = LocalCharacterClassCopy.current

    ElevatedCard(
        onClick = { isExpanded = !isExpanded },
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .animateContentSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 4.dp),
        ) {
            AnimatedContent(
                targetState = characterClass.className,
                label = CLASS_NAME_ANIM_LABEL,
            ) { className ->
                Text(
                    text = className,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (selectedBy != null) {
                Text(
                    text = "Chosen by: $selectedBy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedStatChip(icon = Res.drawable.ic_health, value = characterClass.hitPoints)
                AnimatedStatChip(icon = Res.drawable.ic_armor, value = characterClass.armorClass)
                AnimatedStatChip(icon = Res.drawable.ic_speed, value = characterClass.speed)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AbilityChip(
                    abilityName =
                        classCopy.forAbility(ability = characterClass.offensiveAbility).name
                )
                AbilityChip(
                    abilityName =
                        classCopy.forAbility(ability = characterClass.defensiveAbility).name
                )
            }

            if (isExpanded) {
                ClassStatDetails(characterClass = characterClass, classCopy = classCopy)
            }
        }
    }
}

@Composable
private fun AnimatedStatChip(icon: DrawableResource, value: Int, modifier: Modifier = Modifier) {
    val animatedValue by animateIntAsState(targetValue = value, label = STAT_VALUE_ANIM_LABEL)

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(resource = icon),
            contentDescription = null,
            modifier = Modifier.size(size = 18.dp),
        )
        Text(
            text = animatedValue.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 3.dp),
        )
    }
}

@Composable
private fun AbilityChip(abilityName: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(resource = Res.drawable.ic_spc),
            contentDescription = null,
            modifier = Modifier.size(size = 18.dp),
        )
        AnimatedContent(targetState = abilityName, label = ABILITY_NAME_ANIM_LABEL) { name ->
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 3.dp),
            )
        }
    }
}

@Composable
private fun ClassStatDetails(
    characterClass: CharacterClassState,
    classCopy: CharacterClassCopy,
    modifier: Modifier = Modifier,
) {
    val health = classCopy[ClassCopyKeys.HEALTH]
    val armor = classCopy[ClassCopyKeys.ARMOR]
    val speed = classCopy[ClassCopyKeys.SPEED]
    val offensive = classCopy.forAbility(ability = characterClass.offensiveAbility)
    val defensive = classCopy.forAbility(ability = characterClass.defensiveAbility)

    Column(
        modifier = modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        HorizontalDivider()
        DetailItem(
            title = "${health.name} · ${characterClass.hitPoints}",
            description = health.short,
        )
        DetailItem(
            title = "${armor.name} · ${characterClass.armorClass}",
            description = armor.short,
        )
        DetailItem(title = "${speed.name} · ${characterClass.speed}", description = speed.short)
        DetailItem(title = offensive.name, description = offensive.short)
        DetailItem(title = defensive.name, description = defensive.short)
    }
}

@Composable
private fun DetailItem(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun rotationForIndex(index: Int) = -index * DEGREES_PER_CLASS

private fun centeredIndex(rotationDegrees: Float, count: Int): Int {
    val rawIndex = (-rotationDegrees / DEGREES_PER_CLASS).roundToInt()
    return ((rawIndex % count) + count) % count
}

// The rotation that brings [index] to the top, picking whichever full-turn representation
// is closest to [fromDegrees] so the wheel always animates the short way round.
private fun nearestRotationForIndex(index: Int, fromDegrees: Float): Float {
    val base = rotationForIndex(index = index)
    val turns = ((fromDegrees - base) / 360f).roundToInt()
    return base + turns * 360f
}

internal fun classArtwork(type: CharacterClassType): DrawableResource =
    when (type) {
        CharacterClassType.Rogue -> Res.drawable.class_rogue
        CharacterClassType.Gambler -> Res.drawable.class_gambler
        CharacterClassType.Duelist -> Res.drawable.class_duelist
        CharacterClassType.Necromancer -> Res.drawable.class_necromancer
        CharacterClassType.Bard -> Res.drawable.class_bard
        CharacterClassType.Illusionist -> Res.drawable.class_illusionist
        CharacterClassType.Mage -> Res.drawable.class_mage
        CharacterClassType.Paladin -> Res.drawable.class_paladin
        CharacterClassType.None -> Res.drawable.class_rogue
    }

private fun spokeColor(type: CharacterClassType): Color =
    when (type) {
        CharacterClassType.Rogue -> Color(color = 0xFF9D2920)
        CharacterClassType.Gambler -> Color(color = 0xFFDAA73B)
        CharacterClassType.Duelist -> Color(color = 0xFF852B1A)
        CharacterClassType.Necromancer -> Color(color = 0xFF49533B)
        CharacterClassType.Bard -> Color(color = 0xFFE8C53A)
        CharacterClassType.Illusionist -> Color(color = 0xFF1F3A5D)
        CharacterClassType.Mage -> Color(color = 0xFF243346)
        CharacterClassType.Paladin -> Color(color = 0xFF3C7A3C)
        CharacterClassType.None -> Color(color = 0xFF654E35)
    }

@Preview
@Composable
private fun CharacterClassWheelPreview() {
    SorcerersAndSpiritsTheme {
        CompositionLocalProvider(LocalCharacterClassCopy provides previewCharacterClassCopy()) {
            CharacterClassWheel(characterClasses = previewCharacterClassStates())
        }
    }
}
