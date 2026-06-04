package com.linklabs.sorcerersandspirits.domain.arena.utilities

import com.linklabs.sorcerersandspirits.domain.arena.model.DoublesBonus
import kotlin.random.Random

interface DiceRoller {
    fun roll1D6(userId: String): RollResult.OneDie

    fun roll2D6(userId: String): RollResult.TwoDice

    fun getDoublesBonus(): DoublesBonus?

    fun setDoublesBonus(userId: String)
}

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
    private var doublesBonus: DoublesBonus? = initialBonus

    override fun roll1D6(userId: String): RollResult.OneDie {
        val bonus = consumeBonus(userId = userId)
        return RollResult.OneDie(die = Random.nextInt(from = 1, until = 7), bonus = bonus)
    }

    override fun roll2D6(userId: String): RollResult.TwoDice {
        val bonus = consumeBonus(userId = userId)
        return RollResult.TwoDice(
                firstDie = Random.nextInt(from = 1, until = 7),
                secondDie = Random.nextInt(from = 1, until = 7),
                bonus = bonus,
            )
            .also { result ->
                if (result.isDoubles) {
                    setDoublesBonus(userId = userId)
                }
            }
    }

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
