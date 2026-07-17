package com.linklabs.sorcerersandspirits.domain.arena.model

// TODO: Rename to DiceRoll (most likely)

sealed interface MyDiceRoll {
    val doublesBonus: DoublesBonus?

    data class OneDie(val value: Int, override val doublesBonus: DoublesBonus? = null) : MyDiceRoll

    data class TwoDice(
        val firstDie: Int,
        val secondDie: Int,
        override val doublesBonus: DoublesBonus? = null,
    ) : MyDiceRoll

    data class Reroll(
        val originalFirstDie: Int,
        val originalSecondDie: Int,
        val rerolledFirstDie: Int? = null,
        val rerolledSecondDie: Int? = null,
        override val doublesBonus: DoublesBonus? = null,
    ) : MyDiceRoll
}
