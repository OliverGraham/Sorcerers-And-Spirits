package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType

/**
 * A combat event the seated avatars act out (SS-191): one cue per flash-worthy message, played in
 * step with that message's flash callout. The cue carries the combatants' userIds so each seat
 * resolves its own role — the attacker plays a striking gesture while the defender plays a struck
 * one — via [gestureFor]. New ability animations (SS-192…SS-199) become new cue variants.
 */
@Immutable
sealed interface CombatAnimationCue {
    val id: String
    val attackerUserId: String
    val defenderUserId: String

    @Immutable
    data class NormalAttack(
        override val id: String,
        override val attackerUserId: String,
        override val defenderUserId: String,
        val outcome: NormalRollMessageType,
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
    Flinch,
    FlinchHeavy,
    Sidestep,
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
                    defenderUserId = defenderUserId,
                    outcome = type,
                )

            // TODO(SS-192): Rogue ability animations.
            is ArenaMessageState.KillingMomentum,
            is ArenaMessageState.Dodge -> null

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
    }

private fun CombatAnimationCue.NormalAttack.normalAttackGestureFor(userId: String): AvatarGesture? {
    val isAttacker = userId == attackerUserId
    val isDefender = userId == defenderUserId
    return when {
        isAttacker ->
            when (outcome) {
                NormalRollMessageType.ArmorClassHit -> AvatarGesture.Lunge
                NormalRollMessageType.ArmorClassMiss -> AvatarGesture.LungeOvershoot
                NormalRollMessageType.Damage -> AvatarGesture.Lunge
                NormalRollMessageType.Knockout -> AvatarGesture.LungeStrong
            }

        isDefender ->
            when (outcome) {
                NormalRollMessageType.ArmorClassHit -> AvatarGesture.Flinch
                NormalRollMessageType.ArmorClassMiss -> AvatarGesture.Sidestep
                NormalRollMessageType.Damage -> AvatarGesture.FlinchHeavy
                NormalRollMessageType.Knockout -> AvatarGesture.Collapse
            }

        else -> null
    }
}
