package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

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
        val didSurvive = result.total in HOLY_SHIELD_SUCCESS_RANGE

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
        val attackerAbility = attacker.offensiveAbility

        when (attackerAbility) {
            OffensiveAbility.KillingMomentum ->
                TriggerKillingMomentum(combatMutator = this).invoke()

            OffensiveAbility.Encore -> EncoreHandler(combatMutator = combatMutator).triggerEncore()

            else -> resolveTurnEnd()
        }
    }

    private fun CombatMutator.wrathRoll() {
        val result = diceRoller.roll1D6(userId = defenderUserId).total

        addMessage(
            message =
                ArenaMessage.HolyShield.Wrath(
                    paladinUserId = defenderUserId,
                    attackerUserId =
                        attackerUserId, // TODO(SS-054) SpellPotency - what if is splash damage?
                    singleDie = result,
                )
        )

        updateAttackerClass { attacker -> attacker.copy(hitPoints = attacker.hitPoints - result) }

        resolveWrath()
    }

    private fun CombatMutator.resolveWrath() {
        val isAttackerConscious = attacker.isConscious()
        val attackerAbility = attacker.offensiveAbility

        when {
            isAttackerConscious && attackerAbility == OffensiveAbility.KillingMomentum ->
                TriggerKillingMomentum(combatMutator = this).invoke()

            isAttackerConscious && attackerAbility == OffensiveAbility.Encore ->
                EncoreHandler(combatMutator = combatMutator).triggerEncore()

            else -> resolveTurnEnd()
        }
    }

    companion object {
        // TODO(SS-058): From RemoteConfig in some way
        private val HOLY_SHIELD_SUCCESS_RANGE = 10..12

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
