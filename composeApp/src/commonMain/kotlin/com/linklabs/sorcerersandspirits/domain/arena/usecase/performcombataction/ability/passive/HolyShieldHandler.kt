package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.effective
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

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
        addMessage(message = ArenaMessage.HolyShield.Trigger(paladinUserId = holyShieldUserId))

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
                    paladinUserId = defenderUserId,
                    firstDie = result.firstDie,
                    secondDie = result.secondDie,
                    didSurvive = didSurvive,
                )
        )

        if (didSurvive) {
            if (result.isDoubles) {
                addMessage(message = ArenaMessage.Doubles(userId = defenderUserId))
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

    /**
     * Wrath retaliates against the attacker: the player whose turn dealt the damage. This holds
     * even when the Paladin was knocked out by Mage Spell Potency splash damage rather than a
     * direct hit, because the Mage is still the attacker, so the unblockable Wrath damage lands on
     * them.
     */
    private fun CombatMutator.wrathRoll() {
        val result = diceRoller.roll1D6(userId = defenderUserId).total

        updateAttackerClass { attacker -> attacker.copy(hitPoints = attacker.hitPoints - result) }

        addMessage(
            message =
                ArenaMessage.HolyShield.Wrath(
                    paladinUserId = defenderUserId,
                    attackerUserId = attackerUserId,
                    singleDie = result,
                    isAttackerKnockedOut = !attacker.isConscious(),
                )
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
        ): Boolean {
            val shouldTriggerHolyShield = isHolyShieldAvailable(defenderDefensiveAbility)

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
            if (isHolyShieldAvailable(defenderDefensiveAbility)) {
                combatMutator.updatePlayerClass(userId = holyShieldUserId) { playerClass ->
                    playerClass.copy(
                        defensiveAbility = defenderDefensiveAbility.copy(usedThisMatch = true)
                    )
                }
            }
        }
    }
}
