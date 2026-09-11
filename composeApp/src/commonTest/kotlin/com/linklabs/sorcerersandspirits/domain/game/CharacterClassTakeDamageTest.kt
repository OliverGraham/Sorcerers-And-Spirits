package com.linklabs.sorcerersandspirits.domain.game

import com.linklabs.sorcerersandspirits.domain.game.ability.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.shieldPoints
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CharacterClassTakeDamageTest {

    @Test
    fun `Given a class with no minions, when it takes damage, then base hit points absorb it`() {
        val damaged = TestCharacterClasses.rogue().takeDamage(damage = 3)

        assertEquals(expected = 2, actual = damaged.hitPoints)
    }

    @Test
    fun `Given two skeletons, when damage exceeds the shield, then skeletons absorb first and the rest hits base`() {
        val necromancer = necromancerWithMinions(skeletonCount = 2)

        val damaged = necromancer.takeDamage(damage = 4)

        val minions = assertIs<OffensiveAbility.SummonMinion>(value = damaged.offensiveAbility)
        assertEquals(expected = 0, actual = minions.skeletonCount)
        assertEquals(expected = 0, actual = minions.golemCount)
        assertEquals(expected = 5, actual = damaged.hitPoints)
    }

    @Test
    fun `Given minion shields larger than the damage, when it takes damage, then base hit points are untouched`() {
        val necromancer = necromancerWithMinions(skeletonCount = 1, golemCount = 1)

        val damaged = necromancer.takeDamage(damage = 2)

        val minions = assertIs<OffensiveAbility.SummonMinion>(value = damaged.offensiveAbility)
        assertEquals(expected = 1, actual = minions.shieldPoints)
        assertEquals(expected = 7, actual = damaged.hitPoints)
    }

    @Test
    fun `Given only a golem, when it takes partial damage, then the golem downgrades to a skeleton-equivalent`() {
        val necromancer = necromancerWithMinions(golemCount = 1)

        val damaged = necromancer.takeDamage(damage = 1)

        val minions = assertIs<OffensiveAbility.SummonMinion>(value = damaged.offensiveAbility)
        assertEquals(expected = 1, actual = minions.skeletonCount)
        assertEquals(expected = 0, actual = minions.golemCount)
        assertEquals(expected = 7, actual = damaged.hitPoints)
    }

    @Test
    fun `Given skeletons and a golem, when damage exactly equals the shield total, then all minions are destroyed and base is untouched`() {
        val necromancer = necromancerWithMinions(skeletonCount = 1, golemCount = 1)

        val damaged = necromancer.takeDamage(damage = 3)

        val minions = assertIs<OffensiveAbility.SummonMinion>(value = damaged.offensiveAbility)
        assertEquals(expected = 0, actual = minions.shieldPoints)
        assertEquals(expected = 7, actual = damaged.hitPoints)
    }

    @Test
    fun `Given an Illusionist impersonating a Necromancer with minions, when it takes damage, then the shield absorbs and the Impersonate wrapper is kept`() {
        val illusionist =
            TestCharacterClasses.illusionist()
                .copy(
                    offensiveAbility =
                    OffensiveAbility.Impersonate(
                        impersonatedAbility =
                        OffensiveAbility.SummonMinion(skeletonCount = 1, golemCount = 1),
                    ),
                )

        val damaged = illusionist.takeDamage(damage = 2)

        val impersonate = assertIs<OffensiveAbility.Impersonate>(value = damaged.offensiveAbility)
        val minions =
            assertIs<OffensiveAbility.SummonMinion>(value = impersonate.impersonatedAbility)
        assertEquals(expected = 1, actual = minions.shieldPoints)
        assertEquals(expected = illusionist.hitPoints, actual = damaged.hitPoints)
    }

    private fun necromancerWithMinions(
        skeletonCount: Int = 0,
        golemCount: Int = 0,
    ): CharacterClass = TestCharacterClasses.necromancer()
        .copy(
            offensiveAbility =
            OffensiveAbility.SummonMinion(
                skeletonCount = skeletonCount,
                golemCount = golemCount,
            ),
        )
}
