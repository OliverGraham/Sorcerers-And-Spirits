package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.runtime.compositionLocalOf
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.LatestRoll

/**
 * Supplies the most recent roll so the shake-to-roll dice can settle on the value the Domain
 * actually rolled (SS-077) and keep tumbling until a fresh result lands (SS-117). Defaults to an
 * empty roll for previews and tests; [LoadedArenaScreen] provides the live roll derived from the
 * message stream.
 */
internal val LocalLatestRoll = compositionLocalOf { LatestRoll() }
