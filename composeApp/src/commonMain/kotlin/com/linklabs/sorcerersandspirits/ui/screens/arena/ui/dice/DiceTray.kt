package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.holdToRollGesture

/**
 * The dice the whole arena watches (SS-112), and the only way to roll them (SS-112 follow-up):
 * there is no separate roll button. How many dice the tray shows is driven by the current combat
 * step: when the user has a roll to make, [expectedDiceCount] is that step's die count (one for a
 * 1d6 ability, two for a normal attack); otherwise the tray mirrors the most recent roll by ANY
 * player so the user watches it tumble to a new value when someone else rolls (SS-117).
 *
 * On the user's turn, when a roll is available, [onRoll] is non-null and the dice become the roll
 * control: grab and shake the phone to throw them around the tray, then release to roll, or
 * double-tap to roll. Either way the dice keep tumbling for the whole round trip the committed roll
 * makes to the Domain and back, settling only once the result lands (SS-117). When [onRoll] is null
 * the dice are display-only. [needsTargetHint] is true when a roll is waiting only on the user
 * picking a target.
 */
@Composable
internal fun DiceTray(
    onRoll: (() -> Unit)?,
    needsTargetHint: Boolean,
    expectedDiceCount: Int?,
    modifier: Modifier = Modifier,
) {
    val roll = rememberDiceRoll()
    val isRollable = onRoll != null

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .holdToRollGesture(
                    enabled = isRollable,
                    onHoldStart = { roll.startHold(diceCount = expectedDiceCount) },
                    onRelease = { isDoubleTap ->
                        roll.release(isDoubleTap = isDoubleTap) { onRoll?.invoke() }
                    },
                )
                .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Row(
            horizontalArrangement =
                Arrangement.spacedBy(space = 12.dp, alignment = Alignment.CenterHorizontally)
        ) {
            roll.dieFaces.forEach { face ->
                Die3d(
                    value = face,
                    isRolling = roll.isRolling,
                    enabled = true,
                    shakeTick = roll.shakeTick,
                )
            }
        }

        val caption =
            diceCaption(
                isRollable = isRollable,
                needsTargetHint = needsTargetHint,
                isHolding = roll.isHolding,
                isArmed = roll.isArmed,
                isAwaitingResult = roll.isAwaitingResult,
            )
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun diceCaption(
    isRollable: Boolean,
    needsTargetHint: Boolean,
    isHolding: Boolean,
    isArmed: Boolean,
    isAwaitingResult: Boolean,
): String? =
    when {
        isAwaitingResult -> "Rolling…"
        isHolding && isArmed -> "Release to roll"
        isHolding -> "Shake!"
        needsTargetHint -> "Tap an enemy to target"
        isRollable -> "Shake or double-tap the dice to roll"
        else -> null
    }
