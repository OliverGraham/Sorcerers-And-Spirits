package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.linklabs.sorcerersandspirits.domain.haptics.HapticStrength
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals.LocalHaptics
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals.LocalLatestRoll
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals.LocalShakeDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Before any roll lands the dice rest on ones, matching a normal attack's two dice.
internal const val DIE_RESTING_FACE = 1
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
 * The roll state machine shared by the dice tray and the combat buttons (SS-149): who is holding,
 * whether the hold has been armed by a shake, the shake-tick that kicks the physics, and whether a
 * committed roll is still travelling to the Domain. The gesture surface (the dice, or a combat
 * button) drives it via [startHold]/[release]; the dice render from [dieFaces]/[isRolling]/
 * [shakeTick]. Build one with [rememberDiceRoll] so its effects (shake collection, await-result
 * tumble, haptics) are wired up.
 */
@Stable
internal class DiceRoll {
    var isHolding by mutableStateOf(value = false)
        private set

    var isArmed by mutableStateOf(value = false)
        private set

    var shakeTick by mutableStateOf(value = 0)
        private set

    var rollDiceCount by mutableStateOf<Int?>(value = null)
        private set

    var isRollPending by mutableStateOf(value = false)
        private set

    private var pendingFromRollId by mutableStateOf<String?>(value = null)

    // Updated every composition by [rememberDiceRoll] from the latest roll on show.
    internal var latestRollId by mutableStateOf<String?>(value = null)

    internal var dieFaces by mutableStateOf(value = emptyList<Int>())

    internal var displayedDiceCount by mutableStateOf(value = DEFAULT_DICE_COUNT)

    val isAwaitingResult: Boolean
        get() = isRollPending && latestRollId == pendingFromRollId

    val isRolling: Boolean
        get() = isHolding || isAwaitingResult

    /** A button or the dice tray began a hold for a roll of [diceCount] dice. */
    fun startHold(diceCount: Int?) {
        isArmed = false
        rollDiceCount = diceCount
        isHolding = true
    }

    /**
     * The hold was released. The roll commits — calling [commit] and tumbling until the result
     * lands — only if a shake armed it or it was a double-tap, and not while a prior roll is still
     * in flight.
     */
    fun release(isDoubleTap: Boolean, commit: () -> Unit) {
        isHolding = false
        val shouldRoll = isArmed || isDoubleTap
        if (shouldRoll && !isAwaitingResult) {
            pendingFromRollId = latestRollId
            isRollPending = true
            commit()
        }
    }

    internal fun registerShake() {
        isArmed = true
        shakeTick++
    }

    internal fun tick() {
        shakeTick++
    }

    internal fun finishAwaiting() {
        isRollPending = false
    }
}

/**
 * Remembers a [DiceRoll] and wires its effects: while held, each phone shake arms the roll and
 * kicks the dice with a haptic buzz; after a roll commits, the dice keep tumbling for the whole
 * round trip to the Domain and settle only once the result lands (SS-117).
 */
@Composable
internal fun rememberDiceRoll(): DiceRoll {
    val latestRoll = LocalLatestRoll.current
    val shakeDetector = LocalShakeDetector.current
    val haptics = LocalHaptics.current
    val roll = remember { DiceRoll() }

    roll.latestRollId = latestRoll.messageId

    val displayedDiceCount =
        when {
            roll.isRolling && roll.rollDiceCount != null -> roll.rollDiceCount!!
            latestRoll.dice.isNotEmpty() -> latestRoll.dice.size
            else -> DEFAULT_DICE_COUNT
        }
    roll.displayedDiceCount = displayedDiceCount
    roll.dieFaces =
        remember(latestRoll.dice, displayedDiceCount) {
            // The latest roll's faces only belong on the dice when it has as many as is shown;
            // otherwise it is a leftover from a differently shaped roll, so rest the faces.
            val showsLatestRoll = latestRoll.dice.size == displayedDiceCount
            List(size = displayedDiceCount) { index ->
                if (showsLatestRoll) latestRoll.dice[index] else DIE_RESTING_FACE
            }
        }

    // While the dice are held, each phone shake kicks them around, arms the roll so the release
    // commits it, and drives a smooth haptic buzz (SS-112).
    LaunchedEffect(key1 = roll.isHolding) {
        if (!roll.isHolding) return@LaunchedEffect
        var stopBuzzJob: Job? = null
        try {
            shakeDetector.shakes.collect {
                roll.registerShake()
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

    // A committed roll thumps once, then the dice keep tumbling for the whole round trip, stopping
    // if
    // the result somehow never arrives so they can't spin forever (SS-117).
    LaunchedEffect(key1 = roll.isAwaitingResult) {
        if (!roll.isAwaitingResult) return@LaunchedEffect
        haptics.vibrate(strength = HapticStrength.HEAVY)
        repeat(times = MAX_ROLL_TUMBLE_IMPULSES) {
            delay(timeMillis = ROLL_TUMBLE_IMPULSE_MILLIS)
            roll.tick()
        }
        roll.finishAwaiting()
    }

    return roll
}
