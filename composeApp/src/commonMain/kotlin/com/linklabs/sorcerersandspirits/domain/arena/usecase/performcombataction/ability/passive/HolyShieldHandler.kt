package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.effective

internal class HolyShieldHandler(private val combatMutator: CombatMutator) {

    fun triggerHolyShield(holyShield: DefensiveAbility.HolyShield, holyShieldUserId: String) =
        combatMutator.triggerHolyShield(
            holyShield = holyShield,
            holyShieldUserId = holyShieldUserId,
        )

    fun attemptPrayer() = combatMutator.attemptPrayer()

    fun wrathRoll() = combatMutator.wrathRoll()

    private fun CombatMutator.triggerHolyShield(
        holyShield: DefensiveAbility.HolyShield,
        holyShieldUserId: String,
    ) {
        addMessage(message = ArenaMessage.HolyShield.Trigger)

        updatePlayerClass(userId = holyShieldUserId) { playerClass ->
            playerClass.copy(defensiveAbility = holyShield.copy(usedThisMatch = true))
        }

        setNextAbilityRoll(
            purpose = AbilityAction.Passive.HolyShield.Prayer,
            activeUserIds = setOf(element = holyShieldUserId),
            defenderUserId = holyShieldUserId,
        )
    }

    private fun CombatMutator.attemptPrayer() {
        val result = diceRoller.roll2D6(userId = defenderUserId)
        val didSurvive = result.total in abilityConfig.holyShieldSuccessRange

        addMessage(
            message =
                ArenaMessage.HolyShield.Prayer(
                    firstDie = result.firstDie,
                    secondDie = result.secondDie,
                    didSurvive = didSurvive,
                )
        )

        if (didSurvive) {
            prayerSuccess()
        } else {
            prayerFailure()
        }
    }

    private fun CombatMutator.prayerSuccess() {
        updateDefenderClass { defender -> defender.copy(hitPoints = 1) }

        setNextAbilityRoll(
            purpose = AbilityAction.Passive.HolyShield.Wrath,
            activeUserIds = setOf(element = defenderUserId),
        )
    }

    private fun CombatMutator.prayerFailure() {
        val attackerAbility = attacker.offensiveAbility.effective

        when (attackerAbility) {
            OffensiveAbility.KillingMomentum ->
                TriggerKillingMomentum(combatMutator = this).invoke()

            OffensiveAbility.Encore -> EncoreHandler(combatMutator = combatMutator).triggerEncore()

            else -> resolveTurnEnd()
        }
    }

    /**
     * Wrath retaliates against the attacker: the player whose turn dealt the damage. This holds
     * even when the Paladin was knocked out by Mage Spell Potency splash damage rather than a
     * direct hit, because the Mage is still the attacker, so the unblockable Wrath damage lands on
     * them.
     */
    private fun CombatMutator.wrathRoll() {
        val result = diceRoller.roll1D6(userId = defenderUserId).total

        addMessage(
            message =
                ArenaMessage.HolyShield.Wrath(
                    paladinUserId = defenderUserId,
                    attackerUserId = attackerUserId,
                    singleDie = result,
                )
        )

        updateAttackerClass { attacker -> attacker.copy(hitPoints = attacker.hitPoints - result) }

        resolveWrath()
    }

    /**
     * Wrath is only reached after a successful Holy Shield, so the Paladin survived: the attacker
     * scored no knockout and Killing Momentum is not earned. The attacker did still deal damage,
     * however, so a still-conscious Bard's Encore applies.
     */
    private fun CombatMutator.resolveWrath() {
        val isAttackerConscious = attacker.isConscious()
        val shouldTriggerEncore =
            isAttackerConscious && attacker.offensiveAbility.effective == OffensiveAbility.Encore

        if (shouldTriggerEncore) {
            EncoreHandler(combatMutator = combatMutator).triggerEncore()
        } else {
            resolveTurnEnd()
        }
    }

    companion object {
        fun triggerHolyShieldIfApplicable(
            combatMutator: CombatMutator,
            defenderDefensiveAbility: DefensiveAbility,
            holyShieldUserId: String = combatMutator.defenderUserId,
        ): Boolean {
            val shouldTriggerHolyShield =
                defenderDefensiveAbility is DefensiveAbility.HolyShield &&
                    !defenderDefensiveAbility.usedThisMatch

            if (shouldTriggerHolyShield) {
                HolyShieldHandler(combatMutator = combatMutator)
                    .triggerHolyShield(
                        holyShield = defenderDefensiveAbility,
                        holyShieldUserId = holyShieldUserId,
                    )
            }

            return shouldTriggerHolyShield
        }

        fun bypassHolyShieldIfApplicable(
            combatMutator: CombatMutator,
            defenderDefensiveAbility: DefensiveAbility,
            holyShieldUserId: String = combatMutator.defenderUserId,
        ) {
            if (
                defenderDefensiveAbility is DefensiveAbility.HolyShield &&
                    !defenderDefensiveAbility.usedThisMatch
            ) {
                combatMutator.updatePlayerClass(userId = holyShieldUserId) { playerClass ->
                    playerClass.copy(
                        defensiveAbility = defenderDefensiveAbility.copy(usedThisMatch = true)
                    )
                }
            }
        }
    }
}
