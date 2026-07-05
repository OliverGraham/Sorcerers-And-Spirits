package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CombatAnimationCueTest {

    @Test
    fun `Given a normal roll message, then its cue carries the combatants and the outcome`() {
        val message = normalRollMessage(type = NormalRollMessageType.ArmorClassHit)

        assertEquals(
            expected =
                CombatAnimationCue.NormalAttack(
                    id = MESSAGE_ID,
                    attackerUserId = ATTACKER_ID,
                    attackerClassType = CharacterClassType.Mage,
                    defenderUserId = DEFENDER_ID,
                    outcome = NormalRollMessageType.ArmorClassHit,
                ),
            actual = message.combatAnimationCue,
        )
    }

    @Test
    fun `Given a message the avatars sit out, then it has no cue`() {
        val message = ArenaMessageState.NewRound(id = MESSAGE_ID, roundNumber = "2")

        assertNull(actual = message.combatAnimationCue)
    }

    @Test
    fun `Given an armor hit, then the attacker lunges and the defender flinches`() {
        val cue = normalAttackCue(outcome = NormalRollMessageType.ArmorClassHit)

        assertEquals(expected = AvatarGesture.Lunge, actual = cue.gestureFor(userId = ATTACKER_ID))
        assertEquals(expected = AvatarGesture.Flinch, actual = cue.gestureFor(userId = DEFENDER_ID))
    }

    @Test
    fun `Given an armor miss, then the attacker overshoots and the defender sidesteps`() {
        val cue = normalAttackCue(outcome = NormalRollMessageType.ArmorClassMiss)

        assertEquals(
            expected = AvatarGesture.LungeOvershoot,
            actual = cue.gestureFor(userId = ATTACKER_ID),
        )
        assertEquals(
            expected = AvatarGesture.Sidestep,
            actual = cue.gestureFor(userId = DEFENDER_ID),
        )
    }

    @Test
    fun `Given a damaging health roll, then the attacker lunges and the defender flinches heavily`() {
        val cue = normalAttackCue(outcome = NormalRollMessageType.Damage)

        assertEquals(expected = AvatarGesture.Lunge, actual = cue.gestureFor(userId = ATTACKER_ID))
        assertEquals(
            expected = AvatarGesture.FlinchHeavy,
            actual = cue.gestureFor(userId = DEFENDER_ID),
        )
    }

    @Test
    fun `Given a knockout, then the attacker lunges hard and the defender collapses`() {
        val cue = normalAttackCue(outcome = NormalRollMessageType.Knockout)

        assertEquals(
            expected = AvatarGesture.LungeStrong,
            actual = cue.gestureFor(userId = ATTACKER_ID),
        )
        assertEquals(
            expected = AvatarGesture.Collapse,
            actual = cue.gestureFor(userId = DEFENDER_ID),
        )
    }

    @Test
    fun `Given a Rogue armor hit, then the Rogue stabs and the defender flinches`() {
        val cue = rogueAttackCue(outcome = NormalRollMessageType.ArmorClassHit)

        assertEquals(expected = AvatarGesture.Stab, actual = cue.gestureFor(userId = ATTACKER_ID))
        assertEquals(expected = AvatarGesture.Flinch, actual = cue.gestureFor(userId = DEFENDER_ID))
    }

    @Test
    fun `Given a Rogue armor miss, then the stab overshoots`() {
        val cue = rogueAttackCue(outcome = NormalRollMessageType.ArmorClassMiss)

        assertEquals(
            expected = AvatarGesture.StabOvershoot,
            actual = cue.gestureFor(userId = ATTACKER_ID),
        )
    }

    @Test
    fun `Given a Rogue damaging health roll, then the Rogue stabs`() {
        val cue = rogueAttackCue(outcome = NormalRollMessageType.Damage)

        assertEquals(expected = AvatarGesture.Stab, actual = cue.gestureFor(userId = ATTACKER_ID))
    }

    @Test
    fun `Given a Rogue knockout, then the Rogue finishes with a stab flurry`() {
        val cue = rogueAttackCue(outcome = NormalRollMessageType.Knockout)

        assertEquals(
            expected = AvatarGesture.StabFlurry,
            actual = cue.gestureFor(userId = ATTACKER_ID),
        )
    }

    @Test
    fun `Given a Duelist armor hit, then the Duelist slashes and the defender flinches`() {
        val cue = duelistAttackCue(outcome = NormalRollMessageType.ArmorClassHit)

        assertEquals(expected = AvatarGesture.Slash, actual = cue.gestureFor(userId = ATTACKER_ID))
        assertEquals(expected = AvatarGesture.Flinch, actual = cue.gestureFor(userId = DEFENDER_ID))
    }

    @Test
    fun `Given a Duelist armor miss, then the slash whiffs`() {
        val cue = duelistAttackCue(outcome = NormalRollMessageType.ArmorClassMiss)

        assertEquals(
            expected = AvatarGesture.SlashWhiff,
            actual = cue.gestureFor(userId = ATTACKER_ID),
        )
    }

    @Test
    fun `Given a Duelist damaging health roll, then the Duelist slashes`() {
        val cue = duelistAttackCue(outcome = NormalRollMessageType.Damage)

        assertEquals(expected = AvatarGesture.Slash, actual = cue.gestureFor(userId = ATTACKER_ID))
    }

    @Test
    fun `Given a Duelist knockout, then the Duelist finishes with a slash`() {
        val cue = duelistAttackCue(outcome = NormalRollMessageType.Knockout)

        assertEquals(
            expected = AvatarGesture.SlashFinish,
            actual = cue.gestureFor(userId = ATTACKER_ID),
        )
    }

    @Test
    fun `Given a spectator, then a normal attack gives them no gesture`() {
        val cue = normalAttackCue(outcome = NormalRollMessageType.ArmorClassHit)

        assertNull(actual = cue.gestureFor(userId = SPECTATOR_ID))
    }

    @Test
    fun `Given a Killing Momentum message, then only the attacker re-arms`() {
        val message =
            ArenaMessageState.KillingMomentum(id = MESSAGE_ID, attackerUserId = ATTACKER_ID)

        val cue = message.combatAnimationCue

        assertEquals(
            expected =
                CombatAnimationCue.KillingMomentum(id = MESSAGE_ID, attackerUserId = ATTACKER_ID),
            actual = cue,
        )
        assertEquals(expected = AvatarGesture.ReArm, actual = cue?.gestureFor(userId = ATTACKER_ID))
        assertNull(actual = cue?.gestureFor(userId = SPECTATOR_ID))
    }

    @Test
    fun `Given a Killing Momentum message with no known attacker, then it has no cue`() {
        val message = ArenaMessageState.KillingMomentum(id = MESSAGE_ID, attackerUserId = null)

        assertNull(actual = message.combatAnimationCue)
    }

    @Test
    fun `Given a Dodge without an armor increase, then only the dodger flourishes`() {
        val cue = dodgeMessage(didArmorIncrease = false).combatAnimationCue

        assertEquals(
            expected = AvatarGesture.DodgeFlourish,
            actual = cue?.gestureFor(userId = DEFENDER_ID),
        )
        assertNull(actual = cue?.gestureFor(userId = SPECTATOR_ID))
    }

    @Test
    fun `Given a Dodge with an armor increase, then the dodger braces`() {
        val cue = dodgeMessage(didArmorIncrease = true).combatAnimationCue

        assertEquals(
            expected = AvatarGesture.DodgeBrace,
            actual = cue?.gestureFor(userId = DEFENDER_ID),
        )
    }

    @Test
    fun `Given a Dodge with no known dodger, then it has no cue`() {
        val message =
            ArenaMessageState.Dodge(id = MESSAGE_ID, didArmorIncrease = true, dodgerUserId = null)

        assertNull(actual = message.combatAnimationCue)
    }

    private fun normalRollMessage(type: NormalRollMessageType) =
        ArenaMessageState.NormalRoll(
            id = MESSAGE_ID,
            attackerUserId = ATTACKER_ID,
            attackerName = "Mage",
            attackerClassType = CharacterClassType.Mage,
            defenderUserId = DEFENDER_ID,
            defenderName = "Bard",
            firstDie = 4,
            secondDie = 5,
            bonus = 0,
            type = type,
        )

    private fun normalAttackCue(outcome: NormalRollMessageType) =
        CombatAnimationCue.NormalAttack(
            id = MESSAGE_ID,
            attackerUserId = ATTACKER_ID,
            attackerClassType = CharacterClassType.Mage,
            defenderUserId = DEFENDER_ID,
            outcome = outcome,
        )

    private fun rogueAttackCue(outcome: NormalRollMessageType) =
        CombatAnimationCue.NormalAttack(
            id = MESSAGE_ID,
            attackerUserId = ATTACKER_ID,
            attackerClassType = CharacterClassType.Rogue,
            defenderUserId = DEFENDER_ID,
            outcome = outcome,
        )

    private fun duelistAttackCue(outcome: NormalRollMessageType) =
        CombatAnimationCue.NormalAttack(
            id = MESSAGE_ID,
            attackerUserId = ATTACKER_ID,
            attackerClassType = CharacterClassType.Duelist,
            defenderUserId = DEFENDER_ID,
            outcome = outcome,
        )

    private fun dodgeMessage(didArmorIncrease: Boolean) =
        ArenaMessageState.Dodge(
            id = MESSAGE_ID,
            didArmorIncrease = didArmorIncrease,
            dodgerUserId = DEFENDER_ID,
        )

    private companion object {
        const val MESSAGE_ID = "message-1"
        const val ATTACKER_ID = "attacker"
        const val DEFENDER_ID = "defender"
        const val SPECTATOR_ID = "spectator"
    }
}
