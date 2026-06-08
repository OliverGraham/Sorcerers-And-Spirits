package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.DoublesBonus
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ActionOutcome
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PerformCombatActionUseCaseTest {

    private val diceRoller =
        mockk<DiceRoller> {
            every { getDoublesBonus() } returns null
            every { setDoublesBonus(userId = any()) } returns Unit
        }

    private fun setupTwoDiceRoll(
        userId: String,
        firstDie: Int = 5,
        secondDie: Int = 6,
        bonus: Int = 0,
    ) {
        every { diceRoller.roll2D6(userId = userId) } returns
            RollResult.TwoDice(firstDie = firstDie, secondDie = secondDie, bonus = bonus)
    }

    @Test
    fun `Given a 2-player game, when an attacker rolls higher than a defender's armor, the next combat action is a roll against the defender's health`() {
        // Arrange
        val arguments = createTwoPlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()
        val attackerUserId = arguments.attacker.userId

        setupTwoDiceRoll(userId = attackerUserId)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.NormalAttack)
        assertTrue(actual = nextAction.roll is NormalAttackRoll.Health)
        assertTrue(actual = nextAction.isThisUserNext(userId = attackerUserId))
    }

    @Test
    fun `Given a 2-player game, when an attacker rolls less than a defender's armor, the next combat action is an idle action for the next player`() {
        // Arrange
        val arguments = createTwoPlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(userId = arguments.attacker.userId, firstDie = 1, secondDie = 2)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.Idle)
        assertTrue(actual = nextAction.isThisUserNext(userId = DEFAULT_DEFENDER_ID))
    }

    @Test
    fun `Given a 2-player game, when an attacker knocks out the defender, then the turn ends and it proceeds to the next round`() {
        // Arrange
        val arguments = createTwoPlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)

        val currentAction = getHealthAttackAction()
        setupTwoDiceRoll(userId = arguments.attacker.userId)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(actual = outcome.isNewRound)
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)

        // TODO: Add MatchFinished type somewhere, somehow
    }

    @Test
    fun `Given a 2-player game, when a health roll deals damage but no knockout, then the turn ends and the next player becomes active`() {
        // Arrange
        val arguments = createTwoPlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()

        setupTwoDiceRoll(userId = arguments.attacker.userId, firstDie = 2, secondDie = 2)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)

        val updatedDefender = outcome.getDefenderById(userId = DEFAULT_DEFENDER_ID)
        assertEquals(expected = 3, actual = updatedDefender.characterClass.hitPoints)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = DEFAULT_DEFENDER_ID))
    }

    @Test
    fun `Given a 3-player game, when an attacker knocks out a defender, then the next player becomes active`() {
        // Arrange
        val arguments = createThreePlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()

        setupTwoDiceRoll(userId = arguments.attacker.userId)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.Idle)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = THIRD_PLAYER_ID))
    }

    @Test
    fun `Given a 3-player game, when a health roll deals damage but no knockout, then the turn moves to the next player in speed order`() {
        // Arrange
        val defender = createCharacterClass(hitPoints = 7)
        val arguments = createThreePlayerArguments(defender = defender)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()

        setupTwoDiceRoll(userId = arguments.attacker.userId, firstDie = 2, secondDie = 2)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.Idle)
        assertTrue(actual = nextAction.isThisUserNext(userId = DEFAULT_DEFENDER_ID))

        val updatedDefender = outcome.getDefenderById(userId = DEFAULT_DEFENDER_ID)
        assertEquals(expected = 3, actual = updatedDefender.characterClass.hitPoints)
    }

    @Test
    fun `Given a 3-player game, when an armor class roll misses, then the turn moves to the next player's turn`() {
        // Arrange
        val arguments = createThreePlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(userId = arguments.attacker.userId, firstDie = 1, secondDie = 1)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.Idle)
        assertTrue(actual = nextAction.isThisUserNext(userId = DEFAULT_DEFENDER_ID))
    }

    @Test
    fun `Given a 3-player game, when the last player of the round finishes their turn, then the game transitions to a new round`() {
        // Arrange
        val attacker =
            createArenaPlayer(
                userId = THIRD_PLAYER_ID,
                characterClass = createCharacterClass(speed = 1),
            )
        val defender =
            createArenaPlayer(
                userId = DEFAULT_DEFENDER_ID,
                characterClass = createCharacterClass(speed = 5, hasActedThisRound = true),
            )
        val otherPlayer =
            createArenaPlayer(
                userId = DEFAULT_ATTACKER_ID,
                characterClass = createCharacterClass(speed = 8, hasActedThisRound = true),
            )

        val arguments =
            createThreePlayerArguments(
                userId = THIRD_PLAYER_ID,
                attacker = attacker,
                defender = defender,
                thirdPlayer = otherPlayer,
                originalTurnUserId = THIRD_PLAYER_ID,
            )

        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction =
            CurrentCombatAction.NormalAttack(
                roll =
                    NormalAttackRoll.ArmorClass(
                        attackerUserId = THIRD_PLAYER_ID,
                        defenderUserId = DEFAULT_DEFENDER_ID,
                    )
            )

        setupTwoDiceRoll(userId = THIRD_PLAYER_ID, firstDie = 1, secondDie = 1)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(actual = outcome.isNewRound)
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.Idle)
        assertTrue(actual = nextAction.isThisUserNext(userId = DEFAULT_ATTACKER_ID))
    }

    @Test
    fun `Given a 3-player game where one player is knocked out, then that player is skipped when transitioning turns`() {
        // Arrange
        val attacker =
            createArenaPlayer(
                userId = DEFAULT_ATTACKER_ID,
                characterClass = createCharacterClass(type = CharacterClassType.Rogue, speed = 8),
            )
        val defender =
            createArenaPlayer(
                userId = DEFAULT_DEFENDER_ID,
                characterClass =
                    createCharacterClass(
                        type = CharacterClassType.Paladin,
                        speed = 5,
                        hitPoints = 0,
                    ),
            )
        val thirdPlayer =
            createArenaPlayer(
                userId = THIRD_PLAYER_ID,
                characterClass = createCharacterClass(type = CharacterClassType.Mage, speed = 1),
            )

        val arguments =
            createThreePlayerArguments(
                attacker = attacker,
                defender = defender,
                thirdPlayer = thirdPlayer,
            )

        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(userId = DEFAULT_ATTACKER_ID, firstDie = 1, secondDie = 1)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.Idle)
        assertTrue(actual = nextAction.isThisUserNext(userId = THIRD_PLAYER_ID))
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
            attackerUserId: String = DEFAULT_ATTACKER_ID,
            defenderUserId: String = DEFAULT_DEFENDER_ID,
            attackerClass: CharacterClass = createCharacterClass(speed = 8),
            defenderClass: CharacterClass = createCharacterClass(speed = 1),
            roundNumber: Int = 1,
            originalTurnUserId: String = DEFAULT_ATTACKER_ID,
            doublesBonus: DoublesBonus? = null,
        ) =
            CombatActionArguments(
                arenaId = arenaId,
                userId = userId,
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
                playerMap =
                    mapOf(
                        attackerUserId to
                            createArenaPlayer(
                                userId = attackerUserId,
                                characterClass = attackerClass,
                            ),
                        defenderUserId to
                            createArenaPlayer(
                                userId = defenderUserId,
                                characterClass = defenderClass,
                            ),
                    ),
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
                    characterClass = createCharacterClass(speed = 8),
                ),
            defender: ArenaPlayer.Selected =
                createArenaPlayer(
                    userId = DEFAULT_DEFENDER_ID,
                    characterClass = createCharacterClass(speed = 6),
                ),
            thirdPlayer: ArenaPlayer.Selected =
                createArenaPlayer(
                    userId = THIRD_PLAYER_ID,
                    characterClass = createCharacterClass(speed = 1),
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

        private fun ActionOutcome.getDefenderById(userId: String): ArenaPlayer.Selected =
            updatedPlayerMap.getValue(key = userId)

        private fun NextCombatAction.isThisUserNext(userId: String) = userId in activeUserIds
    }
}
