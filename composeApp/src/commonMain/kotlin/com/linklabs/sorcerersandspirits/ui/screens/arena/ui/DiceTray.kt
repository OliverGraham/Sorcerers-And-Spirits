package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Before any roll lands the tray rests on a pair of ones, matching a normal attack's two dice.
private const val DIE_RESTING_FACE = 1
private const val DEFAULT_DICE_COUNT = 2

/**
 * The dice the whole arena watches (SS-112). Unlike the per-turn roll controls, this tray stays on
 * screen for the entire match: it shows the faces of the most recent roll by ANY player and re-runs
 * the tumble whenever that roll changes, so the user keeps seeing the dice they rolled and watches
 * them tumble to a new value when someone else rolls.
 *
 * The tray is display-only on its own; [isRolling] is driven by the user holding a roll control on
 * their turn, which spins these dice in place until they release and the new value settles.
 */
@Composable
internal fun DiceTray(isRolling: Boolean, modifier: Modifier = Modifier) {
    val latestRollDice = LocalLatestRollDice.current
    val diceCount = if (latestRollDice.isEmpty()) DEFAULT_DICE_COUNT else latestRollDice.size
    val dieFaces =
        remember(latestRollDice) {
            List(size = diceCount) { index ->
                latestRollDice.getOrElse(index = index) { DIE_RESTING_FACE }
            }
        }

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement =
            Arrangement.spacedBy(space = 12.dp, alignment = Alignment.CenterHorizontally),
    ) {
        dieFaces.forEach { face -> Die3d(value = face, isRolling = isRolling, enabled = true) }
    }
}
