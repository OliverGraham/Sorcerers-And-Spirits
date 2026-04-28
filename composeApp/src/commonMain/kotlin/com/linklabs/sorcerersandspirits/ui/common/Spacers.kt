package com.linklabs.sorcerersandspirits.ui.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun RowScope.WeightSpacer(weight: Float = 1f, modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.weight(weight = 1f))
}
