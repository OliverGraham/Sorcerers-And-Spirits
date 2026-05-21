package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CombatResult
import com.linklabs.sorcerersandspirits.domain.arena.model.RollType
import com.linklabs.sorcerersandspirits.domain.arena.model.TurnInfo
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class PerformCombatActionUseCaseTest {

    private val rogue =
        ArenaPlayer.Selected(
            userId = "rogue_id",
            displayName = "Rogue Player",
            characterClass = CharacterClass.getRogue(), // AC 5, HP 6, Speed 8
        )

    private val mage =
        ArenaPlayer.Selected(
            userId = "mage_id",
            displayName = "Mage Player",
            characterClass = CharacterClass.getMage(), // AC 4, HP 4, Speed 2
        )

    private val paladin =
        ArenaPlayer.Selected(
            userId = "paladin_id",
            displayName = "Paladin Player",
            characterClass = CharacterClass.getPaladin(), // AC 7, HP 7, Speed 1
        )

    private val playerMap =
        mapOf(rogue.userId to rogue, mage.userId to mage, paladin.userId to paladin)

    private val repository: ArenaRepository = mockk()

    @Test
    fun `attack miss - turn order updates correctly`() = runTest {
        // Given
        val diceRollValue = 6
        val useCase = PerformCombatActionUseCase(repository, diceRoller = { diceRollValue })
        val resultSlot = slot<CombatResult>()
        coEvery { repository.applyCombatResult(any(), capture(resultSlot)) } returns Unit

        val turnInfo =
            TurnInfo(
                activePlayerUserId = rogue.userId,
                actedThisRoundUserIds = emptySet(),
                roundNumber = 1,
            )

        // When
        useCase(
            arenaId = "arena_1",
            playerMap = playerMap,
            attackerUserId = rogue.userId,
            targetUserId = paladin.userId,
            rollType = RollType.ArmorClassRoll,
            turnInfo = turnInfo,
        )

        // Then
        val result = resultSlot.captured
        assertIs<CombatResult.Miss>(result)

        assertEquals(mage.userId, result.nextTurnInfo.activePlayerUserId)
        assertEquals(setOf(rogue.userId), result.nextTurnInfo.actedThisRoundUserIds)
        assertEquals(1, result.nextTurnInfo.roundNumber)
    }

    @Test
    fun `attack hit - pending target set correctly`() = runTest {
        // Given
        val diceRollValue = 8
        val useCase = PerformCombatActionUseCase(repository, diceRoller = { diceRollValue })
        val resultSlot = slot<CombatResult>()
        coEvery { repository.applyCombatResult(any(), capture(resultSlot)) } returns Unit

        val turnInfo =
            TurnInfo(
                activePlayerUserId = rogue.userId,
                actedThisRoundUserIds = emptySet(),
                roundNumber = 1,
            )

        // When
        useCase(
            arenaId = "arena_1",
            playerMap = playerMap,
            attackerUserId = rogue.userId,
            targetUserId = paladin.userId,
            rollType = RollType.ArmorClassRoll,
            turnInfo = turnInfo,
        )

        // Then
        val result = resultSlot.captured
        assertIs<CombatResult.Hit>(result)
        assertEquals(paladin.userId, result.targetId)
        assertEquals(result.nextTurnInfo, null)
    }

    @Test
    fun `attack kills target - turn order updates among survivors`() = runTest {
        // Given
        val diceRollValue = 5
        val useCase = PerformCombatActionUseCase(repository, diceRoller = { diceRollValue })
        val resultSlot = slot<CombatResult>()
        coEvery { repository.applyCombatResult(any(), capture(resultSlot)) } returns Unit

        val turnInfo =
            TurnInfo(
                activePlayerUserId = rogue.userId,
                actedThisRoundUserIds = emptySet(),
                roundNumber = 1,
            )

        // When
        useCase(
            arenaId = "arena_1",
            playerMap = playerMap,
            attackerUserId = rogue.userId,
            targetUserId = mage.userId,
            rollType = RollType.HealthRoll,
            turnInfo = turnInfo,
        )

        // Then
        val result = resultSlot.captured
        assertIs<CombatResult.TargetDefeated>(result)
        assertEquals(mage.userId, result.defeatedPlayerId)

        assertEquals(paladin.userId, result.nextTurnInfo.activePlayerUserId)
        assertEquals(setOf(rogue.userId), result.nextTurnInfo.actedThisRoundUserIds)
    }

    @Test
    fun `last player kills target - new round starts with fastest survivor`() = runTest {
        // Given
        val diceRollValue = 10
        val useCase = PerformCombatActionUseCase(repository, diceRoller = { diceRollValue })
        val resultSlot = slot<CombatResult>()
        coEvery { repository.applyCombatResult(any(), capture(resultSlot)) } returns Unit

        val turnInfo =
            TurnInfo(
                activePlayerUserId = paladin.userId,
                actedThisRoundUserIds = setOf(rogue.userId, mage.userId),
                roundNumber = 1,
            )

        // When
        useCase(
            arenaId = "arena_1",
            playerMap = playerMap,
            attackerUserId = paladin.userId,
            targetUserId = mage.userId,
            rollType = RollType.HealthRoll,
            turnInfo = turnInfo,
        )

        // Then
        val result = resultSlot.captured
        assertIs<CombatResult.TargetDefeated>(result)

        assertEquals(rogue.userId, result.nextTurnInfo.activePlayerUserId)
        assertEquals(emptySet<String>(), result.nextTurnInfo.actedThisRoundUserIds)
        assertEquals(2, result.nextTurnInfo.roundNumber)
    }

    @Test
    fun `victory - last survivor wins`() = runTest {
        // Given
        val survivorsMap = mapOf(rogue.userId to rogue, paladin.userId to paladin)
        val diceRollValue = 10
        val useCase = PerformCombatActionUseCase(repository, diceRoller = { diceRollValue })
        val resultSlot = slot<CombatResult>()
        coEvery { repository.applyCombatResult(any(), capture(resultSlot)) } returns Unit

        val turnInfo =
            TurnInfo(
                activePlayerUserId = rogue.userId,
                actedThisRoundUserIds = emptySet(),
                roundNumber = 3,
            )

        // When
        useCase(
            arenaId = "arena_1",
            playerMap = survivorsMap,
            attackerUserId = rogue.userId,
            targetUserId = paladin.userId,
            rollType = RollType.HealthRoll,
            turnInfo = turnInfo,
        )

        // Then
        val result = resultSlot.captured
        assertIs<CombatResult.Victory>(result)
        assertEquals(rogue.characterClass.className, result.winnerClassName)
    }
}
