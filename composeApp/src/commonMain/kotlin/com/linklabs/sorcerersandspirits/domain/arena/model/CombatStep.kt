package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

/** Action the user is taking (UI -> Domain -> Data) */
sealed interface CurrentCombatAction {
    data class NormalAttack(val roll: NormalAttackRoll) : CurrentCombatAction

    data class AbilityRoll(val purpose: AbilityRollPurpose) : CurrentCombatAction
}

sealed interface NormalAttackRoll {
    data class ArmorClass(
        val attackerUserId: String,
        val defenderUserId: String,
        val originalTurnUserId: String = attackerUserId,
    ) : NormalAttackRoll

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

        sealed interface LuckyCoin : Passive {
            data class Flip(val attackerRollTotal: Int, val activeUserId: String) : LuckyCoin

            data class HighRoller(
                val attackerUserId: String,
                val attackerRollTotal: Int,
                val activeUserId: String,
            ) : LuckyCoin
        }

        data class Riposte(val attackerRollTotal: Int) : Passive

        sealed interface Persuade : Passive {
            data class StrumLute(val attackerRollTotal: Int, val bardUserId: String) : Persuade

            // TODO Persuade: In UI - need to pass in selectedCharmTargetUserId so the
            //  defenderUserId is Bard's selected target
            data class Charm(
                val attackerUserId: String,
                val bardUserId: String,
                val charmTargetUserIds: Set<String>,
                val selectedCharmTargetUserId: String? = null,
            ) : Persuade
        }

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

        sealed interface HolyShield : Passive {
            data class Prayer(val activeUserId: String) : HolyShield

            data class Wrath(val activeUserId: String) : HolyShield
        }

        /**
         * A NormalAttack against ArmorClass can trigger one of these, and then the flow of combat
         * changes such that the defender is now Active and they will resolve their ability
         */
        sealed interface Reaction : Passive {

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
