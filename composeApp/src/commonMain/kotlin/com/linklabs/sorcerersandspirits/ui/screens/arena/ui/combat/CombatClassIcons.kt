package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.ClassHeadIcon
import com.linklabs.sorcerersandspirits.ui.common.sharedBounds
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState

@Composable
internal fun ClassIcons(
    headIconPlayers: List<PlayerState.Selected>,
    isTargetable: Boolean,
    onSelectTarget: (String) -> Unit,
    onInspect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        headIconPlayers.forEach { player ->
            ClassIcon(
                player = player,
                isTargetable = isTargetable,
                onSelectTarget = onSelectTarget,
                onInspect = onInspect,
            )
        }
    }
}

@Composable
private fun ClassIcon(
    player: PlayerState.Selected,
    isTargetable: Boolean,
    onSelectTarget: (String) -> Unit,
    onInspect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConscious = player.characterClass.isConscious
    val isTargetingTap = isTargetable && isConscious

    ClassHeadIcon(
        characterClass = player.characterClass,
        isEnabled = isConscious,
        modifier =
            modifier.sharedBounds(key = player.userId).clickable {
                if (isTargetingTap) onSelectTarget(player.userId) else onInspect(player.userId)
            },
    )
}
