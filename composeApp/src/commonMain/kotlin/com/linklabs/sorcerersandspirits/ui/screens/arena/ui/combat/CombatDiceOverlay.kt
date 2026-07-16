package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiceStateHolder
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.Die3d
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DieState

private const val DICE_HOME_DIE_COUNT = 2
internal val diceSpacing = 152.dp

/** The x-center a die rests at for its [slot], shared so overlays can anchor to a specific die. */
internal fun dieRestCenterX(homeCenterX: Float, slot: Int, spacingPx: Float): Float =
    homeCenterX + (slot - (DICE_HOME_DIE_COUNT - 1) / 2f) * spacingPx

/**
 * The empty resting slot below the avatars that reserves space for the dice and reports its bounds.
 * The dice themselves are drawn in the screen-spanning [DiceOverlay] so they can roll over all the
 * content; at rest they sit centered in this slot (SS-149).
 */
@Composable
internal fun dieRestCenterX(
    homeCenterX: Float,
    isFirstDie: Boolean,
    spacingPx: Float = with(receiver = LocalDensity.current) { diceSpacing.toPx() },
): Float {
    val diePositionNumber = if (isFirstDie) -0 else 1

    return homeCenterX + (diePositionNumber / 2f) * spacingPx
}

@Composable
internal fun DiceOverlay(
    diceStateHolder: DiceStateHolder,
    homeBounds: Rect,
    modifier: Modifier = Modifier,
) {
    var overlayOrigin by remember { mutableStateOf(value = Offset.Zero) }

    BoxWithConstraints(
        modifier =
            modifier.fillMaxSize().onGloballyPositioned {
                overlayOrigin = it.localToRoot(relativeToLocal = Offset.Zero)
            }
    ) {
        val homeCenter = homeBounds.center - overlayOrigin
        val shakeTick = diceStateHolder.shakeTick
        val isRolling = diceStateHolder.isRolling

        SingleDie(
            dieState = diceStateHolder.firstDieState,
            isRolling = isRolling,
            isFirstDie = true,
            homeCenter = homeCenter,
            shakeTick = shakeTick,
        )
        SingleDie(
            dieState = diceStateHolder.secondDieState,
            isRolling = isRolling,
            isFirstDie = false,
            homeCenter = homeCenter,
            shakeTick = shakeTick,
        )
    }
}

@Composable
private fun BoxWithConstraintsScope.SingleDie(
    dieState: DieState,
    isRolling: Boolean,
    isFirstDie: Boolean,
    homeCenter: Offset,
    shakeTick: Int,
    modifier: Modifier = Modifier,
) {
    val restCenter =
        Offset(
            x = dieRestCenterX(homeCenterX = homeCenter.x, isFirstDie = isFirstDie),
            y = homeCenter.y,
        )

    val isActive = dieState.isActive
    Die3d(
        value = dieState.value,
        isRolling = isActive && isRolling,
        enabled = isActive,
        shakeTick = shakeTick,
        trayWidth = maxWidth,
        trayHeight = maxHeight,
        restCenter = restCenter,
        modifier = modifier,
    )
}

// TODO: Possible to get a preview?
