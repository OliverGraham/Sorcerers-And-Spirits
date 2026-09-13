package com.linklabs.sorcerersandspirits.domain.game.ability.summonminion

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.ability.OffensiveAbility

interface SummonMinionAbility<out M : MinionAttributes> {
    val minions: List<M>
    val usedThisTurn: Boolean
}

internal data class SummonerDamagedResult(
    val damageToSummoner: Int,
    val updatedSummonMinion: OffensiveAbility.SummonMinion,
    val minionMessages: List<ArenaMessage.SummonMinion>,
)

internal fun OffensiveAbility.SummonMinion.getSummonerDamagedResult(
    damage: Int,
    summonerUserId: String,
    summonerClassName: String,
    summonerClassType: CharacterClassType,
): SummonerDamagedResult {
    if (minions.isEmpty()) {
        return SummonerDamagedResult(
            damageToSummoner = damage,
            updatedSummonMinion = this,
            minionMessages = emptyList(),
        )
    }

    val updatedMinions = mutableListOf<Minion>()
    val minionMessages = mutableListOf<ArenaMessage.SummonMinion>()
    var remainingDamageToApply = damage

    minions.forEachIndexed { index, minion ->
        when {
            remainingDamageToApply <= 0 ->
                updatedMinions.add(element = minion)

            remainingDamageToApply >= minion.hitPoints -> minionMessages.add(
                element = ArenaMessage.SummonMinion.MinionDestroyed(
                    summonerUserId = summonerUserId,
                    summonerClassName = summonerClassName,
                    summonerClassType = summonerClassType,
                    minionName = minion.name,
                    minionType = minion.type,
                    minionIndex = index,
                    damageAbsorbed = minion.hitPoints,
                ),
            )

            else -> {
                val updatedMinion = minion.copy(hitPoints = minion.hitPoints - remainingDamageToApply)
                updatedMinions.add(element = updatedMinion)
                minionMessages.add(
                    element = ArenaMessage.SummonMinion.MinionDamaged(
                        summonerUserId = summonerUserId,
                        summonerClassName = summonerClassName,
                        summonerClassType = summonerClassType,
                        minionName = updatedMinion.name,
                        minionType = updatedMinion.type,
                        minionIndex = index,
                        damageAbsorbed = remainingDamageToApply,
                        minionHitPointsRemaining = updatedMinion.hitPoints,
                    ),
                )
            }
        }

        remainingDamageToApply -= minion.hitPoints
    }

    return SummonerDamagedResult(
        damageToSummoner = remainingDamageToApply.coerceAtLeast(minimumValue = 0),
        updatedSummonMinion = copy(minions = updatedMinions),
        minionMessages = minionMessages,
    )
}

fun OffensiveAbility.SummonMinion.recordMinionAttack(): OffensiveAbility.SummonMinion {
    val nextMinionIndex = minions.indexOfFirst { minion -> !minion.hasAttackedThisRound }
    val updatedMinions = minions.mapIndexed { index, minion ->
        if (index == nextMinionIndex) {
            minion.copy(hasAttackedThisRound = true)
        } else {
            minion
        }
    }

    return copy(minions = updatedMinions)
}

fun OffensiveAbility.SummonMinion.resetMinionAttacks() = copy(
    usedThisTurn = false,
    minions = minions.map { minion -> minion.copy(hasAttackedThisRound = false) },
)
