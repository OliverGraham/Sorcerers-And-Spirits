package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.fadedBlackBackground
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_info

private const val STAT_CHIP_VALUE_ANIMATION_LABEL = "StatChipValueAnimationLabel"

@Composable
internal fun ClassPageStats(
    characterClass: CharacterClassState,
    onInspect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val classCopy = LocalCharacterClassCopy.current
    val stats = avatarStats(characterClass = characterClass, classCopy = classCopy)

    Box(modifier = modifier.padding(horizontal = 12.dp)) {
        FlowRow(
            modifier =
            Modifier.fillMaxWidth()
                .clip(shape = RoundedCornerShape(size = 20.dp))
                .fadedBlackBackground()
                .clickable(onClick = onInspect)
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            maxItemsInEachRow = 3,
        ) {
            stats.forEach { stat -> StatChip(stat = stat) }
        }

        Image(
            painter = painterResource(resource = Res.drawable.ic_info),
            contentDescription = null,
            modifier =
            Modifier.align(alignment = Alignment.BottomEnd).padding(all = 12.dp).size(size = 24.dp),
        )
    }
}

@Composable
private fun StatChip(stat: AvatarStat, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(resource = stat.icon),
            contentDescription = stat.name,
            modifier = Modifier.size(size = 36.dp),
        )
        val label = stat.value ?: stat.name
        AnimatedContent(targetState = label, label = STAT_CHIP_VALUE_ANIMATION_LABEL) { animatedLabel ->
            Text(
                text = animatedLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}
