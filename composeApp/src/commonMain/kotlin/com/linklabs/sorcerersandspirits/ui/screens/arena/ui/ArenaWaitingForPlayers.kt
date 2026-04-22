package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.linklabs.sorcerersandspirits.ui.common.LoadingIndicator

// TODO Arena: Loading for now, add some UI that gives people waiting something to do

@Composable
internal fun ArenaWaitingForPlayers(modifier: Modifier = Modifier) {
    LoadingIndicator(modifier = modifier)
}

// TODO Arena: Previews
