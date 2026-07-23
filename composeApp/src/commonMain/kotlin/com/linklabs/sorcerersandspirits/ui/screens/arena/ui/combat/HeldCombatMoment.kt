package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatMoment
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatMomentHold
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.advancedWith

@Composable
internal fun rememberHeldCombatMoment(
    liveMoment: CombatMoment,
    latestAnimationMessageId: String?,
    isSequencing: Boolean,
): CombatMoment {
    var hold by remember {
        mutableStateOf(
            value =
                CombatMomentHold.initial(
                    liveMoment = liveMoment,
                    latestAnimationMessageId = latestAnimationMessageId,
                )
        )
    }

    val nextHold =
        hold.advancedWith(
            liveMoment = liveMoment,
            latestAnimationMessageId = latestAnimationMessageId,
            isSequencing = isSequencing,
        )

    SideEffect { hold = nextHold }

    return nextHold.displayedMoment
}
