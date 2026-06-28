package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toAvatarResource
import com.linklabs.sorcerersandspirits.ui.common.sharedBounds
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun AvatarsRow(
    activePlayer: PlayerState.Selected,
    targetPlayer: PlayerState.Selected?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        ClassAvatar(
            characterClass = activePlayer.characterClass,
            modifier = Modifier.weight(weight = 1f).sharedBounds(key = activePlayer.userId),
        )

        if (targetPlayer != null) {
            ClassAvatar(
                characterClass = targetPlayer.characterClass,
                modifier = Modifier.weight(weight = 1f).sharedBounds(key = targetPlayer.userId),
            )
        }
    }
}

@Composable
internal fun ClassAvatar(characterClass: CharacterClassState, modifier: Modifier = Modifier) {
    // TODO(SS-068): combat animations will drive this static art.
    Image(
        painter = painterResource(resource = characterClass.type.toAvatarResource()),
        contentDescription = characterClass.className,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}
