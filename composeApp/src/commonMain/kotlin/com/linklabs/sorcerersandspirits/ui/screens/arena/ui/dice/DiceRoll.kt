package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.linklabs.sorcerersandspirits.domain.haptics.HapticStrength
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.RollResult
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals.LocalHaptics
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals.LocalLatestRoll
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals.LocalShakeDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Before any roll lands the two dice rest on ones, as a normal attack would show them.
internal const val DIE_RESTING_FACE = 1
internal const val DIE_COUNT = 2
private val ALL_SLOTS = setOf(0, 1)

// How long the continuous buzz lingers after the last detected shake, bridging brief gaps in
// movement so the buzz stays smooth instead of stuttering.
private const val BUZZ_LINGER_MILLIS = 200L

// While a committed roll travels to the Domain and back, the dice keep tumbling: a fresh impulse on
// this cadence keeps them lively instead of coasting to a stop before the result lands (SS-117).
private const val ROLL_TUMBLE_IMPULSE_MILLIS = 220L

// A safety net so the dice can never tumble forever if a committed roll's result never arrives:
// stop after this many impulses (≈ the cadence × this count) and settle on whatever is shown.
private const val MAX_ROLL_TUMBLE_IMPULSES = 27

/**
 * The roll state machine shared by the dice tray and the combat buttons (SS-149). The dice are two
 * persistent slots, each holding the last face it showed, like two real dice resting on a table
 * (SS-166): [slotFaces] is what each die reads, and a roll only ever updates the slots it touches.
 * The gesture surface (the dice, or a combat button) drives the roll via [startHold]/[release],
 * naming which slots it rolls; the dice render from
 * [dieFaces]/[activeSlots]/[isRolling]/[shakeTick]. Build one with [rememberDiceRoll] so its
 * effects (shake collection, await-result tumble, haptics) are wired up and landed rolls are
 * committed to the slots.
 */
@Stable
internal class DiceRoll {
    var isHolding by mutableStateOf(value = false)
        private set

    var isArmed by mutableStateOf(value = false)
        private set

    var shakeTick by mutableStateOf(value = 0)
        private set

    var isRollPending by mutableStateOf(value = false)
        private set

    private var pendingFromRollId by mutableStateOf<String?>(value = null)

    // Updated every composition by [rememberDiceRoll] from the latest roll on show.
    internal var latestRollId by mutableStateOf<String?>(value = null)

    var slotFaces by mutableStateOf(value = List(size = DIE_COUNT) { DIE_RESTING_FACE })
        private set

    var restingSlots by mutableStateOf(value = ALL_SLOTS)
        private set

    private var rollSlots by mutableStateOf<Set<Int>?>(value = null)

    val dieFaces: List<Int>
        get() = slotFaces

    val activeSlots: Set<Int>
        get() = if (isRolling) rollSlots ?: restingSlots else restingSlots

    val isAwaitingResult: Boolean
        get() = isRollPending && latestRollId == pendingFromRollId

    val isRolling: Boolean
        get() = isHolding || isAwaitingResult

    /** A button or the dice tray began a hold for a roll of the given [slots]. */
    fun startHold(slots: Set<Int>?) {
        isArmed = false
        rollSlots = slots
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

    /**
     * Whether a roll landing after [previousRollId] is the result of the roll this device started,
     * told apart by the message that was latest when the roll was released.
     */
    internal fun isResultOfOwnRoll(previousRollId: String?): Boolean =
        isRollPending && pendingFromRollId == previousRollId

    /**
     * Writes a landed roll onto the dice. [isOwnRoll] is true when this is the result of the roll
     * this device started, so a Mirror Image reroll lands on the very die the player chose; for
     * everyone else the rerolled die is found by matching the face it replaced, so every device
     * shows the same two dice.
     */
    internal fun commitRoll(result: RollResult, isOwnRoll: Boolean) {
        val updatedFaces = slotFaces.toMutableList()
        restingSlots =
            when (result) {
                is RollResult.Faces -> {
                    result.slotFaces.forEach { (slot, face) ->
                        if (slot in updatedFaces.indices) updatedFaces[slot] = face
                    }
                    result.slotFaces.keys
                }

                is RollResult.Reroll -> {
                    val slot = rerollSlot(originalFace = result.originalFace, isOwnRoll = isOwnRoll)
                    updatedFaces[slot] = result.newFace
                    setOf(element = slot)
                }
            }
        slotFaces = updatedFaces
        if (isOwnRoll) isRollPending = false
    }

    private fun rerollSlot(originalFace: Int, isOwnRoll: Boolean): Int {
        val chosenSlot = rollSlots?.singleOrNull()
        if (isOwnRoll && chosenSlot != null && chosenSlot in slotFaces.indices) return chosenSlot
        val matchedSlot = slotFaces.indexOfFirst { it == originalFace }
        return if (matchedSlot >= 0) matchedSlot else 0
    }
}

/**
 * Remembers a [DiceRoll] and wires its effects: while held, each phone shake arms the roll and
 * kicks the dice with a haptic buzz; landed rolls (this player's or anyone's) are committed to the
 * dice; after a roll commits, the dice keep tumbling for the whole round trip to the Domain and
 * settle only once the result lands (SS-117).
 */
@Composable
internal fun rememberDiceRoll(): DiceRoll {
    val latestRoll = LocalLatestRoll.current
    val shakeDetector = LocalShakeDetector.current
    val haptics = LocalHaptics.current
    val roll = remember { DiceRoll() }

    roll.latestRollId = latestRoll.messageId

    var committedRollId by remember { mutableStateOf<String?>(value = null) }
    val result = latestRoll.result
    if (result != null && latestRoll.messageId != committedRollId) {
        val isOwnRoll = roll.isResultOfOwnRoll(previousRollId = committedRollId)
        committedRollId = latestRoll.messageId
        roll.commitRoll(result = result, isOwnRoll = isOwnRoll)
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
    // if the result somehow never arrives so they can't spin forever (SS-117).
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
