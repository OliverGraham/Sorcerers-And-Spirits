package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.runtime.compositionLocalOf

/**
 * Supplies the die faces of the most recent roll so the shake-to-roll dice can settle on the value
 * the Domain actually rolled (SS-077). Defaults to empty for previews and tests;
 * [LoadedArenaScreen] provides the live roll derived from the message stream.
 */
internal val LocalLatestRollDice = compositionLocalOf<List<Int>> { emptyList() }
