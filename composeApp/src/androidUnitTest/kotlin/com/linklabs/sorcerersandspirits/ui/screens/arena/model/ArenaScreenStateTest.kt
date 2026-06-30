package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
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
    fun `Given a successful Riposte reversed the attack, then the attacker and target avatars are different players`() {
        // Rogue (turn owner) attacked the Duelist, who riposted successfully and now attacks the
        // Rogue back. It stays the Rogue's turn, but the Duelist is the one pressing the attack, so
        // the left avatar must seat the Duelist - not the turn owner - or both avatar slots
        // collapse
        // onto the Rogue and the Duelist drops to a head icon (SS-165).
        val reversedAttack =
            CombatActionState.NormalAttack(
                roll = NormalAttackType.ArmorClass,
                attackerUserId = OPPONENT_ID,
                defenderUserId = USER_ID,
                originalTurnUserId = USER_ID,
                activeUserIds = setOf(element = OPPONENT_ID),
            )
        val state = combatState(combatActionState = reversedAttack)

        val targetUserId = state.resolveTargetUserId(selectedOpponentUserId = "")

        val seatedUserIds = state.seatedAvatarUserIds(targetUserId = targetUserId)

        assertEquals(expected = USER_ID, actual = state.turnOwnerUserId)
        assertEquals(expected = OPPONENT_ID, actual = state.attackingUserId)
        assertEquals(expected = USER_ID, actual = targetUserId)
        assertTrue(actual = state.attackingUserId != targetUserId)
        assertEquals(expected = setOf(OPPONENT_ID, USER_ID), actual = seatedUserIds)
        assertTrue(actual = state.headIconPlayersFor(seatedUserIds = seatedUserIds).isEmpty())
    }

    @Test
    fun `Given a self-targeted ability names the actor as its own defender, then the actor is not seated in both avatar slots`() {
        // A self-targeted ability (e.g. Summon Minion) sets the defender to the actor, so the
        // attacker and target resolve to the same player. Only the actor is seated; the opponent
        // stays a head icon rather than the actor taking both slots (SS-168).
        val selfTargeted =
            CombatActionState.AbilityRoll(
                action = AbilityActionState.Activated.PrepareAttack,
                attackerUserId = OPPONENT_ID,
                defenderUserId = OPPONENT_ID,
                originalTurnUserId = OPPONENT_ID,
                activeUserIds = setOf(element = OPPONENT_ID),
            )
        val state = combatState(combatActionState = selfTargeted)

        val targetUserId = state.resolveTargetUserId(selectedOpponentUserId = "")
        val seatedUserIds = state.seatedAvatarUserIds(targetUserId = targetUserId)

        assertNull(actual = state.seatedTargetPlayer(targetUserId = targetUserId))
        assertEquals(expected = setOf(OPPONENT_ID), actual = seatedUserIds)
        assertEquals(
            expected = listOf(USER_ID),
            actual = state.headIconPlayersFor(seatedUserIds = seatedUserIds).map { it.userId },
        )
    }

    @Test
    fun `Given a match finishes from a combat turn, then the combat view is held and the finished state is pending`() {
        val previousState =
            screenState(
                arenaState =
                    combatState(
                        combatActionState =
                            CombatActionState.Idle(activeUserIds = setOf(element = OPPONENT_ID))
                    )
            )
        // The hold is unconditional on Combat→Finished now (SS-154): it does not depend on any
        // unhandled-message count, so an empty closing log still holds for the flash.
        val finishedMatch = finishedMatch(messageCount = 0, numberOfNewMessages = 0)

        val nextState =
            finishedMatch.toUpdatedState(previousState = previousState, userId = USER_ID).nextState

        assertTrue(actual = nextState.arenaState is ArenaState.Combat)
        assertNotNull(actual = nextState.pendingFinishedState)
    }

    @Test
    fun `Given a match finishes from a combat turn, then the combat timer is stopped`() {
        val previousState =
            screenState(
                arenaState =
                    combatState(
                        combatActionState =
                            CombatActionState.Idle(activeUserIds = setOf(element = OPPONENT_ID))
                    )
            )
        val finishedMatch = finishedMatch(messageCount = 0, numberOfNewMessages = 0)

        val update = finishedMatch.toUpdatedState(previousState = previousState, userId = USER_ID)

        assertEquals(expected = TimerAction.Stop, actual = update.timerAction)
    }

    @Test
    fun `Given the screen opens directly onto a finished match, then it shows the finished screen without holding`() {
        // No prior combat view to hold (e.g. reconnecting into an already-finished match), so there
        // is nothing to hold for and the finished screen shows immediately.
        val finishedMatch = finishedMatch(messageCount = 0, numberOfNewMessages = 0)

        val nextState =
            finishedMatch.toUpdatedState(previousState = null, userId = USER_ID).nextState

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

    @Test
    fun `Given the match finishes with nobody readied, then the finished screen has no countdown`() {
        val previousState =
            screenState(
                arenaState =
                    combatState(
                        combatActionState =
                            CombatActionState.Idle(activeUserIds = setOf(element = OPPONENT_ID))
                    )
            )
        val finishedMatch =
            finishedMatch(messageCount = 0, numberOfNewMessages = 0, isUserReady = false)

        val update = finishedMatch.toUpdatedState(previousState = previousState, userId = USER_ID)

        assertEquals(expected = TimerAction.Stop, actual = update.timerAction)
    }

    @Test
    fun `Given the first player readies on the finished screen, then a one-minute countdown starts`() {
        val previousState = screenState(arenaState = ArenaState.Finished(players = emptyList()))
        val finishedMatch =
            finishedMatch(messageCount = 0, numberOfNewMessages = 0, isUserReady = true)

        val update = finishedMatch.toUpdatedState(previousState = previousState, userId = USER_ID)

        assertEquals(expected = TimerAction.Start(timerDuration = 60), actual = update.timerAction)
    }

    @Test
    fun `Given the countdown is already running, then another player readying does not restart it`() {
        val previousState =
            screenState(
                arenaState = ArenaState.Finished(players = emptyList(), hasAnyPlayerReadied = true)
            )
        val finishedMatch =
            finishedMatch(messageCount = 0, numberOfNewMessages = 0, isUserReady = true)

        val update = finishedMatch.toUpdatedState(previousState = previousState, userId = USER_ID)

        assertEquals(expected = TimerAction.None, actual = update.timerAction)
    }

    @Test
    fun `Given an opponent selected a class whose stats diverge from the template, then that class type is still unavailable to the user`() {
        // A selected player's class carries live, mutated stats (here reduced HP), so it is not
        // value-equal to the master template. The no-duplicate rule keys on class type, so the
        // type must still be excluded — otherwise a second chooser (e.g. the computer) could pick
        // the same class (SS-122).
        val woundedGambler = TestCharacterClasses.gambler().copy(hitPoints = 1)
        val match =
            ArenaMatch.ChoosingClasses(
                messages = emptyList(),
                numberOfNewMessages = 0,
                playerMap =
                    mapOf(
                        USER_ID to ArenaPlayer.Unselected(userId = USER_ID, displayName = "Player"),
                        OPPONENT_ID to
                            ArenaPlayer.Selected(
                                userId = OPPONENT_ID,
                                displayName = "Gambler",
                                characterClass = woundedGambler,
                            ),
                    ),
                activePlayer = ArenaPlayer.Unselected(userId = USER_ID, displayName = "Player"),
                activePlayerUserId = USER_ID,
            )

        val nextState =
            match.toUpdatedState(previousState = null, userId = USER_ID).nextState.arenaState

        assertTrue(actual = nextState is ArenaState.ChoosingClasses)
        assertEquals(
            expected = setOf(CharacterClassType.Gambler),
            actual = nextState.unavailableClasses,
        )
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
        hasReadiedForNextMatch: Boolean = false,
    ) =
        ArenaScreenState(
            messages = emptyList(),
            secondsRemaining = 0,
            showForfeitDialog = false,
            isUserPlayerKnockedOut = false,
            hasReadiedForNextMatch = hasReadiedForNextMatch,
            arenaState = arenaState,
            participantClassNames = emptyMap(),
            numberOfNewMessages = 0,
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
