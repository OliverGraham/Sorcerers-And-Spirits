package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import kotlin.test.Test
import kotlin.test.assertEquals

class CombatControlDiceCountTest {

    @Test
    fun `normal attack rolls two dice`() {
        val control = ArenaCombatControl.NormalAttack(requiresTarget = true)

        assertEquals(expected = NumberOfDice.Two, actual = control.numberOfDice)
    }

    @Test
    fun `single-die ability rolls report one die`() {
        assertEquals(
            expected = NumberOfDice.One,
            actual = abilityRoll(AbilityActionState.Passive.Encore).numberOfDice,
        )
        assertEquals(
            expected = NumberOfDice.One,
            actual = abilityRoll(AbilityActionState.Passive.Persuade.StrumLute).numberOfDice,
        )
        assertEquals(
            expected = NumberOfDice.One,
            actual = abilityRoll(AbilityActionState.Passive.HolyShield.Wrath).numberOfDice,
        )
    }

    @Test
    fun `two-die ability rolls report two dice`() {
        assertEquals(
            expected = NumberOfDice.Two,
            actual =
            abilityRoll(AbilityActionState.Passive.Riposte(attackerRollTotal = 7)).numberOfDice,
        )
        assertEquals(
            expected = NumberOfDice.Two,
            actual = abilityRoll(AbilityActionState.Passive.HolyShield.Prayer).numberOfDice,
        )
    }

    @Test
    fun `activated abilities report their roll size`() {
        val allIn =
            AbilityActionState.Activated.AllIn(
                allInAbility =
                OffensiveAbility.AllIn(hasAnotherAttack = false, usedThisTurn = false),
            )

        assertEquals(expected = NumberOfDice.One, actual = activatedAbility(allIn).numberOfDice)
        assertEquals(
            expected = NumberOfDice.Two,
            actual = activatedAbility(AbilityActionState.Activated.PrepareAttack).numberOfDice,
        )
    }

    @Test
    fun `choice-only controls have no dice`() {
        val charm =
            AbilityActionState.Passive.Persuade.Charm(
                targetableUserIds = emptySet(),
                selectedCharmTargetUserId = null,
            )
        val impersonate =
            AbilityActionState.Activated.Impersonate(
                impersonateAbility =
                OffensiveAbility.Impersonate(impersonatedAbility = OffensiveAbility.Encore),
            )

        assertEquals(expected = NumberOfDice.None, actual = abilityRoll(charm).numberOfDice)
        assertEquals(
            expected = NumberOfDice.None,
            actual = activatedAbility(impersonate).numberOfDice,
        )
    }

    private fun abilityRoll(action: AbilityActionState) = ArenaCombatControl.AbilityRoll(
        action = action,
        label = "",
        requiresTarget = false,
        rollingDiePositions = emptySet(),
    )

    private fun activatedAbility(action: AbilityActionState.Activated) = ArenaCombatControl.ActivatedAbility(
        action = action,
        label = "",
        requiresTarget = false,
        enabled = true,
    )
}
