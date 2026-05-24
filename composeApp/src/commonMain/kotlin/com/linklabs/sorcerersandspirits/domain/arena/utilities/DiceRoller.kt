package com.linklabs.sorcerersandspirits.domain.arena.utilities

import kotlin.random.Random

fun roll1D6(): Int = Random.nextInt(from = 1, until = 7)
