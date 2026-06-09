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
            data class Flip(val attackerRollTotal: Int) : LuckyCoin

            data class HighRoller(val attackerUserId: String, val attackerRollTotal: Int) :
                LuckyCoin
        }

        data class Riposte(val attackerRollTotal: Int) : Passive

        sealed interface Persuade : Passive {
            data object StrumLute : Persuade

            // TODO Persuade: In UI - need to pass in selectedCharmTargetUserId so the
            //  defenderUserId is Bard's selected target
            data class Charm(
                val attackerUserId: String,
                val charmTargetUserIds: Set<String>,
                val selectedCharmTargetUserId: String? = null,
            ) : Persuade
        }

        data object Encore : Passive

        data class Teleport(val attackerRoll: Int? = null, val defenderRoll: Int? = null) : Passive

        data class MirrorImage(
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
            data object Prayer : HolyShield

            data object Wrath : HolyShield
        }
    }
}
