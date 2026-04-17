package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

//  TODO State is Choosing:
//      Pager of cards when choosing
//      Timer of 20 seconds
//      When choice is made, gets selected
//      When all selections made, go to next state,
//          transition of cards into Grid Arrangement (that are simplified and smaller but can be
// expanded)
//          Arrangement moves in queue-like fashion, with current player's Card at top
//          then next up, etc.
//          Current player has fancy border and is large version and top-center
//          This arrangement is different depending on # of players
//              2 players: current on Top, other on Bottom (both large cards)
//              3+ players: Current on Top, Grid (smaller cards)

@Composable
internal fun ArenaChoosingClasses(modifier: Modifier = Modifier) {
    Text(text = "ArenaChoosingClasses")
}

// TODO Arena: Preview
