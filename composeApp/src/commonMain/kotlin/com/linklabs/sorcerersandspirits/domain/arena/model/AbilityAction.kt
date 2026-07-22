package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.MirrorImageRerollSelection
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
            val attackerRollTotal: Int

            data class Flip(override val attackerRollTotal: Int) : LuckyCoin

            data class HighRoller(override val attackerRollTotal: Int) : LuckyCoin
        }

        data class Riposte(val attackerRollTotal: Int) : Passive

        sealed interface Persuade : Passive {

            data object StrumLute : Persuade

            data class Charm(
                val targetableUserIds: Set<String>,
                val selectedCharmTargetUserId: String? = null,
            ) : Persuade
        }

        data object Encore : Passive

        data class Teleport(
            val attackerRoll: TeleportDie,
            val defenderRoll: TeleportDie,
            val attackerDoublesBonus: Int,
        ) : Passive {

            data class TeleportDie(val userId: String, val roll: Int? = null)
        }

        data class MirrorImage(
            val firstDie: MirrorImageDie,
            val secondDie: MirrorImageDie,
            val attackType: NormalAttackType,
            val existingDoublesBonus: Int,
            val mirrorImageDefensiveAbility: DefensiveAbility.MirrorImage,
            val mirrorImageRerollSelection: MirrorImageRerollSelection =
                MirrorImageRerollSelection.None,
        ) : Passive {
            data class MirrorImageDie(val originalRoll: Int, val reroll: Int? = null) {
                val finalValue = if (reroll == null) originalRoll else reroll - 1
            }
        }

        sealed interface HolyShield : Passive {
            data object Prayer : HolyShield

            data object Wrath : HolyShield
        }
    }
}
