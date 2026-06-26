package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Press-and-hold gesture shared by the dice tray and the combat buttons: the user holds to roll and
 * releases to commit. Consumes the touch so any surrounding scroll can't steal it: [onHoldStart]
 * fires on press, and [onRelease] fires on lift with whether the press was the second tap of a
 * double-tap. Inert unless [enabled].
 */
internal fun Modifier.holdToRollGesture(
    enabled: Boolean,
    onHoldStart: () -> Unit,
    onRelease: (isDoubleTap: Boolean) -> Unit,
): Modifier =
    pointerInput(enabled) {
        if (!enabled) return@pointerInput
        val doubleTapTimeoutMillis = viewConfiguration.doubleTapTimeoutMillis
        var previousUpTimeMillis = 0L

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            val isSecondTapOfDoubleTap =
                down.uptimeMillis - previousUpTimeMillis < doubleTapTimeoutMillis
            onHoldStart()

            val up = awaitPointerUp(pointerId = down.id)

            previousUpTimeMillis = up?.uptimeMillis ?: 0L
            onRelease(isSecondTapOfDoubleTap)
        }
    }

/**
 * Suspends until the pointer with [pointerId] lifts, consuming its events so the gesture keeps
 * ownership of the touch for the whole hold. Returns the lifting change, or null if the pointer
 * disappeared without a cleanup.
 */
private suspend fun AwaitPointerEventScope.awaitPointerUp(
    pointerId: PointerId
): PointerInputChange? {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == pointerId } ?: return null
        change.consume()
        if (!change.pressed) return change
    }
}
