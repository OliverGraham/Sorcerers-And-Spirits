package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CombatControlDiceCountTest {

    @Test
    fun `normal attack rolls two dice`() {
        val control = ArenaCombatControl.NormalAttack(requiresTarget = true)

        assertEquals(expected = 2, actual = control.diceCount)
    }

    @Test
    fun `single-die ability rolls report one die`() {
        assertEquals(
            expected = 1,
            actual = abilityRoll(AbilityActionState.Passive.Encore).diceCount,
        )
        assertEquals(
            expected = 1,
            actual = abilityRoll(AbilityActionState.Passive.Persuade.StrumLute).diceCount,
        )
        assertEquals(
            expected = 1,
            actual = abilityRoll(AbilityActionState.Passive.HolyShield.Wrath).diceCount,
        )
    }

    @Test
    fun `two-die ability rolls report two dice`() {
        assertEquals(
            expected = 2,
            actual =
                abilityRoll(AbilityActionState.Passive.Riposte(attackerRollTotal = 7)).diceCount,
        )
        assertEquals(
            expected = 2,
            actual = abilityRoll(AbilityActionState.Passive.HolyShield.Prayer).diceCount,
        )
    }

    @Test
    fun `activated abilities report their roll size`() {
        val allIn =
            AbilityActionState.Activated.AllIn(
                allInAbility =
                    OffensiveAbility.AllIn(hasAnotherAttack = false, usedThisTurn = false)
            )

        assertEquals(expected = 1, actual = activatedAbility(allIn).diceCount)
        assertEquals(
            expected = 2,
            actual = activatedAbility(AbilityActionState.Activated.PrepareAttack).diceCount,
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
                    OffensiveAbility.Impersonate(impersonatedAbility = OffensiveAbility.Encore)
            )

        assertNull(actual = abilityRoll(charm).diceCount)
        assertNull(actual = activatedAbility(impersonate).diceCount)
    }

    private fun abilityRoll(action: AbilityActionState) =
        ArenaCombatControl.AbilityRoll(action = action, label = "", requiresTarget = false)

    private fun activatedAbility(action: AbilityActionState.Activated) =
        ArenaCombatControl.ActivatedAbility(
            action = action,
            label = "",
            requiresTarget = false,
            enabled = true,
        )
}
