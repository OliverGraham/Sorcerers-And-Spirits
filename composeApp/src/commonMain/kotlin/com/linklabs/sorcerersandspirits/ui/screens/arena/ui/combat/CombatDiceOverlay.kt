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
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiceStateHolder
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.Die3d
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DieState

internal val diceSpacing = 76.dp

@Composable
internal fun dieRestCenterX(
    homeCenterX: Float,
    diePosition: DiePosition,
    spacingPx: Float = with(receiver = LocalDensity.current) { diceSpacing.toPx() },
): Float {
    val dieOffsetFromCenter =
        when (diePosition) {
            DiePosition.First -> -0.5f
            DiePosition.Second -> 0.5f
        }

    return homeCenterX + dieOffsetFromCenter * spacingPx
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
        val homeCenter =
            Offset(x = constraints.maxWidth / 2f, y = homeBounds.center.y - overlayOrigin.y)
        val shakeTick = diceStateHolder.shakeTick

        val onDiceSettled = ArenaActionsProvider.current.onDiceSettled
        SingleDie(
            dieState = diceStateHolder.firstDieState,
            isRolling = diceStateHolder.isDieRolling(diePosition = DiePosition.First),
            diePosition = DiePosition.First,
            homeCenter = homeCenter,
            shakeTick = shakeTick,
            onSettled = { diceStateHolder.markDieSettled(diePosition = DiePosition.First, onDiceSettled = onDiceSettled) },
        )
        SingleDie(
            dieState = diceStateHolder.secondDieState,
            isRolling = diceStateHolder.isDieRolling(diePosition = DiePosition.Second),
            diePosition = DiePosition.Second,
            homeCenter = homeCenter,
            shakeTick = shakeTick,
            onSettled = { diceStateHolder.markDieSettled(diePosition = DiePosition.Second, onDiceSettled = onDiceSettled) },
        )

        DiceBadgeOverlay(
            diceBadgeState =
                diceStateHolder.firstDieState.badge ?: diceStateHolder.secondDieState.badge,
            homeCenter = homeCenter,
        )
    }
}

@Composable
private fun BoxWithConstraintsScope.SingleDie(
    dieState: DieState,
    isRolling: Boolean,
    diePosition: DiePosition,
    homeCenter: Offset,
    shakeTick: Int,
    onSettled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val restCenter =
        Offset(
            x = dieRestCenterX(homeCenterX = homeCenter.x, diePosition = diePosition),
            y = homeCenter.y,
        )

    Die3d(
        value = dieState.value,
        isRolling = isRolling,
        enabled = dieState.isEnabled,
        shakeTick = shakeTick,
        trayWidth = maxWidth,
        trayHeight = maxHeight,
        restCenter = restCenter,
        tint = dieState.tint,
        settleKey = dieState.settleGeneration,
        onSettled = onSettled,
        modifier = modifier,
    )
}

// TODO: Possible to get a preview?
