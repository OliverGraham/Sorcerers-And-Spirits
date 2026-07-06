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

    @Immutable
    data class NormalAttack(
        override val id: String,
        val attackerUserId: String,
        val attackerClassType: CharacterClassType,
        val defenderUserId: String,
        val outcome: NormalRollMessageType,
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
                )

            is ArenaMessageState.KillingMomentum ->
                if (attackerUserId != null) {
                    CombatAnimationCue.KillingMomentum(id = id, attackerUserId = attackerUserId)
                } else {
                    null
                }

            is ArenaMessageState.Dodge ->
                if (dodgerUserId != null) {
                    CombatAnimationCue.Dodge(
                        id = id,
                        dodgerUserId = dodgerUserId,
                        didArmorIncrease = didArmorIncrease,
                    )
                } else {
                    null
                }

            is ArenaMessageState.AllIn ->
                CombatAnimationCue.AllIn(id = id, gamblerUserId = gamblerUserId, outcome = outcome)

            is ArenaMessageState.LuckyCoin.Trigger -> null

            is ArenaMessageState.LuckyCoin.Flip -> {
                val isHighRollerArmorResolution = singleDie == HIGH_ROLLER_ROLL
                if (gamblerUserId == null) {
                    null
                } else if (isHighRollerArmorResolution) {
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
                if (gamblerUserId != null) {
                    CombatAnimationCue.LuckyCoinHighRoller(id = id, gamblerUserId = gamblerUserId)
                } else {
                    null
                }

            is ArenaMessageState.LuckyCoin.HighRollerDamage ->
                if (gamblerUserId == null) {
                    null
                } else if (attackerUserId == null) {
                    null
                } else {
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

            // TODO(SS-196): Bard ability animations.
            is ArenaMessageState.Encore,
            is ArenaMessageState.Persuade -> null

            // TODO(SS-197): Illusionist ability animations.
            is ArenaMessageState.Impersonate,
            is ArenaMessageState.MirrorImage -> null

            // TODO(SS-198): Mage ability animations.
            is ArenaMessageState.SpellPotency,
            is ArenaMessageState.Teleport -> null

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
 * Every class gets its own attacker-side strike set (SS-192…SS-199); the generic lunges are the
 * fallback for classes whose set has not landed yet. The defender-side reactions stay shared.
 */
private fun CombatAnimationCue.NormalAttack.attackerStrike(): AvatarGesture =
    when (attackerClassType) {
        CharacterClassType.Rogue -> rogueStrike()
        CharacterClassType.Gambler -> gamblerStrike()
        CharacterClassType.Duelist -> duelistStrike()
        CharacterClassType.Necromancer -> AvatarGesture.Cast
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
