package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.game.TestCharacterClasses
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArenaScreenStateTest {

    @Test
    fun `Given the user's Paladin is knocked out but still owes a Holy Shield roll, then no knocked-out decision is offered`() {
        val state =
            combatScreenState(
                isUserPlayerKnockedOut = true,
                combatActionState =
                    holyShieldRoll(
                        action = AbilityActionState.Passive.HolyShield.Prayer,
                        defenderUserId = USER_ID,
                    ),
            )

        assertFalse(actual = state.isKnockedOutDecisionOffered)
    }

    @Test
    fun `Given the user is knocked out after Holy Shield resolves, then a knocked-out decision is offered`() {
        val state =
            combatScreenState(
                isUserPlayerKnockedOut = true,
                combatActionState =
                    CombatActionState.Idle(activeUserIds = setOf(element = OPPONENT_ID)),
            )

        assertTrue(actual = state.isKnockedOutDecisionOffered)
    }

    @Test
    fun `Given another player owes a Holy Shield roll while the user is knocked out, then a knocked-out decision is offered`() {
        val state =
            combatScreenState(
                isUserPlayerKnockedOut = true,
                combatActionState =
                    holyShieldRoll(
                        action = AbilityActionState.Passive.HolyShield.Prayer,
                        defenderUserId = OPPONENT_ID,
                    ),
            )

        assertTrue(actual = state.isKnockedOutDecisionOffered)
    }

    @Test
    fun `Given the user already readied for the next match, then no knocked-out decision is offered`() {
        val state =
            combatScreenState(
                isUserPlayerKnockedOut = true,
                hasReadiedForNextMatch = true,
                combatActionState =
                    CombatActionState.Idle(activeUserIds = setOf(element = OPPONENT_ID)),
            )

        assertFalse(actual = state.isKnockedOutDecisionOffered)
    }

    @Test
    fun `Given the match has finished, then no knocked-out decision is offered`() {
        val state = screenState(arenaState = ArenaState.Finished(players = emptyList()))

        assertFalse(actual = state.isKnockedOutDecisionOffered)
    }

    @Test
    fun `Given the user owes a follow-up roll for their own normal attack, then the target is locked to the defender`() {
        val state =
            combatState(
                combatActionState =
                    CombatActionState.NormalAttack(
                        roll = NormalAttackType.Health,
                        attackerUserId = USER_ID,
                        defenderUserId = OPPONENT_ID,
                        originalTurnUserId = USER_ID,
                        activeUserIds = setOf(element = USER_ID),
                    )
            )

        assertEquals(expected = OPPONENT_ID, actual = state.lockedTargetUserId)
    }

    @Test
    fun `Given it is the user's turn to start, then no target is locked`() {
        val state =
            combatState(
                combatActionState = CombatActionState.Idle(activeUserIds = setOf(element = USER_ID))
            )

        assertNull(actual = state.lockedTargetUserId)
    }

    @Test
    fun `Given an opponent is mid normal attack, then the user has no locked target`() {
        val state =
            combatState(
                combatActionState =
                    CombatActionState.NormalAttack(
                        roll = NormalAttackType.Health,
                        attackerUserId = OPPONENT_ID,
                        defenderUserId = USER_ID,
                        originalTurnUserId = OPPONENT_ID,
                        activeUserIds = setOf(element = OPPONENT_ID),
                    )
            )

        assertNull(actual = state.lockedTargetUserId)
    }

    @Test
    fun `Given the user owes an ability roll, then no target is locked`() {
        val state =
            combatState(
                combatActionState =
                    holyShieldRoll(
                        action = AbilityActionState.Passive.HolyShield.Prayer,
                        defenderUserId = USER_ID,
                    )
            )

        assertNull(actual = state.lockedTargetUserId)
    }

    private fun combatState(combatActionState: CombatActionState) =
        ArenaState.Combat(
            playerMap = players(),
            userPlayerId = USER_ID,
            combatActionState = combatActionState,
            roundNumber = 1,
            isNewRound = false,
            doublesBonus = null,
        )

    private fun holyShieldRoll(
        action: AbilityActionState.Passive.HolyShield,
        defenderUserId: String,
    ) =
        CombatActionState.AbilityRoll(
            action = action,
            attackerUserId = OPPONENT_ID,
            defenderUserId = defenderUserId,
            originalTurnUserId = OPPONENT_ID,
            activeUserIds = setOf(defenderUserId),
        )

    private fun combatScreenState(
        isUserPlayerKnockedOut: Boolean,
        combatActionState: CombatActionState,
        hasReadiedForNextMatch: Boolean = false,
    ) =
        screenState(
            isUserPlayerKnockedOut = isUserPlayerKnockedOut,
            hasReadiedForNextMatch = hasReadiedForNextMatch,
            arenaState = combatState(combatActionState = combatActionState),
        )

    private fun screenState(
        arenaState: ArenaState,
        isUserPlayerKnockedOut: Boolean = false,
        hasReadiedForNextMatch: Boolean = false,
    ) =
        ArenaScreenState(
            messages = emptyList(),
            unhandledMessagesCount = 0,
            secondsRemaining = 0,
            showForfeitDialog = false,
            isUserPlayerKnockedOut = isUserPlayerKnockedOut,
            hasReadiedForNextMatch = hasReadiedForNextMatch,
            arenaState = arenaState,
            participantNames = emptyMap(),
        )

    private fun players() =
        mapOf(
            USER_ID to
                PlayerState.Selected(
                    userId = USER_ID,
                    displayName = "Paladin",
                    characterClass = TestCharacterClasses.paladin().toState(),
                ),
            OPPONENT_ID to
                PlayerState.Selected(
                    userId = OPPONENT_ID,
                    displayName = "Rogue",
                    characterClass = TestCharacterClasses.rogue().toState(),
                ),
        )

    private companion object {
        const val USER_ID = "player1"
        const val OPPONENT_ID = "player2"
    }
}
