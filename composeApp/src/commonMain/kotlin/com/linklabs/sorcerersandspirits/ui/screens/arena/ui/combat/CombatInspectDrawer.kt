package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.LocalCharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import org.jetbrains.compose.resources.painterResource

private val inspectDrawerColor = Color(color = 0xF21A1A1A)
private val inspectScrimColor = Color(color = 0x99000000)

/**
 * A right-side drawer that slides in to inspect a class (SS-150): the full avatar plus every stat
 * with its explanation, opened by a non-targeting tap on a head icon. A scrim dims the combat
 * behind it; tapping the scrim, tapping the drawer itself, or a normal targeting tap next turn
 * dismisses it.
 */
@Composable
internal fun InspectDrawer(
    player: PlayerState.Selected?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOpen = player != null
    var shownPlayer by remember { mutableStateOf<PlayerState.Selected?>(value = null) }
    if (player != null) shownPlayer = player

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(visible = isOpen, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier =
                    Modifier.fillMaxSize().background(color = inspectScrimColor).clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        onClose()
                    }
            )
        }

        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(alignment = Alignment.CenterEnd),
        ) {
            val current = shownPlayer
            if (current != null) InspectContent(player = current, onClose = onClose)
        }
    }
}

@Composable
internal fun InspectContent(
    player: PlayerState.Selected,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val classCopy = LocalCharacterClassCopy.current
    val stats = remember(player, classCopy) { avatarStats(player.characterClass, classCopy) }

    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .width(width = 280.dp)
                .background(color = inspectDrawerColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose,
                )
                .verticalScroll(state = rememberScrollState())
                .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        Text(
            text = player.characterClass.className,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        ClassAvatar(
            characterClass = player.characterClass,
            modifier = Modifier.fillMaxWidth().height(height = 200.dp),
        )
        stats.forEach { stat -> InspectStatRow(stat = stat) }
    }
}

@Composable
private fun InspectStatRow(stat: AvatarStat, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(resource = stat.icon),
            contentDescription = stat.name,
            modifier = Modifier.size(size = STAT_ICON_SIZE),
        )
        Column {
            val title = if (stat.value != null) "${stat.name} · ${stat.value}" else stat.name
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = stat.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
            )
        }
    }
}
