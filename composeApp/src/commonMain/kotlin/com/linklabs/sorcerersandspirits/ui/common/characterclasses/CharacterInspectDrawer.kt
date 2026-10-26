package com.linklabs.sorcerersandspirits.ui.common.characterclasses

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
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.AvatarImage
import org.jetbrains.compose.resources.painterResource

private val inspectDrawerColor = Color(color = 0xCC1A1A1A)
private val inspectScrimColor = Color(color = 0x99000000)

@Composable
internal fun InspectDrawer(
    characterClass: CharacterClassState?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isDetailed: Boolean = false,
) {
    val isOpen = characterClass != null
    var shownClass by remember { mutableStateOf<CharacterClassState?>(value = null) }
    if (characterClass != null) shownClass = characterClass

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(visible = isOpen, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier =
                Modifier.fillMaxSize().background(color = inspectScrimColor).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    onClose()
                },
            )
        }

        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(alignment = Alignment.CenterEnd),
        ) {
            val current = shownClass
            if (current != null) {
                InspectContent(characterClass = current, onClose = onClose, isDetailed = isDetailed)
            }
        }
    }
}

@Composable
internal fun InspectContent(
    characterClass: CharacterClassState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isDetailed: Boolean = false,
) {
    val classCopy = LocalCharacterClassCopy.current
    val stats =
        remember(characterClass, classCopy, isDetailed) {
            avatarStats(characterClass = characterClass, classCopy = classCopy, isDetailed = isDetailed)
        }

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
            text = characterClass.className,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        AvatarImage(
            classType = characterClass.type,
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
