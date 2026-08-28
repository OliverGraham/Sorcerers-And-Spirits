package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.AttackTurn
import com.linklabs.sorcerersandspirits.domain.arena.model.Gauntlet
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import com.linklabs.sorcerersandspirits.domain.game.TestCharacterClasses
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.DoublesBonusState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiceRollState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
                    attackTurn = AttackTurn.Standard(attackerUserId = USER_ID),
                    defenderUserId = OPPONENT_ID,
                    activeUserIds = setOf(element = USER_ID),
                ),
            )

        assertEquals(expected = OPPONENT_ID, actual = state.lockedTargetUserId)
    }

    @Test
    fun `Given it is the user's turn to start, then no target is locked`() {
        val state =
            combatState(
                combatActionState = CombatActionState.Idle(activeUserIds = setOf(element = USER_ID)),
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
                    attackTurn = AttackTurn.Standard(attackerUserId = OPPONENT_ID),
                    defenderUserId = USER_ID,
                    activeUserIds = setOf(element = OPPONENT_ID),
                ),
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
                ),
            )

        assertNull(actual = state.lockedTargetUserId)
    }

    @Test
    fun `Given the match-ending roll lands, then the combat view is held and the match is ending`() {
        val nextState =
            endingCombatMatch().toUpdatedState(previousState = null, userId = USER_ID).nextState

        assertTrue(actual = nextState.arenaMatchState is ArenaMatchState.Combat)
        assertTrue(actual = nextState.isMatchEnding)
    }

    @Test
    fun `Given the match-ending roll lands, then the combat timer is stopped`() {
        val update = endingCombatMatch().toUpdatedState(previousState = null, userId = USER_ID)

        assertEquals(expected = TimerAction.Stop, actual = update.timerAction)
    }

    @Test
    fun `Given the screen opens directly onto a finished match, then it shows the finished screen without holding`() {
        // No prior combat view to hold (e.g. reconnecting into an already-finished match), so there
        // is nothing to hold for and the finished screen shows immediately.
        val finishedMatch = finishedMatch(messageCount = 0)

        val nextState =
            finishedMatch.toUpdatedState(previousState = null, userId = USER_ID).nextState

        assertTrue(actual = nextState.arenaMatchState is ArenaMatchState.Finished)
        assertFalse(actual = nextState.isMatchEnding)
    }

    @Test
    fun `Given the user has readied in the match data, then the finished screen shows them as readied`() {
        val finishedMatch = finishedMatch(messageCount = 0, isUserReady = true)

        val nextState =
            finishedMatch.toUpdatedState(previousState = null, userId = USER_ID).nextState

        assertTrue(actual = nextState.hasReadiedForNextMatch)
    }

    @Test
    fun `Given the user readied last match but the next match has not, then readiness does not carry over`() {
        val previousState = screenState(hasReadiedForNextMatch = true)
        val finishedMatch = finishedMatch(messageCount = 0, isUserReady = false)

        val nextState =
            finishedMatch.toUpdatedState(previousState = previousState, userId = USER_ID).nextState

        assertFalse(actual = nextState.hasReadiedForNextMatch)
    }

    @Test
    fun `Given the gauntlet finishes with nobody readied, then the finished screen has no countdown`() {
        val previousState =
            screenState(
                arenaMatchState =
                combatState(
                    combatActionState =
                    CombatActionState.Idle(activeUserIds = setOf(element = OPPONENT_ID)),
                ),
            )
        val finishedMatch =
            finishedMatch(
                messageCount = 0,
                isUserReady = false,
                matchNumberInGauntlet = FINAL_MATCH_NUMBER,
            )

        val update = finishedMatch.toUpdatedState(previousState = previousState, userId = USER_ID)

        assertEquals(expected = TimerAction.Stop, actual = update.timerAction)
    }

    @Test
    fun `Given the first player readies on the gauntlet finished screen, then a one-minute countdown starts`() {
        val previousState =
            screenState(arenaMatchState = ArenaMatchState.Finished(players = emptyList()))
        val finishedMatch =
            finishedMatch(
                messageCount = 0,
                isUserReady = true,
                matchNumberInGauntlet = FINAL_MATCH_NUMBER,
            )

        val update = finishedMatch.toUpdatedState(previousState = previousState, userId = USER_ID)

        assertEquals(expected = TimerAction.Start(timerDuration = 60), actual = update.timerAction)
    }

    @Test
    fun `Given the gauntlet countdown is already running, then another player readying does not restart it`() {
        val previousState =
            screenState(
                arenaMatchState =
                ArenaMatchState.Finished(players = emptyList()),
            )
        val finishedMatch =
            finishedMatch(
                messageCount = 0,
                isUserReady = true,
                matchNumberInGauntlet = FINAL_MATCH_NUMBER,
            )

        val update = finishedMatch.toUpdatedState(previousState = previousState, userId = USER_ID)

        assertEquals(expected = TimerAction.None, actual = update.timerAction)
    }

    @Test
    fun `Given a non-final match finishes, then a thirty-second countdown starts immediately without anyone readying`() {
        val timerAction =
            determineTimerAction(
                previous =
                combatState(
                    combatActionState =
                    CombatActionState.Idle(activeUserIds = setOf(element = OPPONENT_ID)),
                ),
                next = ArenaMatchState.Finished(players = players().values.toList()),
                isFinalMatch = false,
            )

        assertEquals(expected = TimerAction.Start(timerDuration = 30), actual = timerAction)
    }

    @Test
    fun `Given a non-final match countdown is already running, then a later update does not restart it`() {
        val timerAction =
            determineTimerAction(
                previous = ArenaMatchState.Finished(players = players().values.toList()),
                next = ArenaMatchState.Finished(players = players().values.toList()),
                isFinalMatch = false,
            )

        assertEquals(expected = TimerAction.None, actual = timerAction)
    }

    @Test
    fun `Given a non-final match finishes with a lone survivor, then no countdown starts`() {
        val timerAction =
            determineTimerAction(
                previous =
                combatState(
                    combatActionState =
                    CombatActionState.Idle(activeUserIds = setOf(element = OPPONENT_ID)),
                ),
                next = ArenaMatchState.Finished(players = players().values.take(n = 1)),
                isFinalMatch = false,
            )

        assertEquals(expected = TimerAction.Stop, actual = timerAction)
    }

    @Test
    fun `Given the active player used an activated ability that kept the turn, then the combat timer restarts`() {
        val idleTurn = CombatActionState.Idle(activeUserIds = setOf(element = USER_ID))
        val previous = combatState(combatActionState = idleTurn)
        val next =
            combatState(combatActionState = idleTurn, playerMap = playersAfterActivatedAbility())

        val timerAction =
            determineTimerAction(previous = previous, next = next, isFinalMatch = false)

        assertEquals(expected = TimerAction.Start(timerDuration = 45), actual = timerAction)
    }

    @Test
    fun `Given an unrelated update leaves the active player's turn untouched, then the combat timer keeps running`() {
        val idleTurn = CombatActionState.Idle(activeUserIds = setOf(element = USER_ID))
        val previous = combatState(combatActionState = idleTurn)
        val next = combatState(combatActionState = idleTurn)

        val timerAction =
            determineTimerAction(previous = previous, next = next, isFinalMatch = false)

        assertEquals(expected = TimerAction.None, actual = timerAction)
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
            match.toUpdatedState(previousState = null, userId = USER_ID).nextState.arenaMatchState

        assertTrue(actual = nextState is ArenaMatchState.ChoosingClasses)
        assertEquals(
            expected = setOf(CharacterClassType.Gambler),
            actual = nextState.unavailableClasses,
        )
    }

    @Test
    fun `Given two players remain in a finished match, then the next match can start`() {
        val finished = ArenaMatchState.Finished(players = players().values.toList())

        assertTrue(actual = finished.canStartNextMatch)
    }

    @Test
    fun `Given only one player remains in a finished match, then the next match cannot start`() {
        val finished = ArenaMatchState.Finished(players = players().values.take(1))

        assertFalse(actual = finished.canStartNextMatch)
    }

    private fun finishedMatch(
        messageCount: Int,
        isUserReady: Boolean = false,
        matchNumberInGauntlet: Int = 1,
    ) = ArenaMatch.Finished(
        messages =
        (1..messageCount).map { index ->
            IncomingArenaMessage(
                id = "message-$index",
                roundNumber = index,
                message = ArenaMessage.NewRound(roundNumber = index),
            )
        },
        matchNumberInGauntlet = matchNumberInGauntlet,
        playerMap = combatPlayers(isUserReady = isUserReady),
        participants = matchParticipants,
    )

    private fun endingCombatMatch() = ArenaMatch.Combat(
        messages = emptyList(),
        playerMap = combatPlayers(),
        nextCombatAction = NextCombatAction.Idle(activeUserIds = setOf(element = OPPONENT_ID)),
        roundNumber = 1,
        isNewRound = false,
        isMatchOver = true,
        diceRoll = null,
        participants = matchParticipants,
    )

    private fun combatPlayers(isUserReady: Boolean = false) = mapOf(
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

    @Test
    fun `Given the pending doubles bonus is owned by the player about to roll, then the doubles badge shows`() {
        val state =
            combatState(
                combatActionState =
                CombatActionState.NormalAttack(
                    roll = NormalAttackType.Health,
                    attackTurn = AttackTurn.Standard(attackerUserId = USER_ID),
                    defenderUserId = OPPONENT_ID,
                    activeUserIds = setOf(element = USER_ID),
                ),
                diceRollState =
                DiceRollState.TwoDice(
                    firstDie = 3,
                    secondDie = 3,
                    doublesBonus = DoublesBonusState(userId = USER_ID),
                ),
            )

        assertEquals(
            expected = DiceBadgeState.DoublesBonus(diePosition = DiePosition.Second),
            actual = state.diceRollState?.diceBadge,
        )
    }

    @Test
    fun `Given the resting dice still show a doubles roll after the turn passed, then the doubles badge stays`() {
        val state =
            combatState(
                combatActionState =
                CombatActionState.Idle(activeUserIds = setOf(element = OPPONENT_ID)),
                diceRollState =
                DiceRollState.TwoDice(
                    firstDie = 3,
                    secondDie = 3,
                    doublesBonus = DoublesBonusState(userId = USER_ID),
                ),
            )

        assertEquals(
            expected = DiceBadgeState.DoublesBonus(diePosition = DiePosition.Second),
            actual = state.diceRollState?.diceBadge,
        )
    }

    @Test
    fun `Given the same resting doubles roll, then the doubles badge is unchanged by whose turn it is`() {
        val doublesRoll =
            DiceRollState.TwoDice(
                firstDie = 3,
                secondDie = 3,
                doublesBonus = DoublesBonusState(userId = USER_ID),
            )
        val whileRollerActive =
            combatState(
                combatActionState =
                CombatActionState.Idle(activeUserIds = setOf(element = USER_ID)),
                diceRollState = doublesRoll,
            )
        val afterTurnPassed =
            combatState(
                combatActionState =
                CombatActionState.Idle(activeUserIds = setOf(element = OPPONENT_ID)),
                diceRollState = doublesRoll,
            )

        assertEquals(
            expected = whileRollerActive.diceRollState?.diceBadge,
            actual = afterTurnPassed.diceRollState?.diceBadge,
        )
        assertEquals(
            expected = DiceBadgeState.DoublesBonus(diePosition = DiePosition.Second),
            actual = afterTurnPassed.diceRollState?.diceBadge,
        )
    }

    @Test
    fun `Given a mirror image reroll die is showing, then the mirror badge shows even after the turn passes`() {
        val state =
            combatState(
                combatActionState =
                CombatActionState.Idle(activeUserIds = setOf(element = OPPONENT_ID)),
                diceRollState =
                DiceRollState.OneDie(
                    value = 4,
                    diePosition = DiePosition.First,
                    doublesBonus = null,
                    isMirrorImageReroll = true,
                ),
            )

        assertEquals(
            expected = DiceBadgeState.MirrorImageBonus(diePosition = DiePosition.First),
            actual = state.diceRollState?.diceBadge,
        )
    }

    @Test
    fun `Given a Teleport roll-off doubles bonus, then the badge sits over the attacker's roll-off die`() {
        val state =
            combatState(
                combatActionState =
                CombatActionState.AbilityRoll(
                    action =
                    AbilityActionState.Passive.Teleport(
                        attackerRoll =
                        AbilityActionState.Passive.TeleportDieState(
                            userId = OPPONENT_ID,
                            diePosition = DiePosition.First,
                        ),
                        defenderRoll =
                        AbilityActionState.Passive.TeleportDieState(
                            userId = USER_ID,
                            diePosition = DiePosition.Second,
                        ),
                        attackerDoublesBonus = 1,
                    ),
                    attackTurn = AttackTurn.Standard(attackerUserId = OPPONENT_ID),
                    defenderUserId = USER_ID,
                    activeUserIds = setOf(OPPONENT_ID, USER_ID),
                ),
                diceRollState =
                DiceRollState.OneDie(
                    value = 4,
                    diePosition = DiePosition.First,
                    doublesBonus = DoublesBonusState(userId = OPPONENT_ID),
                ),
            )

        assertEquals(
            expected = DiceBadgeState.DoublesBonus(diePosition = DiePosition.First),
            actual = state.diceRollState?.diceBadge,
        )
    }

    @Test
    fun `Given a single die roll that is not a mirror image reroll, then no mirror badge shows`() {
        val state =
            combatState(
                combatActionState =
                CombatActionState.Idle(activeUserIds = setOf(element = OPPONENT_ID)),
                diceRollState =
                DiceRollState.OneDie(
                    value = 4,
                    diePosition = DiePosition.First,
                    doublesBonus = null,
                    isMirrorImageReroll = false,
                ),
            )

        assertNull(actual = state.diceRollState?.diceBadge)
    }

    private fun combatState(
        combatActionState: CombatActionState,
        playerMap: Map<String, PlayerState.Selected> = players(),
        diceRollState: DiceRollState? = null,
    ) = ArenaMatchState.Combat(
        playerMap = playerMap,
        userPlayerId = USER_ID,
        combatActionState = combatActionState,
        roundNumber = 1,
        isNewRound = false,
        isMatchOver = false,
        turnOwnerUserId = USER_ID,
        lockedTargetUserId = lockedTarget(combatActionState = combatActionState),
        diceRollState = diceRollState,
    )

    private fun lockedTarget(combatActionState: CombatActionState): String? {
        val attack = combatActionState as? CombatActionState.NormalAttack ?: return null
        return if (attack.attackTurn.attackerUserId == USER_ID) attack.defenderUserId else null
    }

    private fun holyShieldRoll(
        action: AbilityActionState.Passive.HolyShield,
        defenderUserId: String,
    ) = CombatActionState.AbilityRoll(
        action = action,
        attackTurn = AttackTurn.Standard(attackerUserId = OPPONENT_ID),
        defenderUserId = defenderUserId,
        activeUserIds = setOf(defenderUserId),
    )

    private fun screenState(
        arenaMatchState: ArenaMatchState = ArenaMatchState.Finished(players = emptyList()),
        hasReadiedForNextMatch: Boolean = false,
        messages: List<ArenaMessageState> = emptyList(),
    ) = ArenaScreenState(
        messages = messages,
        secondsRemaining = 0,
        showForfeitDialog = false,
        isUserPlayerKnockedOut = false,
        hasReadiedForNextMatch = hasReadiedForNextMatch,
        arenaMatchState = arenaMatchState,
        participants = emptyMap(),
    )

    private fun players() = mapOf(
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

    private fun playersAfterActivatedAbility(): Map<String, PlayerState.Selected> {
        val actingPlayer = players().getValue(USER_ID)
        val actedClass =
            actingPlayer.characterClass.copy(
                attacksRemaining = actingPlayer.characterClass.attacksRemaining + 1,
            )
        return players() + (USER_ID to actingPlayer.copy(characterClass = actedClass))
    }

    private val matchParticipants =
        mapOf(USER_ID to CharacterClassType.Paladin, OPPONENT_ID to CharacterClassType.Rogue)

    private companion object {
        const val USER_ID = "player1"
        const val OPPONENT_ID = "player2"
        const val FINAL_MATCH_NUMBER = Gauntlet.MATCHES_PER_GAUNTLET
    }
}
