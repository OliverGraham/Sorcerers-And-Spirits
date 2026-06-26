package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.haptics.HapticStrength
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals.LocalHaptics
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals.LocalLatestRoll
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals.LocalShakeDetector
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.holdToRollGesture
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Before any roll lands the tray rests on a pair of ones, matching a normal attack's two dice.
private const val DIE_RESTING_FACE = 1
private const val DEFAULT_DICE_COUNT = 2

// How long the continuous buzz lingers after the last detected shake, bridging brief gaps in
// movement so the buzz stays smooth instead of stuttering.
private const val BUZZ_LINGER_MILLIS = 200L

// While a committed roll travels to the Domain and back, the dice keep tumbling: a fresh impulse on
// this cadence keeps them lively instead of coasting to a stop before the result lands (SS-117).
private const val ROLL_TUMBLE_IMPULSE_MILLIS = 220L

// A safety net so the dice can never tumble forever if a committed roll's result never arrives:
// stop
// after this many impulses (≈ the cadence × this count) and settle on whatever is shown.
private const val MAX_ROLL_TUMBLE_IMPULSES = 27

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
    val latestRoll = LocalLatestRoll.current
    val shakeDetector = LocalShakeDetector.current
    val haptics = LocalHaptics.current

    var isHolding by remember { mutableStateOf(value = false) }
    var isArmed by remember { mutableStateOf(value = false) }
    // Set when a roll is committed, alongside the roll id showing then; the dice keep tumbling
    // until
    // a newer roll lands (a different id), which is the committed roll's result returning (SS-117).
    var isRollPending by remember { mutableStateOf(value = false) }
    var pendingFromRollId by remember { mutableStateOf<String?>(value = null) }
    // Ticks once per detected shake, and on a cadence while awaiting a result, so the dice get a
    // fresh impulse without restarting their physics loop.
    var shakeTick by remember { mutableStateOf(value = 0) }

    val isAwaitingResult = isRollPending && latestRoll.messageId == pendingFromRollId
    val isRolling = isHolding || isAwaitingResult

    // While the user's own roll travels to the Domain and back, the tray shows that roll's die
    // count
    // so it tumbles the right number of dice for the action being made rather than the previous
    // roll's shape; otherwise it mirrors the latest roll, whose own count is already correct for
    // the
    // result on show (SS-117).
    val displayedDiceCount =
        when {
            isAwaitingResult && expectedDiceCount != null -> expectedDiceCount
            latestRoll.dice.isNotEmpty() -> latestRoll.dice.size
            else -> expectedDiceCount ?: DEFAULT_DICE_COUNT
        }
    val dieFaces =
        remember(latestRoll.dice, displayedDiceCount) {
            // The latest roll's faces only belong on the tray when it has as many dice as is shown;
            // otherwise it is a leftover from a differently shaped roll, so rest the faces.
            val showsLatestRoll = latestRoll.dice.size == displayedDiceCount
            List(size = displayedDiceCount) { index ->
                if (showsLatestRoll) latestRoll.dice[index] else DIE_RESTING_FACE
            }
        }
    val isRollable = onRoll != null
    val currentOnRoll by rememberUpdatedState(newValue = onRoll)
    val currentLatestRoll by rememberUpdatedState(newValue = latestRoll)
    // The release gesture is captured by pointerInput and only re-created when the dice become
    // (un)rollable, so it reads the await flag through a state holder to avoid committing a second
    // roll over one still in flight.
    val currentIsAwaitingResult by rememberUpdatedState(newValue = isAwaitingResult)

    // While the dice are held, each phone shake kicks them around the tray, arms the roll so the
    // release commits it, and drives a smooth haptic buzz (SS-112).
    LaunchedEffect(key1 = isHolding) {
        if (!isHolding) return@LaunchedEffect
        var stopBuzzJob: Job? = null
        try {
            shakeDetector.shakes.collect {
                shakeTick++
                isArmed = true
                haptics.startContinuous(strength = HapticStrength.MEDIUM)
                stopBuzzJob?.cancel()
                stopBuzzJob = launch {
                    delay(timeMillis = BUZZ_LINGER_MILLIS)
                    haptics.stopContinuous()
                }
            }
        } finally {
            stopBuzzJob?.cancel()
            haptics.stopContinuous()
        }
    }

    // Keep the dice tumbling for the whole round trip a committed roll makes, then stop if the
    // result somehow never arrives so they can't spin forever (SS-117).
    LaunchedEffect(key1 = isAwaitingResult) {
        if (!isAwaitingResult) return@LaunchedEffect
        repeat(times = MAX_ROLL_TUMBLE_IMPULSES) {
            delay(timeMillis = ROLL_TUMBLE_IMPULSE_MILLIS)
            shakeTick++
        }
        isRollPending = false
    }

    val commitRoll = {
        haptics.vibrate(strength = HapticStrength.HEAVY)
        pendingFromRollId = currentLatestRoll.messageId
        isRollPending = true
        currentOnRoll?.invoke()
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .holdToRollGesture(
                    enabled = isRollable,
                    onHoldStart = {
                        isArmed = false
                        isHolding = true
                    },
                    onRelease = { isDoubleTap ->
                        isHolding = false
                        val shouldRoll = isArmed || isDoubleTap
                        if (shouldRoll && !currentIsAwaitingResult) commitRoll()
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
            dieFaces.forEach { face ->
                Die3d(value = face, isRolling = isRolling, enabled = true, shakeTick = shakeTick)
            }
        }

        val caption =
            diceCaption(
                isRollable = isRollable,
                needsTargetHint = needsTargetHint,
                isHolding = isHolding,
                isArmed = isArmed,
                isAwaitingResult = isAwaitingResult,
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
