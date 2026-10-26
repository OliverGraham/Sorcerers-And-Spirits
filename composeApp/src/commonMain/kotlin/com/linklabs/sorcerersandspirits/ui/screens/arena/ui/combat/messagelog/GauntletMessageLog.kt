package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    LaunchedEffect(key1 = gauntletLog) { listState.animateScrollToItem(index = 0) }

    LazyColumn(
        state = listState,
        modifier = modifier.nestedScroll(connection = dismissConnection),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(space = 2.dp),
    ) {
        gauntletLog.forEach { matchMessages ->
            item(key = "match-header-${matchMessages.matchNumber}") {
                MatchSectionHeader(
                    matchNumber = matchMessages.matchNumber,
                    modifier = Modifier.animateItem(),
                )
            }

            messageGroupItems(
                messageGroups = matchMessages.groups,
                isCombatOngoing = false,
                keyNamespace = "match-${matchMessages.matchNumber}",
                expandedGroups = expandedGroups,
            )
        }
    }
}

@Composable
private fun MatchSectionHeader(matchNumber: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(resource = Res.string.arena_log_match, matchNumber.toString()),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        letterSpacing = 2.sp,
        modifier = modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
    )
}
