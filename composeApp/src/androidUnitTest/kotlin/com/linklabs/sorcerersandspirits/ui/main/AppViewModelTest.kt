package com.linklabs.sorcerersandspirits.ui.main

import app.cash.turbine.test
import com.linklabs.sorcerersandspirits.domain.authentication.IsUserSignedInUseCase
import com.linklabs.sorcerersandspirits.domain.connectivity.ConnectivityObserver
import com.linklabs.sorcerersandspirits.domain.game.usecase.InitializeGameConfigUseCase
import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.profile.usecase.CreateProfileUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetProfileFlowUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetRandomTravelerNameUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.UpdateProfileDisplayNameUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.ConnectTravelerPresenceUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val connectivityObserver: ConnectivityObserver = mockk()
    private val connectTravelerPresenceUseCase: ConnectTravelerPresenceUseCase =
        mockk(relaxed = true)
    private val createProfileUseCase: CreateProfileUseCase = mockk(relaxed = true)
    private val getProfileFlowUseCase: GetProfileFlowUseCase = mockk()
    private val getRandomTravelerNameUseCase: GetRandomTravelerNameUseCase = mockk()
    private val initializeGameConfigUseCase: InitializeGameConfigUseCase = mockk(relaxed = true)
    private val isUserSignedInUseCase: IsUserSignedInUseCase = mockk()
    private val updateProfileDisplayNameUseCase: UpdateProfileDisplayNameUseCase =
        mockk(relaxed = true)

    private val profileFlow = MutableStateFlow<Profile?>(null)
    private val isOnlineFlow = MutableStateFlow(value = true)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getProfileFlowUseCase() } returns profileFlow
        every { getRandomTravelerNameUseCase() } returns RANDOM_NAME
        every { connectivityObserver.isOnline } returns isOnlineFlow
        coEvery { isUserSignedInUseCase() } returns true
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        AppViewModel(
            connectivityObserver = connectivityObserver,
            connectTravelerPresenceUseCase = connectTravelerPresenceUseCase,
            createProfileUseCase = createProfileUseCase,
            getProfileFlowUseCase = getProfileFlowUseCase,
            getRandomTravelerNameUseCase = getRandomTravelerNameUseCase,
            initializeGameConfigUseCase = initializeGameConfigUseCase,
            isUserSignedInUseCase = isUserSignedInUseCase,
            updateProfileDisplayNameUseCase = updateProfileDisplayNameUseCase,
        )

    private fun UiState<AppState>.loaded(): AppState =
        assertIs<UiState.Loaded<AppState>>(value = this).data

    @Test
    fun `Given a signed-in user offline with no cached profile, then the offline shell is shown instead of hanging on loading`() =
        runTest {
            isOnlineFlow.value = false
            profileFlow.value = null

            createViewModel().uiStateFlow.test {
                assertEquals(expected = UiState.Loading, actual = awaitItem())

                val data = awaitItem().loaded()
                assertTrue(actual = data.isOffline)
                assertFalse(actual = data.showSignInDialog)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given a signed-in user online whose profile has not loaded, then the app stays on the loading indicator`() =
        runTest {
            isOnlineFlow.value = true
            profileFlow.value = null

            createViewModel().uiStateFlow.test {
                assertEquals(expected = UiState.Loading, actual = awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun `Given a signed-in user online when their profile loads, then the profile is shown without the offline flag and presence is connected`() =
        runTest {
            isOnlineFlow.value = true

            createViewModel().uiStateFlow.test {
                assertEquals(expected = UiState.Loading, actual = awaitItem())

                profileFlow.value = Profile(userId = USER_ID, displayName = DISPLAY_NAME)

                val data = awaitItem().loaded()
                assertEquals(expected = DISPLAY_NAME, actual = data.displayName)
                assertFalse(actual = data.isOffline)

                cancelAndIgnoreRemainingEvents()
            }

            coVerify { connectTravelerPresenceUseCase(profile = Profile(USER_ID, DISPLAY_NAME)) }
        }

    @Test
    fun `Given the offline shell is showing, when the connection returns and the profile loads, then the offline state clears`() =
        runTest {
            isOnlineFlow.value = false

            createViewModel().uiStateFlow.test {
                assertEquals(expected = UiState.Loading, actual = awaitItem())
                assertTrue(actual = awaitItem().loaded().isOffline)

                isOnlineFlow.value = true
                profileFlow.value = Profile(userId = USER_ID, displayName = DISPLAY_NAME)

                val data = awaitItem().loaded()
                assertFalse(actual = data.isOffline)
                assertEquals(expected = DISPLAY_NAME, actual = data.displayName)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given a cached profile while offline, then the profile is shown with the offline flag and presence is not connected`() =
        runTest {
            isOnlineFlow.value = false
            profileFlow.value = Profile(userId = USER_ID, displayName = DISPLAY_NAME)

            createViewModel().uiStateFlow.test {
                assertEquals(expected = UiState.Loading, actual = awaitItem())

                val data = awaitItem().loaded()
                assertEquals(expected = DISPLAY_NAME, actual = data.displayName)
                assertTrue(actual = data.isOffline)

                cancelAndIgnoreRemainingEvents()
            }

            coVerify(exactly = 0) { connectTravelerPresenceUseCase(profile = any()) }
        }

    @Test
    fun `When the user accepts a name while offline, then the sign-in dialog stays up with an offline notice and no profile is created`() =
        runTest {
            coEvery { isUserSignedInUseCase() } returns false
            isOnlineFlow.value = false
            val viewModel = createViewModel()

            viewModel.uiStateFlow.test {
                assertEquals(expected = UiState.Loading, actual = awaitItem())
                assertTrue(actual = awaitItem().loaded().showSignInDialog)

                viewModel.createProfile(newDisplayName = ENTERED_NAME)

                val data = awaitItem().loaded()
                assertTrue(actual = data.showSignInDialog)
                assertTrue(actual = data.isOffline)
                assertEquals(expected = ENTERED_NAME, actual = data.displayName)

                cancelAndIgnoreRemainingEvents()
            }

            coVerify(exactly = 0) { createProfileUseCase(displayName = any()) }
        }

    @Test
    fun `When the user accepts a name while online, then the profile is created`() = runTest {
        coEvery { isUserSignedInUseCase() } returns false
        isOnlineFlow.value = true
        val viewModel = createViewModel()

        viewModel.uiStateFlow.test {
            assertEquals(expected = UiState.Loading, actual = awaitItem())
            assertTrue(actual = awaitItem().loaded().showSignInDialog)

            viewModel.createProfile(newDisplayName = ENTERED_NAME)
            // Signing in flips the screen back to Loading while it works.
            assertEquals(expected = UiState.Loading, actual = awaitItem())

            cancelAndIgnoreRemainingEvents()
        }

        coVerify { createProfileUseCase(displayName = ENTERED_NAME) }
    }

    private companion object {
        private const val USER_ID = "user_123"
        private const val DISPLAY_NAME = "Gandalf the Grey"
        private const val ENTERED_NAME = "Radagast"
        private const val RANDOM_NAME = "a mysterious traveler"
    }
}
