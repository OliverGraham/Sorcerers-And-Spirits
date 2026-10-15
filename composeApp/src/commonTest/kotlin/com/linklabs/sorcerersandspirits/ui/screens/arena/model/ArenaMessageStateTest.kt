package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ArenaMessageStateTest {

    @Test
    fun `Given a Mirror Image reroll, when mapped to state, then it surfaces the raw die and its -1 penalty`() {
        val message =
            ArenaMessage.MirrorImage.TemptFate(
                illusionistUserId = DEFENDER_ID,
                rerolledMirrorImageDie =
                AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 5, reroll = 4),
                attackerUserId = ATTACKER_ID,
                attackerClassName = ATTACKER_CLASS_NAME,
                attackerClassType = CharacterClassType.Mage,
                messageType = NormalRollMessageType.ArmorClassMiss,
            )

        val state = message.toSingleState()

        val temptFate = assertIs<ArenaMessageState.MirrorImage.TemptFate>(value = state)
        assertEquals(expected = 4, actual = temptFate.rawReroll)
        assertEquals(expected = 3, actual = temptFate.finalRoll)
        assertEquals(expected = "4 − 1 = 3", actual = temptFate.rerollDisplay)
    }

    @Test
    fun `Given a Mirror Image reroll of a 1, when mapped to state, then the penalised result is 0`() {
        val message =
            ArenaMessage.MirrorImage.TemptFate(
                illusionistUserId = DEFENDER_ID,
                rerolledMirrorImageDie =
                AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 6, reroll = 1),
                attackerUserId = ATTACKER_ID,
                attackerClassName = ATTACKER_CLASS_NAME,
                attackerClassType = CharacterClassType.Mage,
                messageType = NormalRollMessageType.ArmorClassMiss,
            )

        val state = message.toSingleState()

        val temptFate = assertIs<ArenaMessageState.MirrorImage.TemptFate>(value = state)
        assertEquals(expected = 1, actual = temptFate.rawReroll)
        assertEquals(expected = 0, actual = temptFate.finalRoll)
        assertEquals(expected = "1 − 1 = 0", actual = temptFate.rerollDisplay)
    }

    @Test
    fun `Given a kept Mirror Image health roll, then the state carries the attack outcome like a normal hit`() {
        val message =
            ArenaMessage.MirrorImage.Pass(
                illusionistUserId = DEFENDER_ID,
                attackerUserId = ATTACKER_ID,
                attackerClassName = ATTACKER_CLASS_NAME,
                attackerClassType = CharacterClassType.Mage,
                messageType = NormalRollMessageType.Damage,
                baseRoll = 7,
                doublesBonus = 1,
            )

        val pass = assertIs<ArenaMessageState.MirrorImage.Pass>(value = message.toSingleState())
        assertEquals(expected = 7, actual = pass.baseRoll)
        assertEquals(expected = 1, actual = pass.doublesBonus)
        assertEquals(expected = NormalRollMessageType.Damage, actual = pass.messageType)
        assertEquals(expected = CharacterClassType.Mage, actual = pass.attackerClassType)
    }

    @Test
    fun `Given a Killing Momentum trigger, then it carries the attacker of the knockout roll`() {
        val states =
            listOf(
                incomingNormalRoll(id = "roll", type = NormalRollMessageType.Knockout),
                IncomingArenaMessage(
                    id = "trigger",
                    roundNumber = 1,
                    message = ArenaMessage.KillingMomentum(attackerUserId = ATTACKER_ID),
                ),
            )
                .toMessageStates()

        assertEquals(
            expected =
            ArenaMessageState.KillingMomentum(
                id = "trigger",
                attackerUserId = ATTACKER_ID,
                gestures = mapOf(ATTACKER_ID to AvatarGesture.ReArm),
            ),
            actual = states.last(),
        )
    }

    @Test
    fun `Given a Dodge trigger, then it carries the defender of the missed roll`() {
        val states =
            listOf(
                incomingNormalRoll(id = "roll", type = NormalRollMessageType.ArmorClassMiss),
                IncomingArenaMessage(
                    id = "trigger",
                    roundNumber = 1,
                    message =
                    ArenaMessage.Dodge(
                        didArmorIncrease = true,
                        dodgerUserId = DEFENDER_ID,
                        newArmorClass = 11,
                        armorBonus = 1,
                        maxArmorBonus = 2,
                    ),
                ),
            )
                .toMessageStates()

        assertEquals(
            expected =
            ArenaMessageState.Dodge(
                id = "trigger",
                didArmorIncrease = true,
                dodgerUserId = DEFENDER_ID,
                newArmorClass = 11,
                armorBonus = 1,
                maxArmorBonus = 2,
                gestures = mapOf(DEFENDER_ID to AvatarGesture.DodgeBrace),
            ),
            actual = states.last(),
        )
    }

    @Test
    fun `Given a normal damage roll with a doubles bonus, then the state carries the base roll and the bonus separately`() {
        val state = outcomeRoll(type = NormalRollMessageType.Damage).toSingleState()

        val outcome = assertIs<ArenaMessageState.NormalRoll.Outcome>(value = state)
        assertEquals(expected = 8, actual = outcome.baseRoll)
        assertEquals(expected = 1, actual = outcome.bonus)
        assertEquals(expected = NormalRollMessageType.Damage, actual = outcome.normalRollMessageType)
        assertEquals(expected = CharacterClassType.Mage, actual = outcome.attackerClassType)
    }

    @Test
    fun `Given a knockout roll with a doubles bonus, then the state carries the base roll and the bonus separately`() {
        val state = outcomeRoll(type = NormalRollMessageType.Knockout).toSingleState()

        val outcome = assertIs<ArenaMessageState.NormalRoll.Outcome>(value = state)
        assertEquals(expected = 8, actual = outcome.baseRoll)
        assertEquals(expected = 1, actual = outcome.bonus)
        assertEquals(expected = NormalRollMessageType.Knockout, actual = outcome.normalRollMessageType)
    }

    @Test
    fun `Given an armor-miss roll with a doubles bonus, then the state carries the base roll and the bonus separately`() {
        val state = outcomeRoll(type = NormalRollMessageType.ArmorClassMiss).toSingleState()

        val outcome = assertIs<ArenaMessageState.NormalRoll.Outcome>(value = state)
        assertEquals(expected = 8, actual = outcome.baseRoll)
        assertEquals(expected = 1, actual = outcome.bonus)
        assertEquals(expected = NormalRollMessageType.ArmorClassMiss, actual = outcome.normalRollMessageType)
    }

    private fun ArenaMessage.toSingleState(): ArenaMessageState = listOf(IncomingArenaMessage(id = MESSAGE_ID, roundNumber = 1, message = this))
        .toMessageStates()
        .single()

    private fun outcomeRoll(type: NormalRollMessageType) = ArenaMessage.NormalRoll.Outcome(
        attackerUserId = ATTACKER_ID,
        defenderUserId = DEFENDER_ID,
        attackerClassName = ATTACKER_CLASS_NAME,
        attackerClassType = CharacterClassType.Mage,
        minionAttackerName = null,
        defenderClassName = DEFENDER_CLASS_NAME,
        defenderClassType = CharacterClassType.Rogue,
        defenderArmorClass = 10,
        firstDie = 4,
        secondDie = 4,
        bonus = 1,
        type = type,
        damageAfterMinionShield = null,
    )

    private fun incomingNormalRoll(id: String, type: NormalRollMessageType) = IncomingArenaMessage(
        id = id,
        roundNumber = 1,
        message =
        ArenaMessage.NormalRoll.Outcome(
            attackerUserId = ATTACKER_ID,
            defenderUserId = DEFENDER_ID,
            attackerClassName = ATTACKER_CLASS_NAME,
            attackerClassType = CharacterClassType.Mage,
            minionAttackerName = null,
            defenderClassName = DEFENDER_CLASS_NAME,
            defenderClassType = CharacterClassType.Rogue,
            defenderArmorClass = 10,
            firstDie = 2,
            secondDie = 3,
            bonus = 0,
            type = type,
            damageAfterMinionShield = null,
        ),
    )

    private companion object {
        private const val MESSAGE_ID = "message-1"
        private const val ATTACKER_ID = "attacker"
        private const val DEFENDER_ID = "defender"
        private const val ATTACKER_CLASS_NAME = "Mage"
        private const val DEFENDER_CLASS_NAME = "Rogue"
    }
}
