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
    fun `Given a spectator, then a normal attack gives them no gesture`() {
        val cue = normalAttackCue(outcome = NormalRollMessageType.ArmorClassHit)

        assertNull(actual = cue.gestureFor(userId = SPECTATOR_ID))
    }

    private fun normalRollMessage(type: NormalRollMessageType) =
        ArenaMessageState.NormalRoll(
            id = MESSAGE_ID,
            attackerUserId = ATTACKER_ID,
            attackerName = "Rogue",
            attackerClassType = CharacterClassType.Rogue,
            defenderUserId = DEFENDER_ID,
            defenderName = "Mage",
            firstDie = 4,
            secondDie = 5,
            bonus = 0,
            type = type,
        )

    private fun normalAttackCue(outcome: NormalRollMessageType) =
        CombatAnimationCue.NormalAttack(
            id = MESSAGE_ID,
            attackerUserId = ATTACKER_ID,
            defenderUserId = DEFENDER_ID,
            outcome = outcome,
        )

    private companion object {
        const val MESSAGE_ID = "message-1"
        const val ATTACKER_ID = "attacker"
        const val DEFENDER_ID = "defender"
        const val SPECTATOR_ID = "spectator"
    }
}
