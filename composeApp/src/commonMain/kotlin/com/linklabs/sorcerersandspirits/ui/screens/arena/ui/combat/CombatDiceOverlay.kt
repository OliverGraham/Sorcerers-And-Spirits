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
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.DiceBadgeState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiceStateHolder
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.Die3d
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DieState

internal val diceSpacing = 152.dp

// TODO: The dice are nice and centered in multiplayer (there's the classHeadIcons on the side)
//       Otherwise, for two player, they're skewed - needs to always be centered. Should NOT
//       use a method of changing values based on the 2 player vs multiplayer - do this later,
// Claude
@Composable
internal fun dieRestCenterX(
    homeCenterX: Float,
    diePosition: DiePosition,
    spacingPx: Float = with(receiver = LocalDensity.current) { diceSpacing.toPx() },
): Float {
    val diePositionNumber =
        when (diePosition) {
            DiePosition.First -> 0
            DiePosition.Second -> -1
        }

    return homeCenterX + (diePositionNumber / 2f) * spacingPx
}

@Composable
internal fun DiceOverlay(
    diceStateHolder: DiceStateHolder,
    diceBadgeState: DiceBadgeState?,
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
            diePosition = DiePosition.First,
            homeCenter = homeCenter,
            shakeTick = shakeTick,
        )
        SingleDie(
            dieState = diceStateHolder.secondDieState,
            isRolling = isRolling,
            diePosition = DiePosition.Second,
            homeCenter = homeCenter,
            shakeTick = shakeTick,
        )

        DiceBadgeOverlay(diceBadgeState = diceBadgeState, homeCenter = homeCenter)
    }
}

@Composable
private fun BoxWithConstraintsScope.SingleDie(
    dieState: DieState,
    isRolling: Boolean,
    diePosition: DiePosition,
    homeCenter: Offset,
    shakeTick: Int,
    modifier: Modifier = Modifier,
) {
    val restCenter =
        Offset(
            x = dieRestCenterX(homeCenterX = homeCenter.x, diePosition = diePosition),
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
