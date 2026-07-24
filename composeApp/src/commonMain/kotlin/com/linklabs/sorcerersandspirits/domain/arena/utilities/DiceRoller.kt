package com.linklabs.sorcerersandspirits.domain.arena.utilities

import com.linklabs.sorcerersandspirits.domain.arena.model.DiceRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.DoublesBonus
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import kotlin.random.Random

interface DiceRoller {
    fun getDiceRoll(): DiceRoll?

    fun roll1D6(
        userId: String,
        diePosition: DiePosition = DiePosition.getRandomPosition(),
        isMirrorImageReroll: Boolean = false,
        isRollOff: Boolean = false,
    ): RollResult.OneDie

    fun roll2D6(userId: String): RollResult.TwoDice

    fun mirrorImagePass(firstDie: Int, secondDie: Int): DiceRoll.TwoDice

    fun getDoublesBonus(): DoublesBonus?
}

// TODO: RollResult isn't really being used anymore - it can be refactored out
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
    private var diceRoll: DiceRoll? = null

    private var doublesBonus: DoublesBonus? = initialBonus

    override fun roll1D6(
        userId: String,
        diePosition: DiePosition,
        isMirrorImageReroll: Boolean,
        isRollOff: Boolean,
    ): RollResult.OneDie {
        val roll = getRandom1D6Value()
        val result = RollResult.OneDie(die = roll, bonus = consumeBonus(userId = userId))
        diceRoll =
            DiceRoll.OneDie(
                value = roll,
                diePosition = diePosition,
                doublesBonus = doublesBonus,
                isMirrorImageReroll = isMirrorImageReroll,
                rollOffOwnerUserId = userId.takeIf { isRollOff },
            )

        return result
    }

    override fun mirrorImagePass(firstDie: Int, secondDie: Int): DiceRoll.TwoDice {
        val untouchedDiceRoll =
            DiceRoll.TwoDice(
                firstDie = firstDie,
                secondDie = secondDie,
                doublesBonus = doublesBonus,
            )
        diceRoll = untouchedDiceRoll

        return untouchedDiceRoll
    }

    override fun roll2D6(userId: String): RollResult.TwoDice {
        val firstDie = getRandom1D6Value()
        val secondDie = getRandom1D6Value()
        val result =
            RollResult.TwoDice(
                firstDie = firstDie,
                secondDie = secondDie,
                bonus = consumeBonus(userId = userId),
            )

        if (result.isDoubles) {
            setDoublesBonus(userId = userId)
        }
        diceRoll =
            DiceRoll.TwoDice(
                firstDie = firstDie,
                secondDie = secondDie,
                doublesBonus = doublesBonus,
            )

        return result
    }

    override fun getDiceRoll() = diceRoll

    override fun getDoublesBonus(): DoublesBonus? = doublesBonus

    private fun consumeBonus(userId: String): Int {
        val bonus = if (doublesBonus?.userId == userId) 1 else 0
        doublesBonus = null
        return bonus
    }

    private fun setDoublesBonus(userId: String) {
        doublesBonus = DoublesBonus(userId = userId)
    }

    private fun getRandom1D6Value() = Random.nextInt(from = 1, until = 7)
}
