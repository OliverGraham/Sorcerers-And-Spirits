package com.linklabs.sorcerersandspirits.domain.game

import com.linklabs.sorcerersandspirits.domain.game.ability.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.Minion
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.MinionType
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.getSummonerDamagedResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CharacterClassTakeDamageTest {

    @Test
    fun `Given no minions, when the summoner takes damage, then all of it passes through to the summoner`() {
        val result = OffensiveAbility.SummonMinion().damaged(damage = 3)

        assertEquals(expected = 3, actual = result.damageToSummoner)
        assertTrue(actual = result.updatedSummonMinion.minions.isEmpty())
    }

    @Test
    fun `Given two skeletons, when damage exceeds the shield, then skeletons absorb first and the rest hits the summoner`() {
        val result = summonMinion(skeletonCount = 2).damaged(damage = 4)

        assertEquals(expected = 2, actual = result.damageToSummoner)
        assertTrue(actual = result.updatedSummonMinion.minions.isEmpty())
    }

    @Test
    fun `Given minion shields larger than the damage, when it takes damage, then no damage passes to the summoner`() {
        val result = summonMinion(skeletonCount = 1, golemCount = 1).damaged(damage = 2)

        assertEquals(expected = 0, actual = result.damageToSummoner)
        assertEquals(expected = 1, actual = result.updatedSummonMinion.shieldPoints())
    }

    @Test
    fun `Given only a golem, when it takes partial damage, then the golem survives with a reduced shield`() {
        val result = summonMinion(golemCount = 1).damaged(damage = 1)

        assertEquals(expected = 0, actual = result.damageToSummoner)
        assertEquals(expected = 1, actual = result.updatedSummonMinion.shieldPoints())
        assertEquals(expected = MinionType.Golem, actual = result.updatedSummonMinion.minions.single().type)
    }

    @Test
    fun `Given skeletons and a golem, when damage exactly equals the shield total, then all minions are destroyed and no damage passes through`() {
        val result = summonMinion(skeletonCount = 1, golemCount = 1).damaged(damage = 3)

        assertEquals(expected = 0, actual = result.damageToSummoner)
        assertTrue(actual = result.updatedSummonMinion.minions.isEmpty())
    }

    private fun OffensiveAbility.SummonMinion.damaged(damage: Int) = getSummonerDamagedResult(
        damage = damage,
        summonerUserId = SUMMONER_USER_ID,
        summonerClassName = SUMMONER_CLASS_NAME,
        summonerClassType = CharacterClassType.Necromancer,
    )

    private fun OffensiveAbility.SummonMinion.shieldPoints(): Int = minions.sumOf { minion -> minion.hitPoints }

    private fun summonMinion(
        skeletonCount: Int = 0,
        golemCount: Int = 0,
    ): OffensiveAbility.SummonMinion {
        val skeletons = List(size = skeletonCount) { index ->
            Minion(
                type = MinionType.Skeleton,
                name = "skeleton-$index",
                hasAttackedThisRound = false,
                hitPoints = 1,
            )
        }
        val golems = List(size = golemCount) { index ->
            Minion(
                type = MinionType.Golem,
                name = "golem-$index",
                hasAttackedThisRound = false,
                hitPoints = 2,
            )
        }
        return OffensiveAbility.SummonMinion(minions = skeletons + golems)
    }

    private companion object {
        const val SUMMONER_USER_ID = "summoner-1"
        const val SUMMONER_CLASS_NAME = "Necromancer"
    }
}
