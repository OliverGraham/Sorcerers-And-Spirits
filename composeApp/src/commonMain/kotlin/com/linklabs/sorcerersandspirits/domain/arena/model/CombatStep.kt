package com.linklabs.sorcerersandspirits.domain.arena.model

sealed interface CombatStep {
    data object ArmorClassRoll : CombatStep

    data object HealthRoll : CombatStep

    data class AbilityRoll(val purpose: RollPurpose) : CombatStep

    data object Resolution : CombatStep
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
