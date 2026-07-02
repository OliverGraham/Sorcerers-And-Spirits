package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.ui.screens.arena.AttackArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.UseAbilityArguments
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CombatActionDispatchTest {

    @Test
    fun `Given the owed Mirror Image step, then the defender's choice dispatches with the in-flight combatant ids`() {
        val action =
            mirrorImagePayload()
                .asUseAbilityArguments()
                .toDomain(
                    combatActionState =
                        abilityRoll(action = mirrorImagePayload(), defenderUserId = USER_ID)
                )

        val dispatched = assertNotNull(actual = action)
        assertEquals(expected = OPPONENT_ID, actual = dispatched.attackerUserId)
        assertEquals(expected = USER_ID, actual = dispatched.defenderUserId)
        assertEquals(expected = OPPONENT_ID, actual = dispatched.originalTurnUserId)
        assertEquals(expected = USER_ID, actual = dispatched.rollingUserId)
    }

    @Test
    fun `Given the state has moved past the Mirror Image step, then a late Mirror Image tap is dropped`() {
        val healthRollInFlight =
            CombatActionState.NormalAttack(
                roll = NormalAttackType.Health,
                attackerUserId = OPPONENT_ID,
                defenderUserId = USER_ID,
                originalTurnUserId = OPPONENT_ID,
                activeUserIds = setOf(element = OPPONENT_ID),
            )

        val action =
            mirrorImagePayload()
                .asUseAbilityArguments()
                .toDomain(combatActionState = healthRollInFlight)

        assertNull(actual = action)
    }

    @Test
    fun `Given the turn has already passed to the user, then a late passive ability tap is dropped instead of self-targeting`() {
        val action =
            mirrorImagePayload()
                .asUseAbilityArguments()
                .toDomain(
                    combatActionState =
                        CombatActionState.Idle(activeUserIds = setOf(element = USER_ID))
                )

        assertNull(actual = action)
    }

    @Test
    fun `Given a different ability step is owed, then a payload for another ability is dropped`() {
        val action =
            mirrorImagePayload()
                .asUseAbilityArguments()
                .toDomain(
                    combatActionState =
                        abilityRoll(
                            action = AbilityActionState.Passive.Riposte(attackerRollTotal = 7),
                            defenderUserId = USER_ID,
                        )
                )

        assertNull(actual = action)
    }

    @Test
    fun `Given the user's own idle turn, then activating an ability dispatches from the user's ids`() {
        val action =
            UseAbilityArguments(
                    abilityActionState =
                        AbilityActionState.Activated.AllIn(allInAbility = OffensiveAbility.AllIn()),
                    targetedOpponentUserId = "",
                    userPlayerId = USER_ID,
                )
                .toDomain(
                    combatActionState =
                        CombatActionState.Idle(activeUserIds = setOf(element = USER_ID))
                )

        val dispatched = assertNotNull(actual = action)
        assertEquals(expected = USER_ID, actual = dispatched.attackerUserId)
        assertEquals(expected = USER_ID, actual = dispatched.originalTurnUserId)
    }

    @Test
    fun `Given an opponent's idle turn, then the user's activated ability tap is dropped`() {
        val action =
            UseAbilityArguments(
                    abilityActionState =
                        AbilityActionState.Activated.AllIn(allInAbility = OffensiveAbility.AllIn()),
                    targetedOpponentUserId = "",
                    userPlayerId = USER_ID,
                )
                .toDomain(
                    combatActionState =
                        CombatActionState.Idle(activeUserIds = setOf(element = OPPONENT_ID))
                )

        assertNull(actual = action)
    }

    @Test
    fun `Given the user's own idle turn, then an attack dispatches from the user's ids`() {
        val action =
            AttackArguments(targetedOpponentUserId = OPPONENT_ID, userPlayerId = USER_ID)
                .toDomain(
                    combatActionState =
                        CombatActionState.Idle(activeUserIds = setOf(element = USER_ID))
                )

        val dispatched = assertNotNull(actual = action)
        assertEquals(expected = USER_ID, actual = dispatched.attackerUserId)
        assertEquals(expected = OPPONENT_ID, actual = dispatched.defenderUserId)
    }

    @Test
    fun `Given an opponent's idle turn, then the user's late attack tap is dropped`() {
        val action =
            AttackArguments(targetedOpponentUserId = OPPONENT_ID, userPlayerId = USER_ID)
                .toDomain(
                    combatActionState =
                        CombatActionState.Idle(activeUserIds = setOf(element = OPPONENT_ID))
                )

        assertNull(actual = action)
    }

    @Test
    fun `Given the user's own attack owes its Health roll, then the attack continues with the in-flight ids`() {
        val healthRollInFlight =
            CombatActionState.NormalAttack(
                roll = NormalAttackType.Health,
                attackerUserId = USER_ID,
                defenderUserId = OPPONENT_ID,
                originalTurnUserId = USER_ID,
                activeUserIds = setOf(element = USER_ID),
            )

        val action =
            AttackArguments(targetedOpponentUserId = OPPONENT_ID, userPlayerId = USER_ID)
                .toDomain(combatActionState = healthRollInFlight)

        val dispatched = assertNotNull(actual = action)
        assertEquals(expected = NormalAttackType.Health, actual = dispatched.roll)
        assertEquals(expected = USER_ID, actual = dispatched.attackerUserId)
    }

    @Test
    fun `Given an opponent's attack is in flight, then the user's attack tap is dropped`() {
        val opponentsAttack =
            CombatActionState.NormalAttack(
                roll = NormalAttackType.Health,
                attackerUserId = OPPONENT_ID,
                defenderUserId = USER_ID,
                originalTurnUserId = OPPONENT_ID,
                activeUserIds = setOf(element = OPPONENT_ID),
            )

        val action =
            AttackArguments(targetedOpponentUserId = OPPONENT_ID, userPlayerId = USER_ID)
                .toDomain(combatActionState = opponentsAttack)

        assertNull(actual = action)
    }

    private fun mirrorImagePayload() =
        AbilityActionState.Passive.MirrorImage(
            firstDie = AbilityActionState.Passive.MirrorImageDieState(originalRoll = 3),
            secondDie = AbilityActionState.Passive.MirrorImageDieState(originalRoll = 4),
            attackType = NormalAttackType.ArmorClass,
            attackerDoublesBonus = 0,
            mirrorImageDefensiveAbility = DefensiveAbility.MirrorImage(),
            rerollSelection = AbilityAction.Passive.MirrorImage.RerollSelection.None,
        )

    private fun AbilityActionState.asUseAbilityArguments() =
        UseAbilityArguments(
            abilityActionState = this,
            targetedOpponentUserId = USER_ID,
            userPlayerId = USER_ID,
        )

    private fun abilityRoll(action: AbilityActionState, defenderUserId: String) =
        CombatActionState.AbilityRoll(
            action = action,
            attackerUserId = OPPONENT_ID,
            defenderUserId = defenderUserId,
            originalTurnUserId = OPPONENT_ID,
            activeUserIds = setOf(element = defenderUserId),
        )

    private companion object {
        const val USER_ID = "userId"
        const val OPPONENT_ID = "opponentId"
    }
}
