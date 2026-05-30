package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

/** Action the user is taking (UI -> Domain -> Data) */
sealed interface CurrentCombatAction {
    data class NormalAttack(val roll: NormalAttackRoll) : CurrentCombatAction

    data class AbilityRoll(val purpose: AbilityRollPurpose) : CurrentCombatAction
}

sealed interface NormalAttackRoll {
    data class ArmorClass(val attackerUserId: String, val defenderUserId: String) : NormalAttackRoll

    data class Health(val attackerUserId: String, val defenderUserId: String) : NormalAttackRoll
}

/** The next action for the user (Data -> Domain -> UI) */
sealed interface NextCombatAction {

    data class Idle(val activeUserId: String) : NextCombatAction

    data class NormalAttack(val roll: NormalAttackRoll) : NextCombatAction

    data class AbilityRoll(val purpose: AbilityRollPurpose) : NextCombatAction
}

data class SingleDieRoll(val userId: String, val roll: Int? = null)

sealed interface AbilityRollPurpose {

    sealed interface Activated : AbilityRollPurpose {
        data class AllIn(val allInAbility: OffensiveAbility.AllIn) : Activated

        data class SummonMinion(val summonMinionAbility: OffensiveAbility.SummonMinion) : Activated

        data object PrepareAttack : Activated

        data class Impersonate(val impersonateAbility: OffensiveAbility.Impersonate) : Activated
    }

    sealed interface Passive : AbilityRollPurpose {

        data class Teleport(
            val attackerDieRoll: SingleDieRoll,
            val defenderDieRoll: SingleDieRoll,
        ) : Passive {
            companion object {

                /** On first break of Mage's ArmorClass, each roll is null and blah blah */
                fun getPreRolledState(attackerUserId: String, defenderUserId: String) =
                    Teleport(
                        attackerDieRoll = SingleDieRoll(userId = attackerUserId),
                        defenderDieRoll = SingleDieRoll(userId = defenderUserId),
                    )
            }
        }

        // TODO HolyShield - do the userIds need to be passed in?
        sealed interface HolyShield : Passive {
            data class Prayer(val userId: String) : HolyShield

            data class Wrath(val userId: String) : HolyShield
        }



        /**
         * A NormalAttack against ArmorClass can trigger one of these, and then the flow of combat
         * changes such that the defender is now Active and they will resolve their ability
         */
        sealed interface Reaction : Passive {

            // If someone attacks MAge, then this would signal that now the Mage is going to roll

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

            // TODO LuckyCoin SATURDAY TOMORROW

            // TODO: All these need state to be passed through, likely
            sealed interface LuckyCoin : Outcome {
                data object ArmorClass : LuckyCoin

                data object Reflect : LuckyCoin
            }

            sealed interface HolyShield : Outcome {
                data object Survival : HolyShield

                data object Reflect : HolyShield
            }

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
