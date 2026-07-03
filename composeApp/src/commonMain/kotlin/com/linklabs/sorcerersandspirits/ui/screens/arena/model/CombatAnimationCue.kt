package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

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

            // TODO(SS-193): Gambler ability animations.
            is ArenaMessageState.AllIn,
            is ArenaMessageState.LuckyCoin -> null

            // TODO(SS-194): Duelist ability animations.
            is ArenaMessageState.PrepareAttack,
            is ArenaMessageState.Riposte -> null

            // TODO(SS-195): Necromancer ability animations.
            is ArenaMessageState.SummonMinion -> null

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
 * The gesture the player with [userId] performs for this cue, or null when the cue is not theirs to
 * act — a spectator seat holds still.
 */
fun CombatAnimationCue.gestureFor(userId: String): AvatarGesture? =
    when (this) {
        is CombatAnimationCue.NormalAttack -> normalAttackGestureFor(userId = userId)

        is CombatAnimationCue.KillingMomentum ->
            if (userId == attackerUserId) AvatarGesture.ReArm else null

        is CombatAnimationCue.Dodge -> dodgeGestureFor(userId = userId)
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
