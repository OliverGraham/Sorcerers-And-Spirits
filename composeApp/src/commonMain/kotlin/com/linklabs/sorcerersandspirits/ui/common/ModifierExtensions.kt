package com.linklabs.sorcerersandspirits.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.fadedBlackBackground(alpha: Float = 0.35f): Modifier = background(
    color = Color.Black.copy(alpha = alpha),
    shape = RoundedCornerShape(size = 20.dp),
)
