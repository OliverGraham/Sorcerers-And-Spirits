package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

/**
 * The Lucky Coin roll that triggers High Roller. A defensive flip only ever surfaces 1–5, since a 6
 * routes to High Roller instead, so a [ArenaMessageState.LuckyCoin.Flip] showing this value is the
 * armor-bonus resolution that follows the High Roller throw — the coin has already been thrown, so
 * that flip plays no fresh coin animation.
 */
private const val HIGH_ROLLER_ROLL = 6

/**
 * A combat event the seated avatars act out (SS-191): one cue per flash-worthy message, played in
 * step with that message's flash callout. Each cue carries the userIds of the players it involves,
 * so a seat resolves its own role — striking, reacting, or holding still — via [gestureFor]. New
 * ability animations (SS-193…SS-199) become new cue variants.
 */
@Immutable
sealed interface CombatAnimationCue {
    val id: String

    /**
     * [impersonatedClassType] is set only when the attacker is an Illusionist that has copied an
     * offensive ability, naming the class whose weapon its borrowed normal attack renders (SS-197);
     * null for every real class and for an Illusionist that has not impersonated anyone.
     */
    @Immutable
    data class NormalAttack(
        override val id: String,
        val attackerUserId: String,
        val attackerClassType: CharacterClassType,
        val defenderUserId: String,
        val outcome: NormalRollMessageType,
        val impersonatedClassType: CharacterClassType?,
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
    data class Encore(override val id: String, val phase: EncorePhase) : CombatAnimationCue

    /**
     * The Illusionist copies a living opponent's offensive ability (SS-208): a personal hand mirror
     * is pulled out showing the Illusionist's own face, which then crossfades through a blur into
     * the [impersonatedClassType]'s face under an Illusionist-blue tint. [impersonatedClassType] is
     * the Illusionist's own class only in the degenerate case where nothing was copied.
     */
    @Immutable
    data class Impersonate(
        override val id: String,
        val illusionistUserId: String,
        val impersonatedClassType: CharacterClassType,
    ) : CombatAnimationCue

    /**
     * The Illusionist's Mirror Image (SS-209): when struck, the Illusionist splits into two blurry,
     * translucent copies of itself ([MirrorImagePhase.Split]); when the reroll resolves
     * ([MirrorImagePhase.Resolve]), one copy is struck and poofs while the other slides back to
     * reveal the real Illusionist, which reacts to [outcome] — dodging a miss, or flinching and
     * collapsing under a hit. [outcome] is null for the opening split, before the roll resolves.
     */
    @Immutable
    data class MirrorImage(
        override val id: String,
        val illusionistUserId: String,
        val phase: MirrorImagePhase,
        val outcome: NormalRollMessageType?,
    ) : CombatAnimationCue

    /**
     * The Mage's Spell Potency (SS-210): the amplified form of its normal-attack projectile
     * (SS-198). A doubles roll fires the singular bolt [mainDamage] times at the main target and
     * wraps it in a large area-of-effect (a Blizzard for Ice, etc.), while every other conscious
     * opponent takes the splash off-stage. The element renders from [id] the same way the normal
     * attack picks it, so the amplified cast reads as the same spell, scaled up.
     */
    @Immutable
    data class SpellPotency(
        override val id: String,
        val mageUserId: String,
        val mainTargetUserId: String,
        val mainDamage: Int,
    ) : CombatAnimationCue

    /**
     * The Mage's Teleport (SS-211): the instant the Mage's armor is passed a swirling blue portal
     * opens beside them ([TeleportPhase.Open]) and churns while the roll-off is made
     * ([TeleportPhase.Hold]); on the resolving roll ([TeleportPhase.Resolve]) the Mage either slips
     * through the portal and vanishes before reappearing when [didEscape] is true, or lunges for a
     * portal that snaps shut too soon and the scene grays out when it is false. [didEscape] is null
     * until the roll resolves. [mageUserId] is the defender of the armor-break roll that preceded
     * this, read the same way Mirror Image finds its Illusionist; null only when no such roll is on
     * record, in which case no cue is emitted.
     */
    @Immutable
    data class Teleport(
        override val id: String,
        val mageUserId: String,
        val phase: TeleportPhase,
        val didEscape: Boolean?,
    ) : CombatAnimationCue
}

/**
 * The stage a Teleport is in (SS-211): [Open] is the portal tearing open the moment the Mage's
 * armor is passed; [Hold] keeps it swirling through each roll-off die without re-opening it;
 * [Resolve] is the roll landing — the Mage escaping through the portal, or the portal closing on a
 * failed escape.
 */
enum class TeleportPhase {
    Open,
    Hold,
    Resolve,
}

/**
 * The stage a Mirror Image is in (SS-209): [Split] is the Illusionist fracturing into two copies
 * the moment it is struck, held while the reroll choice is made; [Resolve] is the attack landing on
 * a copy and the illusion collapsing back to the real Illusionist once the roll resolves.
 */
enum class MirrorImagePhase {
    Split,
    Resolve,
}

/**
 * The stage a Bard's Encore is in (SS-206): [SetStage] is the concert scene rising the moment
 * Encore triggers — the arena darkens to a spotlit stage with a standing microphone and a restless
 * crowd of the class avatars, held alive while the roll is made; [Success] and [Failure] are the
 * resolved 1d6 — the mic throws off music as the crowd roars for the encore, or the notes drain to
 * grey as the crowd boos, before the house lights come back up and the darkness lifts either way.
 */
enum class EncorePhase {
    SetStage,
    Success,
    Failure,
}

/**
 * The stage a Persuade is in (SS-207): [Summon] is the Bard's lute conjured the moment their armor
 * is passed, held while the roll is made; [StrumFailed] and [StrumStopped] are the resolved roll —
 * the strings ring and notes bloom, then either drain to grey as the attack presses on, or hold
 * their colour in a flourish as the attack is lulled to a stop. [CharmSummon] is the same stopping
 * flourish followed by the lute crossfading into a flute, and [CharmComplete] is that flute played,
 * air breathing through it, wreathed in the slow hypnotic swirl that redirects the attacker.
 */
enum class PersuadePhase {
    Summon,
    StrumFailed,
    StrumStopped,
    CharmSummon,
    CharmComplete,
}

/**
 * The stage a Riposte is in (SS-204): [Ready] is the paired blades rising into a parry stance the
 * moment the Duelist's armor is passed, held while the roll-off is made; [Landed] and [Missed] are
 * the resolved roll — the parry beat followed by the counter-stab either reaching the attacker or
 * sailing past.
 */
enum class RipostePhase {
    Ready,
    Landed,
    Missed,
}

/**
 * The motion vocabulary a single avatar can perform. Cues pick a gesture per role; the choreography
 * lives with the avatar composables, so adding a gesture here means giving it a choreography there.
 */
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

/**
 * The animation cue for a message, mirroring [flashCallout]: derived from the same message both
 * displays share, with the exhaustive [when] making a new message type a compile error until its
 * cue is chosen. Null means the avatars sit the event out — the branch each ability-animation
 * ticket later replaces with its cue.
 */
val ArenaMessageState.combatAnimationCue: CombatAnimationCue?
    get() =
        when (this) {
            is ArenaMessageState.NormalRoll ->
                CombatAnimationCue.NormalAttack(
                    id = id,
                    attackerUserId = attackerUserId,
                    attackerClassType = attackerClassType,
                    defenderUserId = defenderUserId,
                    outcome = type,
                    impersonatedClassType = impersonatedClassType,
                )

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
                val isHighRollerArmorResolution = singleDie == HIGH_ROLLER_ROLL
                when {
                    gamblerUserId == null || isHighRollerArmorResolution -> null

                    else ->
                        CombatAnimationCue.LuckyCoinFlip(
                            id = id,
                            gamblerUserId = gamblerUserId,
                            faceValue = singleDie,
                            didStopAttack = didStopAttack,
                        )
                }
            }

            is ArenaMessageState.LuckyCoin.HighRollerTrigger ->
                if (gamblerUserId != null) {
                    CombatAnimationCue.LuckyCoinHighRoller(id = id, gamblerUserId = gamblerUserId)
                } else {
                    null
                }

            is ArenaMessageState.LuckyCoin.HighRollerDamage ->
                when {
                    gamblerUserId == null || attackerUserId == null -> null

                    else ->
                        CombatAnimationCue.LuckyCoinHighRollerThrow(
                            id = id,
                            gamblerUserId = gamblerUserId,
                            attackerUserId = attackerUserId,
                            damage = singleDie,
                            isKnockout = isKnockout,
                        )
                }

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
                CombatAnimationCue.Encore(id = id, phase = EncorePhase.SetStage)

            is ArenaMessageState.Encore.Roll ->
                CombatAnimationCue.Encore(
                    id = id,
                    phase = if (isSuccessful) EncorePhase.Success else EncorePhase.Failure,
                )

            is ArenaMessageState.Impersonate ->
                CombatAnimationCue.Impersonate(
                    id = id,
                    illusionistUserId = illusionistUserId,
                    impersonatedClassType = impersonatedClassType,
                )

            is ArenaMessageState.MirrorImage.Trigger ->
                if (illusionistUserId != null) {
                    CombatAnimationCue.MirrorImage(
                        id = id,
                        illusionistUserId = illusionistUserId,
                        phase = MirrorImagePhase.Split,
                        outcome = null,
                    )
                } else {
                    null
                }

            is ArenaMessageState.MirrorImage.Pass ->
                CombatAnimationCue.MirrorImage(
                    id = id,
                    illusionistUserId = illusionistUserId,
                    phase = MirrorImagePhase.Resolve,
                    outcome = outcome,
                )

            is ArenaMessageState.MirrorImage.TemptFate ->
                CombatAnimationCue.MirrorImage(
                    id = id,
                    illusionistUserId = illusionistUserId,
                    phase = MirrorImagePhase.Resolve,
                    outcome = outcome,
                )

            is ArenaMessageState.SpellPotency ->
                CombatAnimationCue.SpellPotency(
                    id = id,
                    mageUserId = mageUserId,
                    mainTargetUserId = mainTargetUserId,
                    mainDamage = mainDamage,
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

            // TODO(SS-199): Paladin ability animations.
            is ArenaMessageState.HolyShield -> null

            is ArenaMessageState.NewRound,
            is ArenaMessageState.ChoseCharacterClass,
            is ArenaMessageState.ConnectedToArena,
            is ArenaMessageState.NewTurn,
            is ArenaMessageState.Doubles -> null
        }

/**
 * The player who folds into a [Collapse][AvatarGesture] for this cue — the one knocked out — or
 * null when the cue knocks no one out. Mirrors the Collapse assignments in [gestureFor]. Used to
 * keep the downed player in their large seat while the collapse plays out, and through the
 * match-ending finish hold, instead of snapping them to a head icon the instant they fall (SS-213).
 */
val CombatAnimationCue.collapsingUserId: String?
    get() =
        when (this) {
            is CombatAnimationCue.NormalAttack ->
                if (outcome == NormalRollMessageType.Knockout) defenderUserId else null

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
                if (outcome == NormalRollMessageType.Knockout) illusionistUserId else null

            is CombatAnimationCue.SpellPotency -> null

            is CombatAnimationCue.Teleport -> null
        }

/**
 * The gesture the player with [userId] performs for this cue, or null when the cue is not theirs to
 * act — a spectator seat holds still.
 */
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
    }

/**
 * The Mage's Spell Potency (SS-210): the amplified projectiles and the area-of-effect are drawn by
 * the effect overlay, so the Mage only casts behind them while the seated main target takes the
 * barrage with a heavy flinch. The splashed opponents are off-stage head icons, animated separately
 * (SS-214).
 */
private fun CombatAnimationCue.SpellPotency.spellPotencyGestureFor(userId: String): AvatarGesture? =
    when (userId) {
        mageUserId -> AvatarGesture.Cast
        mainTargetUserId -> AvatarGesture.FlinchHeavy
        else -> null
    }

/**
 * The Illusionist's Mirror Image (SS-209): the split itself is drawn entirely by the effect
 * overlay, so the seat holds still through it; only on the resolving roll does the real Illusionist
 * react — dodging a foiled attack or flinching and collapsing under one that lands.
 */
private fun CombatAnimationCue.MirrorImage.mirrorImageGestureFor(userId: String): AvatarGesture? {
    if (phase != MirrorImagePhase.Resolve) return null
    if (userId != illusionistUserId) return null
    val resolvedOutcome = outcome ?: return null
    return defenderReactionGesture(outcome = resolvedOutcome)
}

/**
 * The Duelist's Riposte (SS-204): the paired blades of the effect overlay carry the whole reversal
 * — the forward blade's disarming swirl and the rear blade's counter-stab — so the Duelist holds
 * their ground behind them, and only the countered attacker reacts, flinching as a landed reversal
 * turns their own attack around. A failed counter leaves the attacker untouched here; their
 * original attack simply resumes into its own health roll.
 */
private fun CombatAnimationCue.Riposte.riposteGestureFor(userId: String): AvatarGesture? =
    when {
        userId == attackerUserId && phase == RipostePhase.Landed -> AvatarGesture.Flinch
        else -> null
    }

/**
 * The Duelist's Prepare Attack (SS-203): the paired rapiers of the effect overlay carry the show —
 * wind up, gleam, and thrust — so the Duelist only coils and springs behind them, while the target
 * collapses under a landed strike or sidesteps a whiffed one.
 */
private fun CombatAnimationCue.PrepareAttack.prepareAttackGestureFor(
    userId: String
): AvatarGesture? =
    when (userId) {
        duelistUserId ->
            if (isSuccessful) AvatarGesture.PreparedThrust else AvatarGesture.PreparedThrustWhiff

        targetUserId -> if (isSuccessful) AvatarGesture.Collapse else AvatarGesture.Sidestep

        else -> null
    }

/**
 * The High Roller payback (SS-193): the Gambler flings the lucky coin at the attacker — the coin
 * itself is drawn by the effect overlay, so the Gambler only throws and the struck attacker reacts,
 * folding into a collapse when the coin knocks them out.
 */
private fun CombatAnimationCue.LuckyCoinHighRollerThrow.luckyCoinThrowGestureFor(
    userId: String
): AvatarGesture? =
    when (userId) {
        gamblerUserId -> AvatarGesture.Throw
        attackerUserId -> if (isKnockout) AvatarGesture.Collapse else AvatarGesture.FlinchHeavy
        else -> null
    }

/**
 * The Gambler's own body during All-In (SS-193): the coins are the show — drawn by the effect
 * overlay — so the avatar only tosses its winnings up, then rides out the result. A self-knockout
 * folds the toss into a collapse; the other outcomes just settle back.
 */
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

/**
 * The classes whose normal-attack weapon has a built renderer (SS-192…SS-196). The Illusionist
 * borrows from this set: impersonating one of them shows its weapon, and impersonating a class
 * outside it — or no one — falls back to a random pick from it (SS-197). Mage and Paladin join once
 * SS-198/SS-199 land.
 */
internal val builtWeaponClassTypes =
    listOf(
        CharacterClassType.Rogue,
        CharacterClassType.Gambler,
        CharacterClassType.Duelist,
        CharacterClassType.Necromancer,
        CharacterClassType.Bard,
    )

/**
 * The class whose weapon renders for this normal attack. Every real class renders its own; the
 * Illusionist borrows the weapon of the class it impersonated (recolored to Illusionist blue by its
 * overlay), or — with no built weapon to borrow — a per-attack random pick from
 * [builtWeaponClassTypes], keyed on the cue id so every timing and draw site resolves the same
 * weapon for a given attack (SS-197).
 */
internal val CombatAnimationCue.NormalAttack.weaponClassType: CharacterClassType
    get() {
        if (attackerClassType != CharacterClassType.Illusionist) return attackerClassType
        val borrowedWeapon = impersonatedClassType?.takeIf { it in builtWeaponClassTypes }
        return borrowedWeapon ?: randomBuiltWeaponFor(cueId = id)
    }

private fun randomBuiltWeaponFor(cueId: String): CharacterClassType {
    val count = builtWeaponClassTypes.size
    val index = ((cueId.hashCode() % count) + count) % count
    return builtWeaponClassTypes[index]
}

/**
 * Every class gets its own attacker-side strike set (SS-192…SS-199); the generic lunges are the
 * fallback for classes whose set has not landed yet. An Illusionist mirrors the body motion of the
 * weapon it borrows via [weaponClassType]. The defender-side reactions stay shared.
 */
private fun CombatAnimationCue.NormalAttack.attackerStrike(): AvatarGesture =
    when (weaponClassType) {
        CharacterClassType.Rogue -> rogueStrike()
        CharacterClassType.Gambler -> gamblerStrike()
        CharacterClassType.Duelist -> duelistStrike()
        CharacterClassType.Necromancer -> AvatarGesture.Cast
        CharacterClassType.Bard -> AvatarGesture.Cast
        CharacterClassType.Mage -> AvatarGesture.Cast
        else -> genericStrike()
    }

/**
 * The Rogue's dagger work (SS-192): the flying dagger of the weapon overlay carries the attack, and
 * these gestures are the planted body motion behind each throw.
 */
private fun CombatAnimationCue.NormalAttack.rogueStrike(): AvatarGesture =
    when (outcome) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Stab
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.StabOvershoot
        NormalRollMessageType.Damage -> AvatarGesture.Stab
        NormalRollMessageType.Knockout -> AvatarGesture.StabFlurry
    }

/**
 * The Gambler's card throw (SS-193): the spinning playing card of the weapon overlay carries the
 * attack across the gap, and these gestures are the planted throwing flick behind each toss.
 */
private fun CombatAnimationCue.NormalAttack.gamblerStrike(): AvatarGesture =
    when (outcome) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Throw
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.ThrowOvershoot
        NormalRollMessageType.Damage -> AvatarGesture.Throw
        NormalRollMessageType.Knockout -> AvatarGesture.ThrowStrong
    }

/**
 * The Duelist's paired blades (SS-194): the twin swords of the weapon overlay slash the defender
 * one after the other, and these gestures are the two-beat fencing footwork behind the cuts — a
 * lighter commit than a lunge, since the blades carry the attack.
 */
private fun CombatAnimationCue.NormalAttack.duelistStrike(): AvatarGesture =
    when (outcome) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Slash
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.SlashWhiff
        NormalRollMessageType.Damage -> AvatarGesture.Slash
        NormalRollMessageType.Knockout -> AvatarGesture.SlashFinish
    }

private fun CombatAnimationCue.NormalAttack.genericStrike(): AvatarGesture =
    when (outcome) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Lunge
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.LungeOvershoot
        NormalRollMessageType.Damage -> AvatarGesture.Lunge
        NormalRollMessageType.Knockout -> AvatarGesture.LungeStrong
    }

private fun CombatAnimationCue.NormalAttack.defenderReaction(): AvatarGesture =
    defenderReactionGesture(outcome = outcome)

/** The shared defender reaction to a resolved attack roll, reused wherever a struck seat reacts. */
internal fun defenderReactionGesture(outcome: NormalRollMessageType): AvatarGesture =
    when (outcome) {
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
