package com.linklabs.sorcerersandspirits.ui.screens.leaderboard

import app.cash.turbine.test
import com.linklabs.sorcerersandspirits.domain.leaderboard.LeaderboardEntry
import com.linklabs.sorcerersandspirits.domain.leaderboard.usecase.GetLeaderboardUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardScreenViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getLeaderboardUseCase: GetLeaderboardUseCase = mockk()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        LeaderboardScreenViewModel(getLeaderboardUseCase = getLeaderboardUseCase)

    @Test
    fun `Given the leaderboard loads, then the screen shows the ranked rows`() = runTest {
        coEvery { getLeaderboardUseCase() } returns listOf(ENTRY)

        val viewModel = createViewModel()

        viewModel.uiStateFlow.test {
            assertEquals(expected = UiState.Loading, actual = awaitItem())

            val loaded = assertIs<UiState.Loaded<LeaderboardState>>(value = awaitItem())
            assertEquals(expected = 1, actual = loaded.data.entries.single().rank)
            assertEquals(expected = "Gandalf", actual = loaded.data.entries.single().displayName)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given the load fails, then the screen shows an error`() = runTest {
        coEvery { getLeaderboardUseCase() } throws RuntimeException("network down")

        val viewModel = createViewModel()

        viewModel.uiStateFlow.test {
            assertEquals(expected = UiState.Loading, actual = awaitItem())
            assertEquals(expected = UiState.Error, actual = awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When retried after a failure, then the leaderboard reloads`() = runTest {
        coEvery { getLeaderboardUseCase() } throws RuntimeException("network down")
        val viewModel = createViewModel()

        viewModel.uiStateFlow.test {
            assertEquals(expected = UiState.Loading, actual = awaitItem())
            assertEquals(expected = UiState.Error, actual = awaitItem())

            coEvery { getLeaderboardUseCase() } returns listOf(ENTRY)
            viewModel.onRetry()

            assertEquals(expected = UiState.Loading, actual = awaitItem())
            assertIs<UiState.Loaded<LeaderboardState>>(value = awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When sorted by rating, then rows reorder and re-rank by rating`() = runTest {
        // Returned in Gauntlets-won order: the grinder outranks the ace by gauntlets but not
        // rating.
        coEvery { getLeaderboardUseCase() } returns listOf(GRINDER, ACE)
        val viewModel = createViewModel()

        viewModel.uiStateFlow.test {
            assertEquals(expected = UiState.Loading, actual = awaitItem())

            val byGauntlets = assertIs<UiState.Loaded<LeaderboardState>>(value = awaitItem()).data
            assertEquals(
                expected = listOf("Grinder", "Ace"),
                actual = byGauntlets.entries.map { it.displayName },
            )
            assertEquals(expected = LeaderboardSort.GauntletsWon, actual = byGauntlets.sort)

            viewModel.onSortSelected(sort = LeaderboardSort.Rating)

            val byRating = assertIs<UiState.Loaded<LeaderboardState>>(value = awaitItem()).data
            assertEquals(
                expected = listOf("Ace", "Grinder"),
                actual = byRating.entries.map { it.displayName },
            )
            assertEquals(expected = listOf(1, 2), actual = byRating.entries.map { it.rank })
            assertEquals(expected = LeaderboardSort.Rating, actual = byRating.sort)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        val ENTRY =
            LeaderboardEntry(
                displayName = "Gandalf",
                rating = 2_225,
                gauntletsWon = 12,
                matchesWon = 41,
                isCurrentUser = true,
            )

        val GRINDER =
            LeaderboardEntry(
                displayName = "Grinder",
                rating = 100,
                gauntletsWon = 10,
                matchesWon = 0,
                isCurrentUser = false,
            )

        val ACE =
            LeaderboardEntry(
                displayName = "Ace",
                rating = 900,
                gauntletsWon = 1,
                matchesWon = 0,
                isCurrentUser = false,
            )
    }
}
