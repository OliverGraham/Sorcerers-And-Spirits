package com.linklabs.sorcerersandspirits.ui.screens.arena

import app.cash.turbine.test
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ChooseCharacterClassUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CoordinateArenaMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.PerformCombatActionUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.UpdateArenaPlayerUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SetCurrentTravelerAvailableUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ArenaScreenViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val chooseCharacterClassUseCase: ChooseCharacterClassUseCase = mockk()
    private val coordinateArenaMatchUseCase: CoordinateArenaMatchUseCase = mockk()
    private val getUserIdUseCase: GetUserIdUseCase = mockk()
    private val performCombatActionUseCase: PerformCombatActionUseCase = mockk()
    private val setCurrentTravelerAvailableUseCase: SetCurrentTravelerAvailableUseCase =
        mockk(relaxed = true)
    private val updateArenaPlayerUseCase: UpdateArenaPlayerUseCase = mockk()

    private val arguments =
        ArenaScreenViewModelArguments(arenaId = "test_arena", expectedNumberOfPlayers = 2)
    private val arenaMatchFlow = MutableStateFlow<ArenaMatch?>(null)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getUserIdUseCase() } returns "user_123"
        coEvery { coordinateArenaMatchUseCase(any(), any()) } returns arenaMatchFlow
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        ArenaScreenViewModel(
            arguments = arguments,
            chooseCharacterClassUseCase = chooseCharacterClassUseCase,
            coordinateArenaMatchUseCase = coordinateArenaMatchUseCase,
            getUserIdUseCase = getUserIdUseCase,
            performCombatActionUseCase = performCombatActionUseCase,
            setCurrentTravelerAvailableUseCase = setCurrentTravelerAvailableUseCase,
            updateArenaPlayerUseCase = updateArenaPlayerUseCase,
        )

    @Test
    fun `onTryLeaveArena shows forfeit dialog when state is WaitingForAllPlayers`() = runTest {
        arenaMatchFlow.value =
            ArenaMatch.WaitingForAllPlayers(messages = emptyList(), numberOfNewMessages = 0)
        val viewModel = createViewModel()

        viewModel.uiStateFlow.test {
            // Loading
            awaitItem()

            // Loaded (initial)
            val initialState = awaitItem()
            assertTrue(initialState is UiState.Loaded)
            assertFalse(initialState.data.showForfeitDialog)

            viewModel.onTryLeaveArena()

            // Loaded (with dialog)
            val updatedState = awaitItem()
            assertTrue(updatedState is UiState.Loaded)
            assertTrue(updatedState.data.showForfeitDialog, "Dialog should be shown")
        }
    }

    @Test
    fun `onTryLeaveArena confirms leave when state is Finished`() = runTest {
        arenaMatchFlow.value = ArenaMatch.Finished(messages = emptyList(), numberOfNewMessages = 0)
        val viewModel = createViewModel()

        viewModel.uiStateFlow.test {
            // Loading
            awaitItem()
            // Loaded
            awaitItem()

            viewModel.onTryLeaveArena()

            // confirmLeaveArena updates state by calling updateLoaded (showForfeitDialog = false)
            // But if it doesn't change, we skip emission check.

            cancelAndIgnoreRemainingEvents()
        }

        advanceUntilIdle()
        coVerify { setCurrentTravelerAvailableUseCase() }
    }

    @Test
    fun `onConfirmLeaveArena calls use case and hides dialog`() = runTest {
        arenaMatchFlow.value =
            ArenaMatch.WaitingForAllPlayers(messages = emptyList(), numberOfNewMessages = 0)
        val viewModel = createViewModel()

        viewModel.uiStateFlow.test {
            // Loading
            awaitItem()
            // Loaded
            awaitItem()

            viewModel.onTryLeaveArena()
            assertTrue((awaitItem() as UiState.Loaded).data.showForfeitDialog)

            viewModel.onConfirmLeaveArena()
            assertFalse((awaitItem() as UiState.Loaded).data.showForfeitDialog)

            cancelAndIgnoreRemainingEvents()
        }

        advanceUntilIdle()
        coVerify { setCurrentTravelerAvailableUseCase() }
    }

    @Test
    fun `onDismissForfeitDialog hides dialog`() = runTest {
        arenaMatchFlow.value =
            ArenaMatch.WaitingForAllPlayers(messages = emptyList(), numberOfNewMessages = 0)
        val viewModel = createViewModel()

        viewModel.uiStateFlow.test {
            // Loading
            awaitItem()
            // Loaded
            awaitItem()

            viewModel.onTryLeaveArena()
            assertTrue((awaitItem() as UiState.Loaded).data.showForfeitDialog)

            viewModel.onDismissForfeitDialog()
            assertFalse((awaitItem() as UiState.Loaded).data.showForfeitDialog)
        }
    }
}
