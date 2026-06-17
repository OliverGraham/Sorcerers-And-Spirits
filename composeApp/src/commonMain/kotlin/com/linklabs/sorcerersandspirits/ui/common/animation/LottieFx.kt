package com.linklabs.sorcerersandspirits.ui.common.animation

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi
import sorcerersandspirits.composeapp.generated.resources.Res

/**
 * Plays a Lottie animation rendered by Compottie. Works on Android and iOS with no platform code.
 *
 * [assetPath] is a path inside `composeResources` (e.g. `files/combat_fx/slash.json`). If the asset
 * is missing, Compottie keeps the composition `null` and nothing is drawn, so a not-yet-authored
 * effect degrades gracefully rather than crashing.
 *
 * @param iterations how many times to play; [Compottie.IterateForever] loops. [onFinished] fires
 *   once a finite animation completes (not called for looping animations).
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun LottieFx(
    assetPath: String,
    modifier: Modifier = Modifier,
    iterations: Int = 1,
    contentScale: ContentScale = ContentScale.Fit,
    onFinished: () -> Unit = {},
) {
    val composition by
        rememberLottieComposition(assetPath) {
            LottieCompositionSpec.JsonString(Res.readBytes(assetPath).decodeToString())
        }

    val progress by
        animateLottieCompositionAsState(composition = composition, iterations = iterations)

    LaunchedEffect(progress, iterations) {
        if (iterations != Compottie.IterateForever && progress == 1f) onFinished()
    }

    Image(
        painter = rememberLottiePainter(composition = composition, progress = { progress }),
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier,
    )
}
