package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.MinionType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.WeaponType

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

    val coloredClassNames: List<CharacterClassMessageInfoState>
        get() = emptyList()

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
        override val coloredClassNames: List<CharacterClassMessageInfoState> =
            listOf(characterClassInfo)
    }

    @Immutable
    data class ConnectedToArena(
        override val animation: ArenaAnimationState = ArenaAnimationState.None,
        override val id: String,
        val displayName: String,
    ) : ArenaMessageState

    @Immutable
    data class ReadiedForNextMatch(
        override val animation: ArenaAnimationState = ArenaAnimationState.None,
        override val id: String,
        val displayName: String,
    ) : ArenaMessageState

    @Immutable
    data object NextMatchStartingSoon : ArenaMessageState {
        override val animation: ArenaAnimationState = ArenaAnimationState.None
        override val id: String = "NextMatchStartingSoon"
    }

    @Immutable
    data class NewTurn(
        override val animation: ArenaAnimationState = ArenaAnimationState.None,
        override val id: String,
        val characterClassInfo: CharacterClassMessageInfoState,
    ) : ArenaMessageState {
        override val actingClassType: CharacterClassType = characterClassInfo.type
        override val coloredClassNames: List<CharacterClassMessageInfoState> =
            listOf(characterClassInfo)
    }

    @Immutable
    sealed interface NormalRoll : ArenaMessageState {
        val attackerUserId: String
        val defenderUserId: String
        val normalRollMessageType: NormalRollMessageType

        override val flashAnchor: FlashAnchor
            get() = FlashAnchor.Avatar(userId = defenderUserId)

        @Immutable
        data class Attack(
            override val animation: ArenaAnimationState = ArenaAnimationState.Avatar(),
            override val id: String,
            override val attackerUserId: String,
            override val defenderUserId: String,
            override val normalRollMessageType: NormalRollMessageType,
            override val gestures: Map<String, AvatarGesture>,
            val attackerClassName: String,
            val attackerClassType: CharacterClassType,
            val impersonatedClassType: CharacterClassType?,
            val minionAttackerIndex: Int?,
            val minionAttackerType: MinionType?,
            val minionAttackerName: String?,
            val weaponType: WeaponType,
            val accentColor: Color,
        ) : NormalRoll {
            override val coloredClassNames: List<CharacterClassMessageInfoState> =
                if (minionAttackerName != null) {
                    listOf(
                        CharacterClassMessageInfoState(
                            className = attackerClassName,
                            type = attackerClassType,
                        ),
                    )
                } else {
                    emptyList()
                }
        }

        // TODO(SS-258): Does this need to be used for Avatar Animations???
        @Immutable
        data class Outcome(
            override val animation: ArenaAnimationState = ArenaAnimationState.Flash,
            override val id: String,
            override val attackerUserId: String,
            override val defenderUserId: String,
            override val normalRollMessageType: NormalRollMessageType,
            override val gestures: Map<String, AvatarGesture> = emptyMap(),
            val attackerClassName: String,
            val attackerClassType: CharacterClassType,
            val minionAttackerName: String?,
            val defenderClassName: String,
            val defenderClassType: CharacterClassType,
            val defenderArmorClass: Int,
            val firstDie: Int,
            val secondDie: Int,
            val bonus: Int,
            val accentColor: Color,
        ) : NormalRoll {
            val baseRoll: Int = firstDie + secondDie
            val attackerDisplayName: String = minionAttackerName ?: attackerClassName
            override val coloredClassNames: List<CharacterClassMessageInfoState> =
                listOf(
                    CharacterClassMessageInfoState(
                        className = attackerDisplayName,
                        type = attackerClassType,
                    ),
                    CharacterClassMessageInfoState(
                        className = defenderClassName,
                        type = defenderClassType,
                    ),
                )
        }
    }

    @Immutable
    data class Doubles(
        override val animation: ArenaAnimationState = ArenaAnimationState.None,
        override val id: String,
        val className: String,
        val classType: CharacterClassType,
    ) : ArenaMessageState {
        override val coloredClassNames: List<CharacterClassMessageInfoState> =
            listOf(CharacterClassMessageInfoState(className = className, type = classType))
    }

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
        override val coloredClassNames: List<CharacterClassMessageInfoState> =
            listOf(
                CharacterClassMessageInfoState(
                    className = actingClassType.name,
                    type = actingClassType,
                ),
            )
    }

    @Immutable
    data class Dodge(
        override val animation: ArenaAnimationState = ArenaAnimationState.FlashAndAvatar(),
        override val gestures: Map<String, AvatarGesture>,
        override val id: String,
        val didArmorIncrease: Boolean,
        val dodgerUserId: String,
        val newArmorClass: Int,
        val armorBonus: Int,
        val maxArmorBonus: Int,
    ) : ArenaMessageState {
        override val actingClassType: CharacterClassType = CharacterClassType.Rogue
        override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = dodgerUserId)
        override val isAvatarCompletedByGesture: Boolean = true
        override val coloredClassNames: List<CharacterClassMessageInfoState> =
            listOf(
                CharacterClassMessageInfoState(
                    className = actingClassType.name,
                    type = actingClassType,
                ),
            )
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
        override val coloredClassNames: List<CharacterClassMessageInfoState> =
            listOf(CharacterClassMessageInfoState(className = gamblerName, type = actingClassType))
    }

    @Immutable
    sealed interface LuckyCoin : ArenaMessageState {
        val gamblerUserId: String
        val attackerClassName: String
        val attackerClassType: CharacterClassType

        override val actingClassType: CharacterClassType
            get() = CharacterClassType.Gambler

        override val flashAnchor: FlashAnchor
            get() = FlashAnchor.Avatar(userId = gamblerUserId)

        override val coloredClassNames: List<CharacterClassMessageInfoState>
            get() =
                listOf(
                    CharacterClassMessageInfoState(
                        className = actingClassType.name,
                        type = actingClassType,
                    ),
                    CharacterClassMessageInfoState(
                        className = attackerClassName,
                        type = attackerClassType,
                    ),
                )

        @Immutable
        data class Trigger(
            override val animation: ArenaAnimationState = ArenaAnimationState.Flash,
            override val id: String,
            override val gamblerUserId: String,
            override val attackerClassName: String,
            override val attackerClassType: CharacterClassType,
            val attackerRollTotal: Int,
            val minimumCoinToStop: Int,
        ) : LuckyCoin

        @Immutable
        data class Flip(
            override val animation: ArenaAnimationState = ArenaAnimationState.FlashAndAvatar(),
            override val id: String,
            override val gamblerUserId: String,
            override val attackerClassName: String,
            override val attackerClassType: CharacterClassType,
            val defenderArmorClass: Int,
            val singleDie: Int,
            val didStopAttack: Boolean,
        ) : LuckyCoin

        @Immutable
        data class HighRollerTrigger(
            override val animation: ArenaAnimationState =
                ArenaAnimationState.FlashAndAvatar(isContinuous = true),
            override val id: String,
            override val gamblerUserId: String,
            override val attackerClassName: String,
            override val attackerClassType: CharacterClassType,
        ) : LuckyCoin

        @Immutable
        data class HighRollerDamage(
            override val animation: ArenaAnimationState = ArenaAnimationState.FlashAndAvatar(),
            override val id: String,
            override val gamblerUserId: String,
            val attackerUserId: String,
            override val attackerClassName: String,
            override val attackerClassType: CharacterClassType,
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
            val targetClassType: CharacterClassType,
            val firstDie: Int,
            val secondDie: Int,
            val total: Int,
            val knockoutRangeMin: Int,
            val knockoutRangeMax: Int,
            override val isSuccessful: Boolean,
        ) : PrepareAttack {
            override val coloredClassNames: List<CharacterClassMessageInfoState> =
                listOf(
                    CharacterClassMessageInfoState(className = duelistName, type = actingClassType),
                    CharacterClassMessageInfoState(
                        className = targetClassName,
                        type = targetClassType,
                    ),
                )
        }
    }

    @Immutable
    sealed interface Riposte : ArenaMessageState {
        override val actingClassType: CharacterClassType
            get() = CharacterClassType.Duelist

        override val flashAnchor: FlashAnchor
            get() = FlashAnchor.Avatar(userId = duelistUserId)

        override val coloredClassNames: List<CharacterClassMessageInfoState>
            get() =
                listOf(
                    CharacterClassMessageInfoState(
                        className = actingClassType.name,
                        type = actingClassType,
                    ),
                    CharacterClassMessageInfoState(
                        className = attackerClassName,
                        type = attackerClassType,
                    ),
                )

        val duelistUserId: String
        val attackerUserId: String
        val attackerClassName: String
        val attackerClassType: CharacterClassType

        @Immutable
        data class Trigger(
            override val animation: ArenaAnimationState.FlashAndAvatar =
                ArenaAnimationState.FlashAndAvatar(isContinuous = true),
            override val id: String,
            override val duelistUserId: String,
            override val attackerUserId: String,
            override val attackerClassName: String,
            override val attackerClassType: CharacterClassType,
            val successRangeMin: Int,
            val successRangeMax: Int,
        ) : Riposte

        @Immutable
        data class EnGarde(
            override val animation: ArenaAnimationState.FlashAndAvatar =
                ArenaAnimationState.FlashAndAvatar(),
            override val id: String,
            override val duelistUserId: String,
            override val attackerUserId: String,
            override val attackerClassName: String,
            override val attackerClassType: CharacterClassType,
            val firstDie: Int,
            val secondDie: Int,
            val total: Int,
            val isSuccessful: Boolean,
            override val gestures: Map<String, AvatarGesture> = emptyMap(),
        ) : Riposte
    }

    @Immutable
    sealed interface SummonMinion : ArenaMessageState {

        @Immutable
        data class Summon(
            override val animation: ArenaAnimationState = ArenaAnimationState.FlashAndAvatar(),
            override val id: String,
            override val actingClassType: CharacterClassType,
            val necromancerUserId: String,
            val className: String,
            val singleDie: Int,
            val outcome: SummonMinionOutcome,
            val minionIndex: Int,
            val minionName: String?,
            override val gestures: Map<String, AvatarGesture>,
        ) : SummonMinion {
            override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = necromancerUserId)
            override val coloredClassNames: List<CharacterClassMessageInfoState> =
                listOf(CharacterClassMessageInfoState(className = className, type = actingClassType))
        }

        @Immutable
        data class MinionDamaged(
            override val animation: ArenaAnimationState = ArenaAnimationState.Avatar(),
            override val id: String,
            override val actingClassType: CharacterClassType,
            val summonerUserId: String,
            val summonerClassName: String,
            val minionName: String,
            val minionHitPointsRemaining: Int,
        ) : SummonMinion {
            override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = summonerUserId)
            override val coloredClassNames: List<CharacterClassMessageInfoState> =
                listOf(
                    CharacterClassMessageInfoState(className = summonerClassName, type = actingClassType),
                )
        }

        @Immutable
        data class MinionDestroyed(
            override val animation: ArenaAnimationState = ArenaAnimationState.Avatar(),
            override val id: String,
            override val actingClassType: CharacterClassType,
            val summonerUserId: String,
            val summonerClassName: String,
            val minionName: String,
        ) : SummonMinion {
            override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = summonerUserId)
            override val coloredClassNames: List<CharacterClassMessageInfoState> =
                listOf(
                    CharacterClassMessageInfoState(className = summonerClassName, type = actingClassType),
                )
        }
    }

    @Immutable
    sealed interface Encore : ArenaMessageState {
        val bardUserId: String

        override val actingClassType: CharacterClassType

        override val flashAnchor: FlashAnchor
            get() = FlashAnchor.Avatar(userId = bardUserId)

        override val coloredClassNames: List<CharacterClassMessageInfoState>
            get() =
                listOf(
                    CharacterClassMessageInfoState(
                        className = actingClassType.name,
                        type = actingClassType,
                    ),
                )

        @Immutable
        data class Trigger(
            override val animation: ArenaAnimationState =
                ArenaAnimationState.FlashAndAvatar(isContinuous = true),
            override val id: String,
            override val bardUserId: String,
            override val actingClassType: CharacterClassType,
            val successRangeMin: Int,
            val successRangeMax: Int,
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

        override val coloredClassNames: List<CharacterClassMessageInfoState>
            get() =
                listOf(
                    CharacterClassMessageInfoState(
                        className = actingClassType.name,
                        type = actingClassType,
                    ),
                )

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
            val attackerClassType: CharacterClassType,
            val defenderClassName: String,
            val defenderClassType: CharacterClassType,
        ) : Persuade {
            override val coloredClassNames: List<CharacterClassMessageInfoState> =
                listOf(
                    CharacterClassMessageInfoState(
                        className = attackerClassName,
                        type = attackerClassType,
                    ),
                    CharacterClassMessageInfoState(
                        className = defenderClassName,
                        type = defenderClassType,
                    ),
                )
        }
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
        override val coloredClassNames: List<CharacterClassMessageInfoState> =
            listOf(
                CharacterClassMessageInfoState(className = illusionistName, type = actingClassType),
            )
    }

    @Immutable
    sealed interface MirrorImage : ArenaMessageState {
        val illusionistUserId: String

        override val actingClassType: CharacterClassType
            get() = CharacterClassType.Illusionist

        override val flashAnchor: FlashAnchor
            get() = FlashAnchor.Avatar(userId = illusionistUserId)

        override val coloredClassNames: List<CharacterClassMessageInfoState>
            get() =
                listOf(
                    CharacterClassMessageInfoState(
                        className = CharacterClassType.Illusionist.name,
                        type = CharacterClassType.Illusionist,
                    ),
                )

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
            override val coloredClassNames: List<CharacterClassMessageInfoState> =
                listOf(
                    CharacterClassMessageInfoState(
                        className = CharacterClassType.Illusionist.name,
                        type = CharacterClassType.Illusionist,
                    ),
                    CharacterClassMessageInfoState(
                        className = attackerClassName,
                        type = attackerClassType,
                    ),
                )
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
            val attackerClassType: CharacterClassType,
            val messageType: NormalRollMessageType,
            override val gestures: Map<String, AvatarGesture>,
        ) : MirrorImage {
            val rerollDisplay: String = "$rawReroll − 1 = $finalRoll"

            override val isAvatarCompletedByGesture: Boolean = true
            override val coloredClassNames: List<CharacterClassMessageInfoState> =
                listOf(
                    CharacterClassMessageInfoState(
                        className = CharacterClassType.Illusionist.name,
                        type = CharacterClassType.Illusionist,
                    ),
                    CharacterClassMessageInfoState(
                        className = attackerClassName,
                        type = attackerClassType,
                    ),
                )
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
        val mainTargetClassType: CharacterClassType,
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
        override val coloredClassNames: List<CharacterClassMessageInfoState> =
            listOf(
                CharacterClassMessageInfoState(className = mageName, type = actingClassType),
                CharacterClassMessageInfoState(
                    className = mainTargetClassName,
                    type = mainTargetClassType,
                ),
            )
    }

    @Immutable
    sealed interface Teleport : ArenaMessageState {
        val mageUserId: String

        override val actingClassType: CharacterClassType
            get() = CharacterClassType.Mage

        override val flashAnchor: FlashAnchor
            get() = FlashAnchor.Avatar(userId = mageUserId)

        override val coloredClassNames: List<CharacterClassMessageInfoState>
            get() =
                listOf(
                    CharacterClassMessageInfoState(
                        className = actingClassType.name,
                        type = actingClassType,
                    ),
                )

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
            val classType: CharacterClassType,
            val singleDie: Int,
        ) : Teleport {
            override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = rollerUserId)
            override val coloredClassNames: List<CharacterClassMessageInfoState> =
                listOf(CharacterClassMessageInfoState(className = className, type = classType))
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

        override val coloredClassNames: List<CharacterClassMessageInfoState>
            get() =
                listOf(
                    CharacterClassMessageInfoState(
                        className = actingClassType.name,
                        type = actingClassType,
                    ),
                )

        @Immutable
        data class Trigger(
            override val animation: ArenaAnimationState =
                ArenaAnimationState.FlashAndAvatar(isContinuous = true),
            override val id: String,
            override val paladinUserId: String,
            val successRangeMin: Int,
            val successRangeMax: Int,
        ) : HolyShield

        @Immutable
        data class Prayer(
            override val animation: ArenaAnimationState =
                ArenaAnimationState.FlashAndAvatar(isContinuous = true),
            override val id: String,
            override val paladinUserId: String,
            val firstDie: Int,
            val secondDie: Int,
            val total: Int,
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
            val attackerClassType: CharacterClassType,
            val singleDie: Int,
            val isAttackerKnockedOut: Boolean,
            override val gestures: Map<String, AvatarGesture>,
        ) : HolyShield {
            override val flashAnchor: FlashAnchor = FlashAnchor.Avatar(userId = attackerUserId)
            override val coloredClassNames: List<CharacterClassMessageInfoState> =
                listOf(
                    CharacterClassMessageInfoState(className = paladinName, type = actingClassType),
                    CharacterClassMessageInfoState(
                        className = attackerClassName,
                        type = attackerClassType,
                    ),
                )
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

    is ArenaMessage.ReadiedForNextMatch ->
        ArenaMessageState.ReadiedForNextMatch(id = id, displayName = displayName)

    is ArenaMessage.NewTurn ->
        ArenaMessageState.NewTurn(id = id, characterClassInfo = characterClassInfo.toState())

    is ArenaMessage.NormalRoll -> toState(id = id)

    is ArenaMessage.Doubles ->
        ArenaMessageState.Doubles(id = id, className = className, classType = classType)

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
            newArmorClass = newArmorClass,
            armorBonus = armorBonus,
            maxArmorBonus = maxArmorBonus,
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
        ArenaMessageState.LuckyCoin.Trigger(
            id = id,
            gamblerUserId = gamblerUserId,
            attackerClassName = attackerClassName,
            attackerClassType = attackerClassType,
            attackerRollTotal = attackerRollTotal,
            minimumCoinToStop = minimumCoinToStop,
        )

    is ArenaMessage.LuckyCoin.Flip ->
        ArenaMessageState.LuckyCoin.Flip(
            id = id,
            gamblerUserId = gamblerUserId,
            attackerClassName = attackerClassName,
            attackerClassType = attackerClassType,
            defenderArmorClass = defenderArmorClass,
            singleDie = singleDie,
            didStopAttack = didStopAttack,
        )

    is ArenaMessage.LuckyCoin.HighRollerTrigger ->
        ArenaMessageState.LuckyCoin.HighRollerTrigger(
            id = id,
            gamblerUserId = gamblerUserId,
            attackerClassName = attackerClassName,
            attackerClassType = attackerClassType,
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
            attackerClassName = attackerClassName,
            attackerClassType = attackerClassType,
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
            targetClassType = targetClassType,
            firstDie = firstDie,
            secondDie = secondDie,
            total = total,
            knockoutRangeMin = knockoutRangeMin,
            knockoutRangeMax = knockoutRangeMax,
            isSuccessful = isSuccessful,
        )

    is ArenaMessage.Riposte.Trigger ->
        ArenaMessageState.Riposte.Trigger(
            id = id,
            duelistUserId = duelistUserId,
            attackerUserId = attackerUserId,
            attackerClassName = attackerClassName,
            attackerClassType = attackerClassType,
            successRangeMin = successRangeMin,
            successRangeMax = successRangeMax,
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
            attackerClassName = attackerClassName,
            attackerClassType = attackerClassType,
            firstDie = firstDie,
            secondDie = secondDie,
            total = total,
            isSuccessful = isSuccessful,
            gestures = gestures,
        )
    }

    is ArenaMessage.SummonMinion.Summon -> {
        ArenaMessageState.SummonMinion.Summon(
            id = id,
            necromancerUserId = necromancerUserId,
            className = className,
            actingClassType = classType,
            singleDie = singleDie,
            outcome = summonMinionOutcomeFor(singleDie = singleDie),
            minionIndex = minionIndex,
            minionName = minionName,
            gestures = mapOf(necromancerUserId to AvatarGesture.Cast),
        )
    }

    is ArenaMessage.SummonMinion.MinionDamaged ->
        ArenaMessageState.SummonMinion.MinionDamaged(
            id = id,
            actingClassType = summonerClassType,
            summonerUserId = summonerUserId,
            summonerClassName = summonerClassName,
            minionName = minionName,
            minionHitPointsRemaining = minionHitPointsRemaining,
        )

    is ArenaMessage.SummonMinion.MinionDestroyed ->
        ArenaMessageState.SummonMinion.MinionDestroyed(
            id = id,
            actingClassType = summonerClassType,
            summonerUserId = summonerUserId,
            summonerClassName = summonerClassName,
            minionName = minionName,
        )

    is ArenaMessage.Encore.Trigger ->
        ArenaMessageState.Encore.Trigger(
            id = id,
            bardUserId = bardUserId,
            actingClassType = classType,
            successRangeMin = successRangeMin,
            successRangeMax = successRangeMax,
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
            attackerClassType = attackerClassType,
            defenderClassName = defenderClassName,
            defenderClassType = defenderClassType,
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
            attackerClassType = attackerClassType,
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
            mainTargetClassType = mainTargetClassType,
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
            classType = classType,
            singleDie = singleDie,
        )

    is ArenaMessage.Teleport.Tie ->
        ArenaMessageState.Teleport.Tie(id = id, mageUserId = mageUserId)

    is ArenaMessage.Teleport.Trigger ->
        ArenaMessageState.Teleport.Trigger(id = id, mageUserId = mageUserId)

    is ArenaMessage.HolyShield.Trigger ->
        ArenaMessageState.HolyShield.Trigger(
            id = id,
            paladinUserId = paladinUserId,
            successRangeMin = successRangeMin,
            successRangeMax = successRangeMax,
        )

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
            total = total,
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
            attackerClassType = attackerClassType,
            singleDie = singleDie,
            isAttackerKnockedOut = isAttackerKnockedOut,
            gestures = mapOf(attackerUserId to gesture),
        )
    }
}

internal fun ArenaMessageState.minionAttackerIndexFor(attackerUserId: String): Int? {
    if (this !is ArenaMessageState.NormalRoll.Attack) return null
    if (this.attackerUserId != attackerUserId) return null
    return minionAttackerIndex
}

internal fun ArenaMessageState.destroyedMinionsSurvivingCountFor(summonerUserId: String): Int? {
    if (this !is ArenaMessageState.MinionsDestroyed) return null
    if (this.summonerUserId != summonerUserId) return null
    return survivingMinionCount
}

// TODO: Make mapping functions for all types in this manner. ArenaMessage.Type.toState()
private fun ArenaMessage.NormalRoll.toState(id: String) = when (this) {
    is ArenaMessage.NormalRoll.Attack -> {
        val gestures = buildMap {
            put(
                key = attackerUserId,
                value =
                if (minionAttackerIndex != null) {
                    AvatarGesture.MinionCharge
                } else {
                    attackerStrike(
                        weaponClassType =
                        resolveWeaponClassType(
                            attackerClassType = attackerClassType,
                            impersonatedClassType = impersonatedCharacterType,
                            id = id,
                        ),
                        messageType = normalRollMessageType,
                    )
                },
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
            attackerClassName = attackerClassName,
            attackerClassType = attackerClassType,
            impersonatedClassType = impersonatedCharacterType,
            normalRollMessageType = normalRollMessageType,
            minionAttackerIndex = minionAttackerIndex,
            minionAttackerType = minionAttackerType,
            minionAttackerName = minionAttackerName,
            gestures = gestures,
            weaponType = WeaponType.getWeaponType(
                characterClassType = attackerClassType,
                minionAttackerType = minionAttackerType,
                impersonatedCharacterType = impersonatedCharacterType,
            ),
            accentColor = attackerClassType.toColor(),
        )
    }

    is ArenaMessage.NormalRoll.Outcome ->
        ArenaMessageState.NormalRoll.Outcome(
            id = id,
            attackerUserId = attackerUserId,
            defenderUserId = defenderUserId,
            attackerClassName = attackerClassName,
            attackerClassType = attackerClassType,
            minionAttackerName = minionAttackerName,
            defenderClassName = defenderClassName,
            defenderClassType = defenderClassType,
            defenderArmorClass = defenderArmorClass,
            firstDie = firstDie,
            secondDie = secondDie,
            bonus = bonus,
            normalRollMessageType = type,
            accentColor = attackerClassType.toColor(),
        )
}

@Immutable
sealed interface FlashAnchor {
    @Immutable data class Avatar(val userId: String) : FlashAnchor

    @Immutable data object Center : FlashAnchor
}

private fun CharacterClassMessageInfo.toState(): CharacterClassMessageInfoState = CharacterClassMessageInfoState(className = className, type = type)
