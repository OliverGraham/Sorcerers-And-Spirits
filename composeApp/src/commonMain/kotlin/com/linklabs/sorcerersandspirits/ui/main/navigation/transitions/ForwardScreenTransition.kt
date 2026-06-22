package com.linklabs.sorcerersandspirits.ui.main.navigation.transitions

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

private const val FORWARD_DURATION_MILLIS = 360
private const val FADE_THROUGH_FADE_IN_DELAY_MILLIS = 90

private const val PORTAL_ENTER_SCALE = 0.9f
private const val PORTAL_EXIT_SCALE = 1.05f
private const val FADE_THROUGH_ENTER_SCALE = 0.92f
private const val SLIDE_FRACTION = 6

/**
 * The catalogue of forward-navigation transitions. A new one is chosen at random per navigation so
 * moving through the app feels fresh rather than uniform. Backward navigation uses the bespoke
 * disintegration effect instead and is intentionally not part of this set.
 */
enum class ForwardScreenTransition {
    Portal,
    FadeThrough,
    SlideAndFade,
}

fun ForwardScreenTransition.toContentTransform(): ContentTransform =
    when (this) {
        ForwardScreenTransition.Portal -> portalTransform()
        ForwardScreenTransition.FadeThrough -> fadeThroughTransform()
        ForwardScreenTransition.SlideAndFade -> slideAndFadeTransform()
    }

private fun portalTransform(): ContentTransform {
    val spec = tween<Float>(durationMillis = FORWARD_DURATION_MILLIS, easing = FastOutSlowInEasing)
    return scaleIn(animationSpec = spec, initialScale = PORTAL_ENTER_SCALE)
        .plus(fadeIn(animationSpec = spec)) togetherWith
        scaleOut(animationSpec = spec, targetScale = PORTAL_EXIT_SCALE)
            .plus(fadeOut(animationSpec = spec))
}

private fun fadeThroughTransform(): ContentTransform {
    val enterSpec =
        tween<Float>(
            durationMillis = FORWARD_DURATION_MILLIS,
            delayMillis = FADE_THROUGH_FADE_IN_DELAY_MILLIS,
            easing = FastOutSlowInEasing,
        )
    val exitSpec =
        tween<Float>(
            durationMillis = FADE_THROUGH_FADE_IN_DELAY_MILLIS,
            easing = FastOutSlowInEasing,
        )
    return fadeIn(animationSpec = enterSpec)
        .plus(
            scaleIn(animationSpec = enterSpec, initialScale = FADE_THROUGH_ENTER_SCALE)
        ) togetherWith fadeOut(animationSpec = exitSpec)
}

private fun slideAndFadeTransform(): ContentTransform {
    val spec = tween<Float>(durationMillis = FORWARD_DURATION_MILLIS, easing = FastOutSlowInEasing)
    val offsetSpec =
        tween<IntOffset>(durationMillis = FORWARD_DURATION_MILLIS, easing = FastOutSlowInEasing)
    return slideInHorizontally(
            animationSpec = offsetSpec,
            initialOffsetX = { width -> width / SLIDE_FRACTION },
        )
        .plus(fadeIn(animationSpec = spec)) togetherWith
        slideOutHorizontally(
                animationSpec = offsetSpec,
                targetOffsetX = { width -> -width / SLIDE_FRACTION },
            )
            .plus(fadeOut(animationSpec = spec))
}
