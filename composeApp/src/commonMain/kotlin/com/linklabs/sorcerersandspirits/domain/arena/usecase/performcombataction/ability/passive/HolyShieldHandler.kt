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

    /**
     * A doubles prayer only earns its +1 when the Paladin survives: they keep the dice and roll
     * Wrath next. A failed prayer knocks the Paladin out and the attacker's follow-up rolls next,
     * so the bonus is not consecutive and is left unannounced.
     */
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
        /**
         * Whether an unused Holy Shield would trigger for this defender. A pending Holy Shield
         * means a knockout is not yet final — the survival roll still has to happen — so callers
         * use this to phrase the triggering hit as damage rather than as a knockout.
         */
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
