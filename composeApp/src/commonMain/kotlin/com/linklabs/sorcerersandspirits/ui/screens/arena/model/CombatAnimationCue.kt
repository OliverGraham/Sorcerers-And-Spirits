package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

enum class AvatarGesture {
    Lunge,
    LungeStrong,
    LungeOvershoot,
    Stab,
    StabFlurry,
    StabOvershoot,
    Slash,
    SlashFinish,
    SlashWhiff,
    Cast,
    PreparedThrust,
    PreparedThrustWhiff,
    Throw,
    ThrowStrong,
    ThrowOvershoot,
    AllInToss,
    AllInBust,
    ReArm,
    Flinch,
    FlinchHeavy,
    Sidestep,
    DodgeFlourish,
    DodgeBrace,
    Collapse,
}

fun ArenaMessageState.toCollapsingUserId(): String? =
    when (this) {
        is ArenaMessageState.NormalRoll.Attack ->
            if (normalRollMessageType == NormalRollMessageType.Knockout) {
                defenderUserId
            } else {
                null
            }

        is ArenaMessageState.NormalRoll.Outcome ->
            if (type == NormalRollMessageType.Knockout) {
                defenderUserId
            } else {
                null
            }

        is ArenaMessageState.PrepareAttack -> if (isSuccessful) targetUserId else null

        is ArenaMessageState.Riposte -> null

        is ArenaMessageState.LuckyCoin.HighRollerDamage -> if (isKnockout) attackerUserId else null

        is ArenaMessageState.AllIn ->
            if (outcome == AllInOutcome.SelfKnockout) gamblerUserId else null

        is ArenaMessageState.KillingMomentum -> null

        is ArenaMessageState.Dodge -> null

        is ArenaMessageState.LuckyCoin.Flip -> null

        is ArenaMessageState.LuckyCoin.HighRollerTrigger -> null

        is ArenaMessageState.SummonMinion -> null

        is ArenaMessageState.Persuade -> null

        is ArenaMessageState.Encore -> null

        is ArenaMessageState.Impersonate -> null

        is ArenaMessageState.MirrorImage.Pass ->
            if (messageType == NormalRollMessageType.Knockout) illusionistUserId else null

        is ArenaMessageState.MirrorImage.TemptFate ->
            if (messageType == NormalRollMessageType.Knockout) illusionistUserId else null

        is ArenaMessageState.MirrorImage.Trigger -> null

        is ArenaMessageState.SpellPotency -> if (isMainTargetKnockedOut) mainTargetUserId else null

        is ArenaMessageState.Teleport -> null

        is ArenaMessageState.HolyShield.Prayer -> if (!didSurvive) paladinUserId else null

        is ArenaMessageState.HolyShield.Wrath -> if (isAttackerKnockedOut) attackerUserId else null

        is ArenaMessageState.HolyShield.Trigger -> null

        else -> null
    }

// TODO: This can be removed since all weapons have been built
internal val builtWeaponClassTypes =
    listOf(
        CharacterClassType.Rogue,
        CharacterClassType.Gambler,
        CharacterClassType.Duelist,
        CharacterClassType.Necromancer,
        CharacterClassType.Bard,
        CharacterClassType.Mage,
        CharacterClassType.Paladin,
    )
