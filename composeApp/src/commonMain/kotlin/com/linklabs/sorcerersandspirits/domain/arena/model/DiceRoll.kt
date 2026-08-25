package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.DiePosition

sealed interface DiceRoll {
    val doublesBonus: DoublesBonus?

    data class OneDie(
        val value: Int,
        val diePosition: DiePosition,
        override val doublesBonus: DoublesBonus? = null,
        val wasDoublesBonusConsumed: Boolean = false,
        val isMirrorImageReroll: Boolean = false,
        val dieColor: DieColor = DieColor.NormalAttack,
        val disablesSibling: Boolean = true,
    ) : DiceRoll

    data class TwoDice(
        val firstDie: Int,
        val secondDie: Int,
        override val doublesBonus: DoublesBonus? = null,
        val wasDoublesBonusConsumed: Boolean = false,
        val dieColor: DieColor = DieColor.NormalAttack,
    ) : DiceRoll
}
