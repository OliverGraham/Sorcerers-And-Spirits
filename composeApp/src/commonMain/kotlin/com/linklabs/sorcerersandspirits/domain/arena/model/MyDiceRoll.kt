package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.DiePosition

// TODO: Rename to DiceRoll (most likely)

sealed interface MyDiceRoll {
    val doublesBonus: DoublesBonus?

    data class OneDie(
        val value: Int,
        val diePosition: DiePosition,
        override val doublesBonus: DoublesBonus? = null,
    ) : MyDiceRoll

    data class TwoDice(
        val firstDie: Int,
        val secondDie: Int,
        override val doublesBonus: DoublesBonus? = null,
    ) : MyDiceRoll
}
