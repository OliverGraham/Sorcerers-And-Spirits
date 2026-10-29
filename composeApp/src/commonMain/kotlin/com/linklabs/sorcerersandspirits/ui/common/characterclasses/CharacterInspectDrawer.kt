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
private val INSPECT_STAT_ICON_SIZE = 44.dp

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
    val classColor = characterClass.type.toLegibleOnDarkColor()

    Column(
        modifier =
        modifier
            .fillMaxHeight()
            .width(width = 300.dp)
            .background(color = inspectDrawerColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            )
            .verticalScroll(state = rememberScrollState())
            .padding(all = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
            Text(
                text = characterClass.className,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = classColor,
            )
            AvatarImage(
                classType = characterClass.type,
                modifier = Modifier.fillMaxWidth().height(height = 220.dp),
            )
        }
        stats.forEach { stat -> InspectStatRow(stat = stat, classColor = classColor) }
    }
}

@Composable
private fun InspectStatRow(stat: AvatarStat, classColor: Color, modifier: Modifier = Modifier) {
    val nameColor = if (stat.isAbility) classColor else Color.White
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(resource = stat.icon),
                contentDescription = stat.name,
                modifier = Modifier.size(size = INSPECT_STAT_ICON_SIZE),
            )
            val title = if (stat.value != null) "${stat.name} · ${stat.value}" else stat.name
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = nameColor,
            )
        }
        Text(
            text = stat.explanation,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
    }
}
