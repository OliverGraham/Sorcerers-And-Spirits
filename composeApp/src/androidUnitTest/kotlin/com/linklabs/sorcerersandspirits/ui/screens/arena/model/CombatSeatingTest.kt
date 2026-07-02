package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.TestCharacterClasses
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import kotlin.test.Test
import kotlin.test.assertEquals

class CombatSeatingTest {

    @Test
    fun `Given the user's Illusionist owes a Mirror Image choice, then both combatants stay seated with the user at the bottom`() {
        val state =
            combatState(
                combatActionState =
                    mirrorImageRoll(attackerUserId = OPPONENT_ID, defenderUserId = USER_ID),
                playerMap =
                    twoPlayers(
                        userClass = TestCharacterClasses.illusionist(),
                        opponentClass = TestCharacterClasses.rogue(),
                    ),
            )

        assertEquals(
            expected =
                CombatSeating(
                    topSeatUserId = OPPONENT_ID,
                    bottomSeatUserId = USER_ID,
                    headIconUserIds = emptyList(),
                ),
            actual = seatingOf(state = state),
        )
    }

    @Test
    fun `Given the user attacked an Illusionist who owes a Mirror Image choice, then both combatants stay seated with the user at the bottom`() {
        val state =
            combatState(
                combatActionState =
                    mirrorImageRoll(attackerUserId = USER_ID, defenderUserId = OPPONENT_ID),
                playerMap =
                    twoPlayers(
                        userClass = TestCharacterClasses.rogue(),
                        opponentClass = TestCharacterClasses.illusionist(),
                    ),
            )

        assertEquals(
            expected =
                CombatSeating(
                    topSeatUserId = OPPONENT_ID,
                    bottomSeatUserId = USER_ID,
                    headIconUserIds = emptyList(),
                ),
            actual = seatingOf(state = state),
        )
    }

    @Test
    fun `Given an attack passes through a Mirror Image choice and on to the Health roll, then the seating never changes`() {
        val playerMap =
            twoPlayers(
                userClass = TestCharacterClasses.rogue(),
                opponentClass = TestCharacterClasses.illusionist(),
            )
        val armorRollSeating =
            seatingOf(
                state =
                    combatState(
                        combatActionState =
                            normalAttack(
                                roll = NormalAttackType.ArmorClass,
                                attackerUserId = USER_ID,
                                defenderUserId = OPPONENT_ID,
                            ),
                        playerMap = playerMap,
                    )
            )
        val mirrorImageSeating =
            seatingOf(
                state =
                    combatState(
                        combatActionState =
                            mirrorImageRoll(attackerUserId = USER_ID, defenderUserId = OPPONENT_ID),
                        playerMap = playerMap,
                    )
            )
        val healthRollSeating =
            seatingOf(
                state =
                    combatState(
                        combatActionState =
                            normalAttack(
                                roll = NormalAttackType.Health,
                                attackerUserId = USER_ID,
                                defenderUserId = OPPONENT_ID,
                            ),
                        playerMap = playerMap,
                    )
            )

        assertEquals(expected = armorRollSeating, actual = mirrorImageSeating)
        assertEquals(expected = armorRollSeating, actual = healthRollSeating)
    }

    @Test
    fun `Given a successful Riposte reversed the user's attack, then the seating is the same as during the original attack`() {
        val originalAttackSeating =
            seatingOf(
                state =
                    combatState(
                        combatActionState =
                            normalAttack(
                                roll = NormalAttackType.ArmorClass,
                                attackerUserId = USER_ID,
                                defenderUserId = OPPONENT_ID,
                            )
                    )
            )
        val reversedAttackSeating =
            seatingOf(
                state =
                    combatState(
                        combatActionState =
                            normalAttack(
                                roll = NormalAttackType.ArmorClass,
                                attackerUserId = OPPONENT_ID,
                                defenderUserId = USER_ID,
                                originalTurnUserId = USER_ID,
                            )
                    )
            )

        assertEquals(
            expected =
                CombatSeating(
                    topSeatUserId = OPPONENT_ID,
                    bottomSeatUserId = USER_ID,
                    headIconUserIds = emptyList(),
                ),
            actual = reversedAttackSeating,
        )
        assertEquals(expected = originalAttackSeating, actual = reversedAttackSeating)
    }

    @Test
    fun `Given a turn passes between the same two players, then the seating does not change`() {
        val usersTurnSeating =
            seatingOf(
                state =
                    combatState(
                        combatActionState =
                            CombatActionState.Idle(activeUserIds = setOf(element = USER_ID))
                    )
            )
        val opponentsTurnSeating =
            seatingOf(
                state =
                    combatState(
                        combatActionState =
                            CombatActionState.Idle(activeUserIds = setOf(element = OPPONENT_ID))
                    )
            )

        assertEquals(expected = usersTurnSeating, actual = opponentsTurnSeating)
    }

    @Test
    fun `Given a self-targeted ability names the actor as its own defender, then only the actor is seated`() {
        val state =
            combatState(
                combatActionState =
                    CombatActionState.AbilityRoll(
                        action = AbilityActionState.Activated.PrepareAttack,
                        attackerUserId = OPPONENT_ID,
                        defenderUserId = OPPONENT_ID,
                        originalTurnUserId = OPPONENT_ID,
                        activeUserIds = setOf(element = OPPONENT_ID),
                    )
            )

        assertEquals(
            expected =
                CombatSeating(
                    topSeatUserId = OPPONENT_ID,
                    bottomSeatUserId = null,
                    headIconUserIds = listOf(USER_ID),
                ),
            actual = seatingOf(state = state),
        )
    }

    @Test
    fun `Given the user's turn starts against two opponents with no target chosen, then the user sits alone at the bottom`() {
        val state =
            combatState(
                combatActionState =
                    CombatActionState.Idle(activeUserIds = setOf(element = USER_ID)),
                playerMap = threePlayers(),
            )

        assertEquals(
            expected =
                CombatSeating(
                    topSeatUserId = null,
                    bottomSeatUserId = USER_ID,
                    headIconUserIds = listOf(OPPONENT_ID, THIRD_PLAYER_ID),
                ),
            actual = seatingOf(state = state),
        )
    }

    @Test
    fun `Given two opponents fight each other, then they keep the same seats whoever holds the dice`() {
        val opponentAttackingSeating =
            seatingOf(
                state =
                    combatState(
                        combatActionState =
                            normalAttack(
                                roll = NormalAttackType.ArmorClass,
                                attackerUserId = OPPONENT_ID,
                                defenderUserId = THIRD_PLAYER_ID,
                            ),
                        playerMap = threePlayers(),
                    )
            )
        val thirdPlayerAttackingSeating =
            seatingOf(
                state =
                    combatState(
                        combatActionState =
                            normalAttack(
                                roll = NormalAttackType.ArmorClass,
                                attackerUserId = THIRD_PLAYER_ID,
                                defenderUserId = OPPONENT_ID,
                            ),
                        playerMap = threePlayers(),
                    )
            )

        assertEquals(
            expected =
                CombatSeating(
                    topSeatUserId = OPPONENT_ID,
                    bottomSeatUserId = THIRD_PLAYER_ID,
                    headIconUserIds = listOf(USER_ID),
                ),
            actual = opponentAttackingSeating,
        )
        assertEquals(expected = opponentAttackingSeating, actual = thirdPlayerAttackingSeating)
    }

    @Test
    fun `Given the targeted defender is knocked out, then they collapse to a head icon instead of staying seated`() {
        val state =
            combatState(
                combatActionState =
                    normalAttack(
                        roll = NormalAttackType.Health,
                        attackerUserId = OPPONENT_ID,
                        defenderUserId = THIRD_PLAYER_ID,
                    ),
                playerMap =
                    threePlayers(thirdClass = TestCharacterClasses.rogue().copy(hitPoints = 0)),
            )

        assertEquals(
            expected =
                CombatSeating(
                    topSeatUserId = OPPONENT_ID,
                    bottomSeatUserId = null,
                    headIconUserIds = listOf(USER_ID, THIRD_PLAYER_ID),
                ),
            actual = seatingOf(state = state),
        )
    }

    private fun seatingOf(state: ArenaState.Combat): CombatSeating {
        val targetUserId = state.resolveTargetUserId(selectedOpponentUserId = "")
        return state.seatingFor(targetUserId = targetUserId)
    }

    private fun combatState(
        combatActionState: CombatActionState,
        playerMap: Map<String, PlayerState.Selected> = twoPlayers(),
    ) =
        ArenaState.Combat(
            playerMap = playerMap,
            userPlayerId = USER_ID,
            combatActionState = combatActionState,
            roundNumber = 1,
            isNewRound = false,
            doublesBonus = null,
        )

    private fun normalAttack(
        roll: NormalAttackType,
        attackerUserId: String,
        defenderUserId: String,
        originalTurnUserId: String = attackerUserId,
    ) =
        CombatActionState.NormalAttack(
            roll = roll,
            attackerUserId = attackerUserId,
            defenderUserId = defenderUserId,
            originalTurnUserId = originalTurnUserId,
            activeUserIds = setOf(element = attackerUserId),
        )

    private fun mirrorImageRoll(attackerUserId: String, defenderUserId: String) =
        CombatActionState.AbilityRoll(
            action =
                AbilityActionState.Passive.MirrorImage(
                    firstDie = AbilityActionState.Passive.MirrorImageDieState(originalRoll = 3),
                    secondDie = AbilityActionState.Passive.MirrorImageDieState(originalRoll = 4),
                    attackType = NormalAttackType.ArmorClass,
                    mirrorImageDefensiveAbility = DefensiveAbility.MirrorImage(),
                ),
            attackerUserId = attackerUserId,
            defenderUserId = defenderUserId,
            originalTurnUserId = attackerUserId,
            activeUserIds = setOf(element = defenderUserId),
        )

    private fun twoPlayers(
        userClass: CharacterClass = TestCharacterClasses.paladin(),
        opponentClass: CharacterClass = TestCharacterClasses.rogue(),
    ) =
        mapOf(
            USER_ID to player(userId = USER_ID, characterClass = userClass),
            OPPONENT_ID to player(userId = OPPONENT_ID, characterClass = opponentClass),
        )

    private fun threePlayers(thirdClass: CharacterClass = TestCharacterClasses.duelist()) =
        mapOf(
            USER_ID to player(userId = USER_ID, characterClass = TestCharacterClasses.paladin()),
            OPPONENT_ID to
                player(userId = OPPONENT_ID, characterClass = TestCharacterClasses.rogue()),
            THIRD_PLAYER_ID to player(userId = THIRD_PLAYER_ID, characterClass = thirdClass),
        )

    private fun player(userId: String, characterClass: CharacterClass) =
        PlayerState.Selected(
            userId = userId,
            displayName = userId,
            characterClass = characterClass.toState(),
        )

    private companion object {
        const val USER_ID = "userId"
        const val OPPONENT_ID = "opponentId"
        const val THIRD_PLAYER_ID = "thirdPlayerId"
    }
}
