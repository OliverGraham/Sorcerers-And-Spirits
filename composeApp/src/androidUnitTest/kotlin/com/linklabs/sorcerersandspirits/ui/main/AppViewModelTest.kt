package com.linklabs.sorcerersandspirits.ui.main

import app.cash.turbine.test
import com.linklabs.sorcerersandspirits.domain.arena.usecase.SyncArenaMembershipOnConnectUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.IsUserSignedInUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.LinkAccountResult
import com.linklabs.sorcerersandspirits.domain.authentication.SignInMethod
import com.linklabs.sorcerersandspirits.domain.authentication.usecase.SignInWithSocialUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.usecase.SignOutUseCase
import com.linklabs.sorcerersandspirits.domain.connectivity.ConnectivityObserver
import com.linklabs.sorcerersandspirits.domain.crashreporting.CrashReporter
import com.linklabs.sorcerersandspirits.domain.game.usecase.InitializeGameConfigUseCase
import com.linklabs.sorcerersandspirits.domain.profile.DisplayNameResult
import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.profile.usecase.CreateProfileForCurrentUserUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.CreateProfileUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.FlushPendingStatsUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetProfileFlowUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetProfileUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetRandomTravelerNameUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.ConnectTravelerPresenceUseCase
import com.linklabs.sorcerersandspirits.domain.version.AppUpgradeStatus
import com.linklabs.sorcerersandspirits.domain.version.usecase.CheckAppVersionUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val checkAppVersionUseCase: CheckAppVersionUseCase = mockk()
    private val connectivityObserver: ConnectivityObserver = mockk()
    private val connectTravelerPresenceUseCase: ConnectTravelerPresenceUseCase =
        mockk(relaxed = true)
    private val crashReporter: CrashReporter = mockk(relaxed = true)
    private val createProfileForCurrentUserUseCase: CreateProfileForCurrentUserUseCase =
        mockk(relaxed = true)
    private val createProfileUseCase: CreateProfileUseCase = mockk(relaxed = true)
    private val flushPendingStatsUseCase: FlushPendingStatsUseCase = mockk(relaxed = true)
    private val getProfileFlowUseCase: GetProfileFlowUseCase = mockk()
    private val getProfileUseCase: GetProfileUseCase = mockk()
    private val getRandomTravelerNameUseCase: GetRandomTravelerNameUseCase = mockk()
    private val initializeGameConfigUseCase: InitializeGameConfigUseCase = mockk(relaxed = true)
    private val isUserSignedInUseCase: IsUserSignedInUseCase = mockk()
    private val signInWithSocialUseCase: SignInWithSocialUseCase = mockk(relaxed = true)
    private val signOutUseCase: SignOutUseCase = mockk(relaxed = true)
    private val syncArenaMembershipOnConnectUseCase: SyncArenaMembershipOnConnectUseCase =
        mockk(relaxed = true)
    private val firstScreenReadySignal = FirstScreenReadySignal()

    private val profileFlow = MutableStateFlow<Profile?>(null)
    private val isOnlineFlow = MutableStateFlow(value = true)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getProfileFlowUseCase() } returns profileFlow
        every { getRandomTravelerNameUseCase() } returns RANDOM_NAME
        every { connectivityObserver.isOnline } returns isOnlineFlow
        every { checkAppVersionUseCase() } returns AppUpgradeStatus.UP_TO_DATE
        coEvery { isUserSignedInUseCase() } returns true
        // The default signed-in user has a profile, so launch proceeds into the app; tests for the
        // missing-profile recovery override this.
        coEvery { getProfileUseCase() } returns
            Profile(userId = USER_ID, displayName = DISPLAY_NAME)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = AppViewModel(
        checkAppVersionUseCase = checkAppVersionUseCase,
        connectivityObserver = connectivityObserver,
        connectTravelerPresenceUseCase = connectTravelerPresenceUseCase,
        crashReporter = crashReporter,
        createProfileForCurrentUserUseCase = createProfileForCurrentUserUseCase,
        createProfileUseCase = createProfileUseCase,
        flushPendingStatsUseCase = flushPendingStatsUseCase,
        getProfileFlowUseCase = getProfileFlowUseCase,
        getProfileUseCase = getProfileUseCase,
        getRandomTravelerNameUseCase = getRandomTravelerNameUseCase,
        initializeGameConfigUseCase = initializeGameConfigUseCase,
        isUserSignedInUseCase = isUserSignedInUseCase,
        signInWithSocialUseCase = signInWithSocialUseCase,
        signOutUseCase = signOutUseCase,
        syncArenaMembershipOnConnectUseCase = syncArenaMembershipOnConnectUseCase,
        firstScreenReadySignal = firstScreenReadySignal,
    )

    private fun UiState<AppState>.loaded(): AppState = assertIs<UiState.Loaded<AppState>>(value = this).data

    @Test
    fun `Given a signed-in user offline with no cached profile, then the offline shell is shown instead of hanging on loading`() = runTest {
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
    fun `Given a signed-in user online whose profile is missing, then the sign-in dialog is shown for recovery instead of hanging`() = runTest {
        isOnlineFlow.value = true
        coEvery { getProfileUseCase() } returns null

        createViewModel().uiStateFlow.test {
            assertEquals(expected = UiState.Loading, actual = awaitItem())
            assertTrue(actual = awaitItem().loaded().showSignInDialog)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given a signed-in user is missing a profile, when they accept a name, then a profile is created for the current account rather than a new anonymous one`() = runTest {
        isOnlineFlow.value = true
        coEvery { getProfileUseCase() } returns null
        coEvery { createProfileForCurrentUserUseCase(displayName = ENTERED_NAME) } returns
            DisplayNameResult.SUCCESS
        val viewModel = createViewModel()

        viewModel.uiStateFlow.test {
            assertEquals(expected = UiState.Loading, actual = awaitItem())
            assertTrue(actual = awaitItem().loaded().showSignInDialog)

            viewModel.createProfile(newDisplayName = ENTERED_NAME)
            assertEquals(expected = UiState.Loading, actual = awaitItem())

            cancelAndIgnoreRemainingEvents()
        }

        coVerify { createProfileForCurrentUserUseCase(displayName = ENTERED_NAME) }
        coVerify(exactly = 0) { createProfileUseCase(displayName = any()) }
    }

    @Test
    fun `Given a signed-in user online when their profile loads, then the profile is shown without the offline flag and presence is connected`() = runTest {
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
    fun `Given the offline shell is showing, when the connection returns and the profile loads, then the offline state clears`() = runTest {
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
    fun `Given a cached profile while offline, then the profile is shown with the offline flag and presence is not connected`() = runTest {
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
    fun `When the user accepts a name while offline, then the sign-in dialog stays up with an offline notice and no profile is created`() = runTest {
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

    @Test
    fun `When a chosen social sign-in method is unavailable, then the dialog stays up carrying the failure reason`() = runTest {
        coEvery { isUserSignedInUseCase() } returns false
        isOnlineFlow.value = true
        coEvery { signInWithSocialUseCase(method = SignInMethod.GOOGLE) } returns
            LinkAccountResult.NOT_AVAILABLE
        val viewModel = createViewModel()

        viewModel.uiStateFlow.test {
            assertEquals(expected = UiState.Loading, actual = awaitItem())
            assertTrue(actual = awaitItem().loaded().showSignInDialog)

            viewModel.onSignInWithSocialMethod(
                displayName = ENTERED_NAME,
                method = SignInMethod.GOOGLE,
            )
            assertEquals(expected = UiState.Loading, actual = awaitItem())

            val data = awaitItem().loaded()
            assertTrue(actual = data.showSignInDialog)
            assertEquals(expected = LinkAccountResult.NOT_AVAILABLE, actual = data.linkResult)

            cancelAndIgnoreRemainingEvents()
        }

        // A failed sign-in must not provision a profile.
        coVerify(exactly = 0) { createProfileForCurrentUserUseCase(displayName = any()) }
    }

    @Test
    fun `When email sign-in succeeds for a new account, then a profile is created and the app is entered`() = runTest {
        coEvery { isUserSignedInUseCase() } returns false
        isOnlineFlow.value = true
        coEvery {
            signInWithSocialUseCase.withEmail(email = EMAIL, password = PASSWORD)
        } returns LinkAccountResult.SUCCESS
        coEvery { getProfileUseCase() } returns null
        coEvery { createProfileForCurrentUserUseCase(displayName = ENTERED_NAME) } answers
            {
                profileFlow.value = Profile(userId = USER_ID, displayName = ENTERED_NAME)
                DisplayNameResult.SUCCESS
            }
        val viewModel = createViewModel()

        viewModel.uiStateFlow.test {
            assertEquals(expected = UiState.Loading, actual = awaitItem())
            assertTrue(actual = awaitItem().loaded().showSignInDialog)

            viewModel.onSignInWithEmail(
                displayName = ENTERED_NAME,
                email = EMAIL,
                password = PASSWORD,
            )
            assertEquals(expected = UiState.Loading, actual = awaitItem())

            val data = awaitItem().loaded()
            assertFalse(actual = data.showSignInDialog)
            assertEquals(expected = ENTERED_NAME, actual = data.displayName)

            cancelAndIgnoreRemainingEvents()
        }

        coVerify { createProfileForCurrentUserUseCase(displayName = ENTERED_NAME) }
    }

    @Test
    fun `Given a returning player signs in with an existing provider account, then their profile is recovered without creating a new one`() = runTest {
        coEvery { isUserSignedInUseCase() } returns false
        isOnlineFlow.value = true
        coEvery { signInWithSocialUseCase(method = SignInMethod.GOOGLE) } returns
            LinkAccountResult.SUCCESS
        // The recovered account already has a profile.
        coEvery { getProfileUseCase() } returns
            Profile(userId = USER_ID, displayName = DISPLAY_NAME)
        val viewModel = createViewModel()

        viewModel.uiStateFlow.test {
            assertEquals(expected = UiState.Loading, actual = awaitItem())
            assertTrue(actual = awaitItem().loaded().showSignInDialog)

            // collectScreenState observes the profile flow for the recovered account.
            profileFlow.value = Profile(userId = USER_ID, displayName = DISPLAY_NAME)

            viewModel.onSignInWithSocialMethod(
                displayName = ENTERED_NAME,
                method = SignInMethod.GOOGLE,
            )
            assertEquals(expected = UiState.Loading, actual = awaitItem())

            val data = awaitItem().loaded()
            assertFalse(actual = data.showSignInDialog)
            // The recovered profile's name is shown, not the throwaway name typed in the
            // dialog.
            assertEquals(expected = DISPLAY_NAME, actual = data.displayName)

            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { createProfileForCurrentUserUseCase(displayName = any()) }
    }

    @Test
    fun `Given the build is below the minimum supported version, then the force-upgrade gate is exposed`() = runTest {
        every { checkAppVersionUseCase() } returns AppUpgradeStatus.FORCE_UPGRADE

        val viewModel = createViewModel()
        viewModel.appUpgradeStatusFlow.test {
            assertEquals(expected = AppUpgradeStatus.UP_TO_DATE, actual = awaitItem())
            assertEquals(expected = AppUpgradeStatus.FORCE_UPGRADE, actual = awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given a soft update nudge, when it is dismissed, then the gate clears`() = runTest {
        every { checkAppVersionUseCase() } returns AppUpgradeStatus.UPDATE_AVAILABLE

        val viewModel = createViewModel()
        viewModel.appUpgradeStatusFlow.test {
            assertEquals(expected = AppUpgradeStatus.UP_TO_DATE, actual = awaitItem())
            assertEquals(expected = AppUpgradeStatus.UPDATE_AVAILABLE, actual = awaitItem())

            viewModel.onDismissUpdateNudge()
            assertEquals(expected = AppUpgradeStatus.UP_TO_DATE, actual = awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given a force-upgrade gate, when a dismiss is attempted, then the gate stays put`() = runTest {
        every { checkAppVersionUseCase() } returns AppUpgradeStatus.FORCE_UPGRADE

        val viewModel = createViewModel()
        viewModel.appUpgradeStatusFlow.test {
            assertEquals(expected = AppUpgradeStatus.UP_TO_DATE, actual = awaitItem())
            assertEquals(expected = AppUpgradeStatus.FORCE_UPGRADE, actual = awaitItem())

            viewModel.onDismissUpdateNudge()
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        private const val USER_ID = "user_123"
        private const val DISPLAY_NAME = "Gandalf the Grey"
        private const val ENTERED_NAME = "Radagast"
        private const val RANDOM_NAME = "a mysterious traveler"
        private const val EMAIL = "radagast@example.com"
        private const val PASSWORD = "hunter2!"
    }
}
