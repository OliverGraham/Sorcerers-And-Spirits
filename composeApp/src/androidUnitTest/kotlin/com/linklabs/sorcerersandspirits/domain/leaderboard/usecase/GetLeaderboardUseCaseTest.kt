package com.linklabs.sorcerersandspirits.domain.leaderboard.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.GameStats
import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import com.linklabs.sorcerersandspirits.domain.profile.ProfileStats
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class GetLeaderboardUseCaseTest {

    private val profileRepository: ProfileRepository = mockk()
    private val authenticationRepository: AuthenticationRepository = mockk()

    private val useCase =
        GetLeaderboardUseCase(
            profileRepository = profileRepository,
            authenticationRepository = authenticationRepository,
        )

    @Test
    fun `assigns 1-based ranks in the order the repository returns`() = runTest {
        every { authenticationRepository.getUserId() } returns "someone-else"
        coEvery { profileRepository.getTopProfiles(limit = any()) } returns
            listOf(
                profile(userId = "a", displayName = "Alice", gauntletsWon = 5, matchesWon = 20),
                profile(userId = "b", displayName = "Bob", gauntletsWon = 3, matchesWon = 12),
            )

        val entries = useCase()

        assertEquals(expected = listOf(1, 2), actual = entries.map { it.rank })
        assertEquals(expected = "Alice", actual = entries[0].displayName)
        assertEquals(expected = 5L, actual = entries[0].gauntletsWon)
        assertEquals(expected = 20L, actual = entries[0].matchesWon)
    }

    @Test
    fun `marks the signed-in user's own row`() = runTest {
        every { authenticationRepository.getUserId() } returns "b"
        coEvery { profileRepository.getTopProfiles(limit = any()) } returns
            listOf(
                profile(userId = "a", displayName = "Alice", gauntletsWon = 5, matchesWon = 20),
                profile(userId = "b", displayName = "Bob", gauntletsWon = 3, matchesWon = 12),
            )

        val entries = useCase()

        assertFalse(actual = entries[0].isCurrentUser)
        assertTrue(actual = entries[1].isCurrentUser)
    }

    @Test
    fun `marks no row when no user is signed in`() = runTest {
        every { authenticationRepository.getUserId() } returns null
        coEvery { profileRepository.getTopProfiles(limit = any()) } returns
            listOf(profile(userId = "a", displayName = "Alice", gauntletsWon = 5, matchesWon = 20))

        val entries = useCase()

        assertFalse(actual = entries.single().isCurrentUser)
    }

    private fun profile(userId: String, displayName: String, gauntletsWon: Long, matchesWon: Long) =
        Profile(
            userId = userId,
            displayName = displayName,
            stats =
                ProfileStats(
                    overall = GameStats(gauntletsWon = gauntletsWon, matchesWon = matchesWon)
                ),
        )
}
