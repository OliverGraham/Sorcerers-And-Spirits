package com.linklabs.sorcerersandspirits.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.linklabs.sorcerersandspirits.ui.common.loading.SwingingLogoLoader

@Composable
internal fun LoadingIndicator(modifier: Modifier = Modifier) {
    SwingingLogoLoader(modifier = modifier)
}
