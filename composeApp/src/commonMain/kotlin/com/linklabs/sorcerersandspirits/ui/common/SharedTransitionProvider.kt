package com.linklabs.sorcerersandspirits.ui.common

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

private const val SHARED_TRANSITION_SCOPE_NOT_PROVIDED = "SharedTransitionScopes not provided"

@OptIn(ExperimentalSharedTransitionApi::class)
private data class SharedTransitionScopes(
    val sharedTransitionScope: SharedTransitionScope,
    val animatedVisibilityScope: AnimatedVisibilityScope,
)

@OptIn(ExperimentalSharedTransitionApi::class)
private val LocalSharedTransitionScopes =
    staticCompositionLocalOf<SharedTransitionScopes> {
        error(message = SHARED_TRANSITION_SCOPE_NOT_PROVIDED)
    }

private object SharedTransitionScopesProvider {
    val current: SharedTransitionScopes
        @Composable @ReadOnlyComposable
        get() = LocalSharedTransitionScopes.current
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionProvider(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSharedTransitionScopes provides
            SharedTransitionScopes(
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            ),
    ) {
        content()
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedBounds(key: Any): Modifier {
    val scopes = SharedTransitionScopesProvider.current

    return with(scopes.sharedTransitionScope) {
        this@sharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = scopes.animatedVisibilityScope,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedElement(key: Any): Modifier {
    val scopes = SharedTransitionScopesProvider.current

    return with(scopes.sharedTransitionScope) {
        this@sharedElement.sharedElement(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = scopes.animatedVisibilityScope,
        )
    }
}
