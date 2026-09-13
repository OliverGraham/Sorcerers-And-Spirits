package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.ability.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.effective
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

internal class HolyShieldHandler(private val combatMutator: CombatMutator) {

    fun triggerHolyShield(
        holyShield: DefensiveAbility.HolyShield,
        holyShieldUserId: String,
        knockoutDealerUserId: String,
    ) = combatMutator.triggerHolyShield(
        holyShield = holyShield,
        holyShieldUserId = holyShieldUserId,
        knockoutDealerUserId = knockoutDealerUserId,
    )

    fun attemptPrayer() = combatMutator.attemptPrayer()

    fun wrathRoll() = combatMutator.wrathRoll()

    private fun CombatMutator.triggerHolyShield(
        holyShield: DefensiveAbility.HolyShield,
        holyShieldUserId: String,
        knockoutDealerUserId: String,
    ) {
        addMessage(
            message =
            ArenaMessage.HolyShield.Trigger(
                paladinUserId = holyShieldUserId,
                successRangeMin = abilityConfig.holyShieldSuccessRange.first,
                successRangeMax = abilityConfig.holyShieldSuccessRange.last,
            ),
        )

        updatePlayerClass(userId = holyShieldUserId) { playerClass ->
            playerClass.copy(defensiveAbility = holyShield.copy(usedThisMatch = true))
        }

        setNextAbilityRoll(
            purpose = AbilityAction.Passive.HolyShield.Prayer,
            activeUserIds = setOf(element = holyShieldUserId),
            defenderUserId = holyShieldUserId,
            attackTurn = attackTurnFor(attackerUserId = knockoutDealerUserId),
        )
    }

    private fun CombatMutator.attemptPrayer() {
        val result =
            diceRoller.roll2D6(
                userId = defenderUserId,
                dieColor = dieColorFor(userId = defenderUserId),
            )
        val didSurvive = result.total in abilityConfig.holyShieldSuccessRange

        addMessage(
            message =
            ArenaMessage.HolyShield.Prayer(
                paladinUserId = defenderUserId,
                firstDie = result.firstDie,
                secondDie = result.secondDie,
                total = result.total,
                didSurvive = didSurvive,
            ),
        )

        if (didSurvive) {
            if (result.isDoubles) {
                addMessage(
                    message =
                    ArenaMessage.Doubles(className = defender.className, classType = defender.type),
                )
            }
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
                KillingMomentumHandler(combatMutator = this).triggerKillingMomentum()

            OffensiveAbility.Encore -> EncoreHandler(combatMutator = combatMutator).triggerEncore()

            else -> resolveTurnEnd()
        }
    }

    private fun CombatMutator.wrathRoll() {
        val result =
            diceRoller
                .roll1D6(userId = defenderUserId, dieColor = dieColorFor(userId = defenderUserId))
                .total

        updateAttackerClass { attacker -> attacker.copy(hitPoints = attacker.hitPoints - result) }

        addMessage(
            message =
            ArenaMessage.HolyShield.Wrath(
                paladinUserId = defenderUserId,
                paladinName = defender.className,
                attackerUserId = attackerUserId,
                attackerClassName = attacker.className,
                attackerClassType = attacker.type,
                singleDie = result,
                isAttackerKnockedOut = !attacker.isConscious(),
            ),
        )

        resolveWrath()
    }

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
        @OptIn(ExperimentalContracts::class)
        fun isHolyShieldAvailable(defenderDefensiveAbility: DefensiveAbility): Boolean {
            contract {
                returns(true) implies (defenderDefensiveAbility is DefensiveAbility.HolyShield)
            }
            return defenderDefensiveAbility is DefensiveAbility.HolyShield &&
                !defenderDefensiveAbility.usedThisMatch
        }

        fun triggerHolyShieldIfApplicable(
            combatMutator: CombatMutator,
            defenderDefensiveAbility: DefensiveAbility,
            holyShieldUserId: String = combatMutator.defenderUserId,
            knockoutDealerUserId: String = combatMutator.attackerUserId,
        ): Boolean {
            val shouldTriggerHolyShield = isHolyShieldAvailable(defenderDefensiveAbility)

            if (shouldTriggerHolyShield) {
                HolyShieldHandler(combatMutator = combatMutator)
                    .triggerHolyShield(
                        holyShield = defenderDefensiveAbility,
                        holyShieldUserId = holyShieldUserId,
                        knockoutDealerUserId = knockoutDealerUserId,
                    )
            }

            return shouldTriggerHolyShield
        }

        fun bypassHolyShieldIfApplicable(
            combatMutator: CombatMutator,
            defenderDefensiveAbility: DefensiveAbility,
            holyShieldUserId: String = combatMutator.defenderUserId,
        ) {
            if (isHolyShieldAvailable(defenderDefensiveAbility)) {
                combatMutator.updatePlayerClass(userId = holyShieldUserId) { playerClass ->
                    playerClass.copy(
                        defensiveAbility = defenderDefensiveAbility.copy(usedThisMatch = true),
                    )
                }
            }
        }
    }
}
