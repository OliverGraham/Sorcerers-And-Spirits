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
                messageType = NormalRollMessageType.ArmorClassMiss,
            )

        val state = message.toSingleState()

        val temptFate = assertIs<ArenaMessageState.MirrorImage.TemptFate>(value = state)
        assertEquals(expected = 4, actual = temptFate.rawReroll)
        assertEquals(expected = 3, actual = temptFate.finalRoll)
        assertEquals(expected = "4 - 1", actual = temptFate.rerollDisplay)
    }

    @Test
    fun `Given a Mirror Image reroll of a 1, when mapped to state, then the penalised result is 0`() {
        val message =
            ArenaMessage.MirrorImage.TemptFate(
                illusionistUserId = DEFENDER_ID,
                rerolledMirrorImageDie =
                    AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 6, reroll = 1),
                attackerUserId = ATTACKER_ID,
                messageType = NormalRollMessageType.ArmorClassMiss,
            )

        val state = message.toSingleState()

        val temptFate = assertIs<ArenaMessageState.MirrorImage.TemptFate>(value = state)
        assertEquals(expected = 1, actual = temptFate.rawReroll)
        assertEquals(expected = 0, actual = temptFate.finalRoll)
        assertEquals(expected = "1 - 1", actual = temptFate.rerollDisplay)
    }

    @Test
    fun `Given a kept Mirror Image health roll, then its flash callout shows the attack outcome like a normal hit`() {
        val message =
            ArenaMessage.MirrorImage.Pass(
                illusionistUserId = DEFENDER_ID,
                attackerUserId = ATTACKER_ID,
                messageType = NormalRollMessageType.Damage,
                total = 8,
                doublesBonus = 1,
            )

        val callout = message.toSingleState().toFlashCallout()

        assertEquals(
            expected =
                FlashCallout.Damage(
                    amount = 8,
                    actingClassType = CharacterClassType.Mage,
                    doublesBonus = 1,
                ),
            actual = callout,
        )
    }

    @Test
    fun `Given a Killing Momentum trigger, then it carries the attacker of the knockout roll`() {
        val states =
            listOf(
                    incomingNormalRoll(id = "roll", type = NormalRollMessageType.Knockout),
                    IncomingArenaMessage(
                        id = "trigger",
                        message = ArenaMessage.KillingMomentum(attackerUserId = ATTACKER_ID),
                    ),
                )
                .toStates(participants = participants)

        assertEquals(
            expected =
                ArenaMessageState.KillingMomentum(id = "trigger", attackerUserId = ATTACKER_ID),
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
                        message =
                            ArenaMessage.Dodge(didArmorIncrease = true, dodgerUserId = DEFENDER_ID),
                    ),
                )
                .toStates(participants = participants)

        assertEquals(
            expected =
                ArenaMessageState.Dodge(
                    id = "trigger",
                    didArmorIncrease = true,
                    dodgerUserId = DEFENDER_ID,
                ),
            actual = states.last(),
        )
    }

    private fun ArenaMessage.toSingleState(): ArenaMessageState =
        listOf(IncomingArenaMessage(id = MESSAGE_ID, message = this))
            .toStates(participants = participants)
            .single()

    private fun incomingNormalRoll(id: String, type: NormalRollMessageType) =
        IncomingArenaMessage(
            id = id,
            message =
                ArenaMessage.NormalRoll(
                    attackerUserId = ATTACKER_ID,
                    defenderUserId = DEFENDER_ID,
                    firstDie = 2,
                    secondDie = 3,
                    bonus = 0,
                    type = type,
                    impersonatedCharacterType = CharacterClassType.None,
                ),
        )

    private val participants =
        mapOf(
            ATTACKER_ID to ParticipantClass(className = "Mage", type = CharacterClassType.Mage),
            DEFENDER_ID to ParticipantClass(className = "Rogue", type = CharacterClassType.Rogue),
        )

    private companion object {
        private const val MESSAGE_ID = "message-1"
        private const val ATTACKER_ID = "attacker"
        private const val DEFENDER_ID = "defender"
    }
}
