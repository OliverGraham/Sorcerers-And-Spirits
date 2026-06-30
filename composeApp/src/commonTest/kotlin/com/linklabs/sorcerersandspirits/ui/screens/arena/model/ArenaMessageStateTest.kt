package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
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
                rerolledMirrorImageDie =
                    AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 5, reroll = 4),
                attackerUserId = ATTACKER_ID,
                outcome = NormalRollMessageType.ArmorClassMiss,
            )

        val state =
            message.toState(
                id = MESSAGE_ID,
                participants =
                    mapOf(
                        ATTACKER_ID to
                            ParticipantClass(
                                className = ATTACKER_CLASS_NAME,
                                type = CharacterClassType.Mage,
                            )
                    ),
            )

        val temptFate = assertIs<ArenaMessageState.MirrorImage.TemptFate>(value = state)
        assertEquals(expected = 4, actual = temptFate.rawReroll)
        assertEquals(expected = 3, actual = temptFate.finalRoll)
        assertEquals(expected = "4 - 1", actual = temptFate.rerollDisplay)
    }

    @Test
    fun `Given a Mirror Image reroll of a 1, when mapped to state, then the penalised result is 0`() {
        val message =
            ArenaMessage.MirrorImage.TemptFate(
                rerolledMirrorImageDie =
                    AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 6, reroll = 1),
                attackerUserId = ATTACKER_ID,
                outcome = NormalRollMessageType.ArmorClassMiss,
            )

        val state =
            message.toState(
                id = MESSAGE_ID,
                participants =
                    mapOf(
                        ATTACKER_ID to
                            ParticipantClass(
                                className = ATTACKER_CLASS_NAME,
                                type = CharacterClassType.Mage,
                            )
                    ),
            )

        val temptFate = assertIs<ArenaMessageState.MirrorImage.TemptFate>(value = state)
        assertEquals(expected = 1, actual = temptFate.rawReroll)
        assertEquals(expected = 0, actual = temptFate.finalRoll)
        assertEquals(expected = "1 - 1", actual = temptFate.rerollDisplay)
    }

    private companion object {
        private const val MESSAGE_ID = "message-1"
        private const val ATTACKER_ID = "attacker"
        private const val ATTACKER_CLASS_NAME = "Mage"
    }
}
