package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.game.TestCharacterClasses
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArenaScreenStateTest {

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

    @Test
    fun `Given leftover unhandled messages from the prior match exceed the fresh log, then the count is clamped so the log still shows`() {
        val previousState = screenState(unhandledMessagesCount = 10)
        val freshMatch = combatMatch(messageCount = 2, numberOfNewMessages = 2)

        val nextState =
            freshMatch.toUpdatedState(previousState = previousState, userId = USER_ID).nextState

        assertEquals(expected = 2, actual = nextState.unhandledMessagesCount)
        assertNotNull(actual = nextState.latestMessage)
    }

    @Test
    fun `Given a combat result adds more new messages than the capped log holds, then the count is clamped to the log size`() {
        val match = combatMatch(messageCount = 3, numberOfNewMessages = 50)

        val nextState = match.toUpdatedState(previousState = null, userId = USER_ID).nextState

        assertEquals(expected = 3, actual = nextState.unhandledMessagesCount)
        assertNotNull(actual = nextState.latestMessage)
    }

    @Test
    fun `Given a match finishes while closing messages remain, then the combat view is held and the finished state is pending`() {
        val previousState =
            screenState(
                arenaState =
                    combatState(
                        combatActionState =
                            CombatActionState.Idle(activeUserIds = setOf(element = OPPONENT_ID))
                    )
            )
        val finishedMatch = finishedMatch(messageCount = 3, numberOfNewMessages = 3)

        val nextState =
            finishedMatch.toUpdatedState(previousState = previousState, userId = USER_ID).nextState

        assertTrue(actual = nextState.arenaState is ArenaState.Combat)
        assertNotNull(actual = nextState.pendingFinishedState)
    }

    @Test
    fun `Given a match finishes while closing messages remain, then the combat timer is stopped`() {
        val previousState =
            screenState(
                arenaState =
                    combatState(
                        combatActionState =
                            CombatActionState.Idle(activeUserIds = setOf(element = OPPONENT_ID))
                    )
            )
        val finishedMatch = finishedMatch(messageCount = 3, numberOfNewMessages = 3)

        val update = finishedMatch.toUpdatedState(previousState = previousState, userId = USER_ID)

        assertEquals(expected = TimerAction.Stop, actual = update.timerAction)
    }

    @Test
    fun `Given a match finishes with no unhandled messages, then it advances straight to the finished screen`() {
        val previousState =
            screenState(
                arenaState =
                    combatState(
                        combatActionState =
                            CombatActionState.Idle(activeUserIds = setOf(element = OPPONENT_ID))
                    )
            )
        val finishedMatch = finishedMatch(messageCount = 0, numberOfNewMessages = 0)

        val nextState =
            finishedMatch.toUpdatedState(previousState = previousState, userId = USER_ID).nextState

        assertTrue(actual = nextState.arenaState is ArenaState.Finished)
        assertNull(actual = nextState.pendingFinishedState)
    }

    @Test
    fun `Given the user has readied in the match data, then the finished screen shows them as readied`() {
        val finishedMatch =
            finishedMatch(messageCount = 0, numberOfNewMessages = 0, isUserReady = true)

        val nextState =
            finishedMatch.toUpdatedState(previousState = null, userId = USER_ID).nextState

        assertTrue(actual = nextState.hasReadiedForNextMatch)
    }

    @Test
    fun `Given the user readied last match but the next match has not, then readiness does not carry over`() {
        val previousState = screenState(hasReadiedForNextMatch = true)
        val finishedMatch =
            finishedMatch(messageCount = 0, numberOfNewMessages = 0, isUserReady = false)

        val nextState =
            finishedMatch.toUpdatedState(previousState = previousState, userId = USER_ID).nextState

        assertFalse(actual = nextState.hasReadiedForNextMatch)
    }

    private fun finishedMatch(
        messageCount: Int,
        numberOfNewMessages: Int,
        isUserReady: Boolean = false,
    ) =
        ArenaMatch.Finished(
            messages =
                (1..messageCount).map { index ->
                    IncomingArenaMessage(
                        id = "message-$index",
                        message = ArenaMessage.NewRound(roundNumber = index),
                    )
                },
            numberOfNewMessages = numberOfNewMessages,
            playerMap = combatPlayers(isUserReady = isUserReady),
        )

    private fun combatMatch(messageCount: Int, numberOfNewMessages: Int) =
        ArenaMatch.Combat(
            messages =
                (1..messageCount).map { index ->
                    IncomingArenaMessage(
                        id = "message-$index",
                        message = ArenaMessage.NewRound(roundNumber = index),
                    )
                },
            numberOfNewMessages = numberOfNewMessages,
            playerMap = combatPlayers(),
            nextCombatAction = NextCombatAction.Idle(activeUserIds = setOf(element = USER_ID)),
            roundNumber = 1,
            isNewRound = true,
            doublesBonus = null,
        )

    private fun combatPlayers(isUserReady: Boolean = false) =
        mapOf(
            USER_ID to
                ArenaPlayer.Selected(
                    userId = USER_ID,
                    displayName = "Paladin",
                    isReadyForNextMatch = isUserReady,
                    characterClass = TestCharacterClasses.paladin(),
                ),
            OPPONENT_ID to
                ArenaPlayer.Selected(
                    userId = OPPONENT_ID,
                    displayName = "Rogue",
                    characterClass = TestCharacterClasses.rogue(),
                ),
        )

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

    private fun screenState(
        arenaState: ArenaState = ArenaState.Finished(players = emptyList()),
        unhandledMessagesCount: Int = 0,
        hasReadiedForNextMatch: Boolean = false,
    ) =
        ArenaScreenState(
            messages = emptyList(),
            unhandledMessagesCount = unhandledMessagesCount,
            secondsRemaining = 0,
            showForfeitDialog = false,
            isUserPlayerKnockedOut = false,
            hasReadiedForNextMatch = hasReadiedForNextMatch,
            arenaState = arenaState,
            participantClassNames = emptyMap(),
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
