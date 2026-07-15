package com.linklabs.sorcerersandspirits.domain.arena.utilities

import com.linklabs.sorcerersandspirits.domain.arena.model.DoublesBonus
import com.linklabs.sorcerersandspirits.domain.arena.model.MyDiceRoll
import kotlin.random.Random

interface DiceRoller {
    fun getDiceRoll(): MyDiceRoll?

    fun roll1D6(userId: String): RollResult.OneDie

    fun roll2D6(userId: String): RollResult.TwoDice

    fun getDoublesBonus(): DoublesBonus?

    fun setDoublesBonus(userId: String)
}

// TODO(SS-048) Enums for values 1 through 6?
sealed interface RollResult {
    val total: Int

    data class OneDie(val die: Int, val bonus: Int = 0) : RollResult {
        override val total: Int = die + bonus
    }

    data class TwoDice(val firstDie: Int, val secondDie: Int, val bonus: Int = 0) : RollResult {
        override val total: Int = firstDie + secondDie + bonus
        val isDoubles: Boolean = firstDie == secondDie
    }
}

class RealDiceRoller(initialBonus: DoublesBonus? = null) : DiceRoller {
    private var diceRoll: MyDiceRoll? = null

    private var doublesBonus: DoublesBonus? = initialBonus

    override fun roll1D6(userId: String): RollResult.OneDie {
        val roll = Random.nextInt(from = 1, until = 7)
        diceRoll = MyDiceRoll.OneDie(value = roll)

        return RollResult.OneDie(die = roll, bonus = consumeBonus(userId = userId))
    }

    override fun roll2D6(userId: String): RollResult.TwoDice {
        val firstDie = Random.nextInt(from = 1, until = 7)
        val secondDie = Random.nextInt(from = 1, until = 7)
        diceRoll = MyDiceRoll.TwoDice(firstDie = firstDie, secondDie = secondDie)

        return RollResult.TwoDice(
                firstDie = firstDie,
                secondDie = secondDie,
                bonus = consumeBonus(userId = userId),
            )
            .also { result ->
                if (result.isDoubles) {
                    setDoublesBonus(userId = userId)
                }
            }
    }

    override fun getDiceRoll() = diceRoll

    override fun getDoublesBonus(): DoublesBonus? = doublesBonus

    private fun consumeBonus(userId: String): Int {
        val bonus = if (doublesBonus?.userId == userId) 1 else 0
        doublesBonus = null
        return bonus
    }

    override fun setDoublesBonus(userId: String) {
        doublesBonus = DoublesBonus(userId)
    }
}
