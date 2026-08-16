package com.linklabs.sorcerersandspirits.ui.screens.arena

import com.linklabs.sorcerersandspirits.data.arena.model.toDomain
import com.linklabs.sorcerersandspirits.data.arena.model.toEntity
import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.AttackTurn
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.DiceRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.arena.model.lockedTargetUserId
import com.linklabs.sorcerersandspirits.domain.arena.model.turnOwnerUserId
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ActionOutcome
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.PerformCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.utilities.DiceRoller
import com.linklabs.sorcerersandspirits.domain.arena.utilities.RollResult
import com.linklabs.sorcerersandspirits.domain.game.AbilityConfig
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.MirrorImageRerollSelection
import com.linklabs.sorcerersandspirits.domain.game.TestCharacterClasses
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaCombatControl
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMatchState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatSeating
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.seatingFor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toCombatActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toSelectedState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.resolveTargetUserId
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.serialization.json.Json

/**
 * Reproduces the exact two-phone SS-181 flow end to end: the Rogue's phone rolls an armor hit on
 * the Illusionist, the shared next action round-trips through its persisted entity form, the
 * Illusionist's phone derives its controls and taps Keep Roll, and the resulting action resolves.
 * Every step asserts the combatant ids and the seating both phones would render.
 */
class MirrorImageKeepFlowTest {

    private val diceRoller =
        mockk<DiceRoller> {
            every { getDoublesBonus() } returns null
            every { getDiceRoll() } returns null
            every { mirrorImagePass(firstDie = any(), secondDie = any()) } answers
                {
                    DiceRoll.TwoDice(firstDie = firstArg(), secondDie = secondArg())
                }
            every { roll2D6(userId = ROGUE_ID, dieColor = any()) } returns
                RollResult.TwoDice(firstDie = 3, secondDie = 4, bonus = 0)
        }

    @Test
    fun `Given the Rogue's armor hit triggers Mirror Image and the Illusionist keeps the roll, then the ids and seating stay correct at every step`() {
        val players =
            mapOf(
                ROGUE_ID to
                    ArenaPlayer.Selected(
                        userId = ROGUE_ID,
                        displayName = "Rogue",
                        characterClass = TestCharacterClasses.rogue(),
                    ),
                ILLUSIONIST_ID to
                    ArenaPlayer.Selected(
                        userId = ILLUSIONIST_ID,
                        displayName = "Illusionist",
                        characterClass = TestCharacterClasses.illusionist(),
                    ),
            )

        val armorRollOutcome =
            performAction(
                players = players,
                currentAction =
                    AttackArguments(
                            targetedOpponentUserId = ILLUSIONIST_ID,
                            userPlayerId = ROGUE_ID,
                        )
                        .toDomain(
                            combatActionState =
                                idleState(activeUserId = ROGUE_ID, players = players)
                                    .combatActionState
                        )!!,
            )

        val mirrorAction = assertIs<NextCombatAction.AbilityRoll>(value = armorRollOutcome)
        assertIs<AbilityAction.Passive.MirrorImage>(value = mirrorAction.purpose)
        assertEquals(expected = ROGUE_ID, actual = mirrorAction.attackTurn.attackerUserId)
        assertEquals(expected = ILLUSIONIST_ID, actual = mirrorAction.defenderUserId)
        assertEquals(expected = ROGUE_ID, actual = mirrorAction.attackTurn.turnOwnerUserId)
        assertEquals(expected = setOf(ILLUSIONIST_ID), actual = mirrorAction.activeUserIds)

        val illusionistPhoneState =
            combatStateAfterRoundTrip(
                nextAction = mirrorAction,
                players = players,
                userPlayerId = ILLUSIONIST_ID,
            )

        assertEquals(
            expected = bothSeated(userPlayerId = ILLUSIONIST_ID, opponentUserId = ROGUE_ID),
            actual = seatingOf(state = illusionistPhoneState),
        )

        val keepRollAction = keepRollDispatch(illusionistPhoneState = illusionistPhoneState)

        assertEquals(expected = ROGUE_ID, actual = keepRollAction.attackTurn.attackerUserId)
        assertEquals(expected = ILLUSIONIST_ID, actual = keepRollAction.defenderUserId)
        assertEquals(expected = ROGUE_ID, actual = keepRollAction.attackTurn.turnOwnerUserId)
        assertEquals(expected = ILLUSIONIST_ID, actual = keepRollAction.rollingUserId)

        val keepRollOutcome = performAction(players = players, currentAction = keepRollAction)

        val healthRoll = assertIs<NextCombatAction.NormalAttack>(value = keepRollOutcome)
        assertEquals(expected = NormalAttackType.Health, actual = healthRoll.roll)
        assertEquals(expected = ROGUE_ID, actual = healthRoll.attackTurn.attackerUserId)
        assertEquals(expected = ILLUSIONIST_ID, actual = healthRoll.defenderUserId)
        assertEquals(expected = setOf(ROGUE_ID), actual = healthRoll.activeUserIds)

        val illusionistPhoneAfterKeep =
            combatStateAfterRoundTrip(
                nextAction = healthRoll,
                players = players,
                userPlayerId = ILLUSIONIST_ID,
            )
        val roguePhoneAfterKeep =
            combatStateAfterRoundTrip(
                nextAction = healthRoll,
                players = players,
                userPlayerId = ROGUE_ID,
            )

        assertEquals(
            expected = bothSeated(userPlayerId = ILLUSIONIST_ID, opponentUserId = ROGUE_ID),
            actual = seatingOf(state = illusionistPhoneAfterKeep),
        )
        assertEquals(
            expected = bothSeated(userPlayerId = ROGUE_ID, opponentUserId = ILLUSIONIST_ID),
            actual = seatingOf(state = roguePhoneAfterKeep),
        )

        val staleSecondKeepTap =
            UseAbilityArguments(
                    abilityActionState = keepRollPayload(),
                    targetedOpponentUserId =
                        illusionistPhoneAfterKeep.resolveTargetUserId(selectedOpponentUserId = ""),
                    userPlayerId = ILLUSIONIST_ID,
                )
                .toDomain(combatActionState = illusionistPhoneAfterKeep.combatActionState)

        assertNull(actual = staleSecondKeepTap)
    }

    private fun keepRollPayload() =
        AbilityActionState.Passive.MirrorImage(
            firstDie = AbilityActionState.Passive.MirrorImageDieState(originalRoll = 3),
            secondDie = AbilityActionState.Passive.MirrorImageDieState(originalRoll = 4),
            attackType = NormalAttackType.ArmorClass,
            existingDoublesBonus = 0,
            mirrorImageDefensiveAbility = DefensiveAbility.MirrorImage(),
        )

    @Test
    fun `Given Mirror Image interrupts the Health roll, then the trigger carries the new dice so the table matches the reroll labels`() {
        every { diceRoller.roll2D6(userId = ROGUE_ID, dieColor = any()) } returns
            RollResult.TwoDice(firstDie = 2, secondDie = 5, bonus = 0)
        every { diceRoller.getDiceRoll() } returns
            DiceRoll.TwoDice(firstDie = 2, secondDie = 5, doublesBonus = null)

        val healthRollAction =
            CurrentCombatAction.NormalAttack(
                roll = NormalAttackType.Health,
                attackTurn = AttackTurn.Standard(attackerUserId = ROGUE_ID),
                defenderUserId = ILLUSIONIST_ID,
            )

        val outcome =
            performOutcome(
                players =
                    mapOf(
                        ROGUE_ID to
                            ArenaPlayer.Selected(
                                userId = ROGUE_ID,
                                displayName = "Rogue",
                                characterClass = TestCharacterClasses.rogue(),
                            ),
                        ILLUSIONIST_ID to
                            ArenaPlayer.Selected(
                                userId = ILLUSIONIST_ID,
                                displayName = "Illusionist",
                                characterClass = TestCharacterClasses.illusionist(),
                            ),
                    ),
                currentAction = healthRollAction,
            )

        val mirrorAction = assertIs<NextCombatAction.AbilityRoll>(value = outcome.nextCombatAction)
        val purpose = assertIs<AbilityAction.Passive.MirrorImage>(value = mirrorAction.purpose)
        assertEquals(expected = 2, actual = purpose.firstDie.originalRoll)
        assertEquals(expected = 5, actual = purpose.secondDie.originalRoll)

        val trigger = outcome.messages.filterIsInstance<ArenaMessage.MirrorImage.Trigger>().single()
        assertEquals(expected = 2, actual = trigger.firstDie)
        assertEquals(expected = 5, actual = trigger.secondDie)

        assertEquals(
            expected = DiceRoll.TwoDice(firstDie = 2, secondDie = 5, doublesBonus = null),
            actual = outcome.diceRoll,
        )
    }

    /**
     * The Keep Roll tap exactly as the Illusionist's phone dispatches it: the control derived from
     * the live state, the target resolved by the same function the buttons use, and the arguments
     * mapped to the domain action against the live combat action state.
     */
    private fun keepRollDispatch(
        illusionistPhoneState: ArenaMatchState.Combat
    ): CurrentCombatAction.AbilityRoll {
        val keepRollControl =
            illusionistPhoneState.combatControls
                .filterIsInstance<ArenaCombatControl.AbilityRoll>()
                .single { control ->
                    val mirrorImage = control.action
                    mirrorImage is AbilityActionState.Passive.MirrorImage &&
                        mirrorImage.mirrorImageRerollSelection == MirrorImageRerollSelection.None
                }

        val targetedOpponentUserId =
            illusionistPhoneState.resolveTargetUserId(selectedOpponentUserId = "")

        val dispatchedAction =
            UseAbilityArguments(
                    abilityActionState = keepRollControl.action,
                    targetedOpponentUserId = targetedOpponentUserId,
                    userPlayerId = ILLUSIONIST_ID,
                )
                .toDomain(combatActionState = illusionistPhoneState.combatActionState)

        return assertNotNull(actual = dispatchedAction)
    }

    private fun performAction(
        players: Map<String, ArenaPlayer.Selected>,
        currentAction: CurrentCombatAction,
    ): NextCombatAction =
        performOutcome(players = players, currentAction = currentAction).nextCombatAction

    private fun performOutcome(
        players: Map<String, ArenaPlayer.Selected>,
        currentAction: CurrentCombatAction,
    ): ActionOutcome {
        val performCombatAction =
            PerformCombatAction(
                arguments =
                    CombatActionArguments(
                        arenaId = "arenaId",
                        playerMap = players,
                        roundNumber = 1,
                        doublesBonus = null,
                    ),
                diceRoller = diceRoller,
                abilityConfig = AbilityConfig.DEFAULT,
            )

        return performCombatAction.invoke(currentAction = currentAction)
    }

    /**
     * Maps the shared next action the way it reaches a phone: through its persisted entity form and
     * a serialization round trip, then into the UI state model.
     */
    private fun combatStateAfterRoundTrip(
        nextAction: NextCombatAction,
        players: Map<String, ArenaPlayer.Selected>,
        userPlayerId: String,
    ): ArenaMatchState.Combat {
        val entityJson = Json.encodeToString(value = nextAction.toEntity())
        val roundTrippedAction =
            Json.decodeFromString<
                    com.linklabs.sorcerersandspirits.data.arena.model.NextCombatActionEntity
                >(
                    string = entityJson
                )
                .toDomain()

        return ArenaMatchState.Combat(
            playerMap = players.mapValues { it.value.toSelectedState() },
            userPlayerId = userPlayerId,
            combatActionState = roundTrippedAction.toCombatActionState(),
            roundNumber = 1,
            isNewRound = false,
            isMatchOver = false,
            turnOwnerUserId = roundTrippedAction.turnOwnerUserId(),
            lockedTargetUserId = roundTrippedAction.lockedTargetUserId(userId = userPlayerId),
            diceRollState = null,
        )
    }

    private fun idleState(
        activeUserId: String,
        players: Map<String, ArenaPlayer.Selected>,
    ): ArenaMatchState.Combat =
        combatStateAfterRoundTrip(
            nextAction = NextCombatAction.Idle(activeUserIds = setOf(element = activeUserId)),
            players = players,
            userPlayerId = activeUserId,
        )

    private fun seatingOf(state: ArenaMatchState.Combat): CombatSeating {
        val targetUserId = state.resolveTargetUserId(selectedOpponentUserId = "")
        return state.seatingFor(
            targetUserId = targetUserId,
            isMatchEnding = false,
            collapsingTargetUserId = null,
        )
    }

    private fun bothSeated(userPlayerId: String, opponentUserId: String) =
        CombatSeating(
            topSeatUserId = opponentUserId,
            bottomSeatUserId = userPlayerId,
            headIconUserIds = emptyList(),
        )

    private companion object {
        const val ROGUE_ID = "rogueId"
        const val ILLUSIONIST_ID = "illusionistId"
    }
}
