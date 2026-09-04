package com.linklabs.sorcerersandspirits.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.fadedBlackBackground(): Modifier = background(
    color = Color.Black.copy(alpha = 0.35f),
    shape = RoundedCornerShape(size = 20.dp),
)
