package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.PerformCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.utilities.DiceRoller
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PerformCombatActionUseCaseTest {

    private val diceRoller: DiceRoller = mockk()

    @Test
    fun `successful armor break`() {
        // Given
        val attackerId = "attacker"
        val defenderId = "defender"
        val attacker = createArenaPlayer(userId = attackerId, characterClass = createRogue())
        val defender =
            createArenaPlayer(userId = defenderId, characterClass = createMage(armorClass = 6))
        val arguments = createCombatActionArguments(attacker = attacker, defender = defender)

        val currentAction =
            CurrentCombatAction.NormalAttack(
                roll =
                    NormalAttackRoll.ArmorClass(
                        attackerUserId = attackerId,
                        defenderUserId = defenderId,
                    )
            )

        // Mock a roll of 7 (3 + 4), which is >= Mage armor class (6)
        every { diceRoller.roll2D6() } returns Pair(3, 4)

        // When
        val outcome =
            PerformCombatAction(arguments = arguments, diceRoller = diceRoller)
                .invoke(currentAction)

        // Then
        val nextAction = outcome.nextCombatAction
        assertTrue(nextAction is NextCombatAction.NormalAttack)
        assertTrue(nextAction.roll is NormalAttackRoll.Health)
        assertEquals(attackerId, nextAction.roll.attackerUserId)
        assertEquals(defenderId, nextAction.roll.defenderUserId)
    }

    companion object {
        private const val DEFAULT_USER_ID = "userId"
        private const val DEFAULT_DISPLAY_NAME = "displayName"
        private const val DEFAULT_ARENA_ID = "arenaId"
        private const val DEFAULT_ATTACKER_ID = "attackerId"
        private const val DEFAULT_DEFENDER_ID = "defenderId"

        private fun createCharacterClass(
            type: CharacterClassType = CharacterClassType.Rogue,
            hitPoints: Int = 5,
            armorClass: Int = 6,
            speed: Int = 8,
            offensiveAbility: OffensiveAbility = OffensiveAbility.None,
            defensiveAbility: DefensiveAbility = DefensiveAbility.None,
            hasActedThisRound: Boolean = false,
        ) =
            CharacterClass(
                type = type,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbility,
                defensiveAbility = defensiveAbility,
                hasActedThisRound = hasActedThisRound,
            )

        private fun createRogue(
            hitPoints: Int = 5,
            armorClass: Int = 6,
            speed: Int = 8,
            hasActedThisRound: Boolean = false,
        ) =
            createCharacterClass(
                type = CharacterClassType.Rogue,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = OffensiveAbility.KillingMomentum,
                defensiveAbility = DefensiveAbility.Dodge(),
                hasActedThisRound = hasActedThisRound,
            )

        private fun createGambler(
            hitPoints: Int = 3,
            armorClass: Int = 4,
            speed: Int = 7,
            hasActedThisRound: Boolean = false,
        ) =
            createCharacterClass(
                type = CharacterClassType.Gambler,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = OffensiveAbility.AllIn(),
                defensiveAbility = DefensiveAbility.LuckyCoin,
                hasActedThisRound = hasActedThisRound,
            )

        private fun createDuelist(
            hitPoints: Int = 4,
            armorClass: Int = 4,
            speed: Int = 6,
            hasActedThisRound: Boolean = false,
        ) =
            createCharacterClass(
                type = CharacterClassType.Duelist,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = OffensiveAbility.PrepareAttack,
                defensiveAbility = DefensiveAbility.Riposte,
                hasActedThisRound = hasActedThisRound,
            )

        private fun createNecromancer(
            hitPoints: Int = 7,
            armorClass: Int = 7,
            speed: Int = 5,
            hasActedThisRound: Boolean = false,
        ) =
            createCharacterClass(
                type = CharacterClassType.Necromancer,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = OffensiveAbility.SummonMinion(),
                defensiveAbility = DefensiveAbility.None,
                hasActedThisRound = hasActedThisRound,
            )

        private fun createBard(
            hitPoints: Int = 5,
            armorClass: Int = 5,
            speed: Int = 4,
            hasActedThisRound: Boolean = false,
        ) =
            createCharacterClass(
                type = CharacterClassType.Bard,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = OffensiveAbility.Encore,
                defensiveAbility = DefensiveAbility.Persuade,
                hasActedThisRound = hasActedThisRound,
            )

        private fun createIllusionist(
            hitPoints: Int = 5,
            armorClass: Int = 5,
            speed: Int = 3,
            hasActedThisRound: Boolean = false,
        ) =
            createCharacterClass(
                type = CharacterClassType.Illusionist,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = OffensiveAbility.Impersonate(),
                defensiveAbility = DefensiveAbility.MirrorImage(),
                hasActedThisRound = hasActedThisRound,
            )

        private fun createMage(
            hitPoints: Int = 4,
            armorClass: Int = 4,
            speed: Int = 2,
            hasActedThisRound: Boolean = false,
        ) =
            createCharacterClass(
                type = CharacterClassType.Mage,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = OffensiveAbility.SpellPotency,
                defensiveAbility = DefensiveAbility.Teleport,
                hasActedThisRound = hasActedThisRound,
            )

        private fun createPaladin(
            hitPoints: Int = 7,
            armorClass: Int = 7,
            speed: Int = 1,
            hasActedThisRound: Boolean = false,
        ) =
            createCharacterClass(
                type = CharacterClassType.Paladin,
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = OffensiveAbility.None,
                defensiveAbility = DefensiveAbility.HolyShield(),
                hasActedThisRound = hasActedThisRound,
            )

        private fun createArenaPlayer(
            userId: String = DEFAULT_USER_ID,
            displayName: String = DEFAULT_DISPLAY_NAME,
            characterClass: CharacterClass = createRogue(),
        ) =
            ArenaPlayer.Selected(
                userId = userId,
                displayName = displayName,
                characterClass = characterClass,
            )

        private fun createCombatActionArguments(
            arenaId: String = DEFAULT_ARENA_ID,
            userId: String = DEFAULT_USER_ID,
            attacker: ArenaPlayer.Selected = createArenaPlayer(userId = DEFAULT_ATTACKER_ID),
            defender: ArenaPlayer.Selected = createArenaPlayer(userId = DEFAULT_DEFENDER_ID),
            playerMap: Map<String, ArenaPlayer.Selected> =
                mapOf(attacker.userId to attacker, defender.userId to defender),
            roundNumber: Int = 1,
            originalTurnUserId: String = DEFAULT_ATTACKER_ID,
        ) =
            CombatActionArguments(
                arenaId = arenaId,
                userId = userId,
                attacker = attacker,
                defender = defender,
                playerMap = playerMap,
                roundNumber = roundNumber,
                originalTurnUserId = originalTurnUserId,
            )
    }
}
