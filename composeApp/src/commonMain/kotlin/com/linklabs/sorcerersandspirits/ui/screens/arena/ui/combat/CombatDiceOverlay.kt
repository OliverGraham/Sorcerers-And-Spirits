package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DIE_RESTING_FACE
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiceRoll
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.Die3d

private const val DICE_HOME_DIE_COUNT = 2
private val diceHomeHeight = 56.dp
internal val diceRestSpacing = 76.dp

/** The x-center a die rests at for its [slot], shared so overlays can anchor to a specific die. */
internal fun dieRestCenterX(homeCenterX: Float, slot: Int, spacingPx: Float): Float =
    homeCenterX + (slot - (DICE_HOME_DIE_COUNT - 1) / 2f) * spacingPx

/**
 * The empty resting slot below the avatars that reserves space for the dice and reports its bounds.
 * The dice themselves are drawn in the screen-spanning [DiceOverlay] so they can roll over all the
 * content; at rest they sit centered in this slot (SS-149).
 */
@Composable
internal fun DiceHomeSlot(onBoundsChanged: (Rect) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier.fillMaxWidth().height(height = diceHomeHeight).onGloballyPositioned {
                onBoundsChanged(it.boundsInRoot())
            }
    )
}

/**
 * The dice, drawn in a screen-spanning overlay above all other content so a roll tumbles over
 * everything (SS-149). Each die's tray is the whole overlay, so it bounces across the screen while
 * rolling, then glides back to its resting spot in the [homeBounds] slot. Always shows both dice
 * for a steady footprint; on a single-die roll the second die is dimmed and still. The overlay is
 * non-interactive, so taps fall through to the content (the buttons) beneath it.
 */
@Composable
internal fun DiceOverlay(diceRoll: DiceRoll, homeBounds: Rect, modifier: Modifier = Modifier) {
    var overlayOrigin by remember { mutableStateOf(value = Offset.Zero) }

    BoxWithConstraints(
        modifier =
            modifier.fillMaxSize().onGloballyPositioned {
                overlayOrigin = it.localToRoot(relativeToLocal = Offset.Zero)
            }
    ) {
        val spacingPx = with(receiver = LocalDensity.current) { diceRestSpacing.toPx() }
        val homeCenter = homeBounds.center - overlayOrigin

        repeat(times = DICE_HOME_DIE_COUNT) { index ->
            val isActive = index in diceRoll.activeSlots
            val restCenter =
                Offset(
                    x =
                        dieRestCenterX(
                            homeCenterX = homeCenter.x,
                            slot = index,
                            spacingPx = spacingPx,
                        ),
                    y = homeCenter.y,
                )
            Die3d(
                value = diceRoll.dieFaces.getOrNull(index = index) ?: DIE_RESTING_FACE,
                isRolling = isActive && diceRoll.isRolling,
                enabled = isActive,
                shakeTick = diceRoll.shakeTick,
                trayWidth = maxWidth,
                trayHeight = maxHeight,
                restCenter = restCenter,
            )
        }
    }
}
