package com.linklabs.sorcerersandspirits.domain.arena.utilities

import kotlin.random.Random

interface DiceRoller {
    fun roll1D6(): Int

    fun roll2D6(): Pair<Int, Int>
}

class RealDiceRoller : DiceRoller {
    override fun roll1D6(): Int = Random.nextInt(from = 1, until = 7)

    override fun roll2D6(): Pair<Int, Int> =
        Pair(Random.nextInt(from = 1, until = 7), Random.nextInt(from = 1, until = 7))
}
