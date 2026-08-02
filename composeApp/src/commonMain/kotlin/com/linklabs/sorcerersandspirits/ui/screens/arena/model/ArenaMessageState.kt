package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

@Immutable
sealed interface ArenaMessageState {
    val animation: ArenaAnimation
    val id: String
    val collapsingUserId: String?
        get() = null

    val actingClassType: CharacterClassType?
        get() = null

    val flashAnchor: FlashAnchor
        get() = FlashAnchor.Center

    @Immutable
    data class NewRound(
        override val animation: ArenaAnimation = ArenaAnimation.Flash,
        override val id: String,
        val roundNumber: String,
    ) : ArenaMessageState

    @Immutable
    data class ChoseCharacterClass(
        override val animation: ArenaAnimation = ArenaAnimation.None,
        override val id: String,
        val displayName: String,
        val characterClassInfo: CharacterClassMessageInfoState,
    ) : ArenaMessageState {
        override val actingClassType: CharacterClassType = characterClassInfo.type
    }

    @Immutable
    data class ConnectedToArena(
        override val animation: ArenaAnimation = ArenaAnimation.None,
        override val id: String,
        val displayName: String,
    ) : ArenaMessageState

    @Immutable
    data class NewTurn(
        override val animation: ArenaAnimation = ArenaAnimation.None,
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

        override val actingClassType: CharacterClassType
            get() = attackerClassType

        override val flashAnchor: FlashAnchor
            get() = FlashAnchor.Avatar(userId = defenderUserId)

        @Immutable
        data class Attack(
            override val animation: ArenaAnimation,
            override val id: String,
            override val attackerUserId: String,
            override val defenderUserId: String,
            override val attackerClassType: CharacterClassType,
            val impersonatedClassType: CharacterClassType?,
            val normalRollMessageType: NormalRollMessageType,
        ) : NormalRoll {
            override val collapsingUserId: String? =
                if (normalRollMessageType == NormalRollMessageType.Knockout) {
                    defenderUserId
                } else {
                    null
                }
        }

        @Immutable
        data class Outcome(
            override val animation: ArenaAnimation,
            override val id: String,
            override val attackerUserId: String,
            override val defenderUserId: String,
            override val attackerClassType: CharacterClassType,
            val attackerName: String,
            val defenderName: String,
            val firstDie: Int,
            val secondDie: Int,
            val bonus: Int,
            val type: NormalRollMessageType,
        ) : NormalRoll {
            val baseRoll: Int = firstDie + secondDie

            override val collapsingUserId: String? =
                if (type == NormalRollMessageType.Knockout) defenderUserId else null
        }
    }

    @Immutable
    data class KillingMomentum(
        override val animation: ArenaAnimation,
        override val id: String,
        val attackerUserId: String,
    ) : ArenaMessageState {
        override val actingClassType: CharacterClassType = CharacterClassType.Rogue
        override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = attackerUserId)
    }

    @Immutable
    data class Dodge(
        override val animation: ArenaAnimation,
        override val id: String,
        val didArmorIncrease: Boolean,
        val dodgerUserId: String,
    ) : ArenaMessageState {
        override val actingClassType: CharacterClassType = CharacterClassType.Rogue
        override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = dodgerUserId)
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
            override val animation: ArenaAnimation = ArenaAnimation.Flash,
            override val id: String,
            override val mageUserId: String,
        ) : Teleport

        @Immutable
        data class Roll(
            override val animation: ArenaAnimation = ArenaAnimation.Flash,
            override val id: String,
            override val mageUserId: String,
            val rollerUserId: String,
            val rollerClassType: CharacterClassType,
            val playerName: String,
            val singleDie: Int,
        ) : Teleport {
            override val actingClassType: CharacterClassType = rollerClassType
            override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = rollerUserId)
        }

        @Immutable
        data class Tie(
            override val animation: ArenaAnimation = ArenaAnimation.None,
            override val id: String,
            override val mageUserId: String,
        ) : Teleport {
            override val flashAnchor: FlashAnchor = FlashAnchor.Center
        }

        @Immutable
        data class Result(
            override val animation: ArenaAnimation = ArenaAnimation.Flash,
            override val id: String,
            override val mageUserId: String,
            val didDefenderEscape: Boolean,
        ) : Teleport
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
            override val animation: ArenaAnimation = ArenaAnimation.Flash,
            override val id: String,
            override val gamblerUserId: String,
        ) : LuckyCoin

        @Immutable
        data class Flip(
            override val animation: ArenaAnimation = ArenaAnimation.Flash,
            override val id: String,
            override val gamblerUserId: String,
            val singleDie: Int,
            val didStopAttack: Boolean,
        ) : LuckyCoin

        @Immutable
        data class HighRollerTrigger(
            override val animation: ArenaAnimation = ArenaAnimation.Flash,
            override val id: String,
            override val gamblerUserId: String,
            val targetName: String,
        ) : LuckyCoin

        @Immutable
        data class HighRollerDamage(
            override val animation: ArenaAnimation,
            override val id: String,
            override val gamblerUserId: String,
            val attackerUserId: String,
            val singleDie: Int,
            val isKnockout: Boolean,
        ) : LuckyCoin {
            override val collapsingUserId: String? = if (isKnockout) attackerUserId else null
            override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = attackerUserId)
        }
    }

    @Immutable
    sealed interface Riposte : ArenaMessageState {
        override val actingClassType: CharacterClassType
            get() = CharacterClassType.Duelist

        override val flashAnchor: FlashAnchor
            get() = FlashAnchor.Avatar(userId = duelistUserId)

        val duelistUserId: String

        @Immutable
        data class Trigger(
            override val animation: ArenaAnimation = ArenaAnimation.Flash,
            override val id: String,
            override val duelistUserId: String,
            val attackerUserId: String,
        ) : Riposte

        @Immutable
        data class EnGarde(
            override val animation: ArenaAnimation,
            override val id: String,
            override val duelistUserId: String,
            val attackerUserId: String,
            val firstDie: Int,
            val secondDie: Int,
            val isSuccessful: Boolean,
        ) : Riposte
    }

    @Immutable
    data class SummonMinion(
        override val animation: ArenaAnimation,
        override val id: String,
        val necroUserId: String,
        val necroName: String,
        val singleDie: Int,
        val outcome: SummonMinionOutcome,
    ) : ArenaMessageState {
        override val actingClassType: CharacterClassType = CharacterClassType.Necromancer
        override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = necroUserId)
    }

    @Immutable
    sealed interface Persuade : ArenaMessageState {
        override val actingClassType: CharacterClassType
            get() = CharacterClassType.Bard

        override val flashAnchor: FlashAnchor
            get() {
                val bardId =
                    when (this) {
                        is Trigger -> bardUserId
                        is StrumLute -> bardUserId
                        is CharmActivation -> bardUserId
                        is Charm -> bardUserId
                        is Lull -> null
                    }
                return if (bardId != null) {
                    FlashAnchor.Avatar(userId = bardId)
                } else {
                    FlashAnchor.Center
                }
            }

        @Immutable
        data class Trigger(
            override val animation: ArenaAnimation = ArenaAnimation.Flash,
            override val id: String,
            val bardUserId: String,
        ) : Persuade

        @Immutable
        data class StrumLute(
            override val animation: ArenaAnimation = ArenaAnimation.Flash,
            override val id: String,
            val bardUserId: String,
            val diceRoll: Int,
            val isSuccessful: Boolean,
        ) : Persuade

        @Immutable
        data class CharmActivation(
            override val animation: ArenaAnimation = ArenaAnimation.Flash,
            override val id: String,
            val bardUserId: String,
        ) : Persuade

        @Immutable
        data class Lull(
            override val animation: ArenaAnimation = ArenaAnimation.Flash,
            override val id: String,
        ) : Persuade

        @Immutable
        data class Charm(
            override val animation: ArenaAnimation = ArenaAnimation.Flash,
            override val id: String,
            val bardUserId: String,
            val attackerName: String,
            val defenderName: String,
        ) : Persuade
    }

    @Immutable
    sealed interface Encore : ArenaMessageState {
        val bardUserId: String

        override val actingClassType: CharacterClassType
            get() = CharacterClassType.Bard

        override val flashAnchor: FlashAnchor
            get() = FlashAnchor.Avatar(userId = bardUserId)

        @Immutable
        data class Trigger(
            override val animation: ArenaAnimation = ArenaAnimation.Flash,
            override val id: String,
            override val bardUserId: String,
        ) : Encore

        @Immutable
        data class Roll(
            override val animation: ArenaAnimation = ArenaAnimation.Flash,
            override val id: String,
            override val bardUserId: String,
            val encoreDie: Int,
            val isSuccessful: Boolean,
        ) : Encore
    }

    @Immutable
    data class Impersonate(
        override val animation: ArenaAnimation = ArenaAnimation.Flash,
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
            override val animation: ArenaAnimation = ArenaAnimation.Flash,
            override val id: String,
            override val illusionistUserId: String,
            val firstDie: Int?,
            val secondDie: Int?,
        ) : MirrorImage

        @Immutable
        data class Pass(
            override val animation: ArenaAnimation,
            override val id: String,
            override val illusionistUserId: String,
            val attackerName: String,
            val attackerClassType: CharacterClassType,
            val messageType: NormalRollMessageType,
            val baseRoll: Int,
            val doublesBonus: Int,
        ) : MirrorImage {
            override val collapsingUserId: String? =
                if (messageType == NormalRollMessageType.Knockout) illusionistUserId else null
            override val actingClassType: CharacterClassType = attackerClassType
        }

        @Immutable
        data class TemptFate(
            override val animation: ArenaAnimation,
            override val id: String,
            override val illusionistUserId: String,
            val originalRoll: Int,
            val rawReroll: Int,
            val finalRoll: Int,
            val attackerName: String,
            val messageType: NormalRollMessageType,
        ) : MirrorImage {
            val rerollDisplay: String = "$rawReroll - 1"

            override val collapsingUserId: String? =
                if (messageType == NormalRollMessageType.Knockout) illusionistUserId else null
        }
    }

    @Immutable
    data class SpellPotency(
        override val animation: ArenaAnimation,
        override val id: String,
        val mageUserId: String,
        val mageName: String,
        val mainTargetUserId: String,
        val mainTargetName: String,
        val firstDie: Int,
        val secondDie: Int,
        val mainDamage: Int,
        val splashDamage: Int,
        val splashTargetUserIds: List<String>,
        val isMainTargetKnockedOut: Boolean,
    ) : ArenaMessageState {
        override val collapsingUserId: String? =
            if (isMainTargetKnockedOut) mainTargetUserId else null
        override val actingClassType: CharacterClassType = CharacterClassType.Mage
        override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = mageUserId)
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
            override val animation: ArenaAnimation = ArenaAnimation.Flash,
            override val id: String,
            override val paladinUserId: String,
        ) : HolyShield

        @Immutable
        data class Prayer(
            override val animation: ArenaAnimation,
            override val id: String,
            override val paladinUserId: String,
            val firstDie: Int,
            val secondDie: Int,
            val didSurvive: Boolean,
        ) : HolyShield {
            override val collapsingUserId: String? = if (!didSurvive) paladinUserId else null
        }

        @Immutable
        data class Wrath(
            override val animation: ArenaAnimation,
            override val id: String,
            override val paladinUserId: String,
            val paladinName: String,
            val attackerUserId: String,
            val attackerName: String,
            val singleDie: Int,
            val isAttackerKnockedOut: Boolean,
        ) : HolyShield {
            override val collapsingUserId: String? =
                if (isAttackerKnockedOut) attackerUserId else null
            override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = attackerUserId)
        }
    }

    @Immutable
    data class AllIn(
        override val animation: ArenaAnimation,
        override val id: String,
        val gamblerUserId: String,
        val gamblerName: String,
        val singleDie: Int,
        val outcome: AllInOutcome,
    ) : ArenaMessageState {
        override val collapsingUserId: String? =
            if (outcome == AllInOutcome.SelfKnockout) gamblerUserId else null
        override val actingClassType: CharacterClassType = CharacterClassType.Gambler
        override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = gamblerUserId)
    }

    @Immutable
    data class PrepareAttack(
        override val animation: ArenaAnimation,
        override val id: String,
        val duelistUserId: String,
        val duelistName: String,
        val targetUserId: String,
        val targetName: String,
        val firstDie: Int,
        val secondDie: Int,
        val isSuccessful: Boolean,
    ) : ArenaMessageState {
        override val collapsingUserId: String? = if (isSuccessful) targetUserId else null
        override val actingClassType: CharacterClassType = CharacterClassType.Duelist
        override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = duelistUserId)
    }

    @Immutable
    data class Doubles(
        override val animation: ArenaAnimation = ArenaAnimation.None,
        override val id: String,
        val playerName: String,
    ) : ArenaMessageState
}

enum class SummonMinionOutcome {
    Fizzle,
    Skeleton,
    SkeletonAndHeal,
    Golem,
}

enum class AllInOutcome {
    SelfKnockout,
    NoEffect,
    DoubleAttack,
}

private fun summonMinionOutcomeFor(singleDie: Int): SummonMinionOutcome =
    when (singleDie) {
        2,
        3 -> SummonMinionOutcome.Skeleton

        4,
        5 -> SummonMinionOutcome.SkeletonAndHeal

        6 -> SummonMinionOutcome.Golem

        else -> SummonMinionOutcome.Fizzle
    }

private fun allInOutcomeFor(singleDie: Int): AllInOutcome =
    when (singleDie) {
        1 -> AllInOutcome.SelfKnockout

        5,
        6 -> AllInOutcome.DoubleAttack

        else -> AllInOutcome.NoEffect
    }

@Immutable
data class CharacterClassMessageInfoState(val className: String, val type: CharacterClassType)

// TODO: figure out how to do this better
@Immutable data class ParticipantClass(val className: String, val type: CharacterClassType)

internal fun List<IncomingArenaMessage>.toMessageStates(
    participants: Map<String, ParticipantClass>
): List<ArenaMessageState> = map { incomingMessage ->
    incomingMessage.message.toState(id = incomingMessage.id, participants = participants)
}

private fun ArenaMessage.toState(
    id: String,
    participants: Map<String, ParticipantClass>,
): ArenaMessageState =
    when (this) {
        is ArenaMessage.NewRound ->
            ArenaMessageState.NewRound(id = id, roundNumber = roundNumber.toString())

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

        is ArenaMessage.AllIn -> {
            val outcome = allInOutcomeFor(singleDie = singleDie)
            val gestures = buildMap {
                val gesture =
                    when (outcome) {
                        AllInOutcome.SelfKnockout -> AvatarGesture.AllInBust

                        AllInOutcome.NoEffect,
                        AllInOutcome.DoubleAttack -> AvatarGesture.AllInToss
                    }
                put(gamblerUserId, gesture)
            }
            ArenaMessageState.AllIn(
                id = id,
                gamblerUserId = gamblerUserId,
                gamblerName = participants.resolveClassName(userId = gamblerUserId),
                singleDie = singleDie,
                outcome = outcome,
                animation = ArenaAnimation.FlashAndAvatar(gestures = gestures),
            )
        }

        is ArenaMessage.Dodge -> {
            val gesture =
                if (didArmorIncrease) AvatarGesture.DodgeBrace else AvatarGesture.DodgeFlourish
            ArenaMessageState.Dodge(
                id = id,
                didArmorIncrease = didArmorIncrease,
                dodgerUserId = dodgerUserId,
                animation = ArenaAnimation.FlashAndAvatar(gestures = mapOf(dodgerUserId to gesture)),
            )
        }

        is ArenaMessage.Encore.Trigger ->
            ArenaMessageState.Encore.Trigger(id = id, bardUserId = bardUserId)

        is ArenaMessage.Encore.Roll ->
            ArenaMessageState.Encore.Roll(
                id = id,
                bardUserId = bardUserId,
                encoreDie = encoreDie,
                isSuccessful = isSuccessful,
            )

        is ArenaMessage.HolyShield.Wrath -> {
            val gesture =
                if (isAttackerKnockedOut) AvatarGesture.Collapse else AvatarGesture.FlinchHeavy
            ArenaMessageState.HolyShield.Wrath(
                id = id,
                paladinUserId = paladinUserId,
                paladinName = participants.resolveClassName(userId = paladinUserId),
                attackerUserId = attackerUserId,
                attackerName = participants.resolveClassName(userId = attackerUserId),
                singleDie = singleDie,
                isAttackerKnockedOut = isAttackerKnockedOut,
                animation =
                    ArenaAnimation.FlashAndAvatar(gestures = mapOf(attackerUserId to gesture)),
            )
        }

        is ArenaMessage.HolyShield.Prayer -> {
            val animation =
                if (!didSurvive) {
                    ArenaAnimation.FlashAndAvatar(
                        gestures = mapOf(paladinUserId to AvatarGesture.Collapse)
                    )
                } else {
                    ArenaAnimation.Flash
                }
            ArenaMessageState.HolyShield.Prayer(
                id = id,
                paladinUserId = paladinUserId,
                firstDie = firstDie,
                secondDie = secondDie,
                didSurvive = didSurvive,
                animation = animation,
            )
        }

        is ArenaMessage.HolyShield.Trigger ->
            ArenaMessageState.HolyShield.Trigger(id = id, paladinUserId = paladinUserId)

        is ArenaMessage.Impersonate ->
            ArenaMessageState.Impersonate(
                id = id,
                illusionistUserId = illusionistUserId,
                illusionistName = participants.resolveClassName(userId = illusionistUserId),
                impersonatedClassType = impersonateAbility.impersonatedClassType,
                impersonatedAbilityName =
                    impersonateAbility.impersonatedAbility::class.simpleName.orEmpty(),
            )

        is ArenaMessage.KillingMomentum -> {
            ArenaMessageState.KillingMomentum(
                id = id,
                attackerUserId = attackerUserId,
                animation =
                    ArenaAnimation.FlashAndAvatar(
                        gestures = mapOf(attackerUserId to AvatarGesture.ReArm)
                    ),
            )
        }

        is ArenaMessage.LuckyCoin.Flip ->
            ArenaMessageState.LuckyCoin.Flip(
                id = id,
                gamblerUserId = gamblerUserId,
                singleDie = singleDie,
                didStopAttack = didStopAttack,
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
                animation = ArenaAnimation.FlashAndAvatar(gestures = gestures),
            )
        }

        is ArenaMessage.LuckyCoin.HighRollerTrigger ->
            ArenaMessageState.LuckyCoin.HighRollerTrigger(
                id = id,
                gamblerUserId = gamblerUserId,
                targetName = participants.resolveClassName(userId = attackerUserId),
            )

        is ArenaMessage.LuckyCoin.Trigger ->
            ArenaMessageState.LuckyCoin.Trigger(id = id, gamblerUserId = gamblerUserId)

        is ArenaMessage.MirrorImage.Pass -> {
            val gesture = defenderReactionGesture(normalRollMessageType = messageType)
            ArenaMessageState.MirrorImage.Pass(
                id = id,
                illusionistUserId = illusionistUserId,
                attackerName = participants.resolveClassName(userId = attackerUserId),
                attackerClassType = participants.resolveClassType(userId = attackerUserId),
                messageType = messageType,
                baseRoll = baseRoll,
                doublesBonus = doublesBonus,
                animation =
                    ArenaAnimation.FlashAndAvatar(gestures = mapOf(illusionistUserId to gesture)),
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
                attackerName = participants.resolveClassName(userId = attackerUserId),
                messageType = messageType,
                animation =
                    ArenaAnimation.FlashAndAvatar(gestures = mapOf(illusionistUserId to gesture)),
            )
        }

        is ArenaMessage.NormalRoll.Attack -> {
            val attackerClassType = participants.resolveClassType(userId = attackerUserId)
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
                animation = ArenaAnimation.Avatar(gestures = gestures),
            )
        }

        is ArenaMessage.NormalRoll.Outcome -> {
            val attackerClassType = participants.resolveClassType(userId = attackerUserId)
            val gestures = buildMap {
                put(
                    attackerUserId,
                    attackerStrike(
                        weaponClassType =
                            resolveWeaponClassType(
                                attackerClassType = attackerClassType,
                                impersonatedClassType = null,
                                id = id,
                            ),
                        messageType = type,
                    ),
                )
                put(defenderUserId, defenderReactionGesture(type))
            }

            ArenaMessageState.NormalRoll.Outcome(
                id = id,
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
                attackerClassType = attackerClassType,
                attackerName = participants.resolveClassName(userId = attackerUserId),
                defenderName = participants.resolveClassName(userId = defenderUserId),
                firstDie = firstDie,
                secondDie = secondDie,
                bonus = bonus,
                type = type,
                animation = ArenaAnimation.FlashAndAvatar(gestures = gestures),
            )
        }

        is ArenaMessage.Persuade.Charm ->
            ArenaMessageState.Persuade.Charm(
                id = id,
                bardUserId = bardUserId,
                attackerName = participants.resolveClassName(userId = attackerUserId),
                defenderName = participants.resolveClassName(userId = defenderUserId),
            )

        is ArenaMessage.Persuade.CharmActivation ->
            ArenaMessageState.Persuade.CharmActivation(id = id, bardUserId = bardUserId)

        is ArenaMessage.Persuade.Lull -> ArenaMessageState.Persuade.Lull(id = id)

        is ArenaMessage.Persuade.StrumLute ->
            ArenaMessageState.Persuade.StrumLute(
                id = id,
                bardUserId = bardUserId,
                diceRoll = diceRoll,
                isSuccessful = isSuccessful,
            )

        is ArenaMessage.Persuade.Trigger ->
            ArenaMessageState.Persuade.Trigger(id = id, bardUserId = bardUserId)

        is ArenaMessage.PrepareAttack -> {
            val gestures = buildMap {
                put(
                    duelistUserId,
                    if (isSuccessful) {
                        AvatarGesture.PreparedThrust
                    } else {
                        AvatarGesture.PreparedThrustWhiff
                    },
                )
                put(
                    targetUserId,
                    if (isSuccessful) AvatarGesture.Collapse else AvatarGesture.Sidestep,
                )
            }
            ArenaMessageState.PrepareAttack(
                id = id,
                duelistUserId = duelistUserId,
                duelistName = participants.resolveClassName(userId = duelistUserId),
                targetUserId = targetUserId,
                targetName = participants.resolveClassName(userId = targetUserId),
                firstDie = firstDie,
                secondDie = secondDie,
                isSuccessful = isSuccessful,
                animation = ArenaAnimation.FlashAndAvatar(gestures = gestures),
            )
        }

        is ArenaMessage.Riposte.EnGarde -> {
            val animation =
                if (isSuccessful) {
                    ArenaAnimation.FlashAndAvatar(
                        gestures = mapOf(attackerUserId to AvatarGesture.Flinch)
                    )
                } else {
                    ArenaAnimation.Flash
                }
            ArenaMessageState.Riposte.EnGarde(
                id = id,
                duelistUserId = duelistUserId,
                attackerUserId = attackerUserId,
                firstDie = firstDie,
                secondDie = secondDie,
                isSuccessful = isSuccessful,
                animation = animation,
            )
        }

        is ArenaMessage.Riposte.Trigger ->
            ArenaMessageState.Riposte.Trigger(
                id = id,
                duelistUserId = duelistUserId,
                attackerUserId = attackerUserId,
            )

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
                mageName = participants.resolveClassName(userId = mageUserId),
                mainTargetUserId = mainTargetUserId,
                mainTargetName = participants.resolveClassName(userId = mainTargetUserId),
                firstDie = firstDie,
                secondDie = secondDie,
                mainDamage = mainDamage,
                splashDamage = splashDamage,
                splashTargetUserIds = splashTargetUserIds,
                isMainTargetKnockedOut = isMainTargetKnockedOut,
                animation = ArenaAnimation.FlashAndAvatar(gestures = gestures),
            )
        }

        is ArenaMessage.SummonMinion -> {
            ArenaMessageState.SummonMinion(
                id = id,
                necroUserId = necroUserId,
                necroName = participants.resolveClassName(userId = necroUserId),
                singleDie = singleDie,
                outcome = summonMinionOutcomeFor(singleDie = singleDie),
                animation =
                    ArenaAnimation.FlashAndAvatar(
                        gestures = mapOf(necroUserId to AvatarGesture.Cast)
                    ),
            )
        }

        is ArenaMessage.Teleport.Result ->
            ArenaMessageState.Teleport.Result(
                id = id,
                mageUserId = mageUserId,
                didDefenderEscape = didDefenderEscape,
            )

        is ArenaMessage.Teleport.Roll ->
            ArenaMessageState.Teleport.Roll(
                id = id,
                mageUserId = mageUserId,
                rollerUserId = userId,
                rollerClassType = participants.resolveClassType(userId = userId),
                playerName = participants.resolveClassName(userId = userId),
                singleDie = singleDie,
            )

        is ArenaMessage.Teleport.Tie ->
            ArenaMessageState.Teleport.Tie(id = id, mageUserId = mageUserId)

        is ArenaMessage.Teleport.Trigger ->
            ArenaMessageState.Teleport.Trigger(id = id, mageUserId = mageUserId)

        is ArenaMessage.Doubles ->
            ArenaMessageState.Doubles(
                id = id,
                playerName = participants.resolveClassName(userId = userId),
            )
    }

@Immutable
sealed interface FlashAnchor {
    @Immutable data class Avatar(val userId: String) : FlashAnchor

    @Immutable data object Center : FlashAnchor
}

@Immutable
sealed interface ArenaAnimation {
    data object None : ArenaAnimation

    data object Flash : ArenaAnimation

    data class Avatar(val gestures: Map<String, AvatarGesture>) : ArenaAnimation

    data class FlashAndAvatar(val gestures: Map<String, AvatarGesture>) : ArenaAnimation

    fun gestureFor(userId: String): AvatarGesture? =
        when (this) {
            is Avatar -> gestures[userId]
            is FlashAndAvatar -> gestures[userId]
            else -> null
        }
}

private fun resolveWeaponClassType(
    attackerClassType: CharacterClassType,
    impersonatedClassType: CharacterClassType?,
    id: String,
): CharacterClassType {
    if (attackerClassType != CharacterClassType.Illusionist) return attackerClassType
    val borrowedWeapon = impersonatedClassType?.takeIf { it in classWeapons }
    return borrowedWeapon ?: randomBuiltWeaponFor(cueId = id)
}

private fun randomBuiltWeaponFor(cueId: String): CharacterClassType {
    val count = classWeapons.size
    val index = ((cueId.hashCode() % count) + count) % count
    return classWeapons[index]
}

private val classWeapons =
    listOf(
        CharacterClassType.Rogue,
        CharacterClassType.Gambler,
        CharacterClassType.Duelist,
        CharacterClassType.Necromancer,
        CharacterClassType.Bard,
        CharacterClassType.Mage,
        CharacterClassType.Paladin,
    )

private fun attackerStrike(
    weaponClassType: CharacterClassType,
    messageType: NormalRollMessageType,
): AvatarGesture =
    when (weaponClassType) {
        CharacterClassType.Rogue -> rogueStrike(messageType)
        CharacterClassType.Gambler -> gamblerStrike(messageType)
        CharacterClassType.Duelist -> duelistStrike(messageType)
        CharacterClassType.Necromancer -> AvatarGesture.Cast
        CharacterClassType.Bard -> AvatarGesture.Cast
        CharacterClassType.Mage -> AvatarGesture.Cast
        CharacterClassType.Paladin -> paladinStrike(messageType)
        else -> genericStrike(messageType)
    }

private fun paladinStrike(messageType: NormalRollMessageType): AvatarGesture =
    when (messageType) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Slash
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.SlashWhiff
        NormalRollMessageType.Damage -> AvatarGesture.Slash
        NormalRollMessageType.Knockout -> AvatarGesture.SlashFinish
    }

private fun rogueStrike(messageType: NormalRollMessageType): AvatarGesture =
    when (messageType) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Stab
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.StabOvershoot
        NormalRollMessageType.Damage -> AvatarGesture.Stab
        NormalRollMessageType.Knockout -> AvatarGesture.StabFlurry
    }

private fun gamblerStrike(messageType: NormalRollMessageType): AvatarGesture =
    when (messageType) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Throw
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.ThrowOvershoot
        NormalRollMessageType.Damage -> AvatarGesture.Throw
        NormalRollMessageType.Knockout -> AvatarGesture.ThrowStrong
    }

private fun duelistStrike(messageType: NormalRollMessageType): AvatarGesture =
    when (messageType) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Slash
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.SlashWhiff
        NormalRollMessageType.Damage -> AvatarGesture.Slash
        NormalRollMessageType.Knockout -> AvatarGesture.SlashFinish
    }

private fun genericStrike(messageType: NormalRollMessageType): AvatarGesture =
    when (messageType) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Lunge
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.LungeOvershoot
        NormalRollMessageType.Damage -> AvatarGesture.Lunge
        NormalRollMessageType.Knockout -> AvatarGesture.LungeStrong
    }

private fun defenderReactionGesture(normalRollMessageType: NormalRollMessageType): AvatarGesture =
    when (normalRollMessageType) {
        NormalRollMessageType.ArmorClassHit -> AvatarGesture.Flinch
        NormalRollMessageType.ArmorClassMiss -> AvatarGesture.Sidestep
        NormalRollMessageType.Damage -> AvatarGesture.FlinchHeavy
        NormalRollMessageType.Knockout -> AvatarGesture.Collapse
    }

// TODO: These, again, can simply be data passed through as necessary. This is ridiculous
private fun Map<String, ParticipantClass>.resolveClassName(userId: String) =
    getValue(key = userId).className

private fun Map<String, ParticipantClass>.resolveClassType(userId: String) =
    getValue(key = userId).type

private fun CharacterClassMessageInfo.toState(): CharacterClassMessageInfoState =
    CharacterClassMessageInfoState(className = className, type = type)
