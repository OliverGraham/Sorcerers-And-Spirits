package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.DiePosition

sealed interface DiceRoll {
    val doublesBonus: DoublesBonus?

    data class OneDie(
        val value: Int,
        val diePosition: DiePosition,
        val wasDoublesBonusConsumed: Boolean = false,
        val isMirrorImageReroll: Boolean = false,
        val dieColor: DieColor = DieColor.NormalAttack,
        val disablesSibling: Boolean = true,
        override val doublesBonus: DoublesBonus? = null,
    ) : DiceRoll

    data class TwoDice(
        val firstDie: Int,
        val secondDie: Int,
        val wasDoublesBonusConsumed: Boolean = false,
        val isMirrorImagePass: Boolean = false,
        val dieColor: DieColor = DieColor.NormalAttack,
        override val doublesBonus: DoublesBonus? = null,
    ) : DiceRoll
}
