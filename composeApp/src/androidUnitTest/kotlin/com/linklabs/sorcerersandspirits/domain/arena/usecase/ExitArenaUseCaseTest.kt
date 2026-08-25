package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExitArenaUseCaseTest {

    private val authenticationRepository: AuthenticationRepository = mockk(relaxed = true)
    private val arenaRepository: ArenaRepository = mockk(relaxed = true)
    private val tavernRepository: TavernRepository = mockk(relaxed = true)

    private val exitArenaUseCase =
        ExitArenaUseCase(
            arenaRepository = arenaRepository,
            authenticationRepository = authenticationRepository,
            tavernRepository = tavernRepository,
        )

    @Test
    fun `invoke should call exitArena and setTravelerAvailableIfPresent when userId is present`() = runTest {
        val arenaId = "test_arena"
        val userId = "user_123"
        coEvery { authenticationRepository.getUserId() } returns userId

        exitArenaUseCase(arenaId = arenaId)

        coVerify { arenaRepository.exitArena(arenaId = arenaId, userId = userId) }
        coVerify { tavernRepository.setTravelerAvailableIfPresent(userId = userId) }
    }

    @Test
    fun `invoke should not call repository methods when userId is null`() = runTest {
        val arenaId = "test_arena"
        coEvery { authenticationRepository.getUserId() } returns null

        exitArenaUseCase(arenaId = arenaId)

        coVerify(exactly = 0) { arenaRepository.exitArena(any(), any()) }
        coVerify(exactly = 0) { tavernRepository.setTravelerAvailableIfPresent(any()) }
    }
}
