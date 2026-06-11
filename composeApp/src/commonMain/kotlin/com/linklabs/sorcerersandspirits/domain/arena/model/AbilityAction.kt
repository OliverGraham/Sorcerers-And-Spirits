package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

sealed interface AbilityAction {

    sealed interface Activated : AbilityAction {
        data class AllIn(val allInAbility: OffensiveAbility.AllIn) : Activated

        data class SummonMinion(val summonMinionAbility: OffensiveAbility.SummonMinion) : Activated

        data object PrepareAttack : Activated

        data class Impersonate(val impersonateAbility: OffensiveAbility.Impersonate) : Activated
    }

    sealed interface Passive : AbilityAction {

        sealed interface LuckyCoin : Passive {
            data class Flip(val attackerUserId: String, val attackerRollTotal: Int) : LuckyCoin

            data class HighRoller(val attackerUserId: String, val attackerRollTotal: Int) :
                LuckyCoin
        }

        data class Riposte(val attackerUserId: String, val attackerRollTotal: Int) : Passive

        sealed interface Persuade : Passive {
            data class StrumLute(val attackerUserId: String) : Persuade

            // TODO Persuade: In UI - need to pass in selectedCharmTargetUserId so the
            //  defenderUserId is Bard's selected target
            data class Charm(
                val attackerUserId: String,
                val targetableUserIds: Set<String>,
                val selectedCharmTargetUserId: String? = null,
            ) : Persuade
        }

        data class Encore(val defenderUserId: String) : Passive

        data class Teleport(val attackerRoll: TeleportDie, val defenderRoll: TeleportDie) :
            Passive {

            data class TeleportDie(val userId: String, val roll: Int? = null)
        }

        data class MirrorImage(
            val attackerUserId: String,
            val firstDie: MirrorImageDie,
            val secondDie: MirrorImageDie,
            val attackType: NormalAttackType,
            val mirrorImageDefensiveAbility: DefensiveAbility.MirrorImage,
        ) : Passive {
            val isReroll = firstDie.isRerolled || secondDie.isRerolled

            data class MirrorImageDie(val originalRoll: Int, val reroll: Int? = null) {
                val isRerolled = reroll != null
                val finalValue = if (reroll == null) originalRoll else reroll - 1
            }
        }

        sealed interface HolyShield : Passive {
            data class Prayer(val attackerUserId: String) : HolyShield

            data class Wrath(val attackerUserId: String) : HolyShield
        }
    }
}
