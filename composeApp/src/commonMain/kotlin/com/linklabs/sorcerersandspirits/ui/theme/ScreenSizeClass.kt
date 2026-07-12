package com.linklabs.sorcerersandspirits.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val mediumMinWidth = 380.dp
private val largeMinWidth = 600.dp

private const val SMALL_TEXT_SCALE = 0.75f
private const val MEDIUM_TEXT_SCALE = 1f
private const val LARGE_TEXT_SCALE = 1.1f

/**
 * The window's coarse size bucket, tuned for this game's phone-first layouts. [Medium] is the
 * baseline reference size (a typical phone) and is left unscaled; [Small] and [Large] scale UI
 * chrome down or up around it. Combat avatars size proportionally to their container and are
 * intentionally not driven by this class.
 */
internal sealed interface ScreenSizeClass {
    data object Small : ScreenSizeClass

    data object Medium : ScreenSizeClass

    data object Large : ScreenSizeClass
}

/** Multiplier applied to text so copy shrinks on small phones and grows on tablets. */
internal fun ScreenSizeClass.textScale(): Float =
    when (this) {
        is ScreenSizeClass.Small -> SMALL_TEXT_SCALE
        is ScreenSizeClass.Medium -> MEDIUM_TEXT_SCALE
        is ScreenSizeClass.Large -> LARGE_TEXT_SCALE
    }

internal val LocalScreenSizeClass =
    staticCompositionLocalOf<ScreenSizeClass> { ScreenSizeClass.Medium }

/** Reads the live window width and maps it to a [ScreenSizeClass]. Works on Android and iOS. */
@Composable
internal fun rememberScreenSizeClass(): ScreenSizeClass {
    val windowSize = LocalWindowInfo.current.containerSize
    val widthDp = with(receiver = LocalDensity.current) { windowSize.width.toDp() }

    return remember(key1 = widthDp) { screenSizeClassFor(width = widthDp) }
}

private fun screenSizeClassFor(width: Dp): ScreenSizeClass =
    when {
        width >= largeMinWidth -> ScreenSizeClass.Large
        width >= mediumMinWidth -> ScreenSizeClass.Medium
        else -> ScreenSizeClass.Small
    }
