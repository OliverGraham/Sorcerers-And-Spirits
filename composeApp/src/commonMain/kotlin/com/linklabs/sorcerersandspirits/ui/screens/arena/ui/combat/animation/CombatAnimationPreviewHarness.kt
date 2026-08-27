package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActions
import com.linklabs.sorcerersandspirits.ui.screens.arena.LocalArenaActions
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.AvatarBoundsStateHolder
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_background

internal const val PREVIEW_WIDTH_DP = 360
internal const val PREVIEW_HEIGHT_DP = 720
internal const val PREVIEW_ACTOR_ID = "actor"
internal const val PREVIEW_TARGET_ID = "target"

private const val AVATAR_MARKER_ALPHA = 0.24f

@Stable
internal class PreviewAvatars(private val boundsByUserId: Map<String, Rect>) {
    val userIds: List<String> = boundsByUserId.keys.toList()

    val boundsStateHolder: AvatarBoundsStateHolder =
        AvatarBoundsStateHolder().apply {
            boundsByUserId.forEach { (userId, bounds) ->
                reportAvatarBounds(userId = userId, boundsInRoot = bounds)
            }
        }

    fun boundsFor(userId: String): Rect = boundsByUserId.getValue(userId)
}

@Composable
internal fun rememberSoloAvatar(): PreviewAvatars {
    val density = LocalDensity.current
    return remember(key1 = density) {
        PreviewAvatars(
            boundsByUserId =
                mapOf(PREVIEW_ACTOR_ID to avatarBounds(density = density, centerYFraction = 0.5f))
        )
    }
}

@Composable
internal fun rememberDuoAvatars(): PreviewAvatars {
    val density = LocalDensity.current
    return remember(key1 = density) {
        PreviewAvatars(
            boundsByUserId =
                mapOf(
                    PREVIEW_ACTOR_ID to avatarBounds(density = density, centerYFraction = 0.66f),
                    PREVIEW_TARGET_ID to avatarBounds(density = density, centerYFraction = 0.2f),
                )
        )
    }
}

private fun avatarBounds(density: Density, centerYFraction: Float): Rect =
    with(density) {
        val width = 110.dp.toPx()
        val height = 150.dp.toPx()
        val centerX = PREVIEW_WIDTH_DP.dp.toPx() / 2f
        val centerY = PREVIEW_HEIGHT_DP.dp.toPx() * centerYFraction
        Rect(
            offset = Offset(x = centerX - width / 2f, y = centerY - height / 2f),
            size = Size(width = width, height = height),
        )
    }

@Composable
internal fun <MessageState : ArenaMessageState> AnimationPreview(
    accentClass: CharacterClassType,
    avatars: PreviewAvatars,
    buildMessage: (id: String) -> MessageState,
    content: @Composable (animatingMessage: MessageState) -> Unit,
) {
    var replay by remember { mutableIntStateOf(0) }
    val message = remember(key1 = replay) { buildMessage("preview-$replay") }
    val actions = remember { ArenaActions(onAnimationComplete = { replay++ }) }
    val markerColor = accentClass.toColor()

    CompositionLocalProvider(LocalArenaActions provides actions) {
        SorcerersAndSpiritsTheme {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .paint(
                            painter = painterResource(resource = Res.drawable.arena_background),
                            contentScale = ContentScale.Crop,
                        )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    avatars.userIds.forEach { userId ->
                        val bounds = avatars.boundsFor(userId)
                        drawRoundRect(
                            color = markerColor,
                            topLeft = bounds.topLeft,
                            size = bounds.size,
                            cornerRadius = CornerRadius(x = bounds.width * 0.2f),
                            alpha = AVATAR_MARKER_ALPHA,
                        )
                    }
                }
                content(message)
            }
        }
    }
}
