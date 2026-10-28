package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.GauntletMatchMessages
import org.jetbrains.compose.resources.stringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_log_match

@Composable
internal fun GauntletMessageLog(
    gauntletLog: List<GauntletMatchMessages>,
    listState: LazyListState,
    dismissConnection: NestedScrollConnection,
    modifier: Modifier = Modifier,
) {
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }
    val expandedMatches = remember { mutableStateMapOf<Int, Boolean>() }

    LaunchedEffect(key1 = gauntletLog) { listState.animateScrollToItem(index = 0) }

    LazyColumn(
        state = listState,
        modifier = modifier.nestedScroll(connection = dismissConnection),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(space = 2.dp),
    ) {
        gauntletLog.forEach { matchMessages ->
            val isMatchExpanded = expandedMatches[matchMessages.matchNumber] ?: false

            item(key = "match-header-${matchMessages.matchNumber}") {
                MatchSectionHeader(
                    matchNumber = matchMessages.matchNumber,
                    isExpanded = isMatchExpanded,
                    onToggle = {
                        expandedMatches[matchMessages.matchNumber] = !isMatchExpanded
                    },
                    modifier = Modifier.animateItem(),
                )
            }

            if (!isMatchExpanded) return@forEach

            messageGroupItems(
                messageGroups = matchMessages.groups,
                isCombatOngoing = false,
                keyNamespace = "match-${matchMessages.matchNumber}",
                expandedGroups = expandedGroups,
                areGroupsCollapsedByDefault = true,
            )
        }
    }
}

@Composable
private fun MatchSectionHeader(
    matchNumber: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CollapsibleHeader(
        text = stringResource(resource = Res.string.arena_log_match, matchNumber.toString()),
        color = MaterialTheme.colorScheme.secondary,
        textStyle = MaterialTheme.typography.headlineMedium,
        isExpanded = isExpanded,
        onToggle = onToggle,
        contentPadding = PaddingValues(top = 16.dp, bottom = 4.dp),
        modifier = modifier,
        horizontalSpacing = 8.dp,
    )
}
