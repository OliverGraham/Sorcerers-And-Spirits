package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.ui.screens.arena.AttackArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.UseAbilityArguments

@Immutable
sealed interface CombatActionState {
    val activeUserIds: Set<String>

    @Immutable data class Idle(override val activeUserIds: Set<String>) : CombatActionState

    @Immutable
    data class NormalAttack(
        val roll: NormalAttackType,
        val attackerUserId: String,
        val defenderUserId: String,
        val originalTurnUserId: String,
        override val activeUserIds: Set<String>,
    ) : CombatActionState

    @Immutable
    data class AbilityRoll(
        val action: AbilityActionState,
        val attackerUserId: String,
        val defenderUserId: String,
        val originalTurnUserId: String,
        override val activeUserIds: Set<String>,
    ) : CombatActionState
}

/**
 * The player whose turn it is — the turn owner. While a turn resolves this stays the original actor
 * even when a defensive ability hands the dice to someone else (Riposte, Persuade). It drives
 * whose-turn decisions (target resolution, auto-targeting) but not the avatar seating, which
 * follows the combatant currently attacking ([attackingUserId]) so a Riposte reversal does not seat
 * the same player in both seats (SS-157, SS-165). On an idle turn it is the single player about to
 * act.
 */
val CombatActionState.turnOwnerUserId: String
    get() =
        when (this) {
            is CombatActionState.Idle -> activeUserIds.first()
            is CombatActionState.NormalAttack -> originalTurnUserId
            is CombatActionState.AbilityRoll -> originalTurnUserId
        }

/**
 * The combatant currently pressing the attack, always seated as a large avatar. This is the
 * in-flight action's attacker, which is normally the turn owner but diverges after a successful
 * Riposte reverses the attack: the Duelist (defender) becomes the attacker while it stays the
 * original attacker's turn (SS-165). On an idle turn it is the player about to act.
 */
val CombatActionState.attackingUserId: String
    get() =
        when (this) {
            is CombatActionState.Idle -> activeUserIds.first()
            is CombatActionState.NormalAttack -> attackerUserId
            is CombatActionState.AbilityRoll -> attackerUserId
        }

/**
 * The player being targeted by the in-flight action, or null on an idle turn with no target yet.
 */
val CombatActionState.targetedDefenderUserId: String?
    get() =
        when (this) {
            is CombatActionState.Idle -> null
            is CombatActionState.NormalAttack -> defenderUserId
            is CombatActionState.AbilityRoll -> defenderUserId
        }

@Immutable
sealed interface AbilityActionState {

    @Immutable
    sealed interface Activated : AbilityActionState {
        @Immutable data class AllIn(val allInAbility: OffensiveAbility.AllIn) : Activated

        @Immutable
        data class SummonMinion(val summonMinionAbility: OffensiveAbility.SummonMinion) : Activated

        @Immutable data object PrepareAttack : Activated

        @Immutable
        data class Impersonate(val impersonateAbility: OffensiveAbility.Impersonate) : Activated
    }

    @Immutable
    sealed interface Passive : AbilityActionState {

        @Immutable
        sealed interface LuckyCoin : Passive {
            val attackerRollTotal: Int

            @Immutable data class Flip(override val attackerRollTotal: Int) : LuckyCoin

            @Immutable data class HighRoller(override val attackerRollTotal: Int) : LuckyCoin
        }

        @Immutable data class Riposte(val attackerRollTotal: Int) : Passive

        @Immutable
        sealed interface Persuade : Passive {
            @Immutable data object StrumLute : Persuade

            @Immutable
            data class Charm(
                val targetableUserIds: Set<String>,
                val selectedCharmTargetUserId: String?,
            ) : Persuade
        }

        @Immutable data object Encore : Passive

        @Immutable
        data class Teleport(
            val attackerRoll: TeleportDieState,
            val defenderRoll: TeleportDieState,
            val attackerDoublesBonus: Int,
        ) : Passive

        @Immutable data class TeleportDieState(val userId: String, val roll: Int? = null)

        @Immutable
        data class MirrorImage(
            val firstDie: MirrorImageDieState,
            val secondDie: MirrorImageDieState,
            val attackType: NormalAttackType,
            val attackerDoublesBonus: Int,
            val mirrorImageDefensiveAbility: DefensiveAbility.MirrorImage,
            val rerollSelection: AbilityAction.Passive.MirrorImage.RerollSelection =
                AbilityAction.Passive.MirrorImage.RerollSelection.None,
        ) : Passive

        @Immutable data class MirrorImageDieState(val originalRoll: Int, val reroll: Int? = null)

        @Immutable
        sealed interface HolyShield : Passive {
            @Immutable data object Prayer : HolyShield

            @Immutable data object Wrath : HolyShield
        }
    }
}

/**
 * Maps the user's attack input to its domain counterpart against the live [combatActionState]
 * (never a value captured when the control was built, so a held button always continues the current
 * step rather than restarting the attack). Null when the input is stale: an attack can only be
 * started on the user's own idle turn, and an in-flight attack can only be continued by its
 * attacker. A tap that lands after the state has already moved past its step — the shared state
 * takes a network round trip to advance, so the buttons outlive the action they belonged to — is
 * dropped rather than resolved against the wrong step (SS-181).
 */
internal fun AttackArguments.toDomain(
    combatActionState: CombatActionState
): CurrentCombatAction.NormalAttack? =
    when (combatActionState) {
        is CombatActionState.AbilityRoll -> null

        is CombatActionState.Idle -> {
            val isUsersTurn = userPlayerId in combatActionState.activeUserIds
            if (isUsersTurn) {
                CurrentCombatAction.NormalAttack(
                    roll = NormalAttackType.ArmorClass,
                    attackerUserId = userPlayerId,
                    defenderUserId = targetedOpponentUserId,
                    originalTurnUserId = userPlayerId,
                )
            } else {
                null
            }
        }

        is CombatActionState.NormalAttack ->
            if (combatActionState.attackerUserId == userPlayerId) {
                CurrentCombatAction.NormalAttack(
                    roll = combatActionState.roll,
                    attackerUserId = combatActionState.attackerUserId,
                    defenderUserId = combatActionState.defenderUserId,
                    originalTurnUserId = combatActionState.originalTurnUserId,
                )
            } else {
                null
            }
    }

/**
 * Maps the user's ability input to its domain counterpart. The combatant ids come from the
 * in-flight [combatActionState] when responding to an ability roll already in progress, or from the
 * user and their selected target when activating an ability on their own idle turn.
 *
 * Null when the input is stale. A passive ability roll only exists as a response to the in-flight
 * ability step, so its payload must match the action the state currently owes; an activated ability
 * can only be started on the user's own idle turn. Anything else is a tap that landed after the
 * state had already moved on — the previous fallback fabricated an action from the user's own ids
 * instead, turning a defender's late Mirror Image tap into the defender attacking themselves, which
 * corrupted the match for everyone (SS-181).
 */
internal fun UseAbilityArguments.toDomain(
    combatActionState: CombatActionState
): CurrentCombatAction.AbilityRoll? {
    val purpose = abilityActionState.withSelectedCharmTarget(targetUserId = targetedOpponentUserId)

    return when (combatActionState) {
        is CombatActionState.AbilityRoll -> {
            val isResponseToOwedStep = combatActionState.action::class == abilityActionState::class
            if (isResponseToOwedStep) {
                CurrentCombatAction.AbilityRoll(
                    purpose = purpose,
                    attackerUserId = combatActionState.attackerUserId,
                    defenderUserId = combatActionState.defenderUserId,
                    originalTurnUserId = combatActionState.originalTurnUserId,
                    rollingUserId = userPlayerId,
                )
            } else {
                null
            }
        }

        is CombatActionState.Idle -> {
            val isUsersTurn = userPlayerId in combatActionState.activeUserIds
            val isActivatedAbility = abilityActionState is AbilityActionState.Activated
            val isActivationOfUsersTurn = isUsersTurn && isActivatedAbility
            if (isActivationOfUsersTurn) {
                CurrentCombatAction.AbilityRoll(
                    purpose = purpose,
                    attackerUserId = userPlayerId,
                    defenderUserId = targetedOpponentUserId.ifEmpty { userPlayerId },
                    originalTurnUserId = userPlayerId,
                    rollingUserId = userPlayerId,
                )
            } else {
                null
            }
        }

        is CombatActionState.NormalAttack -> null
    }
}

/**
 * Resolves the Bard's chosen Charm target into the domain ability. Only Persuade's Charm consumes a
 * target, and only a [targetUserId] the Domain already vetted as charmable (a conscious opponent
 * that is neither the Bard nor the attacker, per
 * [AbilityActionState.Passive.Persuade.Charm.targetableUserIds]) is accepted; any other input
 * leaves the target unset. Every other ability ignores [targetUserId].
 */
private fun AbilityActionState.withSelectedCharmTarget(targetUserId: String): AbilityAction =
    if (this is AbilityActionState.Passive.Persuade.Charm && targetUserId in targetableUserIds) {
        copy(selectedCharmTargetUserId = targetUserId).toDomain()
    } else {
        toDomain()
    }

internal fun AbilityActionState.toDomain(): AbilityAction =
    when (this) {
        is AbilityActionState.Activated ->
            when (this) {
                is AbilityActionState.Activated.AllIn ->
                    AbilityAction.Activated.AllIn(allInAbility = allInAbility)

                is AbilityActionState.Activated.SummonMinion ->
                    AbilityAction.Activated.SummonMinion(summonMinionAbility = summonMinionAbility)

                AbilityActionState.Activated.PrepareAttack -> AbilityAction.Activated.PrepareAttack

                is AbilityActionState.Activated.Impersonate ->
                    AbilityAction.Activated.Impersonate(impersonateAbility = impersonateAbility)
            }

        is AbilityActionState.Passive ->
            when (this) {
                is AbilityActionState.Passive.LuckyCoin ->
                    when (this) {
                        is AbilityActionState.Passive.LuckyCoin.Flip ->
                            AbilityAction.Passive.LuckyCoin.Flip(
                                attackerRollTotal = attackerRollTotal
                            )

                        is AbilityActionState.Passive.LuckyCoin.HighRoller ->
                            AbilityAction.Passive.LuckyCoin.HighRoller(
                                attackerRollTotal = attackerRollTotal
                            )
                    }

                is AbilityActionState.Passive.Riposte ->
                    AbilityAction.Passive.Riposte(attackerRollTotal = attackerRollTotal)

                is AbilityActionState.Passive.Persuade ->
                    when (this) {
                        AbilityActionState.Passive.Persuade.StrumLute ->
                            AbilityAction.Passive.Persuade.StrumLute

                        is AbilityActionState.Passive.Persuade.Charm ->
                            AbilityAction.Passive.Persuade.Charm(
                                targetableUserIds = targetableUserIds,
                                selectedCharmTargetUserId = selectedCharmTargetUserId,
                            )
                    }

                AbilityActionState.Passive.Encore -> AbilityAction.Passive.Encore

                is AbilityActionState.Passive.Teleport ->
                    AbilityAction.Passive.Teleport(
                        attackerRoll =
                            AbilityAction.Passive.Teleport.TeleportDie(
                                userId = attackerRoll.userId,
                                roll = attackerRoll.roll,
                            ),
                        defenderRoll =
                            AbilityAction.Passive.Teleport.TeleportDie(
                                userId = defenderRoll.userId,
                                roll = defenderRoll.roll,
                            ),
                        attackerDoublesBonus = attackerDoublesBonus,
                    )

                is AbilityActionState.Passive.MirrorImage ->
                    AbilityAction.Passive.MirrorImage(
                        firstDie =
                            AbilityAction.Passive.MirrorImage.MirrorImageDie(
                                originalRoll = firstDie.originalRoll,
                                reroll = firstDie.reroll,
                            ),
                        secondDie =
                            AbilityAction.Passive.MirrorImage.MirrorImageDie(
                                originalRoll = secondDie.originalRoll,
                                reroll = secondDie.reroll,
                            ),
                        attackType = attackType,
                        attackerDoublesBonus = attackerDoublesBonus,
                        mirrorImageDefensiveAbility = mirrorImageDefensiveAbility,
                        rerollSelection = rerollSelection,
                    )

                is AbilityActionState.Passive.HolyShield ->
                    when (this) {
                        AbilityActionState.Passive.HolyShield.Prayer ->
                            AbilityAction.Passive.HolyShield.Prayer

                        AbilityActionState.Passive.HolyShield.Wrath ->
                            AbilityAction.Passive.HolyShield.Wrath
                    }
            }
    }

internal fun NextCombatAction.toCombatActionState(): CombatActionState =
    when (this) {
        is NextCombatAction.Idle -> CombatActionState.Idle(activeUserIds = activeUserIds)

        is NextCombatAction.NormalAttack ->
            CombatActionState.NormalAttack(
                roll = roll,
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
                originalTurnUserId = originalTurnUserId,
                activeUserIds = activeUserIds,
            )

        is NextCombatAction.AbilityRoll ->
            CombatActionState.AbilityRoll(
                action = purpose.toState(),
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
                originalTurnUserId = originalTurnUserId,
                activeUserIds = activeUserIds,
            )
    }

internal fun AbilityAction.toState(): AbilityActionState =
    when (this) {
        is AbilityAction.Activated ->
            when (this) {
                is AbilityAction.Activated.AllIn ->
                    AbilityActionState.Activated.AllIn(allInAbility = allInAbility)

                is AbilityAction.Activated.SummonMinion ->
                    AbilityActionState.Activated.SummonMinion(
                        summonMinionAbility = summonMinionAbility
                    )

                AbilityAction.Activated.PrepareAttack -> AbilityActionState.Activated.PrepareAttack

                is AbilityAction.Activated.Impersonate ->
                    AbilityActionState.Activated.Impersonate(
                        impersonateAbility = impersonateAbility
                    )
            }

        is AbilityAction.Passive ->
            when (this) {
                is AbilityAction.Passive.LuckyCoin ->
                    when (this) {
                        is AbilityAction.Passive.LuckyCoin.Flip ->
                            AbilityActionState.Passive.LuckyCoin.Flip(
                                attackerRollTotal = attackerRollTotal
                            )

                        is AbilityAction.Passive.LuckyCoin.HighRoller ->
                            AbilityActionState.Passive.LuckyCoin.HighRoller(
                                attackerRollTotal = attackerRollTotal
                            )
                    }

                is AbilityAction.Passive.Riposte ->
                    AbilityActionState.Passive.Riposte(attackerRollTotal = attackerRollTotal)

                is AbilityAction.Passive.Persuade ->
                    when (this) {
                        AbilityAction.Passive.Persuade.StrumLute ->
                            AbilityActionState.Passive.Persuade.StrumLute

                        is AbilityAction.Passive.Persuade.Charm ->
                            AbilityActionState.Passive.Persuade.Charm(
                                targetableUserIds = targetableUserIds,
                                selectedCharmTargetUserId = selectedCharmTargetUserId,
                            )
                    }

                AbilityAction.Passive.Encore -> AbilityActionState.Passive.Encore

                is AbilityAction.Passive.Teleport ->
                    AbilityActionState.Passive.Teleport(
                        attackerRoll =
                            AbilityActionState.Passive.TeleportDieState(
                                userId = attackerRoll.userId,
                                roll = attackerRoll.roll,
                            ),
                        defenderRoll =
                            AbilityActionState.Passive.TeleportDieState(
                                userId = defenderRoll.userId,
                                roll = defenderRoll.roll,
                            ),
                        attackerDoublesBonus = attackerDoublesBonus,
                    )

                is AbilityAction.Passive.MirrorImage ->
                    AbilityActionState.Passive.MirrorImage(
                        firstDie =
                            AbilityActionState.Passive.MirrorImageDieState(
                                originalRoll = firstDie.originalRoll,
                                reroll = firstDie.reroll,
                            ),
                        secondDie =
                            AbilityActionState.Passive.MirrorImageDieState(
                                originalRoll = secondDie.originalRoll,
                                reroll = secondDie.reroll,
                            ),
                        attackType = attackType,
                        attackerDoublesBonus = attackerDoublesBonus,
                        mirrorImageDefensiveAbility = mirrorImageDefensiveAbility,
                        rerollSelection = rerollSelection,
                    )

                is AbilityAction.Passive.HolyShield ->
                    when (this) {
                        AbilityAction.Passive.HolyShield.Prayer ->
                            AbilityActionState.Passive.HolyShield.Prayer

                        AbilityAction.Passive.HolyShield.Wrath ->
                            AbilityActionState.Passive.HolyShield.Wrath
                    }
            }
    }
