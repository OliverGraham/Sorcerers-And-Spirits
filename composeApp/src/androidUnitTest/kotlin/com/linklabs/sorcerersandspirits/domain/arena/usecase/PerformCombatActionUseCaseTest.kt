package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.DoublesBonus
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.PerformCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.utilities.DiceRoller
import com.linklabs.sorcerersandspirits.domain.arena.utilities.RollResult
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertTrue

class PerformCombatActionUseCaseTest {

    private val diceRoller =
        mockk<DiceRoller> {
            every { getDoublesBonus() } returns null
            every { setDoublesBonus(any()) } returns Unit
        }

    @Test
    fun `Given an attacker that rolls higher than a defender's armor, the next combat action is a roll against the defender's health`() {
        // Arrange
        val arguments = createTwoPlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        every { diceRoller.roll2D6(userId = arguments.attacker.userId) } returns
            RollResult.TwoDice(firstDie = 4, secondDie = 5, bonus = 0)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.NormalAttack)
        assertTrue(actual = nextAction.roll is NormalAttackRoll.Health)
    }

    @Test
    fun `Given an attacker that rolls less than a defender's armor, the next combat action is an idle action for the next player`() {
        // Arrange
        val arguments = createTwoPlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        every { diceRoller.roll2D6(userId = arguments.attacker.userId) } returns
            RollResult.TwoDice(firstDie = 1, secondDie = 2, bonus = 0)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.Idle)
        assertTrue(actual = nextAction.activeUserIds.contains(element = DEFAULT_DEFENDER_ID))
    }

    @Test
    fun `Given a 2-player game, when an attacker knocks out the defender, then the turn ends and it proceeds to the next round`() {
        // Arrange
        val arguments = createTwoPlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)

        val currentAction = getHealthAttackAction()
        every { diceRoller.roll2D6(userId = arguments.attacker.userId) } returns
            RollResult.TwoDice(firstDie = 6, secondDie = 5, bonus = 0)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(actual = outcome.isNewRound)
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
    }

    @Test
    fun `Given a 3-player game, when an attacker knocks out a defender, then the next player becomes active`() {
        // Arrange
        val arguments = createThreePlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()

        every { diceRoller.roll2D6(userId = arguments.attacker.userId) } returns
            RollResult.TwoDice(firstDie = 5, secondDie = 6, bonus = 0)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.Idle)
        assertTrue(actual = nextAction.activeUserIds.contains(element = THIRD_PLAYER_ID))
    }

    private fun getPerformCombatAction(arguments: CombatActionArguments) =
        PerformCombatAction(arguments = arguments, diceRoller = diceRoller)

    companion object {
        private const val DEFAULT_DISPLAY_NAME = "displayName"
        private const val DEFAULT_ARENA_ID = "arenaId"
        private const val DEFAULT_ATTACKER_ID = "attackerId"
        private const val DEFAULT_DEFENDER_ID = "defenderId"
        private const val THIRD_PLAYER_ID = "thirdPlayerId"

        private fun getNormalAttackAction() =
            CurrentCombatAction.NormalAttack(
                roll =
                    NormalAttackRoll.ArmorClass(
                        attackerUserId = DEFAULT_ATTACKER_ID,
                        defenderUserId = DEFAULT_DEFENDER_ID,
                    )
            )

        private fun getHealthAttackAction() =
            CurrentCombatAction.NormalAttack(
                roll =
                    NormalAttackRoll.Health(
                        attackerUserId = DEFAULT_ATTACKER_ID,
                        defenderUserId = DEFAULT_DEFENDER_ID,
                    )
            )

        private fun createCharacterClass(
            type: CharacterClassType = CharacterClassType.None,
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

        private fun createArenaPlayer(
            userId: String = DEFAULT_ATTACKER_ID,
            displayName: String = DEFAULT_DISPLAY_NAME,
            characterClass: CharacterClass = createCharacterClass(),
        ) =
            ArenaPlayer.Selected(
                userId = userId,
                displayName = displayName,
                characterClass = characterClass,
            )

        private fun createTwoPlayerArguments(
            arenaId: String = DEFAULT_ARENA_ID,
            userId: String = DEFAULT_ATTACKER_ID,
            attacker: ArenaPlayer.Selected =
                createArenaPlayer(
                    userId = DEFAULT_ATTACKER_ID,
                    characterClass =
                        createCharacterClass(type = CharacterClassType.Rogue, speed = 8),
                ),
            defender: ArenaPlayer.Selected =
                createArenaPlayer(
                    userId = DEFAULT_DEFENDER_ID,
                    characterClass =
                        createCharacterClass(
                            type = CharacterClassType.Paladin,
                            speed = 1,
                            hitPoints = 7,
                            armorClass = 7,
                        ),
                ),
            playerMap: Map<String, ArenaPlayer.Selected> =
                mapOf(attacker.userId to attacker, defender.userId to defender),
            roundNumber: Int = 1,
            originalTurnUserId: String = DEFAULT_ATTACKER_ID,
            doublesBonus: DoublesBonus? = null,
        ) =
            CombatActionArguments(
                arenaId = arenaId,
                userId = userId,
                attacker = attacker,
                defender = defender,
                playerMap = playerMap,
                roundNumber = roundNumber,
                originalTurnUserId = originalTurnUserId,
                doublesBonus = doublesBonus,
            )

        private fun createThreePlayerArguments(
            arenaId: String = DEFAULT_ARENA_ID,
            userId: String = DEFAULT_ATTACKER_ID,
            attacker: ArenaPlayer.Selected =
                createArenaPlayer(
                    userId = DEFAULT_ATTACKER_ID,
                    characterClass =
                        createCharacterClass(type = CharacterClassType.Rogue, speed = 8),
                ),
            defender: ArenaPlayer.Selected =
                createArenaPlayer(
                    userId = DEFAULT_DEFENDER_ID,
                    characterClass =
                        createCharacterClass(
                            type = CharacterClassType.Paladin,
                            speed = 5,
                            hitPoints = 7,
                            armorClass = 7,
                        ),
                ),
            thirdPlayer: ArenaPlayer.Selected =
                createArenaPlayer(
                    userId = THIRD_PLAYER_ID,
                    characterClass = createCharacterClass(type = CharacterClassType.Mage, speed = 1),
                ),
            playerMap: Map<String, ArenaPlayer.Selected> =
                mapOf(
                    attacker.userId to attacker,
                    defender.userId to defender,
                    thirdPlayer.userId to thirdPlayer,
                ),
            roundNumber: Int = 1,
            originalTurnUserId: String = DEFAULT_ATTACKER_ID,
            doublesBonus: DoublesBonus? = null,
        ) =
            CombatActionArguments(
                arenaId = arenaId,
                userId = userId,
                attacker = attacker,
                defender = defender,
                playerMap = playerMap,
                roundNumber = roundNumber,
                originalTurnUserId = originalTurnUserId,
                doublesBonus = doublesBonus,
            )
    }
}
