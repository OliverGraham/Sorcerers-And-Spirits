package com.linklabs.sorcerersandspirits.domain.arena.model

// TODO: Rename to DiceRoll (most likely)

sealed interface MyDiceRoll {

    data class OneDie(val value: Int) : MyDiceRoll

    data class TwoDice(val firstDie: Int, val secondDie: Int) : MyDiceRoll

    data class Reroll(
        val originalFirstDie: Int,
        val originalSecondDie: Int,
        val rerolledFirstDie: Int? = null,
        val rerolledSecondDie: Int? = null,
    ) : MyDiceRoll
}
