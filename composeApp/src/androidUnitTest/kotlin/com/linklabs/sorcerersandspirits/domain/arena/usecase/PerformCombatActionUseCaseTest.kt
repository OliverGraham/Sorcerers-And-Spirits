package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
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

    private fun setupOneDiceRoll(userId: String, die: Int = 3, bonus: Int = 0) {
        every { diceRoller.roll1D6(userId = userId) } returns
            RollResult.OneDie(die = die, bonus = bonus)
    }

    @Test
    fun `Given a 2-player game, when an attacker rolls higher than a defender's armor, the next combat action is a roll against the defender's health`() {
        // Arrange
        val arguments = createTwoPlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()
        val attackerUserId = arguments.attackerUserId

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
    fun `Given a 2-player game, when an attacker rolls equal to a defender's armor, the next combat action is a roll against the defender's health`() {
        // Arrange
        val defenderClass = createCharacterClass(armorClass = 8)
        val arguments = createTwoPlayerArguments(defenderClass = defenderClass)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()
        val attackerUserId = arguments.attackerUserId

        setupTwoDiceRoll(userId = attackerUserId, firstDie = 5, secondDie = 3)

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

        setupTwoDiceRoll(userId = arguments.attackerUserId, firstDie = 1, secondDie = 2)

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
        setupTwoDiceRoll(userId = arguments.attackerUserId)

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
        val defenderClass = createCharacterClass(hitPoints = 7)
        val arguments = createTwoPlayerArguments(defenderClass = defenderClass)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()

        val firstDie = 2
        val secondDie = 2
        val rollTotal = firstDie + secondDie
        setupTwoDiceRoll(
            userId = arguments.attackerUserId,
            firstDie = firstDie,
            secondDie = secondDie,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)

        val updatedDefender = outcome.getPlayerById(userId = DEFAULT_DEFENDER_ID).characterClass
        assertEquals(
            expected = defenderClass.hitPoints - rollTotal,
            actual = updatedDefender.hitPoints,
        )

        assertTrue(actual = updatedDefender.isConscious())
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = DEFAULT_DEFENDER_ID))
    }

    @Test
    fun `Given a 3-player game, when an attacker knocks out a defender, then the next player becomes active`() {
        // Arrange
        val arguments = createThreePlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()

        setupTwoDiceRoll(userId = arguments.attackerUserId)

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
        val defenderClass = createCharacterClass(hitPoints = 7)
        val startingDefenderHitPoints = defenderClass.hitPoints
        val arguments = createThreePlayerArguments(defenderClass = defenderClass)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()

        val firstDie = 2
        val secondDie = 2
        val rollTotal = firstDie + secondDie
        setupTwoDiceRoll(
            userId = arguments.attackerUserId,
            firstDie = firstDie,
            secondDie = secondDie,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.Idle)
        assertTrue(actual = nextAction.isThisUserNext(userId = DEFAULT_DEFENDER_ID))

        val updatedDefender = outcome.getPlayerById(userId = DEFAULT_DEFENDER_ID).characterClass
        assertEquals(
            expected = startingDefenderHitPoints - rollTotal,
            actual = updatedDefender.hitPoints,
        )
        assertTrue(actual = updatedDefender.isConscious())
    }

    @Test
    fun `Given a 3-player game, when an armor class roll misses, then the turn moves to the next player in speed order`() {
        // Arrange
        val arguments = createThreePlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(userId = arguments.attackerUserId, firstDie = 1, secondDie = 1)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.Idle)
        assertTrue(actual = nextAction.isThisUserNext(userId = DEFAULT_DEFENDER_ID))
    }

    @Test
    fun `Given a 3-player game, when a Rogue knocks out a defender, then Killing Momentum triggers and the Rogue acts again`() {
        // Arrange
        val arguments = createThreePlayerArguments(attackerClass = CharacterClass.getRogue())
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()

        setupTwoDiceRoll(userId = arguments.attackerUserId)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.Idle)
        assertTrue(actual = nextAction.isThisUserNext(userId = arguments.attackerUserId))

        val defender = outcome.getPlayerById(userId = DEFAULT_DEFENDER_ID)
        assertTrue(actual = defender.characterClass.hitPoints <= 0)
    }

    @Test
    fun `When an attacker misses a Rogue's armor, then Dodge triggers and the Rogue's armor bonus increases`() {
        // Arrange
        val arguments = createTwoPlayerArguments(defenderClass = CharacterClass.getRogue())
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(userId = arguments.attackerUserId, firstDie = 1, secondDie = 2)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedDefender = outcome.getPlayerById(userId = DEFAULT_DEFENDER_ID).characterClass
        val updatedDodge = updatedDefender.defensiveAbility as DefensiveAbility.Dodge
        assertEquals(expected = 1, actual = updatedDodge.armorBonus)
    }

    @Test
    fun `When an attacker misses a Rogue's armor but Dodge bonus is already at maximum, then the bonus remains at 2`() {
        // Arrange
        val startingArmorBonus = 2
        val rogueWithMaxDodge =
            CharacterClass.getRogue()
                .copy(defensiveAbility = DefensiveAbility.Dodge(armorBonus = startingArmorBonus))
        val arguments = createTwoPlayerArguments(defenderClass = rogueWithMaxDodge)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(userId = arguments.attackerUserId, firstDie = 1, secondDie = 2)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedDefender = outcome.getPlayerById(userId = DEFAULT_DEFENDER_ID).characterClass
        val updatedDodge = updatedDefender.defensiveAbility as DefensiveAbility.Dodge
        assertEquals(expected = startingArmorBonus, actual = updatedDodge.armorBonus)
    }

    @Test
    fun `When Gambler uses All-In and rolls a 1, then the Gambler is knocked out`() {
        // Arrange
        val gamblerClass = CharacterClass.getGambler()
        val arguments = createTwoPlayerArguments(attackerClass = gamblerClass)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getAllInAction(gamblerClass = gamblerClass)

        setupOneDiceRoll(userId = arguments.attackerUserId, die = 1)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedGambler = outcome.getPlayerById(userId = arguments.attackerUserId).characterClass
        assertEquals(expected = 0, actual = updatedGambler.hitPoints)
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = DEFAULT_DEFENDER_ID))
    }

    @Test
    fun `When Gambler uses All-In and rolls a 6, then the Gambler gets another attack`() {
        // Arrange
        val gamblerClass = CharacterClass.getGambler()
        val arguments = createTwoPlayerArguments(attackerClass = gamblerClass)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getAllInAction(gamblerClass = gamblerClass)

        setupOneDiceRoll(userId = arguments.attackerUserId, die = 6)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedGambler = outcome.getPlayerById(userId = arguments.attackerUserId).characterClass
        val updatedAllIn = updatedGambler.offensiveAbility as OffensiveAbility.AllIn
        assertTrue(actual = updatedAllIn.hasAnotherAttack)
        assertTrue(actual = updatedAllIn.usedThisTurn)
        assertTrue(
            actual = outcome.nextCombatAction.isThisUserNext(userId = arguments.attackerUserId)
        )
    }

    @Test
    fun `When Gambler uses All-In and rolls a 3, then nothing happens`() {
        // Arrange
        val gamblerClass = CharacterClass.getGambler()
        val arguments = createTwoPlayerArguments(attackerClass = gamblerClass)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getAllInAction(gamblerClass = gamblerClass)

        setupOneDiceRoll(userId = arguments.attackerUserId, die = 3)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedGambler = outcome.getPlayerById(userId = arguments.attackerUserId).characterClass
        val updatedAllIn = updatedGambler.offensiveAbility as OffensiveAbility.AllIn
        assertTrue(actual = !updatedAllIn.hasAnotherAttack)
        assertTrue(actual = updatedAllIn.usedThisTurn)
        assertTrue(
            actual = outcome.nextCombatAction.isThisUserNext(userId = arguments.attackerUserId)
        )
    }

    @Test
    fun `When someone attacks the Gambler and LuckyCoin stops the attack, it is the next player's turn`() {
        // Arrange
        val gamblerClass = CharacterClass.getGambler()
        val arguments = createTwoPlayerArguments(defenderClass = gamblerClass)
        val gamblerUserId = arguments.defenderUserId
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val attackerRollTotal = 6
        val currentAction = getLuckyCoinFlipAction(attackerRollTotal = attackerRollTotal)

        setupOneDiceRoll(userId = gamblerUserId, die = 3)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = gamblerUserId))
    }

    @Test
    fun `When someone attacks the Gambler and LuckyCoin does not stop the attack, the attacker rolls against Gambler's health`() {
        // Arrange
        val gamblerClass = CharacterClass.getGambler()
        val arguments = createTwoPlayerArguments(defenderClass = gamblerClass)
        val gamblerUserId = arguments.defenderUserId
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val attackerRollTotal = 6
        val currentAction = getLuckyCoinFlipAction(attackerRollTotal = attackerRollTotal)

        setupOneDiceRoll(userId = gamblerUserId, die = 1)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.NormalAttack)
        assertTrue(actual = nextAction.roll is NormalAttackRoll.Health)
        assertTrue(actual = nextAction.isThisUserNext(userId = DEFAULT_ATTACKER_ID))
    }

    @Test
    fun `When someone attacks the Gambler and rolls a 6, then High Roller triggers`() {
        // Arrange
        val gamblerClass = CharacterClass.getGambler()
        val arguments = createTwoPlayerArguments(defenderClass = gamblerClass)
        val gamblerUserId = arguments.defenderUserId
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val attackerRollTotal = 6
        val currentAction = getLuckyCoinFlipAction(attackerRollTotal = attackerRollTotal)

        setupOneDiceRoll(userId = gamblerUserId, die = 6)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.AbilityRoll)
        assertTrue(actual = nextAction.purpose is AbilityAction.Passive.LuckyCoin.HighRoller)
        assertTrue(actual = nextAction.isThisUserNext(userId = gamblerUserId))
    }

    @Test
    fun `Given a 2-player game, when a Gambler uses High Roller and deals damage that knocks out the attacker, then the match ends`() {
        // Arrange
        val gamblerClass = CharacterClass.getGambler()
        val arguments = createTwoPlayerArguments(defenderClass = gamblerClass)
        val gamblerUserId = arguments.defenderUserId
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val attackerRollTotal = 10
        val currentAction =
            getLuckyCoinHighRollerAction(
                attackerUserId = arguments.attackerUserId,
                attackerRollTotal = attackerRollTotal,
            )

        setupOneDiceRoll(userId = gamblerUserId, die = 6)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedAttacker =
            outcome.getPlayerById(userId = arguments.attackerUserId).characterClass
        assertTrue(actual = updatedAttacker.hitPoints <= 0)
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = gamblerUserId))

        // TODO MatchFinished
    }

    @Test
    fun `When a Gambler uses High Roller and deals damage but the attacker survives, then the attacker rolls against health`() {
        // Arrange
        val gamblerClass = CharacterClass.getGambler()
        val attackerClass = createCharacterClass(hitPoints = 10)
        val arguments =
            createTwoPlayerArguments(attackerClass = attackerClass, defenderClass = gamblerClass)

        val attackerUserId = arguments.attackerUserId
        val gamblerUserId = arguments.defenderUserId
        val performCombatAction = getPerformCombatAction(arguments = arguments)

        val attackerRollTotal = 10
        val currentAction =
            getLuckyCoinHighRollerAction(
                attackerUserId = arguments.attackerUserId,
                attackerRollTotal = attackerRollTotal,
            )

        setupOneDiceRoll(userId = gamblerUserId, die = 4)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedAttacker = outcome.getPlayerById(userId = attackerUserId).characterClass
        assertTrue(actual = updatedAttacker.isConscious())

        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.NormalAttack)
        assertTrue(actual = nextAction.roll is NormalAttackRoll.Health)
        assertTrue(actual = nextAction.isThisUserNext(userId = attackerUserId))
    }

    @Test
    fun `When Duelist uses Prepare Attack and rolls a 10, then the defender is knocked out`() {
        // Arrange
        val duelistClass = CharacterClass.getDuelist()
        val arguments = createTwoPlayerArguments(attackerClass = duelistClass)
        val attackerUserId = arguments.attackerUserId

        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getPrepareAttackAction()

        setupTwoDiceRoll(userId = attackerUserId, firstDie = 5, secondDie = 5)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedDefender = outcome.getPlayerById(userId = DEFAULT_DEFENDER_ID).characterClass
        assertEquals(expected = 0, actual = updatedDefender.hitPoints)
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = attackerUserId))

        // TODO MatchFinished
    }

    @Test
    fun `When Duelist uses Prepare Attack and rolls a 7, then nothing happens and the turn ends`() {
        // Arrange
        val duelistClass = CharacterClass.getDuelist()
        val arguments = createTwoPlayerArguments(attackerClass = duelistClass)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getPrepareAttackAction()

        setupTwoDiceRoll(userId = arguments.attackerUserId, firstDie = 3, secondDie = 4)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedDefender = outcome.getPlayerById(userId = DEFAULT_DEFENDER_ID).characterClass
        assertTrue(actual = updatedDefender.isConscious())
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = DEFAULT_DEFENDER_ID))
    }

    @Test
    fun `When an attacker passes a Duelist's ArmorClass, then Riposte triggers`() {
        // Arrange
        val duelistClass = CharacterClass.getDuelist()
        val arguments = createTwoPlayerArguments(defenderClass = duelistClass)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(userId = arguments.attackerUserId, firstDie = 5, secondDie = 5)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.AbilityRoll)

        val purpose = nextAction.purpose
        assertTrue(actual = purpose is AbilityAction.Passive.Riposte)
        assertEquals(expected = 10, actual = purpose.attackerRollTotal)
        assertTrue(actual = nextAction.isThisUserNext(userId = arguments.defenderUserId))
    }

    @Test
    fun `When a Duelist rolls a successful Riposte, then the Duelist attacks the attacker`() {
        // Arrange
        val duelistClass = CharacterClass.getDuelist()
        val arguments = createTwoPlayerArguments(defenderClass = duelistClass)
        val duelistUserId = arguments.defenderUserId
        val initialAttackerUserId = arguments.attackerUserId

        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val attackerRollTotal = 10
        val currentAction = getRiposteAction(attackerRollTotal = attackerRollTotal)

        setupTwoDiceRoll(userId = duelistUserId, firstDie = 3, secondDie = 4)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.NormalAttack)

        val roll = nextAction.roll
        assertTrue(actual = roll is NormalAttackRoll.ArmorClass)
        assertEquals(expected = duelistUserId, actual = roll.attackerUserId)
        assertEquals(expected = initialAttackerUserId, actual = roll.defenderUserId)
        assertTrue(actual = nextAction.isThisUserNext(userId = duelistUserId))
    }

    @Test
    fun `When a Duelist rolls a failed Riposte, then the attacker rolls against the Duelist's health`() {
        // Arrange
        val duelistClass = CharacterClass.getDuelist()
        val arguments = createTwoPlayerArguments(defenderClass = duelistClass)
        val duelistUserId = arguments.defenderUserId
        val initialAttackerUserId = arguments.attackerUserId

        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val attackerRollTotal = 10
        val currentAction = getRiposteAction(attackerRollTotal = attackerRollTotal)

        // total 4 (outside 6..8)
        setupTwoDiceRoll(userId = duelistUserId, firstDie = 2, secondDie = 2)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.NormalAttack)

        val roll = nextAction.roll
        assertTrue(actual = roll is NormalAttackRoll.Health)
        assertEquals(expected = initialAttackerUserId, actual = roll.attackerUserId)
        assertEquals(expected = duelistUserId, actual = roll.defenderUserId)
        assertTrue(actual = nextAction.isThisUserNext(userId = initialAttackerUserId))
    }

    // TODO PrepareAttack against Paladin test

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

        private fun getAllInAction(gamblerClass: CharacterClass) =
            CurrentCombatAction.AbilityRoll(
                purpose =
                    AbilityAction.Activated.AllIn(
                        allInAbility = gamblerClass.offensiveAbility as OffensiveAbility.AllIn
                    )
            )

        private fun getLuckyCoinFlipAction(attackerRollTotal: Int) =
            CurrentCombatAction.AbilityRoll(
                purpose =
                    AbilityAction.Passive.LuckyCoin.Flip(attackerRollTotal = attackerRollTotal)
            )

        private fun getLuckyCoinHighRollerAction(attackerUserId: String, attackerRollTotal: Int) =
            CurrentCombatAction.AbilityRoll(
                purpose =
                    AbilityAction.Passive.LuckyCoin.HighRoller(
                        attackerUserId = attackerUserId,
                        attackerRollTotal = attackerRollTotal,
                    )
            )

        private fun getPrepareAttackAction() =
            CurrentCombatAction.AbilityRoll(purpose = AbilityAction.Activated.PrepareAttack)

        private fun getRiposteAction(attackerRollTotal: Int) =
            CurrentCombatAction.AbilityRoll(
                purpose = AbilityAction.Passive.Riposte(attackerRollTotal = attackerRollTotal)
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
            attackerUserId: String = DEFAULT_ATTACKER_ID,
            defenderUserId: String = DEFAULT_DEFENDER_ID,
            thirdUserId: String = THIRD_PLAYER_ID,
            attackerClass: CharacterClass = createCharacterClass(speed = 8),
            defenderClass: CharacterClass = createCharacterClass(speed = 6),
            thirdClass: CharacterClass = createCharacterClass(speed = 1),
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
                        thirdUserId to
                            createArenaPlayer(userId = THIRD_PLAYER_ID, characterClass = thirdClass),
                    ),
                roundNumber = roundNumber,
                originalTurnUserId = originalTurnUserId,
                doublesBonus = doublesBonus,
            )

        private fun ActionOutcome.getPlayerById(userId: String): ArenaPlayer.Selected =
            updatedPlayerMap.getValue(key = userId)

        private fun NextCombatAction.isThisUserNext(userId: String) = userId in activeUserIds
    }
}
