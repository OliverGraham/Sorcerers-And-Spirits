package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.DiceRoll
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.DoublesBonusState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.toDomain
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.toState

@Immutable
sealed interface DiceRollState {
    val doublesBonus: DoublesBonusState?

    @Immutable
    data class OneDie(
        val value: Int,
        val diePosition: DiePosition,
        override val doublesBonus: DoublesBonusState?,
        val isMirrorImageReroll: Boolean = false,
    ) : DiceRollState

    @Immutable
    data class TwoDice(
        val firstDie: Int,
        val secondDie: Int,
        override val doublesBonus: DoublesBonusState?,
    ) : DiceRollState
}

internal fun DiceRoll.toState() =
    when (this) {
        is DiceRoll.OneDie ->
            DiceRollState.OneDie(
                value = value,
                diePosition = diePosition,
                doublesBonus = doublesBonus?.toState(),
                isMirrorImageReroll = isMirrorImageReroll,
            )

        is DiceRoll.TwoDice ->
            DiceRollState.TwoDice(
                firstDie = firstDie,
                secondDie = secondDie,
                doublesBonus = doublesBonus?.toState(),
            )
    }

internal fun DiceRollState.toDomain() =
    when (this) {
        is DiceRollState.OneDie ->
            DiceRoll.OneDie(
                value = value,
                diePosition = diePosition,
                doublesBonus = doublesBonus?.toDomain(),
                isMirrorImageReroll = isMirrorImageReroll,
            )

        is DiceRollState.TwoDice ->
            DiceRoll.TwoDice(
                firstDie = firstDie,
                secondDie = secondDie,
                doublesBonus = doublesBonus?.toDomain(),
            )
    }
