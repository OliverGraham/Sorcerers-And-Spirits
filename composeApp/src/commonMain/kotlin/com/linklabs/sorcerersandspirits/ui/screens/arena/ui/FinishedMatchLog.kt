package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageGroupState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog.ArenaMessageLog
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog.Caret
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog.messagesBackgroundBrush
import org.jetbrains.compose.resources.stringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_finished_match_log

private const val CARET_ROTATION_LABEL = "FinishedMatchLogCaretRotation"
private const val CARET_EXPANDED_ROTATION = 0f
private const val CARET_COLLAPSED_ROTATION = -90f

private val expandedLogHeight = 260.dp

@Composable
internal fun FinishedMatchLog(
    messageGroups: List<ArenaMessageGroupState>,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(value = false) }
    var backgroundHeight by remember { mutableStateOf(value = 0f) }

    val caretRotation by
        animateFloatAsState(
            targetValue = if (isExpanded) CARET_EXPANDED_ROTATION else CARET_COLLAPSED_ROTATION,
            label = CARET_ROTATION_LABEL,
        )

    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 12.dp))
            .onSizeChanged { backgroundHeight = it.height.toFloat() }
            .background(brush = messagesBackgroundBrush(startY = 0f, endY = backgroundHeight)),
    ) {
        Row(
            modifier =
            Modifier.fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            Caret(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.rotate(degrees = caretRotation),
            )
            Text(
                text = stringResource(resource = Res.string.arena_finished_match_log),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp,
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            ArenaMessageLog(
                messageGroups = messageGroups,
                dismissConnection = remember { object : NestedScrollConnection {} },
                modifier = Modifier.fillMaxWidth().height(height = expandedLogHeight),
            )
        }
    }
}
