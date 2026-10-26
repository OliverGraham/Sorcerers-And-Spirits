package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.finished

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_background

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun VictoryDanceMagePreview() {
    VictoryDanceSeatPreview(classType = CharacterClassType.Mage)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun VictoryDanceRoguePreview() {
    VictoryDanceSeatPreview(classType = CharacterClassType.Rogue)
}

@Composable
private fun VictoryDanceSeatPreview(classType: CharacterClassType) {
    SorcerersAndSpiritsTheme {
        Box(
            modifier =
            Modifier.fillMaxSize()
                .paint(
                    painter = painterResource(resource = Res.drawable.arena_background),
                    contentScale = ContentScale.Crop,
                ),
            contentAlignment = Alignment.Center,
        ) {
            VictoryDanceAvatar(
                classType = classType,
                modifier = Modifier.size(width = 140.dp, height = 190.dp),
            )
        }
    }
}
