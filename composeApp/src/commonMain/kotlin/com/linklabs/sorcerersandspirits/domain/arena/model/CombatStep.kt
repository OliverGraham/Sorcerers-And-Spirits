package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

/** Action the user is taking (UI -> Domain -> Data) */
sealed interface CurrentCombatAction {
    sealed interface NormalAttackRoll : CurrentCombatAction {
        data object ArmorClassRoll : NormalAttackRoll

        data object HealthRoll : NormalAttackRoll
    }

    data class AbilityRoll(val purpose: AbilityRollPurpose) : CurrentCombatAction
}

/** The next action for the user (Data -> Domain -> UI) */
sealed interface NextCombatAction {

    data object Idle : NextCombatAction

    sealed interface NormalAttackRoll : NextCombatAction {
        data object ArmorClassRoll : NormalAttackRoll

        data object HealthRoll : NormalAttackRoll
    }

    data class AbilityRoll(val purpose: AbilityRollPurpose) : NextCombatAction
}

sealed interface AbilityRollPurpose {

    // someone attacks Gambler -> Gambler reacts if AC broken (ReactionType) -> If ReactionType,
    // then
    // Gambler rolls a die and die outcome (OutcomeType) -> Outcome and turn resolultion
    sealed interface Activated : AbilityRollPurpose {
        data class AllIn(val allInAbility: OffensiveAbility.AllIn) : Activated

        data class SummonMinion(val summonMinionAbility: OffensiveAbility.SummonMinion) : Activated

        data object PrepareAttack : Activated

        data class Impersonate(val impersonateAbility: OffensiveAbility.Impersonate) : Activated
    }

    sealed interface Passive : AbilityRollPurpose {

        /**
         * A NormalAttack against ArmorClass can trigger one of these, and then the flow of combat
         * changes such that the defender is now Active and they will resolve their ability
         */
        sealed interface Reaction : Passive {

            // If someone attacks MAge, then this would signal that now the Mage is going to roll
            data object Teleport : Reaction

            data object Riposte : Reaction

            // TODO: Fix Persuade
            data object Persuade : Reaction

            data object LuckyCoin : Reaction

            data object HolyShield : Reaction

            data class MirrorImage(
                val firstDie: Int,
                val secondDie: Int,
                val triggeredByArmorRoll: Boolean,
            ) : Reaction
        }

        /**
         * This is what happens after the [Reaction]. For example, if a Mage Teleports as an initial
         * reaction, then here that will continue and now the Attacker will roll and with those two
         * rolls, the combat outcome can be determined
         */
        sealed interface Outcome : Passive {

            // And then here, the Mage's roll would be compared to the attacker's
            data class Teleport(val defenderRoll: Int) : Outcome

            // TODO: All these need state to be passed through, likely
            data object LuckyCoin : Outcome

            data object HolyShield : Outcome

            data object Encore : Outcome

            // TODO: and these should be consolidated or be sealed
            data class MirrorImagePass(
                val firstDie: Int,
                val secondDie: Int,
                val triggeredByArmorRoll: Boolean,
            ) : Outcome

            data class MirrorImageReroll(
                val firstDie: Int,
                val secondDie: Int,
                val rerollFirstDie: Boolean,
                val triggeredByArmorRoll: Boolean,
            ) : Outcome
        }
    }
}

sealed interface RollPurpose {
    data object TeleportDefenderEscape : RollPurpose

    data class TeleportAttackerChase(val defenderRoll: Int) : RollPurpose

    data object RiposteCounter : RollPurpose

    data object PersuadeStopOrRedirect : RollPurpose

    data object LuckyCoinArmorBonus : RollPurpose

    data object LuckyCoinReflect : RollPurpose

    data object AllInOutcome : RollPurpose

    data object SummonMinionRoll : RollPurpose

    data object PrepareAttackRoll : RollPurpose

    data object EncoreCheck : RollPurpose

    data object HolyShieldSurvival : RollPurpose

    data object HolyShieldRetaliation : RollPurpose

    data class MirrorImageChoice(
        val firstDie: Int,
        val secondDie: Int,
        val triggeredByArmorRoll: Boolean,
    ) : RollPurpose

    data class MirrorImagePass(
        val firstDie: Int,
        val secondDie: Int,
        val triggeredByArmorRoll: Boolean,
    ) : RollPurpose

    data class MirrorImageReroll(
        val firstDie: Int,
        val secondDie: Int,
        val rerollFirstDie: Boolean,
        val triggeredByArmorRoll: Boolean,
    ) : RollPurpose

    data object ImpersonateChoice : RollPurpose
}
