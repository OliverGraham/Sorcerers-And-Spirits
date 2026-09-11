package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.ability.DefensiveAbility

internal class DodgeHandler(
    private val combatMutator: CombatMutator,
    private val dodge: DefensiveAbility.Dodge,
) {
    fun triggerDodge() = combatMutator.triggerDodge()

    private fun CombatMutator.triggerDodge() {
        val didArmorIncrease = dodge.armorBonus < abilityConfig.dodgeMaxArmorBonus
        val updatedArmorBonus = if (didArmorIncrease) dodge.armorBonus + 1 else dodge.armorBonus

        if (didArmorIncrease) {
            val updatedDodge = dodge.copy(armorBonus = updatedArmorBonus)
            updateDefenderClass { playerClass ->
                playerClass.copy(
                    armorClass = playerClass.armorClass + 1,
                    defensiveAbility = updatedDodge,
                )
            }
        }

        addMessage(
            message =
            ArenaMessage.Dodge(
                didArmorIncrease = didArmorIncrease,
                dodgerUserId = defenderUserId,
                newArmorClass = defender.armorClass,
                armorBonus = updatedArmorBonus,
                maxArmorBonus = abilityConfig.dodgeMaxArmorBonus,
            ),
        )
    }
}
