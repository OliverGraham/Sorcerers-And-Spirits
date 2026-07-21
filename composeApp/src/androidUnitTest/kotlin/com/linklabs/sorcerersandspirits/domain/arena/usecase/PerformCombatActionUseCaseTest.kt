package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction.Passive.Teleport.TeleportDie
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.AttackTurn
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.DoublesBonus
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ActionOutcome
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.PerformCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.utilities.DiceRoller
import com.linklabs.sorcerersandspirits.domain.arena.utilities.RollResult
import com.linklabs.sorcerersandspirits.domain.game.AbilityConfig
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.MirrorImageRerollSelection
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.TestCharacterClasses
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PerformCombatActionUseCaseTest {

    private val diceRoller =
        mockk<DiceRoller> {
            every { getDoublesBonus() } returns null
            every { getDiceRoll() } returns null
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
        every { diceRoller.roll1D6(userId = userId, diePosition = any()) } returns
            RollResult.OneDie(die = die, bonus = bonus)
    }

    @Test
    fun `Given a 2-player game, when an attacker rolls higher than a defender's armor, the next combat action is a roll against the defender's health`() {
        // Arrange
        val arguments = createTwoPlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()
        val attackerUserId = currentAction.attackTurn.attackerUserId

        setupTwoDiceRoll(userId = attackerUserId)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.NormalAttack)
        assertEquals(actual = nextAction.roll, expected = NormalAttackType.Health)
        assertTrue(actual = nextAction.isThisUserNext(userId = attackerUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `Given a normal attack, when the roll is doubles, then a Doubles message is emitted for the attacker`() {
        // Arrange
        val arguments = createTwoPlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()
        val attackerUserId = currentAction.attackTurn.attackerUserId

        setupTwoDiceRoll(userId = attackerUserId, firstDie = 4, secondDie = 4)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(
            actual =
                outcome.messages.contains(element = ArenaMessage.Doubles(userId = attackerUserId))
        )
    }

    @Test
    fun `Given a normal attack, when the roll is not doubles, then no Doubles message is emitted`() {
        // Arrange
        val arguments = createTwoPlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()
        val attackerUserId = currentAction.attackTurn.attackerUserId

        setupTwoDiceRoll(userId = attackerUserId, firstDie = 5, secondDie = 6)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertFalse(actual = outcome.messages.any { it is ArenaMessage.Doubles })
    }

    @Test
    fun `Given a normal attack, when doubles miss the defender's armor, then no Doubles message is emitted`() {
        // Arrange
        val arguments = createTwoPlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 2,
            secondDie = 2,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertFalse(actual = outcome.messages.any { it is ArenaMessage.Doubles })
    }

    @Test
    fun `Given a Gambler with a pending All-In second attack, when doubles miss the defender's armor, then a Doubles message is emitted`() {
        // Arrange
        val gambler =
            TestCharacterClasses.gambler()
                .copy(
                    offensiveAbility =
                        OffensiveAbility.AllIn(hasAnotherAttack = true, usedThisTurn = true)
                )
        val arguments = createTwoPlayerArguments(attackerClass = gambler)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()
        val attackerUserId = currentAction.attackTurn.attackerUserId

        setupTwoDiceRoll(userId = attackerUserId, firstDie = 2, secondDie = 2)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(
            actual =
                outcome.messages.contains(element = ArenaMessage.Doubles(userId = attackerUserId))
        )
    }

    @Test
    fun `Given a Duelist defender, when doubles break the armor, then no Doubles message is emitted because Riposte takes the dice`() {
        // Arrange
        val arguments = createTwoPlayerArguments(defenderClass = TestCharacterClasses.duelist())
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 3,
            secondDie = 3,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertFalse(actual = outcome.messages.any { it is ArenaMessage.Doubles })
    }

    @Test
    fun `Given a Mage defender, when doubles break the armor, then a Doubles message is emitted because the attacker's Teleport roll-off die keeps the bonus`() {
        // Arrange
        val arguments = createTwoPlayerArguments(defenderClass = TestCharacterClasses.mage())
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()
        val attackerUserId = currentAction.attackTurn.attackerUserId

        setupTwoDiceRoll(userId = attackerUserId, firstDie = 3, secondDie = 3)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(
            actual =
                outcome.messages.contains(element = ArenaMessage.Doubles(userId = attackerUserId))
        )
    }

    @Test
    fun `Given a Health roll, when doubles deal damage and the turn ends, then no Doubles message is emitted`() {
        // Arrange
        val defenderClass = createCharacterClass(hitPoints = 10)
        val arguments = createTwoPlayerArguments(defenderClass = defenderClass)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 2,
            secondDie = 2,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertFalse(actual = outcome.messages.any { it is ArenaMessage.Doubles })
    }

    @Test
    fun `Given a Bard attacker, when doubles deal damage, then a Doubles message is emitted because the Encore die keeps the bonus`() {
        // Arrange
        val defenderClass = createCharacterClass(hitPoints = 10)
        val arguments =
            createTwoPlayerArguments(
                attackerClass = TestCharacterClasses.bard(),
                defenderClass = defenderClass,
            )
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()
        val attackerUserId = currentAction.attackTurn.attackerUserId

        setupTwoDiceRoll(userId = attackerUserId, firstDie = 2, secondDie = 2)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(
            actual =
                outcome.messages.contains(element = ArenaMessage.Doubles(userId = attackerUserId))
        )
    }

    @Test
    fun `Given a Health roll, when doubles knock out the last opponent, then no Doubles message is emitted`() {
        // Arrange
        val arguments = createTwoPlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 3,
            secondDie = 3,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(actual = outcome.isMatchFinished)
        assertFalse(actual = outcome.messages.any { it is ArenaMessage.Doubles })
    }

    @Test
    fun `Given a Rogue attacker in a 3-player game, when doubles knock out the defender, then a Doubles message is emitted because Killing Momentum keeps the dice`() {
        // Arrange
        val arguments = createThreePlayerArguments(attackerClass = TestCharacterClasses.rogue())
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()
        val attackerUserId = currentAction.attackTurn.attackerUserId

        setupTwoDiceRoll(userId = attackerUserId, firstDie = 3, secondDie = 3)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(
            actual =
                outcome.messages.contains(element = ArenaMessage.Doubles(userId = attackerUserId))
        )
    }

    @Test
    fun `Given a Paladin defender with Holy Shield, when doubles knock the Paladin down, then no Doubles message is emitted`() {
        // Arrange
        val arguments = createTwoPlayerArguments(defenderClass = TestCharacterClasses.paladin())
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 4,
            secondDie = 4,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertFalse(actual = outcome.messages.any { it is ArenaMessage.Doubles })
    }

    @Test
    fun `Given an Illusionist defender with Mirror Image available, when doubles break the armor, then the Doubles message is deferred`() {
        // Arrange
        val arguments = createTwoPlayerArguments(defenderClass = TestCharacterClasses.illusionist())
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 3,
            secondDie = 3,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertFalse(actual = outcome.messages.any { it is ArenaMessage.Doubles })
    }

    @Test
    fun `Given an Illusionist keeping a doubles armor roll, when the armor stays broken, then the Doubles message is emitted`() {
        // Arrange
        val mirrorImagePurpose =
            AbilityAction.Passive.MirrorImage(
                firstDie = AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 3),
                secondDie = AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 3),
                attackType = NormalAttackType.ArmorClass,
                attackerDoublesBonus = 0,
                mirrorImageDefensiveAbility = DefensiveAbility.MirrorImage(),
            )
        val arguments = createTwoPlayerArguments(defenderClass = TestCharacterClasses.illusionist())
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getAbilityRollAction(purpose = mirrorImagePurpose)
        val attackerUserId = currentAction.attackTurn.attackerUserId

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(
            actual =
                outcome.messages.contains(element = ArenaMessage.Doubles(userId = attackerUserId))
        )
    }

    @Test
    fun `Given an Illusionist rerolling a doubles armor roll, when the armor stays broken, then no Doubles message is emitted`() {
        // Arrange
        val mirrorImagePurpose =
            AbilityAction.Passive.MirrorImage(
                firstDie = AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 3),
                secondDie = AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 3),
                attackType = NormalAttackType.ArmorClass,
                attackerDoublesBonus = 0,
                mirrorImageDefensiveAbility = DefensiveAbility.MirrorImage(),
                mirrorImageRerollSelection = MirrorImageRerollSelection.First,
            )
        val arguments = createTwoPlayerArguments(defenderClass = TestCharacterClasses.illusionist())
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getAbilityRollAction(purpose = mirrorImagePurpose)

        setupOneDiceRoll(userId = currentAction.defenderUserId, die = 6)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertFalse(actual = outcome.messages.any { it is ArenaMessage.Doubles })
    }

    @Test
    fun `Given a 2-player game, when an attacker rolls equal to a defender's armor, the next combat action is a roll against the defender's health`() {
        // Arrange
        val defenderClass = createCharacterClass(armorClass = 8)
        val arguments = createTwoPlayerArguments(defenderClass = defenderClass)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()
        val attackerUserId = currentAction.attackTurn.attackerUserId

        setupTwoDiceRoll(userId = attackerUserId, firstDie = 5, secondDie = 3)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.NormalAttack)
        assertEquals(actual = nextAction.roll, expected = NormalAttackType.Health)
        assertTrue(actual = nextAction.isThisUserNext(userId = attackerUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `Given a 2-player game, when an attacker rolls less than a defender's armor, the next combat action is an idle action for the next player`() {
        // Arrange
        val arguments = createTwoPlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 1,
            secondDie = 2,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.Idle)
        assertTrue(actual = nextAction.isThisUserNext(userId = currentAction.defenderUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `Given a 2-player game, when an attacker knocks out the defender, then the turn ends and it proceeds to the next round`() {
        // Arrange
        val arguments = createTwoPlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)

        val currentAction = getHealthAttackAction()
        setupTwoDiceRoll(userId = currentAction.attackTurn.attackerUserId)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
        assertTrue(actual = outcome.isMatchFinished)
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
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = firstDie,
            secondDie = secondDie,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
        assertTrue(actual = !outcome.isMatchFinished)

        val defenderUserId = currentAction.defenderUserId
        val updatedDefender = outcome.getPlayerById(userId = defenderUserId).characterClass
        assertEquals(
            expected = defenderClass.hitPoints - rollTotal,
            actual = updatedDefender.hitPoints,
        )

        assertTrue(actual = updatedDefender.isConscious())
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = defenderUserId))
    }

    @Test
    fun `Given a 3-player game, when an attacker knocks out a defender, then the next player becomes active`() {
        // Arrange
        val arguments = createThreePlayerArguments()
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()

        setupTwoDiceRoll(userId = currentAction.attackTurn.attackerUserId)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.Idle)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = THIRD_PLAYER_ID))
        assertTrue(actual = !outcome.isMatchFinished)
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
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = firstDie,
            secondDie = secondDie,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val defenderUserId = currentAction.defenderUserId
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.Idle)
        assertTrue(actual = nextAction.isThisUserNext(userId = defenderUserId))
        assertTrue(actual = !outcome.isMatchFinished)

        val updatedDefender = outcome.getPlayerById(userId = defenderUserId).characterClass
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

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 1,
            secondDie = 1,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.Idle)
        assertTrue(actual = nextAction.isThisUserNext(userId = currentAction.defenderUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `Given a 3-player game, when a Rogue knocks out a defender, then Killing Momentum triggers and the Rogue acts again`() {
        // Arrange
        val arguments = createThreePlayerArguments(attackerClass = TestCharacterClasses.rogue())
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()
        val rogueUserId = currentAction.attackTurn.attackerUserId

        setupTwoDiceRoll(userId = rogueUserId)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.Idle)
        assertTrue(actual = nextAction.isThisUserNext(userId = rogueUserId))
        assertTrue(actual = !outcome.isMatchFinished)

        val defender = outcome.getPlayerById(userId = currentAction.defenderUserId)
        assertTrue(actual = defender.characterClass.hitPoints <= 0)
    }

    @Test
    fun `Given a 3-player game, when a Rogue deals damage but no knockout, then Killing Momentum does not trigger and the turn moves on`() {
        // Arrange
        val defenderClass = createCharacterClass(hitPoints = 7)
        val arguments =
            createThreePlayerArguments(
                attackerClass = TestCharacterClasses.rogue(),
                defenderClass = defenderClass,
            )
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()
        val rogueUserId = currentAction.attackTurn.attackerUserId

        val firstDie = 2
        val secondDie = 2
        val rollTotal = firstDie + secondDie
        setupTwoDiceRoll(userId = rogueUserId, firstDie = firstDie, secondDie = secondDie)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert: the defender survives, so Killing Momentum must not hand the turn back to the
        // Rogue.
        val defenderUserId = currentAction.defenderUserId
        val updatedDefender = outcome.getPlayerById(userId = defenderUserId).characterClass
        assertEquals(
            expected = defenderClass.hitPoints - rollTotal,
            actual = updatedDefender.hitPoints,
        )
        assertTrue(actual = updatedDefender.isConscious())

        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.Idle)
        assertTrue(actual = !nextAction.isThisUserNext(userId = rogueUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `Given an Illusionist impersonating Killing Momentum, when they knock out a defender, then Killing Momentum triggers and they act again`() {
        // Arrange
        val illusionist =
            TestCharacterClasses.illusionist()
                .copy(
                    offensiveAbility =
                        OffensiveAbility.Impersonate(
                            impersonatedAbility = OffensiveAbility.KillingMomentum
                        )
                )
        val arguments = createThreePlayerArguments(attackerClass = illusionist)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()
        val illusionistUserId = currentAction.attackTurn.attackerUserId

        setupTwoDiceRoll(userId = illusionistUserId)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert: the impersonated Killing Momentum lets the Illusionist take another turn.
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.Idle)
        assertTrue(actual = nextAction.isThisUserNext(userId = illusionistUserId))
        assertTrue(actual = !outcome.isMatchFinished)

        val defender = outcome.getPlayerById(userId = currentAction.defenderUserId)
        assertTrue(actual = defender.characterClass.hitPoints <= 0)
    }

    @Test
    fun `Given an Illusionist impersonating a class, then the normal roll carries that class's type`() {
        // Arrange
        val illusionist =
            TestCharacterClasses.illusionist()
                .copy(
                    offensiveAbility =
                        OffensiveAbility.Impersonate(
                            impersonatedAbility = OffensiveAbility.SpellPotency
                        )
                )
        val arguments = createTwoPlayerArguments(attackerClass = illusionist)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(userId = currentAction.attackTurn.attackerUserId)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val normalRoll = outcome.messages.filterIsInstance<ArenaMessage.NormalRoll>().first()
        assertEquals(
            actual = normalRoll.impersonatedCharacterType,
            expected = CharacterClassType.Mage,
        )
    }

    @Test
    fun `Given an Illusionist that has not impersonated, then the normal roll borrows no opponent class`() {
        // Arrange
        val arguments = createTwoPlayerArguments(attackerClass = TestCharacterClasses.illusionist())
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(userId = currentAction.attackTurn.attackerUserId)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert: no opponent class copied resolves to the Illusionist's own class, which borrows a
        // random weapon rather than an impersonated one.
        val normalRoll = outcome.messages.filterIsInstance<ArenaMessage.NormalRoll>().first()
        assertEquals(
            actual = normalRoll.impersonatedCharacterType,
            expected = CharacterClassType.Illusionist,
        )
    }

    @Test
    fun `Given a non-Illusionist attacker, then the normal roll carries no impersonated type`() {
        // Arrange
        val arguments = createTwoPlayerArguments(attackerClass = TestCharacterClasses.rogue())
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(userId = currentAction.attackTurn.attackerUserId)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val normalRoll = outcome.messages.filterIsInstance<ArenaMessage.NormalRoll>().first()
        assertEquals(
            actual = normalRoll.impersonatedCharacterType,
            expected = CharacterClassType.None,
        )
    }

    @Test
    fun `When an attacker misses a Rogue's armor, then Dodge triggers and the Rogue's armor bonus increases`() {
        // Arrange
        val arguments = createTwoPlayerArguments(defenderClass = TestCharacterClasses.rogue())
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 1,
            secondDie = 2,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedDefender =
            outcome.getPlayerById(userId = currentAction.defenderUserId).characterClass
        val updatedDodge =
            assertIs<DefensiveAbility.Dodge>(value = updatedDefender.defensiveAbility)
        assertEquals(expected = 1, actual = updatedDodge.armorBonus)
        assertEquals(
            expected = TestCharacterClasses.rogue().armorClass + 1,
            actual = updatedDefender.armorClass,
        )
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When an attacker misses a Rogue's armor but Dodge bonus is already at maximum, then the bonus remains at 2`() {
        // Arrange
        val startingArmorBonus = 2
        val rogueWithMaxDodge =
            TestCharacterClasses.rogue()
                .copy(
                    armorClass = TestCharacterClasses.rogue().armorClass + startingArmorBonus,
                    defensiveAbility = DefensiveAbility.Dodge(armorBonus = startingArmorBonus),
                )
        val arguments = createTwoPlayerArguments(defenderClass = rogueWithMaxDodge)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 1,
            secondDie = 2,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedDefender =
            outcome.getPlayerById(userId = currentAction.defenderUserId).characterClass
        val updatedDodge =
            assertIs<DefensiveAbility.Dodge>(value = updatedDefender.defensiveAbility)
        assertEquals(expected = startingArmorBonus, actual = updatedDodge.armorBonus)
        assertEquals(
            expected = TestCharacterClasses.rogue().armorClass + startingArmorBonus,
            actual = updatedDefender.armorClass,
        )
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When Gambler uses All-In and rolls a 1, then the Gambler is knocked out`() {
        // Arrange
        val gambler = TestCharacterClasses.gambler()
        val arguments = createTwoPlayerArguments(attackerClass = gambler)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getAllInAction(gambler = gambler)
        val gamblerUserId = currentAction.attackTurn.attackerUserId

        setupOneDiceRoll(userId = gamblerUserId, die = 1)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedGambler = outcome.getPlayerById(userId = gamblerUserId).characterClass
        assertEquals(expected = 0, actual = updatedGambler.hitPoints)
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
        assertTrue(actual = outcome.isMatchFinished)
    }

    @Test
    fun `When Gambler uses All-In and rolls a 6, then the Gambler gets another attack`() {
        // Arrange
        val gambler = TestCharacterClasses.gambler()
        val arguments = createTwoPlayerArguments(attackerClass = gambler)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getAllInAction(gambler = gambler)
        val gamblerUserId = currentAction.attackTurn.attackerUserId

        setupOneDiceRoll(userId = gamblerUserId, die = 6)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedGambler = outcome.getPlayerById(userId = gamblerUserId).characterClass
        val updatedAllIn = assertIs<OffensiveAbility.AllIn>(value = updatedGambler.offensiveAbility)
        assertTrue(actual = updatedAllIn.hasAnotherAttack)
        assertTrue(actual = updatedAllIn.usedThisTurn)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = gamblerUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When Gambler uses All-In and rolls a 3, then nothing happens`() {
        // Arrange
        val gambler = TestCharacterClasses.gambler()
        val arguments = createTwoPlayerArguments(attackerClass = gambler)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getAllInAction(gambler = gambler)
        val gamblerUserId = currentAction.attackTurn.attackerUserId

        setupOneDiceRoll(userId = gamblerUserId, die = 3)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedGambler = outcome.getPlayerById(userId = gamblerUserId).characterClass
        val updatedAllIn = assertIs<OffensiveAbility.AllIn>(value = updatedGambler.offensiveAbility)
        assertTrue(actual = !updatedAllIn.hasAnotherAttack)
        assertTrue(actual = updatedAllIn.usedThisTurn)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = gamblerUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When an Illusionist plays a copied All-In and rolls a 6, then the copied All-In updates and the Impersonate wrapper is kept`() {
        // Arrange
        val illusionist =
            TestCharacterClasses.illusionist()
                .copy(
                    offensiveAbility =
                        OffensiveAbility.Impersonate(impersonatedAbility = OffensiveAbility.AllIn())
                )
        val arguments = createTwoPlayerArguments(attackerClass = illusionist)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction =
            getAbilityRollAction(
                purpose = AbilityAction.Activated.AllIn(allInAbility = OffensiveAbility.AllIn())
            )
        val illusionistUserId = currentAction.attackTurn.attackerUserId

        setupOneDiceRoll(userId = illusionistUserId, die = 6)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updated = outcome.getPlayerById(userId = illusionistUserId).characterClass
        val impersonate = assertIs<OffensiveAbility.Impersonate>(value = updated.offensiveAbility)
        val copiedAllIn = assertIs<OffensiveAbility.AllIn>(value = impersonate.impersonatedAbility)
        assertTrue(actual = copiedAllIn.hasAnotherAttack)
        assertTrue(actual = copiedAllIn.usedThisTurn)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = illusionistUserId))
    }

    @Test
    fun `When an Illusionist with a copied pending second attack finishes their first attack, the wrapper is kept and they attack again`() {
        // Arrange
        val illusionist =
            TestCharacterClasses.illusionist()
                .copy(
                    offensiveAbility =
                        OffensiveAbility.Impersonate(
                            impersonatedAbility =
                                OffensiveAbility.AllIn(hasAnotherAttack = true, usedThisTurn = true)
                        )
                )
        val arguments = createTwoPlayerArguments(attackerClass = illusionist)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()
        val illusionistUserId = currentAction.attackTurn.attackerUserId

        setupTwoDiceRoll(userId = illusionistUserId, firstDie = 1, secondDie = 2)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updated = outcome.getPlayerById(userId = illusionistUserId).characterClass
        val impersonate = assertIs<OffensiveAbility.Impersonate>(value = updated.offensiveAbility)
        val copiedAllIn = assertIs<OffensiveAbility.AllIn>(value = impersonate.impersonatedAbility)
        assertTrue(actual = !copiedAllIn.hasAnotherAttack)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = illusionistUserId))
    }

    @Test
    fun `When a new round begins, an Illusionist who used a copied All-In can use it again`() {
        // Arrange
        val illusionist =
            TestCharacterClasses.illusionist()
                .copy(
                    offensiveAbility =
                        OffensiveAbility.Impersonate(
                            impersonatedAbility = OffensiveAbility.AllIn(usedThisTurn = true)
                        ),
                    hasActedThisRound = true,
                )
        val arguments = createTwoPlayerArguments(defenderClass = illusionist)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 1,
            secondDie = 1,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(actual = outcome.isNewRound)
        val updated = outcome.getPlayerById(userId = DEFAULT_DEFENDER_ID).characterClass
        val impersonate = assertIs<OffensiveAbility.Impersonate>(value = updated.offensiveAbility)
        val copiedAllIn = assertIs<OffensiveAbility.AllIn>(value = impersonate.impersonatedAbility)
        assertTrue(actual = !copiedAllIn.usedThisTurn)
    }

    @Test
    fun `When an Illusionist plays a copied Summon Minion, then the copied minion count grows and the wrapper is kept`() {
        // Arrange
        val illusionist =
            TestCharacterClasses.illusionist()
                .copy(
                    offensiveAbility =
                        OffensiveAbility.Impersonate(
                            impersonatedAbility = OffensiveAbility.SummonMinion()
                        )
                )
        val arguments = createTwoPlayerArguments(attackerClass = illusionist)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction =
            getAbilityRollAction(
                purpose =
                    AbilityAction.Activated.SummonMinion(
                        summonMinionAbility = OffensiveAbility.SummonMinion()
                    )
            )
        val illusionistUserId = currentAction.attackTurn.attackerUserId

        setupOneDiceRoll(userId = illusionistUserId, die = 2)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updated = outcome.getPlayerById(userId = illusionistUserId).characterClass
        val impersonate = assertIs<OffensiveAbility.Impersonate>(value = updated.offensiveAbility)
        val copiedMinions =
            assertIs<OffensiveAbility.SummonMinion>(value = impersonate.impersonatedAbility)
        assertEquals(expected = 1, actual = copiedMinions.skeletonCount)
    }

    @Test
    fun `When someone attacks the Gambler and LuckyCoin stops the attack, it is the next player's turn`() {
        // Arrange
        val gambler = TestCharacterClasses.gambler()
        val arguments = createTwoPlayerArguments(defenderClass = gambler)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val attackerRollTotal = 6
        val currentAction = getLuckyCoinFlipAction(attackerRollTotal = attackerRollTotal)
        val gamblerUserId = currentAction.defenderUserId

        setupOneDiceRoll(userId = gamblerUserId, die = 3)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = gamblerUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When someone attacks the Gambler and LuckyCoin does not stop the attack, the attacker rolls against Gambler's health`() {
        // Arrange
        val gambler = TestCharacterClasses.gambler()
        val arguments = createTwoPlayerArguments(defenderClass = gambler)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val attackerRollTotal = 6
        val currentAction = getLuckyCoinFlipAction(attackerRollTotal = attackerRollTotal)
        val gamblerUserId = currentAction.defenderUserId

        setupOneDiceRoll(userId = gamblerUserId, die = 1)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.NormalAttack)
        assertEquals(actual = nextAction.roll, expected = NormalAttackType.Health)
        assertTrue(
            actual = nextAction.isThisUserNext(userId = currentAction.attackTurn.attackerUserId)
        )
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When someone attacks the Gambler and rolls a 6, then High Roller triggers`() {
        // Arrange
        val gambler = TestCharacterClasses.gambler()
        val arguments = createTwoPlayerArguments(defenderClass = gambler)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val attackerRollTotal = 6
        val currentAction = getLuckyCoinFlipAction(attackerRollTotal = attackerRollTotal)
        val gamblerUserId = currentAction.defenderUserId

        setupOneDiceRoll(userId = gamblerUserId, die = 6)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.AbilityRoll)
        assertTrue(actual = nextAction.purpose is AbilityAction.Passive.LuckyCoin.HighRoller)
        assertTrue(actual = nextAction.isThisUserNext(userId = gamblerUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `Given a 2-player game, when a Gambler uses High Roller and deals damage that knocks out the attacker, then the match ends`() {
        // Arrange
        val gambler = TestCharacterClasses.gambler()
        val arguments = createTwoPlayerArguments(defenderClass = gambler)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val attackerRollTotal = 10
        val currentAction = getLuckyCoinHighRollerAction(attackerRollTotal = attackerRollTotal)
        val gamblerUserId = currentAction.defenderUserId

        setupOneDiceRoll(userId = gamblerUserId, die = 6)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedAttacker =
            outcome.getPlayerById(userId = currentAction.attackTurn.attackerUserId).characterClass
        assertTrue(actual = updatedAttacker.hitPoints <= 0)
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
        assertTrue(actual = outcome.isMatchFinished)
    }

    @Test
    fun `When a Gambler uses High Roller and deals damage but the attacker survives, then the attacker rolls against health`() {
        // Arrange
        val gambler = TestCharacterClasses.gambler()
        val attackerClass = createCharacterClass(hitPoints = 10)
        val arguments =
            createTwoPlayerArguments(attackerClass = attackerClass, defenderClass = gambler)

        val performCombatAction = getPerformCombatAction(arguments = arguments)

        val attackerRollTotal = 10
        val currentAction = getLuckyCoinHighRollerAction(attackerRollTotal = attackerRollTotal)
        val attackerUserId = currentAction.attackTurn.attackerUserId
        val gamblerUserId = currentAction.defenderUserId

        setupOneDiceRoll(userId = gamblerUserId, die = 4)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedAttacker = outcome.getPlayerById(userId = attackerUserId).characterClass
        assertTrue(actual = updatedAttacker.isConscious())

        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.NormalAttack)
        assertEquals(actual = nextAction.roll, expected = NormalAttackType.Health)
        assertTrue(actual = nextAction.isThisUserNext(userId = attackerUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When Duelist uses Prepare Attack and rolls a 10, then the defender is knocked out`() {
        // Arrange
        val duelist = TestCharacterClasses.duelist()
        val arguments = createTwoPlayerArguments(attackerClass = duelist)

        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getPrepareAttackAction()

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 5,
            secondDie = 5,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedDefender =
            outcome.getPlayerById(userId = currentAction.defenderUserId).characterClass
        assertEquals(expected = 0, actual = updatedDefender.hitPoints)
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
        assertTrue(actual = outcome.isMatchFinished)
    }

    @Test
    fun `When Duelist uses Prepare Attack and rolls a 7, then nothing happens and the turn ends`() {
        // Arrange
        val duelist = TestCharacterClasses.duelist()
        val arguments = createTwoPlayerArguments(attackerClass = duelist)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getPrepareAttackAction()

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 3,
            secondDie = 4,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val defenderUserId = currentAction.defenderUserId
        val updatedDefender = outcome.getPlayerById(userId = defenderUserId).characterClass
        assertTrue(actual = updatedDefender.isConscious())
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = defenderUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When an attacker passes a Duelist's ArmorClass, then Riposte triggers`() {
        // Arrange
        val duelist = TestCharacterClasses.duelist()
        val arguments = createTwoPlayerArguments(defenderClass = duelist)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 5,
            secondDie = 5,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.AbilityRoll)

        val purpose = nextAction.purpose
        assertTrue(actual = purpose is AbilityAction.Passive.Riposte)
        assertEquals(expected = 10, actual = purpose.attackerRollTotal)
        assertTrue(actual = nextAction.isThisUserNext(userId = currentAction.defenderUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When a Duelist rolls a successful Riposte, then the Duelist attacks the attacker`() {
        // Arrange
        val duelist = TestCharacterClasses.duelist()
        val arguments = createTwoPlayerArguments(defenderClass = duelist)

        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val attackerRollTotal = 10
        val currentAction = getRiposteAction(attackerRollTotal = attackerRollTotal)
        val duelistUserId = currentAction.defenderUserId
        val initialAttackerUserId = currentAction.attackTurn.attackerUserId

        setupTwoDiceRoll(userId = duelistUserId, firstDie = 3, secondDie = 4)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.NormalAttack)
        assertEquals(actual = nextAction.roll, expected = NormalAttackType.ArmorClass)
        assertEquals(expected = duelistUserId, actual = nextAction.attackTurn.attackerUserId)
        assertEquals(expected = initialAttackerUserId, actual = nextAction.defenderUserId)
        assertEquals(
            expected = initialAttackerUserId,
            actual = nextAction.attackTurn.turnOwnerUserId,
        )
        assertTrue(actual = nextAction.isThisUserNext(userId = duelistUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When a Duelist rolls a failed Riposte, then the attacker rolls against the Duelist's health`() {
        // Arrange
        val duelist = TestCharacterClasses.duelist()
        val arguments = createTwoPlayerArguments(defenderClass = duelist)

        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val attackerRollTotal = 10
        val currentAction = getRiposteAction(attackerRollTotal = attackerRollTotal)
        val duelistUserId = currentAction.defenderUserId
        val initialAttackerUserId = currentAction.attackTurn.attackerUserId

        // total 4 (outside 6..8)
        setupTwoDiceRoll(userId = duelistUserId, firstDie = 2, secondDie = 2)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.NormalAttack)
        assertEquals(actual = nextAction.roll, expected = NormalAttackType.Health)
        assertEquals(
            expected = initialAttackerUserId,
            actual = nextAction.attackTurn.attackerUserId,
        )
        assertEquals(expected = duelistUserId, actual = nextAction.defenderUserId)
        assertTrue(actual = nextAction.isThisUserNext(userId = initialAttackerUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `Given a 3-player game, when a Duelist performs a Riposte counter-attack and the turn ends, then the Duelist acts next if they have the highest remaining speed`() {
        // Arrange
        val attackerClass = createCharacterClass()
        val duelist = TestCharacterClasses.duelist()
        val attackerUserId = DEFAULT_ATTACKER_ID
        val duelistUserId = DEFAULT_DEFENDER_ID
        val thirdClass = createCharacterClass(speed = 5)

        val arguments =
            createThreePlayerArguments(
                attackerUserId = duelistUserId,
                defenderUserId = attackerUserId,
                attackerClass = duelist,
                defenderClass = attackerClass,
                thirdClass = thirdClass,
            )
        val performCombatAction = getPerformCombatAction(arguments = arguments)

        // Counter-attack AC roll: the Duelist attacks, but the original turn still belongs to the
        // initial attacker
        val currentAction =
            getNormalAttackAction(
                attackerUserId = duelistUserId,
                defenderUserId = attackerUserId,
                originalTurnUserId = attackerUserId,
            )

        // Set up a miss to end the sequence
        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 1,
            secondDie = 1,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        // Original attacker should be marked as acted
        assertTrue(
            actual =
                outcome
                    .getPlayerById(userId = currentAction.attackTurn.turnOwnerUserId)
                    .characterClass
                    .hasActedThisRound
        )

        // Turn should move to Duelist because they are fastest remaining (8 > 5)
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.Idle)
        assertTrue(actual = nextAction.isThisUserNext(userId = duelistUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When Necromancer uses Summon Minion and rolls a 1, then no minion is summoned and no healing occurs`() {
        summonMinionTest(startingHitPoints = 3, expectedHitPoints = 3, dieRoll = 1)
    }

    @Test
    fun `When Necromancer uses Summon Minion and rolls a 2, then a skeleton is summoned`() {
        summonMinionTest(
            startingHitPoints = 3,
            expectedHitPoints = 3,
            dieRoll = 2,
            expectedSkeletonCount = 1,
        )
    }

    @Test
    fun `When Necromancer uses Summon Minion and rolls a 4, then a skeleton is summoned and Necromancer heals to full`() {
        summonMinionTest(
            startingHitPoints = 3,
            expectedHitPoints = 7,
            dieRoll = 4,
            expectedSkeletonCount = 1,
        )
    }

    @Test
    fun `When Necromancer uses Summon Minion and rolls a 6, then a golem is summoned and Necromancer heals to full`() {
        summonMinionTest(
            startingHitPoints = 3,
            expectedHitPoints = 7,
            dieRoll = 6,
            expectedGolemCount = 1,
        )
    }

    @Test
    fun `When Necromancer already has a skeleton and summons another, the skeleton count increases to 2`() {
        summonMinionTest(
            startingHitPoints = 3,
            expectedHitPoints = 3,
            dieRoll = 2,
            startingSkeletonCount = 1,
            expectedSkeletonCount = 2,
        )
    }

    private fun summonMinionTest(
        startingHitPoints: Int,
        expectedHitPoints: Int,
        dieRoll: Int,
        startingSkeletonCount: Int = 0,
        expectedSkeletonCount: Int = 0,
        expectedGolemCount: Int = 0,
    ) {
        // Arrange
        val necromancer =
            TestCharacterClasses.necromancer()
                .copy(
                    hitPoints = startingHitPoints,
                    offensiveAbility =
                        OffensiveAbility.SummonMinion(skeletonCount = startingSkeletonCount),
                )
        val arguments = createTwoPlayerArguments(attackerClass = necromancer)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getSummonMinionAction(necromancer = necromancer)
        val necromancerUserId = currentAction.attackTurn.attackerUserId

        setupOneDiceRoll(userId = necromancerUserId, die = dieRoll)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedNecro = outcome.getPlayerById(userId = necromancerUserId).characterClass
        val updatedSummon =
            assertIs<OffensiveAbility.SummonMinion>(value = updatedNecro.offensiveAbility)
        assertEquals(expected = expectedSkeletonCount, actual = updatedSummon.skeletonCount)
        assertEquals(expected = expectedGolemCount, actual = updatedSummon.golemCount)
        assertEquals(expected = expectedHitPoints, actual = updatedNecro.hitPoints)
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
        assertTrue(
            actual = outcome.nextCombatAction.isThisUserNext(userId = currentAction.defenderUserId)
        )
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When a Necromancer with skeletons is hit, then the shields absorb damage before base hit points`() {
        // Arrange
        val necromancer =
            TestCharacterClasses.necromancer()
                .copy(
                    hitPoints = 7,
                    offensiveAbility = OffensiveAbility.SummonMinion(skeletonCount = 2),
                )
        val arguments = createTwoPlayerArguments(defenderClass = necromancer)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 2,
            secondDie = 2,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedNecro =
            outcome.getPlayerById(userId = currentAction.defenderUserId).characterClass
        val updatedSummon =
            assertIs<OffensiveAbility.SummonMinion>(value = updatedNecro.offensiveAbility)
        assertEquals(expected = 0, actual = updatedSummon.skeletonCount)
        assertEquals(expected = 5, actual = updatedNecro.hitPoints)
        assertTrue(actual = updatedNecro.isConscious())
    }

    @Test
    fun `When a Necromancer holding a skeleton finishes their first attack, they attack again`() {
        // Arrange
        val necromancer =
            TestCharacterClasses.necromancer()
                .copy(offensiveAbility = OffensiveAbility.SummonMinion(skeletonCount = 1))
        val arguments = createTwoPlayerArguments(attackerClass = necromancer)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()
        val necromancerUserId = currentAction.attackTurn.attackerUserId

        setupTwoDiceRoll(userId = necromancerUserId, firstDie = 1, secondDie = 2)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedNecro = outcome.getPlayerById(userId = necromancerUserId).characterClass
        val updatedSummon =
            assertIs<OffensiveAbility.SummonMinion>(value = updatedNecro.offensiveAbility)
        assertEquals(expected = 1, actual = updatedSummon.attacksTakenThisTurn)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = necromancerUserId))
    }

    @Test
    fun `When a Necromancer holding a golem finishes their first attack, they attack again`() {
        // Arrange
        val necromancer =
            TestCharacterClasses.necromancer()
                .copy(offensiveAbility = OffensiveAbility.SummonMinion(golemCount = 1))
        val arguments = createTwoPlayerArguments(attackerClass = necromancer)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()
        val necromancerUserId = currentAction.attackTurn.attackerUserId

        setupTwoDiceRoll(userId = necromancerUserId, firstDie = 1, secondDie = 2)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedNecro = outcome.getPlayerById(userId = necromancerUserId).characterClass
        val updatedSummon =
            assertIs<OffensiveAbility.SummonMinion>(value = updatedNecro.offensiveAbility)
        assertEquals(expected = 1, actual = updatedSummon.attacksTakenThisTurn)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = necromancerUserId))
    }

    @Test
    fun `When a Necromancer exhausts their minion attacks, the turn passes to the next player`() {
        // Arrange
        val necromancer =
            TestCharacterClasses.necromancer()
                .copy(
                    offensiveAbility =
                        OffensiveAbility.SummonMinion(skeletonCount = 1, attacksTakenThisTurn = 1)
                )
        val arguments = createTwoPlayerArguments(attackerClass = necromancer)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()
        val necromancerUserId = currentAction.attackTurn.attackerUserId

        setupTwoDiceRoll(userId = necromancerUserId, firstDie = 1, secondDie = 2)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(
            actual = outcome.nextCombatAction.isThisUserNext(userId = currentAction.defenderUserId)
        )
    }

    @Test
    fun `When a turn passes to a Necromancer holding minions, their minion attacks reset`() {
        // Arrange
        val necromancer =
            TestCharacterClasses.necromancer()
                .copy(
                    offensiveAbility =
                        OffensiveAbility.SummonMinion(skeletonCount = 2, attacksTakenThisTurn = 2)
                )
        val arguments = createTwoPlayerArguments(defenderClass = necromancer)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()
        val necromancerUserId = currentAction.defenderUserId

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 1,
            secondDie = 1,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedNecro = outcome.getPlayerById(userId = necromancerUserId).characterClass
        val updatedSummon =
            assertIs<OffensiveAbility.SummonMinion>(value = updatedNecro.offensiveAbility)
        assertEquals(expected = 0, actual = updatedSummon.attacksTakenThisTurn)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = necromancerUserId))
    }

    @Test
    fun `When a Necromancer summons a minion, they forgo their minion attacks this turn`() {
        // Arrange
        val necromancer =
            TestCharacterClasses.necromancer()
                .copy(offensiveAbility = OffensiveAbility.SummonMinion(skeletonCount = 1))
        val arguments = createTwoPlayerArguments(attackerClass = necromancer)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getSummonMinionAction(necromancer = necromancer)
        val necromancerUserId = currentAction.attackTurn.attackerUserId

        setupOneDiceRoll(userId = necromancerUserId, die = 1)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(
            actual = outcome.nextCombatAction.isThisUserNext(userId = currentAction.defenderUserId)
        )
    }

    @Test
    fun `When an Illusionist impersonating Summon Minion holds a skeleton, they get a follow-up attack`() {
        // Arrange
        val illusionist =
            TestCharacterClasses.illusionist()
                .copy(
                    offensiveAbility =
                        OffensiveAbility.Impersonate(
                            impersonatedAbility = OffensiveAbility.SummonMinion(skeletonCount = 1)
                        )
                )
        val arguments = createTwoPlayerArguments(attackerClass = illusionist)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()
        val illusionistUserId = currentAction.attackTurn.attackerUserId

        setupTwoDiceRoll(userId = illusionistUserId, firstDie = 1, secondDie = 2)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updated = outcome.getPlayerById(userId = illusionistUserId).characterClass
        val impersonate = assertIs<OffensiveAbility.Impersonate>(value = updated.offensiveAbility)
        val copiedMinions =
            assertIs<OffensiveAbility.SummonMinion>(value = impersonate.impersonatedAbility)
        assertEquals(expected = 1, actual = copiedMinions.attacksTakenThisTurn)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = illusionistUserId))
    }

    @Test
    fun `When Bard deals damage, then Encore triggers`() {
        // Arrange
        val bard = TestCharacterClasses.bard()
        val arguments = createTwoPlayerArguments(attackerClass = bard)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()
        val bardUserId = currentAction.attackTurn.attackerUserId

        setupTwoDiceRoll(userId = bardUserId, firstDie = 1, secondDie = 2)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.AbilityRoll)
        assertTrue(actual = nextAction.purpose is AbilityAction.Passive.Encore)
        assertTrue(actual = nextAction.isThisUserNext(userId = bardUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When Bard rolls a successful Encore, then the Bard acts again`() {
        encoreRollTest(dieRoll = 5, expectedNextUserId = DEFAULT_ATTACKER_ID)
    }

    @Test
    fun `When Bard rolls a failed Encore, then the turn ends`() {
        encoreRollTest(dieRoll = 4, expectedNextUserId = DEFAULT_DEFENDER_ID)
    }

    private fun encoreRollTest(dieRoll: Int, expectedNextUserId: String) {
        // Arrange
        val arguments = createTwoPlayerArguments(attackerClass = TestCharacterClasses.bard())
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getAbilityRollAction(purpose = AbilityAction.Passive.Encore)

        setupOneDiceRoll(userId = currentAction.attackTurn.attackerUserId, die = dieRoll)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = expectedNextUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When an attacker passes a Bard's ArmorClass, then Persuade triggers`() {
        // Arrange
        val bard = TestCharacterClasses.bard()
        val arguments = createTwoPlayerArguments(defenderClass = bard)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 5,
            secondDie = 5,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.AbilityRoll)
        assertTrue(actual = nextAction.purpose is AbilityAction.Passive.Persuade.StrumLute)
        assertTrue(actual = nextAction.isThisUserNext(userId = currentAction.defenderUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When a Bard rolls a failed Persuade, then the attacker rolls against the Bard's health`() {
        persuadeRollTest(dieRoll = 4, expectedNextUserId = DEFAULT_ATTACKER_ID)
    }

    @Test
    fun `When a Bard rolls a successful Persuade (Lull), then the turn ends`() {
        persuadeRollTest(dieRoll = 5, expectedNextUserId = DEFAULT_DEFENDER_ID)
    }

    @Test
    fun `When a Bard rolls a 6 for Persuade in a 2-player game, then the turn ends`() {
        // In 2-player game, 6 behaves like 5 (Lull) because Charm needs at least 3 players
        persuadeRollTest(dieRoll = 6, expectedNextUserId = DEFAULT_DEFENDER_ID)
    }

    @Test
    fun `When a Bard rolls a 6 for Persuade in a 3-player game, then the Bard chooses a new target for the attacker`() {
        persuadeRollTest(
            dieRoll = 6,
            expectedNextUserId = DEFAULT_DEFENDER_ID,
            isThreePlayer = true,
        )
    }

    @Test
    fun `When a Bard selects a Charm target, then the attacker performs a new ArmorClass roll against that target and hits`() {
        // Arrange
        val bard = TestCharacterClasses.bard()
        val arguments = createThreePlayerArguments(thirdClass = bard)
        val performCombatAction = getPerformCombatAction(arguments = arguments)

        val newTargetUserId = DEFAULT_DEFENDER_ID
        val charmPurpose =
            AbilityAction.Passive.Persuade.Charm(
                targetableUserIds = setOf(newTargetUserId),
                selectedCharmTargetUserId = newTargetUserId,
            )
        val currentAction =
            getAbilityRollAction(purpose = charmPurpose, defenderUserId = THIRD_PLAYER_ID)
        val charmedAttackerUserId = currentAction.attackTurn.attackerUserId

        // Act 1: Persuade charms the attacker into attacking the Bard's chosen target instead
        val outcome1 = performCombatAction.invoke(currentAction = currentAction)

        // Assert 1: The attacker rolls a new ArmorClass attack against the new target
        val nextAction1 = outcome1.nextCombatAction
        assertTrue(actual = nextAction1 is NextCombatAction.NormalAttack)
        assertEquals(actual = nextAction1.roll, expected = NormalAttackType.ArmorClass)
        assertEquals(
            expected = charmedAttackerUserId,
            actual = nextAction1.attackTurn.attackerUserId,
        )
        assertEquals(expected = newTargetUserId, actual = nextAction1.defenderUserId)
        assertTrue(actual = nextAction1.isThisUserNext(userId = charmedAttackerUserId))

        // Act 2: The redirected attack hits the new target's armor
        setupTwoDiceRoll(userId = charmedAttackerUserId, firstDie = 4, secondDie = 4)
        val performCombatAction2 = getPerformCombatAction(arguments = arguments)
        val outcome2 =
            performCombatAction2.invoke(
                currentAction =
                    getNormalAttackAction(
                        attackerUserId = charmedAttackerUserId,
                        defenderUserId = newTargetUserId,
                    )
            )

        // Assert 2: The attack continues against the new target's health
        val nextAction2 = outcome2.nextCombatAction
        assertTrue(actual = nextAction2 is NextCombatAction.NormalAttack)
        assertEquals(actual = nextAction2.roll, expected = NormalAttackType.Health)
        assertEquals(
            expected = charmedAttackerUserId,
            actual = nextAction2.attackTurn.attackerUserId,
        )
        assertEquals(expected = newTargetUserId, actual = nextAction2.defenderUserId)
        assertTrue(actual = nextAction2.isThisUserNext(userId = charmedAttackerUserId))
        assertTrue(actual = !outcome2.isMatchFinished)
    }

    @Test
    fun `When a Bard selects a Charm target, then the attacker performs a new ArmorClass roll against that target and misses`() {
        // Arrange
        val bard = TestCharacterClasses.bard()
        val arguments = createThreePlayerArguments(thirdClass = bard)
        val performCombatAction = getPerformCombatAction(arguments = arguments)

        val newTargetUserId = DEFAULT_DEFENDER_ID
        val charmPurpose =
            AbilityAction.Passive.Persuade.Charm(
                targetableUserIds = setOf(newTargetUserId),
                selectedCharmTargetUserId = newTargetUserId,
            )
        val currentAction =
            getAbilityRollAction(purpose = charmPurpose, defenderUserId = THIRD_PLAYER_ID)
        val charmedAttackerUserId = currentAction.attackTurn.attackerUserId

        // Act 1: Persuade charms the attacker into attacking the Bard's chosen target instead
        val outcome1 = performCombatAction.invoke(currentAction = currentAction)

        // Assert 1: The attacker rolls a new ArmorClass attack against the new target
        val nextAction1 = outcome1.nextCombatAction
        assertTrue(actual = nextAction1 is NextCombatAction.NormalAttack)
        assertEquals(actual = nextAction1.roll, expected = NormalAttackType.ArmorClass)
        assertEquals(expected = newTargetUserId, actual = nextAction1.defenderUserId)

        // Act 2: The redirected attack misses the new target's armor
        setupTwoDiceRoll(userId = charmedAttackerUserId, firstDie = 1, secondDie = 1)
        val performCombatAction2 = getPerformCombatAction(arguments = arguments)
        val outcome2 =
            performCombatAction2.invoke(
                currentAction =
                    getNormalAttackAction(
                        attackerUserId = charmedAttackerUserId,
                        defenderUserId = newTargetUserId,
                    )
            )

        // Assert 2: The turn ends and the next player is active
        val nextAction2 = outcome2.nextCombatAction
        assertTrue(actual = nextAction2 is NextCombatAction.Idle)
        assertTrue(actual = nextAction2.isThisUserNext(userId = newTargetUserId))
        assertTrue(actual = !outcome2.isMatchFinished)
    }

    private fun persuadeRollTest(
        dieRoll: Int,
        expectedNextUserId: String,
        isThreePlayer: Boolean = false,
    ) {
        // Arrange
        val bard = TestCharacterClasses.bard()
        val arguments =
            if (isThreePlayer) {
                createThreePlayerArguments(defenderClass = bard)
            } else {
                createTwoPlayerArguments(defenderClass = bard)
            }
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getStrumLuteAction()

        setupOneDiceRoll(userId = currentAction.defenderUserId, die = dieRoll)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        when (dieRoll) {
            in 1..4 -> {
                assertTrue(actual = nextAction is NextCombatAction.NormalAttack)
                assertEquals(actual = nextAction.roll, expected = NormalAttackType.Health)
            }

            5 -> assertTrue(actual = nextAction is NextCombatAction.Idle)

            6 ->
                if (isThreePlayer) {
                    assertTrue(actual = nextAction is NextCombatAction.AbilityRoll)
                    assertTrue(actual = nextAction.purpose is AbilityAction.Passive.Persuade.Charm)
                } else {
                    assertTrue(actual = nextAction is NextCombatAction.Idle)
                }
        }

        assertTrue(actual = nextAction.isThisUserNext(userId = expectedNextUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When Illusionist uses Impersonate, then the Illusionist's offensive ability is updated and they can act again`() {
        impersonateTest(abilityToImpersonate = OffensiveAbility.PrepareAttack)
    }

    @Test
    fun `When Illusionist uses Impersonate but already has an impersonated ability, it is replaced with the new one`() {
        impersonateTest(
            startingAbility =
                OffensiveAbility.Impersonate(impersonatedAbility = OffensiveAbility.PrepareAttack),
            abilityToImpersonate = OffensiveAbility.AllIn(),
        )
    }

    @Test
    fun `When Illusionist impersonates a Necromancer that already has a skeleton, then only the behavior is copied and no minions come across`() {
        // Arrange
        val illusionist = TestCharacterClasses.illusionist()
        val arguments = createTwoPlayerArguments(attackerClass = illusionist)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction =
            getImpersonateAction(
                abilityToImpersonate =
                    OffensiveAbility.SummonMinion(skeletonCount = 1, golemCount = 0)
            )
        val illusionistUserId = currentAction.attackTurn.attackerUserId

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedIllusionist = outcome.getPlayerById(userId = illusionistUserId).characterClass
        val updatedImpersonate =
            assertIs<OffensiveAbility.Impersonate>(value = updatedIllusionist.offensiveAbility)
        val copiedMinions =
            assertIs<OffensiveAbility.SummonMinion>(value = updatedImpersonate.impersonatedAbility)
        assertEquals(expected = 0, actual = copiedMinions.skeletonCount)
        assertEquals(expected = 0, actual = copiedMinions.golemCount)
    }

    private fun impersonateTest(
        startingAbility: OffensiveAbility = OffensiveAbility.Impersonate(),
        abilityToImpersonate: OffensiveAbility,
    ) {
        // Arrange
        val illusionist =
            TestCharacterClasses.illusionist().copy(offensiveAbility = startingAbility)
        val arguments = createTwoPlayerArguments(attackerClass = illusionist)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getImpersonateAction(abilityToImpersonate = abilityToImpersonate)
        val illusionistUserId = currentAction.attackTurn.attackerUserId

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedIllusionist = outcome.getPlayerById(userId = illusionistUserId).characterClass
        val updatedImpersonate =
            assertIs<OffensiveAbility.Impersonate>(value = updatedIllusionist.offensiveAbility)

        assertEquals(
            expected = abilityToImpersonate,
            actual = updatedImpersonate.impersonatedAbility,
        )
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = illusionistUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When an attacker hits an Illusionist's ArmorClass, then Mirror Image triggers`() {
        // Arrange
        val illusionist = TestCharacterClasses.illusionist()
        val arguments = createTwoPlayerArguments(defenderClass = illusionist)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 3,
            secondDie = 4,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.AbilityRoll)
        val purpose = assertIs<AbilityAction.Passive.MirrorImage>(value = nextAction.purpose)
        assertEquals(expected = 3, actual = purpose.firstDie.originalRoll)
        assertEquals(expected = 4, actual = purpose.secondDie.originalRoll)
        assertEquals(expected = NormalAttackType.ArmorClass, actual = purpose.attackType)
        assertTrue(actual = nextAction.isThisUserNext(userId = currentAction.defenderUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When an Illusionist uses Mirror Image to reroll and attacker misses, then the turn ends and the ability is reset`() {
        val mirrorImagePurpose =
            AbilityAction.Passive.MirrorImage(
                firstDie = AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 3),
                secondDie = AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 3),
                attackType = NormalAttackType.ArmorClass,
                attackerDoublesBonus = 0,
                mirrorImageDefensiveAbility = DefensiveAbility.MirrorImage(),
                mirrorImageRerollSelection = MirrorImageRerollSelection.First,
            )

        // Reroll of 2 becomes a final value of 1, so the total of 4 misses the Illusionist's armor.
        mirrorImageTest(
            mirrorImagePurpose = mirrorImagePurpose,
            rerollDie = 2,
            expectedUsedThisAttack = false,
            expectedIsNextActionIdle = true,
        )
    }

    @Test
    fun `When an Illusionist passes on Mirror Image during ArmorClass, the attacker then rolls against the Health`() {
        val mirrorImagePurpose =
            AbilityAction.Passive.MirrorImage(
                firstDie = AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 3),
                secondDie = AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 3),
                attackType = NormalAttackType.ArmorClass,
                attackerDoublesBonus = 0,
                mirrorImageDefensiveAbility = DefensiveAbility.MirrorImage(),
            )

        mirrorImageTest(
            mirrorImagePurpose = mirrorImagePurpose,
            expectedUsedThisAttack = false,
            expectedIsNextActionNormalAttack = true,
        )
    }

    @Test
    fun `When an Illusionist uses Mirror Image during ArmorClass, they cannot use it again during the Health roll`() {
        val mirrorImagePurpose =
            AbilityAction.Passive.MirrorImage(
                firstDie = AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 3),
                secondDie = AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 3),
                attackType = NormalAttackType.ArmorClass,
                attackerDoublesBonus = 0,
                mirrorImageDefensiveAbility = DefensiveAbility.MirrorImage(),
                mirrorImageRerollSelection = MirrorImageRerollSelection.First,
            )

        // Reroll of 6 becomes a final value of 5, so the total of 8 breaks the Illusionist's armor.
        mirrorImageTest(
            mirrorImagePurpose = mirrorImagePurpose,
            rerollDie = 6,
            expectedUsedThisAttack = true,
            expectedIsNextActionNormalAttack = true,
        )
    }

    @Test
    fun `When an Illusionist keeps a Mirror Image ArmorClass roll, the attacker's carried doubles bonus still breaks armor`() {
        val mirrorImagePurpose =
            AbilityAction.Passive.MirrorImage(
                firstDie = AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 1),
                secondDie = AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 3),
                attackType = NormalAttackType.ArmorClass,
                attackerDoublesBonus = 1,
                mirrorImageDefensiveAbility = DefensiveAbility.MirrorImage(),
            )

        mirrorImageTest(
            mirrorImagePurpose = mirrorImagePurpose,
            expectedUsedThisAttack = false,
            expectedIsNextActionNormalAttack = true,
        )
    }

    @Test
    fun `When an Illusionist rerolls a Mirror Image ArmorClass roll, the attacker's carried doubles bonus still breaks armor`() {
        val mirrorImagePurpose =
            AbilityAction.Passive.MirrorImage(
                firstDie = AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 2),
                secondDie = AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 2),
                attackType = NormalAttackType.ArmorClass,
                attackerDoublesBonus = 1,
                mirrorImageDefensiveAbility = DefensiveAbility.MirrorImage(),
                mirrorImageRerollSelection = MirrorImageRerollSelection.Second,
            )

        mirrorImageTest(
            mirrorImagePurpose = mirrorImagePurpose,
            rerollDie = 3,
            expectedUsedThisAttack = true,
            expectedIsNextActionNormalAttack = true,
        )
    }

    private fun mirrorImageTest(
        mirrorImagePurpose: AbilityAction.Passive.MirrorImage,
        expectedUsedThisAttack: Boolean,
        rerollDie: Int? = null,
        expectedIsNextActionIdle: Boolean = false,
        expectedIsNextActionNormalAttack: Boolean = false,
    ) {
        // Arrange
        val arguments = createTwoPlayerArguments(defenderClass = TestCharacterClasses.illusionist())
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getAbilityRollAction(purpose = mirrorImagePurpose)

        if (rerollDie != null) {
            setupOneDiceRoll(userId = currentAction.defenderUserId, die = rerollDie)
        }

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedIllusionist =
            outcome.getPlayerById(userId = currentAction.defenderUserId).characterClass
        val updatedMirrorImage =
            assertIs<DefensiveAbility.MirrorImage>(value = updatedIllusionist.defensiveAbility)
        assertEquals(expected = expectedUsedThisAttack, actual = updatedMirrorImage.usedThisAttack)
        assertTrue(actual = !outcome.isMatchFinished)

        when (val nextAction = outcome.nextCombatAction) {
            is NextCombatAction.AbilityRoll ->
                assertTrue(actual = false, message = "Something went wrong")

            is NextCombatAction.Idle -> assertTrue(actual = expectedIsNextActionIdle)

            is NextCombatAction.NormalAttack -> {
                assertTrue(actual = expectedIsNextActionNormalAttack)
                assertEquals(actual = nextAction.roll, expected = NormalAttackType.Health)
            }
        }
    }

    @Test
    fun `When Mage rolls doubles during ArmorClass roll, then Spell Potency triggers, deals damage, and turn ends`() {
        spellPotencyTest(attackRoll = NormalAttackType.ArmorClass, dieValue = 3)
    }

    @Test
    fun `When Mage rolls doubles during Health roll, then Spell Potency triggers, deals damage, and turn ends`() {
        spellPotencyTest(attackRoll = NormalAttackType.Health, dieValue = 4)
    }

    @Test
    fun `When Mage rolls doubles in a 3-player game, then Spell Potency deals splash damage to the third player`() {
        spellPotencyTest(
            attackRoll = NormalAttackType.ArmorClass,
            dieValue = 2,
            isThreePlayer = true,
        )
    }

    private fun spellPotencyTest(
        attackRoll: NormalAttackType,
        dieValue: Int,
        isThreePlayer: Boolean = false,
    ) {
        // Arrange
        val mage = TestCharacterClasses.mage()
        val arguments =
            if (isThreePlayer) {
                createThreePlayerArguments(attackerClass = mage)
            } else {
                createTwoPlayerArguments(attackerClass = mage)
            }
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction =
            CurrentCombatAction.NormalAttack(
                roll = attackRoll,
                attackTurn = AttackTurn.Standard(attackerUserId = DEFAULT_ATTACKER_ID),
                defenderUserId = DEFAULT_DEFENDER_ID,
            )
        val defenderUserId = currentAction.defenderUserId

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = dieValue,
            secondDie = dieValue,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val diceTotal = dieValue * 2

        // Main target hit points
        val updatedDefender = outcome.getPlayerById(userId = defenderUserId).characterClass
        val initialDefenderHp =
            arguments.playerMap.getValue(defenderUserId).characterClass.hitPoints
        assertEquals(expected = initialDefenderHp - diceTotal, actual = updatedDefender.hitPoints)

        // The casting mage never takes its own splash damage (SS-054)
        val updatedMage =
            outcome.getPlayerById(userId = currentAction.attackTurn.attackerUserId).characterClass
        val initialMageHp =
            arguments.playerMap
                .getValue(currentAction.attackTurn.attackerUserId)
                .characterClass
                .hitPoints
        assertEquals(expected = initialMageHp, actual = updatedMage.hitPoints)

        // Splash damage (if 3 players)
        if (isThreePlayer) {
            val thirdPlayer = outcome.getPlayerById(userId = THIRD_PLAYER_ID).characterClass
            val initialThirdHp =
                arguments.playerMap.getValue(THIRD_PLAYER_ID).characterClass.hitPoints
            assertEquals(expected = initialThirdHp - dieValue, actual = thirdPlayer.hitPoints)
        }

        // Spell Potency message reports the mage, target, full damage, and splash amount
        val spellPotencyMessage =
            assertIs<ArenaMessage.SpellPotency>(
                value = outcome.messages.single { it is ArenaMessage.SpellPotency }
            )
        assertEquals(
            expected = currentAction.attackTurn.attackerUserId,
            actual = spellPotencyMessage.mageUserId,
        )
        assertEquals(expected = defenderUserId, actual = spellPotencyMessage.mainTargetUserId)
        assertEquals(expected = diceTotal, actual = spellPotencyMessage.mainDamage)

        // The message carries the doubles that were rolled so the dice can settle on them
        assertEquals(expected = dieValue, actual = spellPotencyMessage.firstDie)
        assertEquals(expected = dieValue, actual = spellPotencyMessage.secondDie)

        // With no other opponents to splash (2-player), the message carries 0 splash (SS-115)
        val expectedSplashDamage = if (isThreePlayer) diceTotal / 2 else 0
        assertEquals(expected = expectedSplashDamage, actual = spellPotencyMessage.splashDamage)

        // The splashed opponents are named so their head icons can decrement (SS-214)
        val expectedSplashTargets = if (isThreePlayer) listOf(THIRD_PLAYER_ID) else emptyList()
        assertEquals(
            expected = expectedSplashTargets,
            actual = spellPotencyMessage.splashTargetUserIds,
        )

        // Turn ends
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
        assertEquals(expected = !isThreePlayer, actual = outcome.isMatchFinished)
    }

    @Test
    fun `When an attacker passes a Mage's ArmorClass, then Teleport triggers`() {
        // Arrange
        val mage = TestCharacterClasses.mage()
        val arguments = createTwoPlayerArguments(defenderClass = mage)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(userId = currentAction.attackTurn.attackerUserId)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        // Both combatants roll simultaneously (SS-169): each holds their own roll button at once,
        // so both the attacker and the Mage are active.
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.AbilityRoll)
        assertTrue(actual = nextAction.purpose is AbilityAction.Passive.Teleport)
        assertTrue(
            actual = nextAction.isThisUserNext(userId = currentAction.attackTurn.attackerUserId)
        )
        assertTrue(actual = nextAction.isThisUserNext(userId = currentAction.defenderUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When an attacker rolls for Teleport, then the Teleport action is updated with the attacker's roll`() {
        // Arrange
        val arguments = createTwoPlayerArguments(defenderClass = TestCharacterClasses.mage())
        val performCombatAction = getPerformCombatAction(arguments = arguments)

        val purpose =
            AbilityAction.Passive.Teleport(
                attackerRoll = TeleportDie(userId = DEFAULT_ATTACKER_ID),
                defenderRoll = TeleportDie(userId = DEFAULT_DEFENDER_ID),
                attackerDoublesBonus = 0,
            )
        val currentAction = getAbilityRollAction(purpose = purpose)

        val attackerRoll = 4
        setupOneDiceRoll(userId = currentAction.attackTurn.attackerUserId, die = attackerRoll)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.AbilityRoll)
        val teleport = assertIs<AbilityAction.Passive.Teleport>(value = nextAction.purpose)
        // The acting attacker rolled only their own die (SS-169); the Mage's die is still unrolled.
        assertEquals(expected = attackerRoll, actual = teleport.attackerRoll.roll)
        assertEquals(expected = null, actual = teleport.defenderRoll.roll)
        // Both combatants stay active until each has rolled their own die.
        assertTrue(actual = nextAction.isThisUserNext(userId = currentAction.defenderUserId))
        assertTrue(
            actual = nextAction.isThisUserNext(userId = currentAction.attackTurn.attackerUserId)
        )
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When a Mage rolls for Teleport and wins the roll-off, then the Mage escapes and the turn ends`() {
        teleportRollOffTest(attackerRoll = 3, mageRoll = 4, isMageExpectedToWin = true)
    }

    @Test
    fun `When a Mage rolls for Teleport and loses the roll-off, then the attacker rolls against the Mage's health`() {
        teleportRollOffTest(attackerRoll = 5, mageRoll = 2, isMageExpectedToWin = false)
    }

    @Test
    fun `When a Mage rolls for Teleport and ties the roll-off, then the attacker rolls against the Mage's health`() {
        teleportRollOffTest(attackerRoll = 4, mageRoll = 4, isMageExpectedToWin = false)
    }

    private fun teleportRollOffTest(
        attackerRoll: Int,
        mageRoll: Int,
        isMageExpectedToWin: Boolean,
    ) {
        // Arrange
        val arguments = createTwoPlayerArguments(defenderClass = TestCharacterClasses.mage())
        val performCombatAction = getPerformCombatAction(arguments = arguments)

        val purpose =
            AbilityAction.Passive.Teleport(
                attackerRoll = TeleportDie(userId = DEFAULT_ATTACKER_ID, roll = attackerRoll),
                defenderRoll = TeleportDie(userId = DEFAULT_DEFENDER_ID),
                attackerDoublesBonus = 0,
            )
        val mageUserId = DEFAULT_DEFENDER_ID
        val currentAction = getAbilityRollAction(purpose = purpose, rollingUserId = mageUserId)

        setupOneDiceRoll(userId = mageUserId, die = mageRoll)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        if (isMageExpectedToWin) {
            assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
            assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = mageUserId))
        } else {
            val nextAction = outcome.nextCombatAction
            assertTrue(actual = nextAction is NextCombatAction.NormalAttack)
            assertEquals(actual = nextAction.roll, expected = NormalAttackType.Health)
            assertTrue(
                actual = nextAction.isThisUserNext(userId = currentAction.attackTurn.attackerUserId)
            )
        }
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When an attacker rolls doubles to pass Mage's armor, then the Teleport roll-off includes the doubles bonus`() {
        // Arrange
        val attackerUserId = DEFAULT_ATTACKER_ID
        val mageUserId = DEFAULT_DEFENDER_ID
        val arguments =
            createTwoPlayerArguments(
                attackerUserId = attackerUserId,
                defenderUserId = mageUserId,
                defenderClass = TestCharacterClasses.mage(),
            )
        val performCombatAction = getPerformCombatAction(arguments = arguments)

        val bonus = 1
        val purpose =
            AbilityAction.Passive.Teleport(
                attackerRoll = TeleportDie(userId = attackerUserId),
                defenderRoll = TeleportDie(userId = mageUserId),
                attackerDoublesBonus = bonus,
            )
        val currentAction = getAbilityRollAction(purpose = purpose)

        val attackerDie = 3
        setupOneDiceRoll(userId = currentAction.attackTurn.attackerUserId, die = attackerDie)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.AbilityRoll)

        val teleport = assertIs<AbilityAction.Passive.Teleport>(value = nextAction.purpose)
        assertEquals(expected = attackerDie + bonus, actual = teleport.attackerRoll.roll)
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `Given the Mage's Teleport die is dispatched first, when the attacker later rolls, then the attacker still gets the doubles bonus`() {
        // Arrange
        val attackerUserId = DEFAULT_ATTACKER_ID
        val mageUserId = DEFAULT_DEFENDER_ID
        val arguments =
            createTwoPlayerArguments(
                attackerUserId = attackerUserId,
                defenderUserId = mageUserId,
                defenderClass = TestCharacterClasses.mage(),
            )

        val bonus = 1
        val mageDie = 5
        val purpose =
            AbilityAction.Passive.Teleport(
                attackerRoll = TeleportDie(userId = attackerUserId),
                defenderRoll = TeleportDie(userId = mageUserId),
                attackerDoublesBonus = bonus,
            )
        val mageRollAction = getAbilityRollAction(purpose = purpose, rollingUserId = mageUserId)
        setupOneDiceRoll(userId = mageUserId, die = mageDie)

        // Act - the Mage's die is dispatched before the attacker's, clearing the roller's bonus
        val mageOutcome =
            getPerformCombatAction(arguments = arguments).invoke(currentAction = mageRollAction)

        val afterMageRoll = mageOutcome.nextCombatAction
        assertTrue(actual = afterMageRoll is NextCombatAction.AbilityRoll)
        val afterMageTeleport =
            assertIs<AbilityAction.Passive.Teleport>(value = afterMageRoll.purpose)

        val attackerDie = 3
        val attackerArguments =
            createTwoPlayerArguments(
                attackerUserId = attackerUserId,
                defenderUserId = mageUserId,
                defenderClass = TestCharacterClasses.mage(),
                doublesBonus = mageOutcome.diceRoll?.doublesBonus,
            )
        val attackerRollAction =
            getAbilityRollAction(purpose = afterMageTeleport, rollingUserId = attackerUserId)
        setupOneDiceRoll(userId = attackerUserId, die = attackerDie)

        val attackerOutcome =
            getPerformCombatAction(arguments = attackerArguments)
                .invoke(currentAction = attackerRollAction)

        // Assert - the attacker's die still carries the +1 despite rolling second
        val attackerRollMessage =
            assertIs<ArenaMessage.Teleport.Roll>(
                value =
                    attackerOutcome.messages.single {
                        it is ArenaMessage.Teleport.Roll && it.userId == attackerUserId
                    }
            )
        assertEquals(expected = attackerDie + bonus, actual = attackerRollMessage.singleDie)
    }

    @Test
    fun `When a Paladin is knocked out, then Holy Shield triggers`() {
        // Arrange
        val arguments = createTwoPlayerArguments(defenderClass = TestCharacterClasses.paladin())
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()
        val paladinUserId = currentAction.defenderUserId

        setupTwoDiceRoll(userId = currentAction.attackTurn.attackerUserId)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.AbilityRoll)
        assertTrue(actual = nextAction.purpose is AbilityAction.Passive.HolyShield.Prayer)
        assertTrue(actual = nextAction.isThisUserNext(userId = paladinUserId))

        val updatedPaladin = outcome.getPlayerById(userId = paladinUserId).characterClass
        val holyShield =
            assertIs<DefensiveAbility.HolyShield>(value = updatedPaladin.defensiveAbility)
        assertTrue(actual = holyShield.usedThisMatch)
        assertTrue(actual = !outcome.isMatchFinished)

        // The triggering hit reads as damage, not a knockout, while the survival roll is pending
        // (SS-100)
        val normalRoll =
            assertIs<ArenaMessage.NormalRoll>(
                value = outcome.messages.single { it is ArenaMessage.NormalRoll }
            )
        assertEquals(expected = NormalRollMessageType.Damage, actual = normalRoll.type)
    }

    @Test
    fun `When a Paladin rolls a successful Holy Shield Prayer, then the Paladin survives with 1 HP and performs Wrath`() {
        // Arrange
        val arguments = createTwoPlayerArguments(defenderClass = TestCharacterClasses.paladin())
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getAbilityRollAction(purpose = AbilityAction.Passive.HolyShield.Prayer)
        val paladinUserId = currentAction.defenderUserId

        setupTwoDiceRoll(userId = paladinUserId)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.AbilityRoll)
        assertTrue(actual = nextAction.purpose is AbilityAction.Passive.HolyShield.Wrath)
        assertTrue(actual = nextAction.isThisUserNext(userId = paladinUserId))

        val updatedPaladin = outcome.getPlayerById(userId = paladinUserId).characterClass
        assertEquals(expected = 1, actual = updatedPaladin.hitPoints)
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When a Paladin rolls a failed Holy Shield Prayer, then the Paladin is knocked out`() {
        // Arrange
        val arguments =
            createTwoPlayerArguments(
                defenderClass = TestCharacterClasses.paladin().copy(hitPoints = 0)
            )
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getAbilityRollAction(purpose = AbilityAction.Passive.HolyShield.Prayer)
        val paladinUserId = currentAction.defenderUserId

        setupTwoDiceRoll(userId = paladinUserId, firstDie = 4, secondDie = 5)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedPaladin = outcome.getPlayerById(userId = paladinUserId).characterClass
        assertTrue(actual = updatedPaladin.hitPoints <= 0)
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
        assertTrue(actual = outcome.isMatchFinished)
    }

    @Test
    fun `When a Paladin performs Wrath, then the attacker takes damage and the turn ends`() {
        // Arrange
        val attackerStartingHitPoints = 10
        val attackerClass = createCharacterClass(hitPoints = attackerStartingHitPoints)
        val arguments =
            createTwoPlayerArguments(
                attackerClass = attackerClass,
                defenderClass = TestCharacterClasses.paladin().copy(hitPoints = 1),
            )
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getAbilityRollAction(purpose = AbilityAction.Passive.HolyShield.Wrath)
        val paladinUserId = currentAction.defenderUserId

        val wrathDamage = 4
        setupOneDiceRoll(userId = paladinUserId, die = wrathDamage)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = paladinUserId))

        val updatedAttacker =
            outcome.getPlayerById(userId = currentAction.attackTurn.attackerUserId).characterClass

        assertEquals(
            expected = attackerStartingHitPoints - wrathDamage,
            actual = updatedAttacker.hitPoints,
        )
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When a Rogue's attack is repelled by a successful Holy Shield, then Killing Momentum does not trigger`() {
        // Arrange
        val attackerStartingHitPoints = 10
        val arguments =
            createTwoPlayerArguments(
                attackerClass =
                    TestCharacterClasses.rogue().copy(hitPoints = attackerStartingHitPoints),
                defenderClass = TestCharacterClasses.paladin().copy(hitPoints = 1),
            )
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getAbilityRollAction(purpose = AbilityAction.Passive.HolyShield.Wrath)
        val paladinUserId = currentAction.defenderUserId

        setupOneDiceRoll(userId = paladinUserId, die = 4)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert: the Paladin survived, so the Rogue earns no Killing Momentum follow-up.
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = paladinUserId))
        assertFalse(
            actual =
                outcome.messages.contains(
                    element =
                        ArenaMessage.KillingMomentum(
                            attackerUserId = currentAction.attackTurn.attackerUserId
                        )
                )
        )
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When a Bard's attack is repelled by a successful Holy Shield, then Encore still triggers because damage was dealt`() {
        // Arrange
        val arguments =
            createTwoPlayerArguments(
                attackerClass = TestCharacterClasses.bard().copy(hitPoints = 10),
                defenderClass = TestCharacterClasses.paladin().copy(hitPoints = 1),
            )
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getAbilityRollAction(purpose = AbilityAction.Passive.HolyShield.Wrath)
        val bardUserId = currentAction.attackTurn.attackerUserId

        setupOneDiceRoll(userId = currentAction.defenderUserId, die = 4)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert: the Paladin survived, but the Bard dealt damage, so Encore is owed to the Bard.
        val nextAction = assertIs<NextCombatAction.AbilityRoll>(value = outcome.nextCombatAction)
        assertTrue(actual = nextAction.purpose is AbilityAction.Passive.Encore)
        assertTrue(actual = nextAction.isThisUserNext(userId = bardUserId))
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When Mage triggers Spell Potency and splash damage knocks out a Paladin, then Holy Shield triggers for the Paladin and deals damage to Mage`() {
        // Arrange
        val defenderStartingHitPoints = 10
        val arguments =
            createThreePlayerArguments(
                attackerClass = TestCharacterClasses.mage(),
                defenderClass = createCharacterClass(hitPoints = defenderStartingHitPoints),
                thirdClass = TestCharacterClasses.paladin().copy(hitPoints = 2),
            )
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        val spellPotencyTotal = 6
        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 3,
            secondDie = 3,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertEquals(
            expected = defenderStartingHitPoints - spellPotencyTotal,
            actual =
                outcome
                    .getPlayerById(userId = currentAction.defenderUserId)
                    .characterClass
                    .hitPoints,
        )

        // Paladin takes 3 splash damage and triggers Holy Shield
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.AbilityRoll)
        assertTrue(actual = nextAction.purpose is AbilityAction.Passive.HolyShield.Prayer)
        assertTrue(actual = nextAction.isThisUserNext(userId = THIRD_PLAYER_ID))

        val updatedPaladin = outcome.getPlayerById(userId = THIRD_PLAYER_ID).characterClass
        val holyShield =
            assertIs<DefensiveAbility.HolyShield>(value = updatedPaladin.defensiveAbility)
        assertTrue(actual = holyShield.usedThisMatch)
        assertTrue(actual = !outcome.isMatchFinished)

        // The Paladin rolls a successful Prayer (10) and survives with 1 HP.
        val prayerArguments =
            CombatActionArguments(
                arenaId = arguments.arenaId,
                playerMap = outcome.updatedPlayerMap,
                roundNumber = outcome.roundNumber,
                doublesBonus = null,
            )
        val prayerAction =
            getAbilityRollAction(
                purpose = AbilityAction.Passive.HolyShield.Prayer,
                attackerUserId = currentAction.attackTurn.attackerUserId,
                defenderUserId = THIRD_PLAYER_ID,
            )
        setupTwoDiceRoll(userId = THIRD_PLAYER_ID, firstDie = 4, secondDie = 6)
        val prayerOutcome =
            getPerformCombatAction(arguments = prayerArguments).invoke(currentAction = prayerAction)

        val wrathNextAction = prayerOutcome.nextCombatAction
        assertTrue(actual = wrathNextAction is NextCombatAction.AbilityRoll)
        assertTrue(actual = wrathNextAction.purpose is AbilityAction.Passive.HolyShield.Wrath)

        // The Paladin's Wrath deals unblockable damage to the Mage, the splash attacker.
        val mageHitPointsBeforeWrath =
            prayerOutcome
                .getPlayerById(userId = currentAction.attackTurn.attackerUserId)
                .characterClass
                .hitPoints
        val wrathArguments =
            CombatActionArguments(
                arenaId = arguments.arenaId,
                playerMap = prayerOutcome.updatedPlayerMap,
                roundNumber = prayerOutcome.roundNumber,
                doublesBonus = null,
            )
        val wrathAction =
            getAbilityRollAction(
                purpose = AbilityAction.Passive.HolyShield.Wrath,
                attackerUserId = currentAction.attackTurn.attackerUserId,
                defenderUserId = THIRD_PLAYER_ID,
            )
        val wrathDamage = 4
        setupOneDiceRoll(userId = THIRD_PLAYER_ID, die = wrathDamage)
        val wrathOutcome =
            getPerformCombatAction(arguments = wrathArguments).invoke(currentAction = wrathAction)

        assertEquals(
            expected = mageHitPointsBeforeWrath - wrathDamage,
            actual =
                wrathOutcome
                    .getPlayerById(userId = currentAction.attackTurn.attackerUserId)
                    .characterClass
                    .hitPoints,
        )
        assertEquals(
            expected = 1,
            actual = wrathOutcome.getPlayerById(userId = THIRD_PLAYER_ID).characterClass.hitPoints,
        )
        assertTrue(actual = wrathOutcome.nextCombatAction is NextCombatAction.Idle)
        assertTrue(actual = !wrathOutcome.isMatchFinished)
    }

    @Test
    fun `When Duelist uses Prepare Attack and rolls a 12 against a Paladin, then the Paladin is knocked out and Holy Shield is marked as used`() {
        // Arrange
        val arguments =
            createTwoPlayerArguments(
                attackerClass = TestCharacterClasses.duelist(),
                defenderClass = TestCharacterClasses.paladin(),
            )
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getPrepareAttackAction()

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 6,
            secondDie = 6,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedDefender =
            outcome.getPlayerById(userId = currentAction.defenderUserId).characterClass
        assertEquals(expected = 0, actual = updatedDefender.hitPoints)
        assertTrue(actual = outcome.nextCombatAction is NextCombatAction.Idle)
        assertTrue(actual = outcome.isMatchFinished)

        assertIs<DefensiveAbility.HolyShield>(value = updatedDefender.defensiveAbility)
        assertTrue(actual = updatedDefender.defensiveAbility.usedThisMatch)
    }

    @Test
    fun `When Duelist uses PrepareAttack against  Paladin and gets a 12, then Mage later triggers Spell Potency, the Paladin's Holy Shield does NOT trigger`() {
        // Arrange
        val duelistUserId = DEFAULT_ATTACKER_ID
        val paladinUserId = DEFAULT_DEFENDER_ID
        val mageUserId = THIRD_PLAYER_ID

        val arguments =
            createThreePlayerArguments(
                attackerUserId = duelistUserId,
                defenderUserId = paladinUserId,
                thirdUserId = mageUserId,
                attackerClass = TestCharacterClasses.duelist(),
                defenderClass = TestCharacterClasses.paladin(),
                thirdClass = TestCharacterClasses.mage(),
            )

        // 1. Duelist attacks Paladin with 12
        val currentAction1 = getPrepareAttackAction()
        setupTwoDiceRoll(
            userId = currentAction1.attackTurn.attackerUserId,
            firstDie = 6,
            secondDie = 6,
        )
        val performCombatAction1 = getPerformCombatAction(arguments = arguments)
        val outcome1 = performCombatAction1.invoke(currentAction = currentAction1)

        val updatedPaladin =
            outcome1.getPlayerById(userId = currentAction1.defenderUserId).characterClass
        assertEquals(expected = 0, actual = updatedPaladin.hitPoints)

        assertTrue(
            actual =
                assertIs<DefensiveAbility.HolyShield>(value = updatedPaladin.defensiveAbility)
                    .usedThisMatch
        )

        // 2. Mage uses Spell Potency (against Duelist)
        val arguments2 =
            CombatActionArguments(
                arenaId = arguments.arenaId,
                playerMap = outcome1.updatedPlayerMap,
                roundNumber = outcome1.roundNumber,
                doublesBonus = null,
            )

        val currentAction2 =
            getNormalAttackAction(attackerUserId = mageUserId, defenderUserId = duelistUserId)
        setupTwoDiceRoll(
            userId = currentAction2.attackTurn.attackerUserId,
            firstDie = 1,
            secondDie = 1,
        )
        val performCombatAction2 = getPerformCombatAction(arguments = arguments2)
        val outcome2 = performCombatAction2.invoke(currentAction = currentAction2)

        // Assert: Holy Shield should NOT be triggered (no AbilityRoll for Prayer)
        val nextAction = outcome2.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.Idle)
        assertTrue(actual = !outcome2.isMatchFinished)
    }

    @Test
    fun `When Duelist uses Prepare Attack and rolls a 10 against a Paladin, then Holy Shield triggers`() {
        // Arrange
        val duelist = TestCharacterClasses.duelist()
        val paladin = TestCharacterClasses.paladin()
        val arguments = createTwoPlayerArguments(attackerClass = duelist, defenderClass = paladin)

        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getPrepareAttackAction()
        val paladinUserId = currentAction.defenderUserId

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 5,
            secondDie = 5,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.AbilityRoll)
        assertTrue(actual = nextAction.purpose is AbilityAction.Passive.HolyShield.Prayer)
        assertTrue(actual = nextAction.isThisUserNext(userId = paladinUserId))

        val updatedDefender = outcome.getPlayerById(userId = paladinUserId).characterClass
        val holyShield =
            assertIs<DefensiveAbility.HolyShield>(value = updatedDefender.defensiveAbility)
        assertTrue(actual = holyShield.usedThisMatch)
        assertTrue(actual = !outcome.isMatchFinished)
    }

    @Test
    fun `When an attacker rolls doubles to pass a defender's ArmorClass, then the subsequent Health roll includes the doubles bonus`() {
        // Arrange
        val attackerUserId = DEFAULT_ATTACKER_ID
        val arguments =
            createTwoPlayerArguments(
                attackerUserId = attackerUserId,
                doublesBonus = DoublesBonus(userId = attackerUserId),
            )
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()

        val firstDie = 3
        val secondDie = 4
        val bonus = 1
        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = firstDie,
            secondDie = secondDie,
            bonus = bonus,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val defenderUserId = currentAction.defenderUserId
        val updatedDefender = outcome.getPlayerById(userId = defenderUserId).characterClass
        val expectedDamage = firstDie + secondDie + bonus
        val initialHp = arguments.playerMap.getValue(defenderUserId).characterClass.hitPoints
        assertEquals(expected = initialHp - expectedDamage, actual = updatedDefender.hitPoints)
    }

    @Test
    fun `When an attacker rolls doubles to pass a Duelist's ArmorClass, the Duelist's Riposte roll does NOT include the bonus`() {
        // Arrange
        val attackerUserId = DEFAULT_ATTACKER_ID
        val arguments =
            createTwoPlayerArguments(
                attackerUserId = attackerUserId,
                defenderClass = TestCharacterClasses.duelist(),
                doublesBonus = DoublesBonus(userId = attackerUserId),
            )
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getRiposteAction(attackerRollTotal = 10)
        val duelistUserId = currentAction.defenderUserId

        val firstDie = 3
        val secondDie = 4
        val bonus = 0 // Duelist should NOT get the attacker's bonus
        setupTwoDiceRoll(
            userId = duelistUserId,
            firstDie = firstDie,
            secondDie = secondDie,
            bonus = bonus,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        // Riposte total is 7 (successful)
        val nextAction = outcome.nextCombatAction
        assertTrue(actual = nextAction is NextCombatAction.NormalAttack)

        assertIs<NormalAttackType>(value = nextAction.roll)
        assertEquals(expected = duelistUserId, actual = nextAction.attackTurn.attackerUserId)
    }

    @Test
    fun `When a Rogue rolls doubles to knockout a defender, Killing Momentum triggers so the Rogue attacks again`() {
        // Arrange
        val arguments = createThreePlayerArguments(attackerClass = TestCharacterClasses.rogue())
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()
        val rogueUserId = currentAction.attackTurn.attackerUserId

        setupTwoDiceRoll(userId = rogueUserId, firstDie = 5, secondDie = 5)
        every { diceRoller.getDoublesBonus() } returns DoublesBonus(userId = rogueUserId)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = rogueUserId))
    }

    @Test
    fun `When a Paladin rolls 10 (doubles) for Holy Shield Prayer, the subsequent Wrath roll follows`() {
        // Arrange
        val arguments = createTwoPlayerArguments(defenderClass = TestCharacterClasses.paladin())
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getAbilityRollAction(purpose = AbilityAction.Passive.HolyShield.Prayer)
        val paladinUserId = currentAction.defenderUserId

        setupTwoDiceRoll(userId = paladinUserId, firstDie = 5, secondDie = 5)
        every { diceRoller.getDoublesBonus() } returns DoublesBonus(userId = paladinUserId)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val nextAction = assertIs<NextCombatAction.AbilityRoll>(value = outcome.nextCombatAction)
        assertTrue(actual = nextAction.purpose is AbilityAction.Passive.HolyShield.Wrath)
        assertTrue(actual = nextAction.isThisUserNext(userId = paladinUserId))
    }

    @Test
    fun `When a Gambler with a pending second attack finishes their first attack, they attack again`() {
        // Arrange
        val gambler =
            TestCharacterClasses.gambler()
                .copy(
                    offensiveAbility =
                        OffensiveAbility.AllIn(hasAnotherAttack = true, usedThisTurn = true)
                )
        val arguments = createTwoPlayerArguments(attackerClass = gambler)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()
        val gamblerUserId = currentAction.attackTurn.attackerUserId

        setupTwoDiceRoll(userId = gamblerUserId, firstDie = 1, secondDie = 2)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        val updatedGambler = outcome.getPlayerById(userId = gamblerUserId).characterClass
        val updatedAllIn = assertIs<OffensiveAbility.AllIn>(value = updatedGambler.offensiveAbility)
        assertTrue(actual = !updatedAllIn.hasAnotherAttack)
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = gamblerUserId))
    }

    @Test
    fun `When a Gambler finishes their granted second attack, the turn passes to the next player`() {
        // Arrange
        val gambler =
            TestCharacterClasses.gambler()
                .copy(
                    offensiveAbility =
                        OffensiveAbility.AllIn(hasAnotherAttack = false, usedThisTurn = true)
                )
        val arguments = createTwoPlayerArguments(attackerClass = gambler)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getHealthAttackAction()
        val gamblerUserId = currentAction.attackTurn.attackerUserId

        setupTwoDiceRoll(userId = gamblerUserId, firstDie = 1, secondDie = 2)

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(actual = outcome.nextCombatAction.isThisUserNext(userId = DEFAULT_DEFENDER_ID))
    }

    @Test
    fun `When a new round begins, a Gambler who already used All-In can use it again`() {
        // Arrange
        val gambler =
            TestCharacterClasses.gambler()
                .copy(
                    offensiveAbility = OffensiveAbility.AllIn(usedThisTurn = true),
                    hasActedThisRound = true,
                )
        val arguments = createTwoPlayerArguments(defenderClass = gambler)
        val performCombatAction = getPerformCombatAction(arguments = arguments)
        val currentAction = getNormalAttackAction()

        setupTwoDiceRoll(
            userId = currentAction.attackTurn.attackerUserId,
            firstDie = 1,
            secondDie = 1,
        )

        // Act
        val outcome = performCombatAction.invoke(currentAction = currentAction)

        // Assert
        assertTrue(actual = outcome.isNewRound)
        val updatedGambler = outcome.getPlayerById(userId = DEFAULT_DEFENDER_ID).characterClass
        val updatedAllIn = assertIs<OffensiveAbility.AllIn>(value = updatedGambler.offensiveAbility)
        assertTrue(actual = !updatedAllIn.usedThisTurn)
    }

    private fun getPerformCombatAction(arguments: CombatActionArguments) =
        PerformCombatAction(
            arguments = arguments,
            diceRoller = diceRoller,
            abilityConfig = AbilityConfig.DEFAULT,
        )

    companion object {
        private const val DEFAULT_DISPLAY_NAME = "displayName"
        private const val DEFAULT_ARENA_ID = "arenaId"
        private const val DEFAULT_ATTACKER_ID = "attackerId"
        private const val DEFAULT_DEFENDER_ID = "defenderId"
        private const val THIRD_PLAYER_ID = "thirdPlayerId"

        private fun attackTurnOf(attackerUserId: String, turnOwnerUserId: String): AttackTurn =
            if (attackerUserId == turnOwnerUserId) {
                AttackTurn.Standard(attackerUserId = attackerUserId)
            } else {
                AttackTurn.RiposteReversal(
                    attackerUserId = attackerUserId,
                    turnOwnerUserId = turnOwnerUserId,
                )
            }

        private fun getNormalAttackAction(
            attackerUserId: String = DEFAULT_ATTACKER_ID,
            defenderUserId: String = DEFAULT_DEFENDER_ID,
            originalTurnUserId: String = attackerUserId,
        ) =
            CurrentCombatAction.NormalAttack(
                roll = NormalAttackType.ArmorClass,
                attackTurn =
                    attackTurnOf(
                        attackerUserId = attackerUserId,
                        turnOwnerUserId = originalTurnUserId,
                    ),
                defenderUserId = defenderUserId,
            )

        private fun getHealthAttackAction() =
            CurrentCombatAction.NormalAttack(
                roll = NormalAttackType.Health,
                attackTurn = AttackTurn.Standard(attackerUserId = DEFAULT_ATTACKER_ID),
                defenderUserId = DEFAULT_DEFENDER_ID,
            )

        private fun getAbilityRollAction(
            purpose: AbilityAction,
            attackerUserId: String = DEFAULT_ATTACKER_ID,
            defenderUserId: String = DEFAULT_DEFENDER_ID,
            originalTurnUserId: String = attackerUserId,
            rollingUserId: String = attackerUserId,
        ) =
            CurrentCombatAction.AbilityRoll(
                purpose = purpose,
                attackTurn =
                    attackTurnOf(
                        attackerUserId = attackerUserId,
                        turnOwnerUserId = originalTurnUserId,
                    ),
                defenderUserId = defenderUserId,
                rollingUserId = rollingUserId,
            )

        private fun getAllInAction(gambler: CharacterClass) =
            getAbilityRollAction(
                purpose =
                    AbilityAction.Activated.AllIn(
                        allInAbility =
                            assertIs<OffensiveAbility.AllIn>(value = gambler.offensiveAbility)
                    )
            )

        private fun getLuckyCoinFlipAction(attackerRollTotal: Int) =
            getAbilityRollAction(
                purpose =
                    AbilityAction.Passive.LuckyCoin.Flip(attackerRollTotal = attackerRollTotal)
            )

        private fun getLuckyCoinHighRollerAction(attackerRollTotal: Int) =
            getAbilityRollAction(
                purpose =
                    AbilityAction.Passive.LuckyCoin.HighRoller(
                        attackerRollTotal = attackerRollTotal
                    )
            )

        private fun getPrepareAttackAction() =
            getAbilityRollAction(purpose = AbilityAction.Activated.PrepareAttack)

        private fun getImpersonateAction(abilityToImpersonate: OffensiveAbility) =
            getAbilityRollAction(
                purpose =
                    AbilityAction.Activated.Impersonate(
                        impersonateAbility =
                            OffensiveAbility.Impersonate(impersonatedAbility = abilityToImpersonate)
                    )
            )

        private fun getSummonMinionAction(necromancer: CharacterClass) =
            getAbilityRollAction(
                purpose =
                    AbilityAction.Activated.SummonMinion(
                        summonMinionAbility =
                            assertIs<OffensiveAbility.SummonMinion>(
                                value = necromancer.offensiveAbility
                            )
                    )
            )

        private fun getStrumLuteAction() =
            getAbilityRollAction(purpose = AbilityAction.Passive.Persuade.StrumLute)

        private fun getRiposteAction(attackerRollTotal: Int) =
            getAbilityRollAction(
                purpose = AbilityAction.Passive.Riposte(attackerRollTotal = attackerRollTotal)
            )

        private fun createCharacterClass(
            type: CharacterClassType = CharacterClassType.None,
            hitPoints: Int = 5,
            maxHitPoints: Int = hitPoints,
            armorClass: Int = 6,
            speed: Int = 8,
            offensiveAbility: OffensiveAbility = OffensiveAbility.None,
            defensiveAbility: DefensiveAbility = DefensiveAbility.None,
            hasActedThisRound: Boolean = false,
        ) =
            CharacterClass(
                type = type,
                hitPoints = hitPoints,
                maxHitPoints = maxHitPoints,
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
            attackerUserId: String = DEFAULT_ATTACKER_ID,
            defenderUserId: String = DEFAULT_DEFENDER_ID,
            attackerClass: CharacterClass = createCharacterClass(speed = 8),
            defenderClass: CharacterClass = createCharacterClass(speed = 1),
            roundNumber: Int = 1,
            doublesBonus: DoublesBonus? = null,
        ) =
            CombatActionArguments(
                arenaId = arenaId,
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
                doublesBonus = doublesBonus,
            )

        private fun createThreePlayerArguments(
            arenaId: String = DEFAULT_ARENA_ID,
            attackerUserId: String = DEFAULT_ATTACKER_ID,
            defenderUserId: String = DEFAULT_DEFENDER_ID,
            thirdUserId: String = THIRD_PLAYER_ID,
            attackerClass: CharacterClass = createCharacterClass(speed = 8),
            defenderClass: CharacterClass = createCharacterClass(speed = 6),
            thirdClass: CharacterClass = createCharacterClass(speed = 1),
            roundNumber: Int = 1,
            doublesBonus: DoublesBonus? = null,
        ) =
            CombatActionArguments(
                arenaId = arenaId,
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
                            createArenaPlayer(userId = thirdUserId, characterClass = thirdClass),
                    ),
                roundNumber = roundNumber,
                doublesBonus = doublesBonus,
            )

        private fun ActionOutcome.getPlayerById(userId: String): ArenaPlayer.Selected =
            updatedPlayerMap.getValue(key = userId)

        private fun NextCombatAction.isThisUserNext(userId: String) = userId in activeUserIds
    }
}
