package com.linklabs.sorcerersandspirits.domain.arena.model

sealed interface NormalAttackRoll {
    data object ArmorClass : NormalAttackRoll

    data object Health : NormalAttackRoll
}
