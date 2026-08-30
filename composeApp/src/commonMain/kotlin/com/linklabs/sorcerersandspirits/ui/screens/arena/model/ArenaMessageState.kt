package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

@Immutable
sealed interface ArenaMessageState {
    val animation: ArenaAnimationState
    val id: String

    // TODO(SS-258): gestures can be fully built into the message itself. top and bottom players are
    //       known at this time. Don't need to query from the composable
    val gestures: Map<String, AvatarGesture>
        get() = emptyMap()

    val actingClassType: CharacterClassType?
        get() = null

    val flashAnchor: FlashAnchor
        get() = FlashAnchor.Center

    val isAvatarCompletedByGesture: Boolean
        get() = false

    fun isFlash() = animation.isFlash()

    fun isAvatar() = animation.isAvatar()

    fun isContinuous() = animation.isAnimationContinuous()

    fun isAnimated() = isFlash() || isAvatar()

    fun gestureFor(userId: String): AvatarGesture? = gestures[userId]

    @Immutable
    data class NewRound(
        override val animation: ArenaAnimationState = ArenaAnimationState.Flash,
        override val id: String,
        val roundNumber: Int,
    ) : ArenaMessageState

    @Immutable
    data class ChoseCharacterClass(
        override val animation: ArenaAnimationState = ArenaAnimationState.None,
        override val id: String,
        val displayName: String,
        val characterClassInfo: CharacterClassMessageInfoState,
    ) : ArenaMessageState {
        override val actingClassType: CharacterClassType = characterClassInfo.type
    }

    @Immutable
    data class ConnectedToArena(
        override val animation: ArenaAnimationState = ArenaAnimationState.None,
        override val id: String,
        val displayName: String,
    ) : ArenaMessageState

    @Immutable
    data class NewTurn(
        override val animation: ArenaAnimationState = ArenaAnimationState.None,
        override val id: String,
        val characterClassInfo: CharacterClassMessageInfoState,
    ) : ArenaMessageState {
        override val actingClassType: CharacterClassType = characterClassInfo.type
    }

    @Immutable
    sealed interface NormalRoll : ArenaMessageState {
        val attackerUserId: String
        val defenderUserId: String
        val attackerClassType: CharacterClassType
        val normalRollMessageType: NormalRollMessageType

        override val actingClassType: CharacterClassType
            get() = attackerClassType

        override val flashAnchor: FlashAnchor
            get() = FlashAnchor.Avatar(userId = defenderUserId)

        @Immutable
        data class Attack(
            override val animation: ArenaAnimationState = ArenaAnimationState.Avatar(),
            override val id: String,
            override val attackerUserId: String,
            override val defenderUserId: String,
            override val attackerClassType: CharacterClassType,
            override val normalRollMessageType: NormalRollMessageType,
            val impersonatedClassType: CharacterClassType?,
            override val gestures: Map<String, AvatarGesture>,
        ) : NormalRoll

        // TODO(SS-258): Does this need to be used for Avatar Animations???
        @Immutable
        data class Outcome(
            override val animation: ArenaAnimationState = ArenaAnimationState.Flash,
            override val id: String,
            override val attackerUserId: String,
            override val defenderUserId: String,
            override val normalRollMessageType: NormalRollMessageType,
            override val attackerClassType: CharacterClassType,
            override val gestures: Map<String, AvatarGesture> = emptyMap(),
            val attackerClassName: String,
            val defenderClassName: String,
            val firstDie: Int,
            val secondDie: Int,
            val bonus: Int,
        ) : NormalRoll {
            val baseRoll: Int = firstDie + secondDie
        }
    }

    @Immutable
    data class Doubles(
        override val animation: ArenaAnimationState = ArenaAnimationState.None,
        override val id: String,
        val className: String,
    ) : ArenaMessageState

    @Immutable
    data class KillingMomentum(
        override val animation: ArenaAnimationState = ArenaAnimationState.FlashAndAvatar(),
        override val gestures: Map<String, AvatarGesture>,
        override val id: String,
        val attackerUserId: String,
    ) : ArenaMessageState {
        override val actingClassType: CharacterClassType = CharacterClassType.Rogue
        override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = attackerUserId)
        override val isAvatarCompletedByGesture: Boolean = true
    }

    @Immutable
    data class Dodge(
        override val animation: ArenaAnimationState = ArenaAnimationState.FlashAndAvatar(),
        override val gestures: Map<String, AvatarGesture>,
        override val id: String,
        val didArmorIncrease: Boolean,
        val dodgerUserId: String,
    ) : ArenaMessageState {
        override val actingClassType: CharacterClassType = CharacterClassType.Rogue
        override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = dodgerUserId)
        override val isAvatarCompletedByGesture: Boolean = true
    }

    @Immutable
    data class AllIn(
        override val animation: ArenaAnimationState = ArenaAnimationState.FlashAndAvatar(),
        override val id: String,
        override val actingClassType: CharacterClassType,
        val gamblerUserId: String,
        val gamblerName: String,
        val singleDie: Int,
        val outcome: AllInOutcome,
        override val gestures: Map<String, AvatarGesture>,
    ) : ArenaMessageState {
        override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = gamblerUserId)
    }

    @Immutable
    sealed interface LuckyCoin : ArenaMessageState {
        val gamblerUserId: String

        override val actingClassType: CharacterClassType
            get() = CharacterClassType.Gambler

        override val flashAnchor: FlashAnchor
            get() = FlashAnchor.Avatar(userId = gamblerUserId)

        @Immutable
        data class Trigger(
            override val animation: ArenaAnimationState = ArenaAnimationState.Flash,
            override val id: String,
            override val gamblerUserId: String,
        ) : LuckyCoin

        @Immutable
        data class Flip(
            override val animation: ArenaAnimationState = ArenaAnimationState.FlashAndAvatar(),
            override val id: String,
            override val gamblerUserId: String,
            val singleDie: Int,
            val didStopAttack: Boolean,
        ) : LuckyCoin

        @Immutable
        data class HighRollerTrigger(
            override val animation: ArenaAnimationState =
                ArenaAnimationState.FlashAndAvatar(isContinuous = true),
            override val id: String,
            override val gamblerUserId: String,
            val attackerClassName: String,
        ) : LuckyCoin

        @Immutable
        data class HighRollerDamage(
            override val animation: ArenaAnimationState = ArenaAnimationState.FlashAndAvatar(),
            override val id: String,
            override val gamblerUserId: String,
            val attackerUserId: String,
            val singleDie: Int,
            val isKnockout: Boolean,
            override val gestures: Map<String, AvatarGesture>,
        ) : LuckyCoin {
            override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = attackerUserId)
        }
    }

    @Immutable
    sealed interface PrepareAttack : ArenaMessageState {
        val duelistUserId: String
        val isSuccessful: Boolean

        override val flashAnchor: FlashAnchor
            get() = FlashAnchor.Avatar(userId = duelistUserId)

        @Immutable
        data class Strike(
            override val animation: ArenaAnimationState = ArenaAnimationState.Avatar(),
            override val id: String,
            override val actingClassType: CharacterClassType,
            override val duelistUserId: String,
            val targetUserId: String,
            override val isSuccessful: Boolean,
            val isTargetKnockedOut: Boolean,
            override val gestures: Map<String, AvatarGesture>,
        ) : PrepareAttack

        @Immutable
        data class Outcome(
            override val animation: ArenaAnimationState = ArenaAnimationState.Flash,
            override val id: String,
            override val actingClassType: CharacterClassType,
            override val duelistUserId: String,
            val duelistName: String,
            val targetClassName: String,
            val firstDie: Int,
            val secondDie: Int,
            override val isSuccessful: Boolean,
        ) : PrepareAttack
    }

    @Immutable
    sealed interface Riposte : ArenaMessageState {
        override val actingClassType: CharacterClassType
            get() = CharacterClassType.Duelist

        override val flashAnchor: FlashAnchor
            get() = FlashAnchor.Avatar(userId = duelistUserId)

        val duelistUserId: String
        val attackerUserId: String

        @Immutable
        data class Trigger(
            override val animation: ArenaAnimationState.FlashAndAvatar =
                ArenaAnimationState.FlashAndAvatar(isContinuous = true),
            override val id: String,
            override val duelistUserId: String,
            override val attackerUserId: String,
        ) : Riposte

        @Immutable
        data class EnGarde(
            override val animation: ArenaAnimationState.FlashAndAvatar =
                ArenaAnimationState.FlashAndAvatar(),
            override val id: String,
            override val duelistUserId: String,
            override val attackerUserId: String,
            val firstDie: Int,
            val secondDie: Int,
            val isSuccessful: Boolean,
            override val gestures: Map<String, AvatarGesture> = emptyMap(),
        ) : Riposte
    }

    @Immutable
    data class SummonMinion(
        override val animation: ArenaAnimationState = ArenaAnimationState.FlashAndAvatar(),
        override val id: String,
        override val actingClassType: CharacterClassType,
        val necromancerUserId: String,
        val className: String,
        val singleDie: Int,
        val outcome: SummonMinionOutcome,
        val minionIndex: Int,
        override val gestures: Map<String, AvatarGesture>,
    ) : ArenaMessageState {
        override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = necromancerUserId)
    }

    @Immutable
    sealed interface Encore : ArenaMessageState {
        val bardUserId: String

        override val actingClassType: CharacterClassType

        override val flashAnchor: FlashAnchor
            get() = FlashAnchor.Avatar(userId = bardUserId)

        @Immutable
        data class Trigger(
            override val animation: ArenaAnimationState =
                ArenaAnimationState.FlashAndAvatar(isContinuous = true),
            override val id: String,
            override val bardUserId: String,
            override val actingClassType: CharacterClassType,
        ) : Encore

        @Immutable
        data class Roll(
            override val animation: ArenaAnimationState = ArenaAnimationState.FlashAndAvatar(),
            override val id: String,
            override val bardUserId: String,
            override val actingClassType: CharacterClassType,
            val encoreDie: Int,
            val isSuccessful: Boolean,
        ) : Encore
    }

    // TODO: Figure out Persuade UI and usage
    @Immutable
    sealed interface Persuade : ArenaMessageState {
        override val actingClassType: CharacterClassType
            get() = CharacterClassType.Bard

        override val flashAnchor: FlashAnchor
            get() {
                return FlashAnchor.Avatar(userId = bardUserId)
            }

        val bardUserId: String

        @Immutable
        data class Trigger(
            override val animation: ArenaAnimationState =
                ArenaAnimationState.FlashAndAvatar(isContinuous = true),
            override val id: String,
            override val bardUserId: String,
        ) : Persuade

        @Immutable
        data class StrumLute(
            override val animation: ArenaAnimationState = ArenaAnimationState.FlashAndAvatar(),
            override val id: String,
            override val bardUserId: String,
            val diceRoll: Int,
            val isSuccessful: Boolean,
        ) : Persuade

        @Immutable
        data class CharmActivation(
            override val animation: ArenaAnimationState = ArenaAnimationState.FlashAndAvatar(),
            override val id: String,
            override val bardUserId: String,
        ) : Persuade

        @Immutable
        data class Charm(
            override val animation: ArenaAnimationState = ArenaAnimationState.FlashAndAvatar(),
            override val id: String,
            override val bardUserId: String,
            val attackerClassName: String,
            val defenderClassName: String,
        ) : Persuade
    }

    @Immutable
    data class Impersonate(
        override val animation: ArenaAnimationState = ArenaAnimationState.FlashAndAvatar(),
        override val id: String,
        val illusionistUserId: String,
        val illusionistName: String,
        val impersonatedClassType: CharacterClassType,
        val impersonatedAbilityName: String,
    ) : ArenaMessageState {
        override val actingClassType: CharacterClassType = CharacterClassType.Illusionist
        override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = illusionistUserId)
    }

    @Immutable
    sealed interface MirrorImage : ArenaMessageState {
        val illusionistUserId: String

        override val actingClassType: CharacterClassType
            get() = CharacterClassType.Illusionist

        override val flashAnchor: FlashAnchor
            get() = FlashAnchor.Avatar(userId = illusionistUserId)

        @Immutable
        data class Trigger(
            override val animation: ArenaAnimationState =
                ArenaAnimationState.FlashAndAvatar(isContinuous = true),
            override val id: String,
            override val illusionistUserId: String,
            val firstDie: Int?,
            val secondDie: Int?,
        ) : MirrorImage {
            override val isAvatarCompletedByGesture: Boolean = true
        }

        @Immutable
        data class Pass(
            override val animation: ArenaAnimationState = ArenaAnimationState.FlashAndAvatar(),
            override val id: String,
            override val illusionistUserId: String,
            val attackerClassName: String,
            val attackerClassType: CharacterClassType,
            val messageType: NormalRollMessageType,
            val baseRoll: Int,
            val doublesBonus: Int,
            override val gestures: Map<String, AvatarGesture>,
        ) : MirrorImage {
            override val actingClassType: CharacterClassType = attackerClassType
            override val isAvatarCompletedByGesture: Boolean = true
        }

        @Immutable
        data class TemptFate(
            override val animation: ArenaAnimationState = ArenaAnimationState.FlashAndAvatar(),
            override val id: String,
            override val illusionistUserId: String,
            val originalRoll: Int,
            val rawReroll: Int,
            val finalRoll: Int,
            val attackerClassName: String,
            val messageType: NormalRollMessageType,
            override val gestures: Map<String, AvatarGesture>,
        ) : MirrorImage {
            val rerollDisplay: String = "$rawReroll - 1"

            override val isAvatarCompletedByGesture: Boolean = true
        }
    }

    @Immutable
    data class SpellPotency(
        override val animation: ArenaAnimationState = ArenaAnimationState.FlashAndAvatar(),
        override val id: String,
        val mageUserId: String,
        val mageName: String,
        val mainTargetUserId: String,
        val mainTargetClassName: String,
        val firstDie: Int,
        val secondDie: Int,
        val mainDamage: Int,
        val splashDamage: Int,
        val splashTargetUserIds: List<String>,
        val isMainTargetKnockedOut: Boolean,
        override val gestures: Map<String, AvatarGesture>,
    ) : ArenaMessageState {
        override val actingClassType: CharacterClassType = CharacterClassType.Mage
        override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = mageUserId)
    }

    @Immutable
    sealed interface Teleport : ArenaMessageState {
        val mageUserId: String

        override val actingClassType: CharacterClassType
            get() = CharacterClassType.Mage

        override val flashAnchor: FlashAnchor
            get() = FlashAnchor.Avatar(userId = mageUserId)

        @Immutable
        data class Trigger(
            override val animation: ArenaAnimationState =
                ArenaAnimationState.FlashAndAvatar(isContinuous = true),
            override val id: String,
            override val mageUserId: String,
        ) : Teleport

        @Immutable
        data class Roll(
            override val animation: ArenaAnimationState =
                ArenaAnimationState.FlashAndAvatar(isContinuous = true),
            override val id: String,
            override val mageUserId: String,
            val rollerUserId: String,
            val className: String,
            val singleDie: Int,
        ) : Teleport {
            override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = rollerUserId)
        }

        @Immutable
        data class Tie(
            override val animation: ArenaAnimationState =
                ArenaAnimationState.FlashAndAvatar(isContinuous = true),
            override val id: String,
            override val mageUserId: String,
        ) : Teleport {
            override val flashAnchor: FlashAnchor = FlashAnchor.Center
        }

        @Immutable
        data class Result(
            override val animation: ArenaAnimationState = ArenaAnimationState.FlashAndAvatar(),
            override val id: String,
            override val mageUserId: String,
            val didDefenderEscape: Boolean,
        ) : Teleport
    }

    @Immutable
    sealed interface HolyShield : ArenaMessageState {
        val paladinUserId: String

        override val actingClassType: CharacterClassType
            get() = CharacterClassType.Paladin

        override val flashAnchor: FlashAnchor
            get() = FlashAnchor.Avatar(userId = paladinUserId)

        @Immutable
        data class Trigger(
            override val animation: ArenaAnimationState =
                ArenaAnimationState.FlashAndAvatar(isContinuous = true),
            override val id: String,
            override val paladinUserId: String,
        ) : HolyShield

        @Immutable
        data class Prayer(
            override val animation: ArenaAnimationState =
                ArenaAnimationState.FlashAndAvatar(isContinuous = true),
            override val id: String,
            override val paladinUserId: String,
            val firstDie: Int,
            val secondDie: Int,
            val didSurvive: Boolean,
            override val gestures: Map<String, AvatarGesture> = emptyMap(),
        ) : HolyShield

        @Immutable
        data class Wrath(
            override val animation: ArenaAnimationState = ArenaAnimationState.FlashAndAvatar(),
            override val id: String,
            override val paladinUserId: String,
            val paladinName: String,
            val attackerUserId: String,
            val attackerClassName: String,
            val singleDie: Int,
            val isAttackerKnockedOut: Boolean,
            override val gestures: Map<String, AvatarGesture>,
        ) : HolyShield {
            override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = attackerUserId)
        }
    }
}

// TODO(SS-259): Back to domain, and passed through
enum class SummonMinionOutcome {
    Fizzle,
    Skeleton,
    SkeletonAndHeal,
    Golem,
}

// TODO(SS-259): Back to domain, and passed through
enum class AllInOutcome {
    SelfKnockout,
    NoEffect,
    DoubleAttack,
}

private fun summonMinionOutcomeFor(singleDie: Int): SummonMinionOutcome = when (singleDie) {
    2,
    3,
    -> SummonMinionOutcome.Skeleton

    4,
    5,
    -> SummonMinionOutcome.SkeletonAndHeal

    6 -> SummonMinionOutcome.Golem

    else -> SummonMinionOutcome.Fizzle
}

private fun allInOutcomeFor(singleDie: Int): AllInOutcome = when (singleDie) {
    1 -> AllInOutcome.SelfKnockout

    5,
    6,
    -> AllInOutcome.DoubleAttack

    else -> AllInOutcome.NoEffect
}

@Immutable
data class CharacterClassMessageInfoState(val className: String, val type: CharacterClassType)

// TODO(SS-259): figure out how to do this better (dedupe with CharacterClassMessageInfoState)
@Immutable data class ParticipantClass(val className: String, val type: CharacterClassType)

internal fun List<IncomingArenaMessage>.toMessageStates(): List<ArenaMessageState> = map { incomingMessage ->
    incomingMessage.message.toState(id = incomingMessage.id)
}

private fun ArenaMessage.toState(id: String): ArenaMessageState = when (this) {
    is ArenaMessage.NewRound -> ArenaMessageState.NewRound(id = id, roundNumber = roundNumber)

    is ArenaMessage.ChoseCharacterClass ->
        ArenaMessageState.ChoseCharacterClass(
            id = id,
            displayName = displayName,
            characterClassInfo = characterClassInfo.toState(),
        )

    is ArenaMessage.ConnectedToArena ->
        ArenaMessageState.ConnectedToArena(id = id, displayName = displayName)

    is ArenaMessage.NewTurn ->
        ArenaMessageState.NewTurn(id = id, characterClassInfo = characterClassInfo.toState())

    is ArenaMessage.NormalRoll -> toState(id = id)

    is ArenaMessage.Doubles -> ArenaMessageState.Doubles(id = id, className = className)

    is ArenaMessage.KillingMomentum -> {
        ArenaMessageState.KillingMomentum(
            id = id,
            attackerUserId = attackerUserId,
            gestures = mapOf(attackerUserId to AvatarGesture.ReArm),
        )
    }

    is ArenaMessage.Dodge -> {
        val gesture =
            if (didArmorIncrease) AvatarGesture.DodgeBrace else AvatarGesture.DodgeFlourish
        ArenaMessageState.Dodge(
            id = id,
            didArmorIncrease = didArmorIncrease,
            dodgerUserId = dodgerUserId,
            gestures = mapOf(dodgerUserId to gesture),
        )
    }

    is ArenaMessage.AllIn -> {
        val outcome = allInOutcomeFor(singleDie = singleDie)
        val gestures = buildMap {
            val gesture =
                when (outcome) {
                    AllInOutcome.SelfKnockout -> AvatarGesture.AllInBust

                    AllInOutcome.NoEffect,
                    AllInOutcome.DoubleAttack,
                    -> AvatarGesture.AllInToss
                }
            put(gamblerUserId, gesture)
        }
        ArenaMessageState.AllIn(
            id = id,
            gamblerUserId = gamblerUserId,
            gamblerName = gamblerName,
            actingClassType = classType,
            singleDie = singleDie,
            outcome = outcome,
            gestures = gestures,
        )
    }

    is ArenaMessage.LuckyCoin.Trigger ->
        ArenaMessageState.LuckyCoin.Trigger(id = id, gamblerUserId = gamblerUserId)

    is ArenaMessage.LuckyCoin.Flip ->
        ArenaMessageState.LuckyCoin.Flip(
            id = id,
            gamblerUserId = gamblerUserId,
            singleDie = singleDie,
            didStopAttack = didStopAttack,
        )

    is ArenaMessage.LuckyCoin.HighRollerTrigger ->
        ArenaMessageState.LuckyCoin.HighRollerTrigger(
            id = id,
            gamblerUserId = gamblerUserId,
            attackerClassName = attackerClassName,
        )

    is ArenaMessage.LuckyCoin.HighRollerDamage -> {
        val gestures = buildMap {
            put(gamblerUserId, AvatarGesture.Throw)
            put(
                attackerUserId,
                if (isKnockout) AvatarGesture.Collapse else AvatarGesture.FlinchHeavy,
            )
        }
        ArenaMessageState.LuckyCoin.HighRollerDamage(
            id = id,
            gamblerUserId = gamblerUserId,
            attackerUserId = attackerUserId,
            singleDie = singleDie,
            isKnockout = isKnockout,
            gestures = gestures,
        )
    }

    is ArenaMessage.PrepareAttack.Strike -> {
        val targetGesture =
            when {
                !isSuccessful -> AvatarGesture.Sidestep
                isTargetKnockedOut -> AvatarGesture.Collapse
                else -> AvatarGesture.FlinchHeavy
            }
        val gestures = buildMap {
            put(
                duelistUserId,
                if (isSuccessful) {
                    AvatarGesture.PreparedThrust
                } else {
                    AvatarGesture.PreparedThrustWhiff
                },
            )
            put(targetUserId, targetGesture)
        }
        ArenaMessageState.PrepareAttack.Strike(
            id = id,
            actingClassType = classType,
            duelistUserId = duelistUserId,
            targetUserId = targetUserId,
            isSuccessful = isSuccessful,
            isTargetKnockedOut = isTargetKnockedOut,
            gestures = gestures,
        )
    }

    is ArenaMessage.PrepareAttack.Outcome ->
        ArenaMessageState.PrepareAttack.Outcome(
            id = id,
            actingClassType = classType,
            duelistUserId = duelistUserId,
            duelistName = duelistName,
            targetClassName = targetClassName,
            firstDie = firstDie,
            secondDie = secondDie,
            isSuccessful = isSuccessful,
        )

    is ArenaMessage.Riposte.Trigger ->
        ArenaMessageState.Riposte.Trigger(
            id = id,
            duelistUserId = duelistUserId,
            attackerUserId = attackerUserId,
        )

    is ArenaMessage.Riposte.EnGarde -> {
        val gestures =
            if (isSuccessful) {
                mapOf(attackerUserId to AvatarGesture.Flinch)
            } else {
                emptyMap()
            }

        ArenaMessageState.Riposte.EnGarde(
            id = id,
            duelistUserId = duelistUserId,
            attackerUserId = attackerUserId,
            firstDie = firstDie,
            secondDie = secondDie,
            isSuccessful = isSuccessful,
            gestures = gestures,
        )
    }

    is ArenaMessage.SummonMinion -> {
        ArenaMessageState.SummonMinion(
            id = id,
            necromancerUserId = necromancerUserId,
            className = className,
            actingClassType = classType,
            singleDie = singleDie,
            outcome = summonMinionOutcomeFor(singleDie = singleDie),
            minionIndex = minionIndex,
            gestures = mapOf(necromancerUserId to AvatarGesture.Cast),
        )
    }

    is ArenaMessage.Encore.Trigger ->
        ArenaMessageState.Encore.Trigger(
            id = id,
            bardUserId = bardUserId,
            actingClassType = classType,
        )

    is ArenaMessage.Encore.Roll ->
        ArenaMessageState.Encore.Roll(
            id = id,
            bardUserId = bardUserId,
            actingClassType = classType,
            encoreDie = encoreDie,
            isSuccessful = isSuccessful,
        )

    is ArenaMessage.Persuade.Charm ->
        ArenaMessageState.Persuade.Charm(
            id = id,
            bardUserId = bardUserId,
            attackerClassName = attackerClassName,
            defenderClassName = defenderClassName,
        )

    is ArenaMessage.Persuade.CharmActivation ->
        ArenaMessageState.Persuade.CharmActivation(
            animation = ArenaAnimationState.FlashAndAvatar(),
            id = id,
            bardUserId = bardUserId,
        )

    is ArenaMessage.Persuade.StrumLute ->
        ArenaMessageState.Persuade.StrumLute(
            animation = ArenaAnimationState.FlashAndAvatar(),
            id = id,
            bardUserId = bardUserId,
            diceRoll = diceRoll,
            isSuccessful = isSuccessful,
        )

    is ArenaMessage.Persuade.Trigger ->
        ArenaMessageState.Persuade.Trigger(
            animation = ArenaAnimationState.FlashAndAvatar(isContinuous = true),
            id = id,
            bardUserId = bardUserId,
        )

    is ArenaMessage.Impersonate ->
        ArenaMessageState.Impersonate(
            id = id,
            illusionistUserId = illusionistUserId,
            illusionistName = illusionistName,
            impersonatedClassType = impersonateAbility.impersonatedClassType,
            impersonatedAbilityName =
            impersonateAbility.impersonatedAbility::class.simpleName.orEmpty(),
        )

    is ArenaMessage.MirrorImage.Pass -> {
        val gesture = defenderReactionGesture(normalRollMessageType = messageType)
        ArenaMessageState.MirrorImage.Pass(
            id = id,
            illusionistUserId = illusionistUserId,
            attackerClassName = attackerClassName,
            attackerClassType = attackerClassType,
            messageType = messageType,
            baseRoll = baseRoll,
            doublesBonus = doublesBonus,
            gestures = mapOf(illusionistUserId to gesture),
        )
    }

    is ArenaMessage.MirrorImage.Trigger ->
        ArenaMessageState.MirrorImage.Trigger(
            id = id,
            illusionistUserId = illusionistUserId,
            firstDie = firstDie,
            secondDie = secondDie,
        )

    is ArenaMessage.MirrorImage.TemptFate -> {
        val gesture = defenderReactionGesture(normalRollMessageType = messageType)
        ArenaMessageState.MirrorImage.TemptFate(
            id = id,
            illusionistUserId = illusionistUserId,
            originalRoll = rerolledMirrorImageDie.originalRoll,
            rawReroll = rerolledMirrorImageDie.reroll ?: rerolledMirrorImageDie.originalRoll,
            finalRoll = rerolledMirrorImageDie.finalValue,
            attackerClassName = attackerClassName,
            messageType = messageType,
            gestures = mapOf(illusionistUserId to gesture),
        )
    }

    is ArenaMessage.SpellPotency -> {
        val gestures = buildMap {
            put(mageUserId, AvatarGesture.Cast)
            put(
                mainTargetUserId,
                if (isMainTargetKnockedOut) {
                    AvatarGesture.Collapse
                } else {
                    AvatarGesture.FlinchHeavy
                },
            )
        }
        ArenaMessageState.SpellPotency(
            id = id,
            mageUserId = mageUserId,
            mageName = mageName,
            mainTargetUserId = mainTargetUserId,
            mainTargetClassName = mainTargetClassName,
            firstDie = firstDie,
            secondDie = secondDie,
            mainDamage = mainDamage,
            splashDamage = splashDamage,
            splashTargetUserIds = splashTargetUserIds,
            isMainTargetKnockedOut = isMainTargetKnockedOut,
            gestures = gestures,
        )
    }

    is ArenaMessage.Teleport.Result ->
        ArenaMessageState.Teleport.Result(
            animation = ArenaAnimationState.FlashAndAvatar(),
            id = id,
            mageUserId = mageUserId,
            didDefenderEscape = didDefenderEscape,
        )

    is ArenaMessage.Teleport.Roll ->
        ArenaMessageState.Teleport.Roll(
            id = id,
            mageUserId = mageUserId,
            rollerUserId = userId,
            className = className,
            singleDie = singleDie,
        )

    is ArenaMessage.Teleport.Tie ->
        ArenaMessageState.Teleport.Tie(id = id, mageUserId = mageUserId)

    is ArenaMessage.Teleport.Trigger ->
        ArenaMessageState.Teleport.Trigger(id = id, mageUserId = mageUserId)

    is ArenaMessage.HolyShield.Trigger ->
        ArenaMessageState.HolyShield.Trigger(id = id, paladinUserId = paladinUserId)

    is ArenaMessage.HolyShield.Prayer -> {
        val gestures =
            if (!didSurvive) {
                mapOf(paladinUserId to AvatarGesture.Collapse)
            } else {
                emptyMap()
            }

        ArenaMessageState.HolyShield.Prayer(
            id = id,
            paladinUserId = paladinUserId,
            firstDie = firstDie,
            secondDie = secondDie,
            didSurvive = didSurvive,
            gestures = gestures,
        )
    }

    is ArenaMessage.HolyShield.Wrath -> {
        val gesture =
            if (isAttackerKnockedOut) AvatarGesture.Collapse else AvatarGesture.FlinchHeavy
        ArenaMessageState.HolyShield.Wrath(
            id = id,
            paladinUserId = paladinUserId,
            paladinName = paladinName,
            attackerUserId = attackerUserId,
            attackerClassName = attackerClassName,
            singleDie = singleDie,
            isAttackerKnockedOut = isAttackerKnockedOut,
            gestures = mapOf(attackerUserId to gesture),
        )
    }
}

// TODO: Make mapping functions for all types in this manner. ArenaMessage.Type.toState()
private fun ArenaMessage.NormalRoll.toState(id: String) = when (this) {
    is ArenaMessage.NormalRoll.Attack -> {
        val gestures = buildMap {
            put(
                key = attackerUserId,
                value =
                attackerStrike(
                    weaponClassType =
                    resolveWeaponClassType(
                        attackerClassType = attackerClassType,
                        impersonatedClassType = impersonatedCharacterType,
                        id = id,
                    ),
                    messageType = normalRollMessageType,
                ),
            )
            put(
                key = defenderUserId,
                value = defenderReactionGesture(normalRollMessageType = normalRollMessageType),
            )
        }

        ArenaMessageState.NormalRoll.Attack(
            id = id,
            attackerUserId = attackerUserId,
            defenderUserId = defenderUserId,
            attackerClassType = attackerClassType,
            impersonatedClassType = impersonatedCharacterType,
            normalRollMessageType = normalRollMessageType,
            gestures = gestures,
        )
    }

    is ArenaMessage.NormalRoll.Outcome ->
        ArenaMessageState.NormalRoll.Outcome(
            id = id,
            attackerUserId = attackerUserId,
            defenderUserId = defenderUserId,
            attackerClassName = attackerClassName,
            attackerClassType = attackerClassType,
            defenderClassName = defenderClassName,
            firstDie = firstDie,
            secondDie = secondDie,
            bonus = bonus,
            normalRollMessageType = type,
        )
}

@Immutable
sealed interface FlashAnchor {
    @Immutable data class Avatar(val userId: String) : FlashAnchor

    @Immutable data object Center : FlashAnchor
}

private fun CharacterClassMessageInfo.toState(): CharacterClassMessageInfoState = CharacterClassMessageInfoState(className = className, type = type)
