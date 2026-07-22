package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

private const val HIGH_ROLLER_ROLL = 6

@Immutable
sealed interface CombatAnimationCue {
    val id: String

    @Immutable
    data class NormalAttack(
        override val id: String,
        val attackerUserId: String,
        val attackerClassType: CharacterClassType,
        val defenderUserId: String,
        val normalRollMessageType: NormalRollMessageType,
        val impersonatedClassType: CharacterClassType,
    ) : CombatAnimationCue

    @Immutable
    data class KillingMomentum(override val id: String, val attackerUserId: String) :
        CombatAnimationCue

    @Immutable
    data class Dodge(
        override val id: String,
        val dodgerUserId: String,
        val didArmorIncrease: Boolean,
    ) : CombatAnimationCue

    @Immutable
    data class AllIn(
        override val id: String,
        val gamblerUserId: String,
        val outcome: AllInOutcome,
    ) : CombatAnimationCue

    @Immutable
    data class LuckyCoinFlip(
        override val id: String,
        val gamblerUserId: String,
        val faceValue: Int,
        val didStopAttack: Boolean,
    ) : CombatAnimationCue

    @Immutable
    data class LuckyCoinHighRoller(override val id: String, val gamblerUserId: String) :
        CombatAnimationCue

    @Immutable
    data class LuckyCoinHighRollerThrow(
        override val id: String,
        val gamblerUserId: String,
        val attackerUserId: String,
        val damage: Int,
        val isKnockout: Boolean,
    ) : CombatAnimationCue

    @Immutable
    data class PrepareAttack(
        override val id: String,
        val duelistUserId: String,
        val targetUserId: String,
        val isSuccessful: Boolean,
    ) : CombatAnimationCue

    @Immutable
    data class Riposte(
        override val id: String,
        val duelistUserId: String,
        val attackerUserId: String,
        val phase: RipostePhase,
    ) : CombatAnimationCue

    @Immutable
    data class SummonMinion(
        override val id: String,
        val necroUserId: String,
        val outcome: SummonMinionOutcome,
    ) : CombatAnimationCue

    @Immutable
    data class Persuade(override val id: String, val bardUserId: String, val phase: PersuadePhase) :
        CombatAnimationCue

    @Immutable
    data class Encore(override val id: String, val bardUserId: String, val phase: EncorePhase) :
        CombatAnimationCue

    @Immutable
    data class Impersonate(
        override val id: String,
        val illusionistUserId: String,
        val impersonatedClassType: CharacterClassType,
    ) : CombatAnimationCue

    @Immutable
    data class MirrorImage(
        override val id: String,
        val illusionistUserId: String,
        val phase: MirrorImagePhase,
        val messageType: NormalRollMessageType?,
    ) : CombatAnimationCue

    @Immutable
    data class SpellPotency(
        override val id: String,
        val mageUserId: String,
        val mainTargetUserId: String,
        val mainDamage: Int,
        val splashDamage: Int,
        val splashTargetUserIds: List<String>,
        val isMainTargetKnockedOut: Boolean,
    ) : CombatAnimationCue

    @Immutable
    data class Teleport(
        override val id: String,
        val mageUserId: String,
        val phase: TeleportPhase,
        val didEscape: Boolean?,
    ) : CombatAnimationCue

    @Immutable
    data class HolyShield(
        override val id: String,
        val paladinUserId: String,
        val phase: HolyShieldPhase,
        val didSurvive: Boolean?,
        val attackerUserId: String?,
        val wrathIntensity: Int?,
        val isAttackerKnockedOut: Boolean,
    ) : CombatAnimationCue
}

enum class HolyShieldPhase {
    Raise,
    Pray,
    Wrath,
}

enum class TeleportPhase {
    Open,
    Hold,
    Resolve,
}

enum class MirrorImagePhase {
    Split,
    Resolve,
}

enum class EncorePhase {
    SetStage,
    Success,
    Failure,
}

enum class PersuadePhase {
    Summon,
    StrumFailed,
    StrumStopped,
    CharmSummon,
    CharmComplete,
}

enum class RipostePhase {
    Ready,
    Landed,
    Missed,
}

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

val ArenaMessageState.combatAnimationCue: CombatAnimationCue?
    get() =
        when (this) {
            is ArenaMessageState.NormalRoll.Attack ->
                CombatAnimationCue.NormalAttack(
                    id = id,
                    attackerUserId = attackerUserId,
                    attackerClassType = attackerClassType,
                    defenderUserId = defenderUserId,
                    normalRollMessageType = normalRollMessageType,
                    impersonatedClassType = impersonatedClassType,
                )

            is ArenaMessageState.NormalRoll.Outcome -> null

            is ArenaMessageState.KillingMomentum ->
                CombatAnimationCue.KillingMomentum(id = id, attackerUserId = attackerUserId)

            is ArenaMessageState.Dodge ->
                CombatAnimationCue.Dodge(
                    id = id,
                    dodgerUserId = dodgerUserId,
                    didArmorIncrease = didArmorIncrease,
                )

            is ArenaMessageState.AllIn ->
                CombatAnimationCue.AllIn(id = id, gamblerUserId = gamblerUserId, outcome = outcome)

            is ArenaMessageState.LuckyCoin.Trigger -> null

            is ArenaMessageState.LuckyCoin.Flip -> {
                if (singleDie == HIGH_ROLLER_ROLL) {
                    null
                } else {
                    CombatAnimationCue.LuckyCoinFlip(
                        id = id,
                        gamblerUserId = gamblerUserId,
                        faceValue = singleDie,
                        didStopAttack = didStopAttack,
                    )
                }
            }

            is ArenaMessageState.LuckyCoin.HighRollerTrigger ->
                CombatAnimationCue.LuckyCoinHighRoller(id = id, gamblerUserId = gamblerUserId)

            is ArenaMessageState.LuckyCoin.HighRollerDamage ->
                CombatAnimationCue.LuckyCoinHighRollerThrow(
                    id = id,
                    gamblerUserId = gamblerUserId,
                    attackerUserId = attackerUserId,
                    damage = singleDie,
                    isKnockout = isKnockout,
                )

            is ArenaMessageState.PrepareAttack ->
                CombatAnimationCue.PrepareAttack(
                    id = id,
                    duelistUserId = duelistUserId,
                    targetUserId = targetUserId,
                    isSuccessful = isSuccessful,
                )

            is ArenaMessageState.Riposte.Trigger ->
                CombatAnimationCue.Riposte(
                    id = id,
                    duelistUserId = duelistUserId,
                    attackerUserId = attackerUserId,
                    phase = RipostePhase.Ready,
                )

            is ArenaMessageState.Riposte.EnGarde ->
                CombatAnimationCue.Riposte(
                    id = id,
                    duelistUserId = duelistUserId,
                    attackerUserId = attackerUserId,
                    phase = if (isSuccessful) RipostePhase.Landed else RipostePhase.Missed,
                )

            is ArenaMessageState.SummonMinion ->
                CombatAnimationCue.SummonMinion(
                    id = id,
                    necroUserId = necroUserId,
                    outcome = outcome,
                )

            is ArenaMessageState.Persuade.Trigger ->
                CombatAnimationCue.Persuade(
                    id = id,
                    bardUserId = bardUserId,
                    phase = PersuadePhase.Summon,
                )

            is ArenaMessageState.Persuade.StrumLute ->
                CombatAnimationCue.Persuade(
                    id = id,
                    bardUserId = bardUserId,
                    phase =
                        if (isSuccessful) PersuadePhase.StrumStopped else PersuadePhase.StrumFailed,
                )

            is ArenaMessageState.Persuade.CharmActivation ->
                CombatAnimationCue.Persuade(
                    id = id,
                    bardUserId = bardUserId,
                    phase = PersuadePhase.CharmSummon,
                )

            is ArenaMessageState.Persuade.Charm ->
                CombatAnimationCue.Persuade(
                    id = id,
                    bardUserId = bardUserId,
                    phase = PersuadePhase.CharmComplete,
                )

            is ArenaMessageState.Persuade.Lull -> null

            is ArenaMessageState.Encore.Trigger ->
                CombatAnimationCue.Encore(
                    id = id,
                    bardUserId = bardUserId,
                    phase = EncorePhase.SetStage,
                )

            is ArenaMessageState.Encore.Roll ->
                CombatAnimationCue.Encore(
                    id = id,
                    bardUserId = bardUserId,
                    phase = if (isSuccessful) EncorePhase.Success else EncorePhase.Failure,
                )

            is ArenaMessageState.Impersonate ->
                CombatAnimationCue.Impersonate(
                    id = id,
                    illusionistUserId = illusionistUserId,
                    impersonatedClassType = impersonatedClassType,
                )

            is ArenaMessageState.MirrorImage.Trigger ->
                CombatAnimationCue.MirrorImage(
                    id = id,
                    illusionistUserId = illusionistUserId,
                    phase = MirrorImagePhase.Split,
                    messageType = null,
                )

            is ArenaMessageState.MirrorImage.Pass ->
                CombatAnimationCue.MirrorImage(
                    id = id,
                    illusionistUserId = illusionistUserId,
                    phase = MirrorImagePhase.Resolve,
                    messageType = messageType,
                )

            is ArenaMessageState.MirrorImage.TemptFate ->
                CombatAnimationCue.MirrorImage(
                    id = id,
                    illusionistUserId = illusionistUserId,
                    phase = MirrorImagePhase.Resolve,
                    messageType = messageType,
                )

            is ArenaMessageState.SpellPotency ->
                CombatAnimationCue.SpellPotency(
                    id = id,
                    mageUserId = mageUserId,
                    mainTargetUserId = mainTargetUserId,
                    mainDamage = mainDamage,
                    splashDamage = splashDamage,
                    splashTargetUserIds = splashTargetUserIds,
                    isMainTargetKnockedOut = isMainTargetKnockedOut,
                )

            is ArenaMessageState.Teleport.Trigger ->
                CombatAnimationCue.Teleport(
                    id = id,
                    mageUserId = mageUserId,
                    phase = TeleportPhase.Open,
                    didEscape = null,
                )

            is ArenaMessageState.Teleport.Roll ->
                CombatAnimationCue.Teleport(
                    id = id,
                    mageUserId = mageUserId,
                    phase = TeleportPhase.Hold,
                    didEscape = null,
                )

            is ArenaMessageState.Teleport.Result ->
                CombatAnimationCue.Teleport(
                    id = id,
                    mageUserId = mageUserId,
                    phase = TeleportPhase.Resolve,
                    didEscape = didDefenderEscape,
                )

            is ArenaMessageState.HolyShield.Trigger ->
                CombatAnimationCue.HolyShield(
                    id = id,
                    paladinUserId = paladinUserId,
                    phase = HolyShieldPhase.Raise,
                    didSurvive = null,
                    attackerUserId = null,
                    wrathIntensity = null,
                    isAttackerKnockedOut = false,
                )

            is ArenaMessageState.HolyShield.Prayer ->
                CombatAnimationCue.HolyShield(
                    id = id,
                    paladinUserId = paladinUserId,
                    phase = HolyShieldPhase.Pray,
                    didSurvive = didSurvive,
                    attackerUserId = null,
                    wrathIntensity = null,
                    isAttackerKnockedOut = false,
                )

            is ArenaMessageState.HolyShield.Wrath ->
                CombatAnimationCue.HolyShield(
                    id = id,
                    paladinUserId = paladinUserId,
                    phase = HolyShieldPhase.Wrath,
                    didSurvive = true,
                    attackerUserId = attackerUserId,
                    wrathIntensity = singleDie,
                    isAttackerKnockedOut = isAttackerKnockedOut,
                )

            is ArenaMessageState.NewRound,
            is ArenaMessageState.ChoseCharacterClass,
            is ArenaMessageState.ConnectedToArena,
            is ArenaMessageState.NewTurn,
            is ArenaMessageState.Doubles -> null
        }

// TODO: This should be data passed from Domain layer type
val CombatAnimationCue.collapsingUserId: String?
    get() =
        when (this) {
            is CombatAnimationCue.NormalAttack ->
                if (normalRollMessageType == NormalRollMessageType.Knockout) {
                    defenderUserId
                } else {
                    null
                }

            is CombatAnimationCue.PrepareAttack -> if (isSuccessful) targetUserId else null

            is CombatAnimationCue.Riposte -> null

            is CombatAnimationCue.LuckyCoinHighRollerThrow ->
                if (isKnockout) attackerUserId else null

            is CombatAnimationCue.AllIn ->
                if (outcome == AllInOutcome.SelfKnockout) gamblerUserId else null

            is CombatAnimationCue.KillingMomentum -> null

            is CombatAnimationCue.Dodge -> null

            is CombatAnimationCue.LuckyCoinFlip -> null

            is CombatAnimationCue.LuckyCoinHighRoller -> null

            is CombatAnimationCue.SummonMinion -> null

            is CombatAnimationCue.Persuade -> null

            is CombatAnimationCue.Encore -> null

            is CombatAnimationCue.Impersonate -> null

            is CombatAnimationCue.MirrorImage ->
                if (messageType == NormalRollMessageType.Knockout) illusionistUserId else null

            is CombatAnimationCue.SpellPotency ->
                if (isMainTargetKnockedOut) mainTargetUserId else null

            is CombatAnimationCue.Teleport -> null

            is CombatAnimationCue.HolyShield ->
                when {
                    phase == HolyShieldPhase.Pray && didSurvive == false -> paladinUserId
                    phase == HolyShieldPhase.Wrath && isAttackerKnockedOut -> attackerUserId
                    else -> null
                }
        }

fun CombatAnimationCue.gestureFor(userId: String): AvatarGesture? =
    when (this) {
        is CombatAnimationCue.NormalAttack -> normalAttackGestureFor(userId = userId)

        is CombatAnimationCue.KillingMomentum ->
            if (userId == attackerUserId) AvatarGesture.ReArm else null

        is CombatAnimationCue.Dodge -> dodgeGestureFor(userId = userId)

        is CombatAnimationCue.AllIn -> allInGestureFor(userId = userId)

        is CombatAnimationCue.LuckyCoinFlip -> null

        is CombatAnimationCue.LuckyCoinHighRoller -> null

        is CombatAnimationCue.LuckyCoinHighRollerThrow -> luckyCoinThrowGestureFor(userId = userId)

        is CombatAnimationCue.PrepareAttack -> prepareAttackGestureFor(userId = userId)

        is CombatAnimationCue.Riposte -> riposteGestureFor(userId = userId)

        is CombatAnimationCue.SummonMinion ->
            if (userId == necroUserId) AvatarGesture.Cast else null

        is CombatAnimationCue.Persuade -> null

        is CombatAnimationCue.Encore -> null

        is CombatAnimationCue.Impersonate -> null

        is CombatAnimationCue.MirrorImage -> mirrorImageGestureFor(userId = userId)

        is CombatAnimationCue.SpellPotency -> spellPotencyGestureFor(userId = userId)

        is CombatAnimationCue.Teleport -> null

        is CombatAnimationCue.HolyShield -> holyShieldGestureFor(userId = userId)
    }

private fun CombatAnimationCue.HolyShield.holyShieldGestureFor(userId: String): AvatarGesture? {
    val isSmoteAttacker = phase == HolyShieldPhase.Wrath && userId == attackerUserId
    if (isSmoteAttacker) {
        return if (isAttackerKnockedOut) AvatarGesture.Collapse else AvatarGesture.FlinchHeavy
    }

    if (userId != paladinUserId) return null
    val didFailPrayer = phase == HolyShieldPhase.Pray && didSurvive == false
    return if (didFailPrayer) AvatarGesture.Collapse else null
}

private fun CombatAnimationCue.SpellPotency.spellPotencyGestureFor(userId: String): AvatarGesture? =
    when (userId) {
        mageUserId -> AvatarGesture.Cast

        mainTargetUserId ->
            if (isMainTargetKnockedOut) AvatarGesture.Collapse else AvatarGesture.FlinchHeavy

        else -> null
    }

private fun CombatAnimationCue.MirrorImage.mirrorImageGestureFor(userId: String): AvatarGesture? {
    if (phase != MirrorImagePhase.Resolve) return null
    if (userId != illusionistUserId) return null
    val resolvedMessageType = messageType ?: return null
    return defenderReactionGesture(normalRollMessageType = resolvedMessageType)
}

private fun CombatAnimationCue.Riposte.riposteGestureFor(userId: String): AvatarGesture? =
    when {
        userId == attackerUserId && phase == RipostePhase.Landed -> AvatarGesture.Flinch
        else -> null
    }

private fun CombatAnimationCue.PrepareAttack.prepareAttackGestureFor(
    userId: String
): AvatarGesture? =
    when (userId) {
        duelistUserId ->
            if (isSuccessful) AvatarGesture.PreparedThrust else AvatarGesture.PreparedThrustWhiff

        targetUserId -> if (isSuccessful) AvatarGesture.Collapse else AvatarGesture.Sidestep

        else -> null
    }

private fun CombatAnimationCue.LuckyCoinHighRollerThrow.luckyCoinThrowGestureFor(
    userId: String
): AvatarGesture? =
    when (userId) {
        gamblerUserId -> AvatarGesture.Throw
        attackerUserId -> if (isKnockout) AvatarGesture.Collapse else AvatarGesture.FlinchHeavy
        else -> null
    }

private fun CombatAnimationCue.AllIn.allInGestureFor(userId: String): AvatarGesture? {
    if (userId != gamblerUserId) return null
    return when (outcome) {
        AllInOutcome.SelfKnockout -> AvatarGesture.AllInBust

        AllInOutcome.NoEffect,
        AllInOutcome.DoubleAttack -> AvatarGesture.AllInToss
    }
}

private fun CombatAnimationCue.NormalAttack.normalAttackGestureFor(userId: String): AvatarGesture? {
    val isAttacker = userId == attackerUserId
    val isDefender = userId == defenderUserId
    return when {
        isAttacker -> attackerStrike()
        isDefender -> defenderReaction()
        else -> null
    }
}

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

internal val CombatAnimationCue.NormalAttack.weaponClassType: CharacterClassType
    get() {
        if (attackerClassType != CharacterClassType.Illusionist) return attackerClassType
        val borrowedWeapon = impersonatedClassType.takeIf { it in builtWeaponClassTypes }
        return borrowedWeapon ?: randomBuiltWeaponFor(cueId = id)
    }

private fun randomBuiltWeaponFor(cueId: String): CharacterClassType {
    val count = builtWeaponClassTypes.size
    val index = ((cueId.hashCode() % count) + count) % count
    return builtWeaponClassTypes[index]
}

private fun CombatAnimationCue.NormalAttack.attackerStrike(): AvatarGesture =
    when (weaponClassType) {
        CharacterClassType.Rogue -> rogueStrike()
        CharacterClassType.Gambler -> gamblerStrike()
        CharacterClassType.Duelist -> duelistStrike()
        CharacterClassType.Necromancer -> AvatarGesture.Cast
        CharacterClassType.Bard -> AvatarGesture.Cast
        CharacterClassType.Mage -> AvatarGesture.Cast
        CharacterClassType.Paladin -> paladinStrike()
        else -> genericStrike()
    }

private fun CombatAnimationCue.NormalAttack.paladinStrike(): AvatarGesture =
    when (normalRollMessageType) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Slash
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.SlashWhiff
        NormalRollMessageType.Damage -> AvatarGesture.Slash
        NormalRollMessageType.Knockout -> AvatarGesture.SlashFinish
    }

private fun CombatAnimationCue.NormalAttack.rogueStrike(): AvatarGesture =
    when (normalRollMessageType) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Stab
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.StabOvershoot
        NormalRollMessageType.Damage -> AvatarGesture.Stab
        NormalRollMessageType.Knockout -> AvatarGesture.StabFlurry
    }

private fun CombatAnimationCue.NormalAttack.gamblerStrike(): AvatarGesture =
    when (normalRollMessageType) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Throw
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.ThrowOvershoot
        NormalRollMessageType.Damage -> AvatarGesture.Throw
        NormalRollMessageType.Knockout -> AvatarGesture.ThrowStrong
    }

private fun CombatAnimationCue.NormalAttack.duelistStrike(): AvatarGesture =
    when (normalRollMessageType) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Slash
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.SlashWhiff
        NormalRollMessageType.Damage -> AvatarGesture.Slash
        NormalRollMessageType.Knockout -> AvatarGesture.SlashFinish
    }

private fun CombatAnimationCue.NormalAttack.genericStrike(): AvatarGesture =
    when (normalRollMessageType) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Lunge
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.LungeOvershoot
        NormalRollMessageType.Damage -> AvatarGesture.Lunge
        NormalRollMessageType.Knockout -> AvatarGesture.LungeStrong
    }

private fun CombatAnimationCue.NormalAttack.defenderReaction(): AvatarGesture =
    defenderReactionGesture(normalRollMessageType = normalRollMessageType)

private fun defenderReactionGesture(normalRollMessageType: NormalRollMessageType): AvatarGesture =
    when (normalRollMessageType) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Flinch
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.Sidestep
        NormalRollMessageType.Damage -> AvatarGesture.FlinchHeavy
        NormalRollMessageType.Knockout -> AvatarGesture.Collapse
    }

private fun CombatAnimationCue.Dodge.dodgeGestureFor(userId: String): AvatarGesture? {
    val isDodger = userId == dodgerUserId
    if (!isDodger) return null
    return if (didArmorIncrease) AvatarGesture.DodgeBrace else AvatarGesture.DodgeFlourish
}
