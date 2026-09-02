package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_info

private const val SELECTED_ALPHA = 1f
private const val UNSELECTED_ALPHA = 0.55f

private const val SELECTED_SCALE = 1.1f
private const val UNSELECTED_SCALE = 1f
private const val GLOW_LIGHTEN = 0.6f
private const val GLOW_MIN_ALPHA = 0.2f
private const val GLOW_MAX_ALPHA = 1f
private const val GLOW_PULSE_DURATION_MILLIS = 1000

private const val PILL_SCALE_ANIM_LABEL = "pill_scale"
private const val PILL_ALPHA_ANIM_LABEL = "pill_alpha"
private const val PILL_GLOW_TRANSITION_LABEL = "pill_glow"
private const val PILL_GLOW_ALPHA_LABEL = "pill_glow_alpha"

private const val AVATAR_GLOW_ALPHA = 0.75f
private const val AVATAR_GLOW_MIN_RADIUS_FRACTION = 0.33f
private const val AVATAR_GLOW_MAX_RADIUS_FRACTION = 0.43f
private const val AVATAR_GLOW_DURATION_MILLIS = 2000
private const val AVATAR_GLOW_TRANSITION_LABEL = "avatar_glow"
private const val AVATAR_GLOW_PROGRESS_LABEL = "avatar_glow_progress"

// TODO: This will replace the CharacterClassWheel eventually

private const val PILLS_PER_ROW = 2

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterClassPager(
    characterClasses: List<CharacterClassState>,
    modifier: Modifier = Modifier,
) {
    if (characterClasses.isEmpty()) return

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { characterClasses.size })
    var inspectedClass by remember { mutableStateOf<CharacterClassState?>(value = null) }

    Box(modifier = modifier.fillMaxSize().windowInsetsPadding(insets = WindowInsets.navigationBars)) {
        Column(modifier = Modifier.fillMaxSize()) {
            FlowRow(
                modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .groupBackground()
                    .padding(all = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(space = 20.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                maxItemsInEachRow = PILLS_PER_ROW,
            ) {
                characterClasses.forEachIndexed { index, characterClass ->
                    ClassPagerIndicator(
                        turnOrderNumber = index + 1,
                        characterClass = characterClass,
                        isSelected = pagerState.targetPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(page = index) } },
                        modifier = Modifier.weight(weight = 1f),
                    )
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(weight = 1f)) { page ->
                CharacterClassPage(
                    characterClass = characterClasses[page],
                    onInspect = { inspectedClass = characterClasses[page] },
                )
            }
        }

        InspectDrawer(
            characterClass = inspectedClass,
            onClose = { inspectedClass = null },
            isDetailed = true,
        )
    }
}

@Composable
private fun ClassPagerIndicator(
    turnOrderNumber: Int,
    characterClass: CharacterClassState,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradient = characterClass.type.toGradient()
    val scale by animateFloatAsState(
        targetValue = if (isSelected) SELECTED_SCALE else UNSELECTED_SCALE,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = PILL_SCALE_ANIM_LABEL,
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (isSelected) SELECTED_ALPHA else UNSELECTED_ALPHA,
        label = PILL_ALPHA_ANIM_LABEL,
    )

    Box(
        modifier =
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = contentAlpha
            }
            .selectionGlow(color = characterClass.type.toColor(), isVisible = isSelected)
            .clip(shape = CircleShape)
            .background(
                brush = Brush.verticalGradient(colors = listOf(gradient.topStop, gradient.bottomStop)),
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = turnOrderNumber.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(alignment = Alignment.CenterStart).width(width = 16.dp),
        )
        Text(
            text = characterClass.className,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

@Composable
private fun Modifier.selectionGlow(color: Color, isVisible: Boolean): Modifier {
    if (!isVisible) return this

    val transition = rememberInfiniteTransition(label = PILL_GLOW_TRANSITION_LABEL)
    val glowAlpha by transition.animateFloat(
        initialValue = GLOW_MIN_ALPHA,
        targetValue = GLOW_MAX_ALPHA,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = GLOW_PULSE_DURATION_MILLIS),
            repeatMode = RepeatMode.Reverse,
        ),
        label = PILL_GLOW_ALPHA_LABEL,
    )
    val glowColor = lerp(start = color, stop = Color.White, fraction = GLOW_LIGHTEN)

    return border(width = 2.dp, color = glowColor.copy(alpha = glowAlpha), shape = CircleShape)
}

private fun Modifier.groupBackground(): Modifier = background(
    color = Color.Black.copy(alpha = 0.35f),
    shape = RoundedCornerShape(size = 20.dp),
)


@Composable
private fun CharacterClassPage(
    characterClass: CharacterClassState,
    onInspect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
            Modifier.fillMaxWidth()
                .weight(weight = 1f)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .classColorGlow(gradient = characterClass.type.toGradient()),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(resource = characterClass.type.toAvatarResource()),
                contentDescription = characterClass.className,
                modifier = Modifier.fillMaxHeight(),
            )
        }

        ClassPageStats(
            characterClass = characterClass,
            onInspect = onInspect,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ClassPageStats(
    characterClass: CharacterClassState,
    onInspect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val classCopy = LocalCharacterClassCopy.current
    val stats = avatarStats(characterClass = characterClass, classCopy = classCopy)

    Box(modifier = modifier.padding(horizontal = 12.dp)) {
        Column(
            modifier =
            Modifier.fillMaxWidth()
                .clip(shape = RoundedCornerShape(size = 20.dp))
                .groupBackground()
                .clickable(onClick = onInspect)
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            stats.forEach { stat -> StatChip(stat = stat) }
        }

        Image(
            painter = painterResource(resource = Res.drawable.ic_info),
            contentDescription = null,
            modifier =
            Modifier.align(alignment = Alignment.TopEnd).padding(all = 12.dp).size(size = 24.dp),
        )
    }
}

@Composable
private fun StatChip(stat: AvatarStat, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(resource = stat.icon),
            contentDescription = stat.name,
            modifier = Modifier.size(size = 36.dp),
        )
        val value = stat.value
        val title = if (value != null) "${stat.name} · $value" else stat.name
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun Modifier.classColorGlow(gradient: ClassGradient): Modifier {
    val transition = rememberInfiniteTransition(label = AVATAR_GLOW_TRANSITION_LABEL)
    val radiusFraction by transition.animateFloat(
        initialValue = AVATAR_GLOW_MIN_RADIUS_FRACTION,
        targetValue = AVATAR_GLOW_MAX_RADIUS_FRACTION,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = AVATAR_GLOW_DURATION_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = AVATAR_GLOW_PROGRESS_LABEL,
    )

    val glowColor = gradient.topStop.copy(alpha = AVATAR_GLOW_ALPHA)

    return drawBehind {
        drawOval(
            brush =
                Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = Offset(x = size.width / 2f, y = size.height),
                    radius = size.width * radiusFraction,
                ),
        )
    }
}

@Preview
@Composable
private fun CharacterClassPagerPreview() {
    SorcerersAndSpiritsTheme {
        CompositionLocalProvider(LocalCharacterClassCopy provides previewCharacterClassCopy()) {
            CharacterClassPager(characterClasses = previewCharacterClassStates())
        }
    }
}
