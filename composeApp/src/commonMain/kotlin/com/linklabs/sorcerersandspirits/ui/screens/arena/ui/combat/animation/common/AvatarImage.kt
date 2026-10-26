package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toAvatarResource
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun AvatarImage(
    classType: CharacterClassType,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    colorFilter: ColorFilter? = null,
) {
    Image(
        painter = painterResource(resource = classType.toAvatarResource()),
        contentDescription = classType.name,
        contentScale = ContentScale.Fit,
        alignment = alignment,
        colorFilter = colorFilter,
        modifier = modifier,
    )
}
