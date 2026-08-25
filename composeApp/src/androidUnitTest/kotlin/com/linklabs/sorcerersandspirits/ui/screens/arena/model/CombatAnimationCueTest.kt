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
                normalRollMessageType = NormalRollMessageType.ArmorClassHit,
                impersonatedClassType = CharacterClassType.None,
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
    fun `Given a Paladin armor hit, then the Paladin swings and the defender flinches`() {
        val cue = paladinAttackCue(outcome = NormalRollMessageType.ArmorClassHit)

        assertEquals(expected = AvatarGesture.Slash, actual = cue.gestureFor(userId = ATTACKER_ID))
        assertEquals(expected = AvatarGesture.Flinch, actual = cue.gestureFor(userId = DEFENDER_ID))
    }

    @Test
    fun `Given a Paladin armor miss, then the swing whiffs`() {
        val cue = paladinAttackCue(outcome = NormalRollMessageType.ArmorClassMiss)

        assertEquals(
            expected = AvatarGesture.SlashWhiff,
            actual = cue.gestureFor(userId = ATTACKER_ID),
        )
    }

    @Test
    fun `Given a Paladin damaging health roll, then the Paladin swings`() {
        val cue = paladinAttackCue(outcome = NormalRollMessageType.Damage)

        assertEquals(expected = AvatarGesture.Slash, actual = cue.gestureFor(userId = ATTACKER_ID))
    }

    @Test
    fun `Given a Paladin knockout, then the Paladin finishes with a heavier swing`() {
        val cue = paladinAttackCue(outcome = NormalRollMessageType.Knockout)

        assertEquals(
            expected = AvatarGesture.SlashFinish,
            actual = cue.gestureFor(userId = ATTACKER_ID),
        )
    }

    @Test
    fun `Given a Holy Shield Wrath, then only the struck attacker shakes`() {
        val cue =
            CombatAnimationCue.HolyShield(
                id = MESSAGE_ID,
                paladinUserId = DEFENDER_ID,
                phase = HolyShieldPhase.Wrath,
                didSurvive = true,
                attackerUserId = ATTACKER_ID,
                wrathIntensity = 5,
                isAttackerKnockedOut = false,
            )

        assertEquals(
            expected = AvatarGesture.FlinchHeavy,
            actual = cue.gestureFor(userId = ATTACKER_ID),
        )
        assertNull(actual = cue.gestureFor(userId = DEFENDER_ID))
        assertNull(actual = cue.gestureFor(userId = SPECTATOR_ID))
        assertNull(actual = cue.toCollapsingUserId())
    }

    @Test
    fun `Given a Holy Shield Wrath knockout, then the struck attacker collapses and holds`() {
        val cue =
            CombatAnimationCue.HolyShield(
                id = MESSAGE_ID,
                paladinUserId = DEFENDER_ID,
                phase = HolyShieldPhase.Wrath,
                didSurvive = true,
                attackerUserId = ATTACKER_ID,
                wrathIntensity = 6,
                isAttackerKnockedOut = true,
            )

        assertEquals(
            expected = AvatarGesture.Collapse,
            actual = cue.gestureFor(userId = ATTACKER_ID),
        )
        assertEquals(expected = ATTACKER_ID, actual = cue.toCollapsingUserId())
    }

    @Test
    fun `Given a Mage normal attack, then the Mage casts and the defender reacts`() {
        val cue = mageAttackCue(outcome = NormalRollMessageType.Damage)

        assertEquals(expected = AvatarGesture.Cast, actual = cue.gestureFor(userId = ATTACKER_ID))
        assertEquals(
            expected = AvatarGesture.FlinchHeavy,
            actual = cue.gestureFor(userId = DEFENDER_ID),
        )
    }

    @Test
    fun `Given a Mage knockout, then the Mage still only casts`() {
        val cue = mageAttackCue(outcome = NormalRollMessageType.Knockout)

        assertEquals(expected = AvatarGesture.Cast, actual = cue.gestureFor(userId = ATTACKER_ID))
        assertEquals(
            expected = AvatarGesture.Collapse,
            actual = cue.gestureFor(userId = DEFENDER_ID),
        )
    }

    @Test
    fun `Given a Spell Potency message, then the Mage casts and the main target flinches`() {
        val message =
            ArenaMessageState.SpellPotency(
                id = MESSAGE_ID,
                mageUserId = ATTACKER_ID,
                mageName = "Mage",
                mainTargetUserId = DEFENDER_ID,
                mainTargetName = "Bard",
                firstDie = 4,
                secondDie = 4,
                mainDamage = 8,
                splashDamage = 4,
                splashTargetUserIds = listOf(SPECTATOR_ID),
                isMainTargetKnockedOut = false,
            )

        val cue = message.combatAnimationCue

        assertEquals(
            expected =
            CombatAnimationCue.SpellPotency(
                id = MESSAGE_ID,
                mageUserId = ATTACKER_ID,
                mainTargetUserId = DEFENDER_ID,
                mainDamage = 8,
                splashDamage = 4,
                splashTargetUserIds = listOf(SPECTATOR_ID),
                isMainTargetKnockedOut = false,
            ),
            actual = cue,
        )
        assertEquals(expected = AvatarGesture.Cast, actual = cue?.gestureFor(userId = ATTACKER_ID))
        assertEquals(
            expected = AvatarGesture.FlinchHeavy,
            actual = cue?.gestureFor(userId = DEFENDER_ID),
        )
        assertNull(actual = cue?.gestureFor(userId = SPECTATOR_ID))
        assertNull(actual = cue?.toCollapsingUserId())
    }

    @Test
    fun `Given a Spell Potency knockout, then the main target collapses and holds their seat`() {
        val message =
            ArenaMessageState.SpellPotency(
                id = MESSAGE_ID,
                mageUserId = ATTACKER_ID,
                mageName = "Mage",
                mainTargetUserId = DEFENDER_ID,
                mainTargetName = "Bard",
                firstDie = 4,
                secondDie = 4,
                mainDamage = 8,
                splashDamage = 4,
                splashTargetUserIds = listOf(SPECTATOR_ID),
                isMainTargetKnockedOut = true,
            )

        val cue = message.combatAnimationCue

        assertEquals(
            expected = AvatarGesture.Collapse,
            actual = cue?.gestureFor(userId = DEFENDER_ID),
        )
        assertEquals(expected = DEFENDER_ID, actual = cue?.toCollapsingUserId())
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
    fun `Given a Mirror Image split, then it holds still with no seat reaction`() {
        val cue =
            ArenaMessageState.MirrorImage.Trigger(
                id = MESSAGE_ID,
                illusionistUserId = DEFENDER_ID,
                firstDie = 3,
                secondDie = 4,
            )
                .combatAnimationCue

        assertEquals(
            expected =
            CombatAnimationCue.MirrorImage(
                id = MESSAGE_ID,
                illusionistUserId = DEFENDER_ID,
                phase = MirrorImagePhase.Split,
                messageType = null,
            ),
            actual = cue,
        )
        assertNull(actual = cue?.gestureFor(userId = DEFENDER_ID))
        assertNull(actual = cue?.toCollapsingUserId())
    }

    @Test
    fun `Given a foiled Mirror Image roll, then the real Illusionist dodges`() {
        val cue = mirrorImageResolveCue(outcome = NormalRollMessageType.ArmorClassMiss)

        assertEquals(
            expected = AvatarGesture.Sidestep,
            actual = cue?.gestureFor(userId = DEFENDER_ID),
        )
        assertNull(actual = cue?.toCollapsingUserId())
        assertNull(actual = cue?.gestureFor(userId = SPECTATOR_ID))
    }

    @Test
    fun `Given a landed Mirror Image knockout, then the real Illusionist collapses`() {
        val cue = mirrorImageResolveCue(outcome = NormalRollMessageType.Knockout)

        assertEquals(
            expected = AvatarGesture.Collapse,
            actual = cue?.gestureFor(userId = DEFENDER_ID),
        )
        assertEquals(expected = DEFENDER_ID, actual = cue?.toCollapsingUserId())
    }

    private fun mirrorImageResolveCue(outcome: NormalRollMessageType) = ArenaMessageState.MirrorImage.Pass(
        id = MESSAGE_ID,
        illusionistUserId = DEFENDER_ID,
        attackerName = "Mage",
        attackerClassType = CharacterClassType.Mage,
        messageType = outcome,
        baseRoll = 8,
        doublesBonus = 0,
    )
        .combatAnimationCue

    @Test
    fun `Given a Teleport trigger, then the portal opens with no seat gesture`() {
        val cue =
            ArenaMessageState.Teleport.Trigger(id = MESSAGE_ID, mageUserId = DEFENDER_ID)
                .combatAnimationCue

        assertEquals(
            expected =
            CombatAnimationCue.Teleport(
                id = MESSAGE_ID,
                mageUserId = DEFENDER_ID,
                phase = TeleportPhase.Open,
                didEscape = null,
            ),
            actual = cue,
        )
        assertNull(actual = cue?.gestureFor(userId = DEFENDER_ID))
        assertNull(actual = cue?.toCollapsingUserId())
    }

    @Test
    fun `Given a Teleport roll-off die, then the portal holds without re-opening`() {
        val cue =
            ArenaMessageState.Teleport.Roll(
                id = MESSAGE_ID,
                mageUserId = DEFENDER_ID,
                rollerUserId = DEFENDER_ID,
                rollerClassType = CharacterClassType.Mage,
                playerName = "Mage",
                singleDie = 5,
            )
                .combatAnimationCue

        assertEquals(
            expected =
            CombatAnimationCue.Teleport(
                id = MESSAGE_ID,
                mageUserId = DEFENDER_ID,
                phase = TeleportPhase.Hold,
                didEscape = null,
            ),
            actual = cue,
        )
    }

    @Test
    fun `Given a resolved Teleport, then the cue carries the escape outcome and never collapses`() {
        val escaped = teleportResultCue(didDefenderEscape = true)
        val failed = teleportResultCue(didDefenderEscape = false)

        assertEquals(expected = true, actual = (escaped as? CombatAnimationCue.Teleport)?.didEscape)
        assertEquals(expected = false, actual = (failed as? CombatAnimationCue.Teleport)?.didEscape)
        assertNull(actual = escaped?.toCollapsingUserId())
        assertNull(actual = failed?.toCollapsingUserId())
    }

    private fun teleportResultCue(didDefenderEscape: Boolean) = ArenaMessageState.Teleport.Result(
        id = MESSAGE_ID,
        mageUserId = DEFENDER_ID,
        didDefenderEscape = didDefenderEscape,
    )
        .combatAnimationCue

    private fun normalRollMessage(type: NormalRollMessageType) = ArenaMessageState.NormalRoll.Attack(
        id = MESSAGE_ID,
        attackerUserId = ATTACKER_ID,
        defenderUserId = DEFENDER_ID,
        attackerClassType = CharacterClassType.Mage,
        impersonatedClassType = CharacterClassType.None,
        normalRollMessageType = type,
    )

    private fun normalAttackCue(outcome: NormalRollMessageType) = CombatAnimationCue.NormalAttack(
        id = MESSAGE_ID,
        attackerUserId = ATTACKER_ID,
        attackerClassType = CharacterClassType.None,
        defenderUserId = DEFENDER_ID,
        normalRollMessageType = outcome,
        impersonatedClassType = CharacterClassType.None,
    )

    private fun mageAttackCue(outcome: NormalRollMessageType) = CombatAnimationCue.NormalAttack(
        id = MESSAGE_ID,
        attackerUserId = ATTACKER_ID,
        attackerClassType = CharacterClassType.Mage,
        defenderUserId = DEFENDER_ID,
        normalRollMessageType = outcome,
        impersonatedClassType = CharacterClassType.None,
    )

    private fun rogueAttackCue(outcome: NormalRollMessageType) = CombatAnimationCue.NormalAttack(
        id = MESSAGE_ID,
        attackerUserId = ATTACKER_ID,
        attackerClassType = CharacterClassType.Rogue,
        defenderUserId = DEFENDER_ID,
        normalRollMessageType = outcome,
        impersonatedClassType = CharacterClassType.None,
    )

    private fun duelistAttackCue(outcome: NormalRollMessageType) = CombatAnimationCue.NormalAttack(
        id = MESSAGE_ID,
        attackerUserId = ATTACKER_ID,
        attackerClassType = CharacterClassType.Duelist,
        defenderUserId = DEFENDER_ID,
        normalRollMessageType = outcome,
        impersonatedClassType = CharacterClassType.None,
    )

    private fun paladinAttackCue(outcome: NormalRollMessageType) = CombatAnimationCue.NormalAttack(
        id = MESSAGE_ID,
        attackerUserId = ATTACKER_ID,
        attackerClassType = CharacterClassType.Paladin,
        defenderUserId = DEFENDER_ID,
        normalRollMessageType = outcome,
        impersonatedClassType = CharacterClassType.None,
    )

    private fun dodgeMessage(didArmorIncrease: Boolean) = ArenaMessageState.Dodge(
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
