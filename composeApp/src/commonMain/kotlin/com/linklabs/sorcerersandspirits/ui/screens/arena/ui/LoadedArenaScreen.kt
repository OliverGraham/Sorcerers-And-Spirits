package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPhase
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_martini
import sorcerersandspirits.composeapp.generated.resources.ic_up_arrow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoadedArenaScreen(arenaState: ArenaState, modifier: Modifier = Modifier) {
    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        modifier = modifier.fillMaxSize(),
        sheetPeekHeight = 48.dp,
        sheetDragHandle = {
            SheetDragHandle(
                isExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
            )
        },
        sheetContent = {
            // TODO Arena: MessageLog
        },
        topBar = { TimerBar(secondsRemaining = arenaState.secondsRemaining) },
    ) { paddingValues ->
        ScreenContent(arenaState = arenaState, paddingValues = paddingValues)
    }

    // TODO Arena: Dialogs go here
    // TODO Arena: Dialog showing that going back will forfeit the game
}

// TODO Arena: Make generic or just fix this one
@Composable
private fun SheetDragHandle(isExpanded: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(all = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        AnimatedContent(
            targetState = if (isExpanded) Res.drawable.ic_martini else Res.drawable.ic_up_arrow,
            label = "DragHandleIcon",
        ) { targetIcon ->
            Icon(
                painter = painterResource(resource = targetIcon),
                contentDescription = null,
                modifier = Modifier.size(size = 36.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerBar(secondsRemaining: Int, modifier: Modifier = Modifier) {
    CenterAlignedTopAppBar(title = {}, actions = {})
}

@Composable
private fun ScreenContent(
    arenaState: ArenaState,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(paddingValues = paddingValues)) {
        Crossfade(targetState = arenaState.phase) { phase ->
            when (phase) {
                ArenaPhase.ChoosingClasses -> ArenaChoosingClasses()
                ArenaPhase.Combat -> ArenaCombat()
                ArenaPhase.WaitingForAllPlayers -> ArenaWaitingForPlayers()
                ArenaPhase.Finished -> {
                    // TODO Arena: figure out
                }
            }
        }
    }
}

// TODO Arena: Previews
