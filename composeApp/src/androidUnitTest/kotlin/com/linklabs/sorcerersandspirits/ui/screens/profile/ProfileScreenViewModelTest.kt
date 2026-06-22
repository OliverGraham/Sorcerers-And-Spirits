package com.linklabs.sorcerersandspirits.ui.screens.profile

import app.cash.turbine.test
import com.linklabs.sorcerersandspirits.domain.achievement.AchievementId
import com.linklabs.sorcerersandspirits.domain.authentication.LinkAccountResult
import com.linklabs.sorcerersandspirits.domain.authentication.usecase.LinkAccountUseCase
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.profile.DisplayNameResult
import com.linklabs.sorcerersandspirits.domain.profile.GameStats
import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.profile.ProfileStats
import com.linklabs.sorcerersandspirits.domain.profile.usecase.CreateProfileUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.DeleteProfileUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetProfileFlowUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetRandomTravelerNameUseCase
import com.linklabs.sorcerersandspirits.domain.profile.usecase.UpdateProfileDisplayNameUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileScreenViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getProfileFlowUseCase: GetProfileFlowUseCase = mockk()
    private val updateProfileDisplayNameUseCase: UpdateProfileDisplayNameUseCase =
        mockk(relaxed = true)
    private val deleteProfileUseCase: DeleteProfileUseCase = mockk(relaxed = true)
    private val createProfileUseCase: CreateProfileUseCase = mockk(relaxed = true)
    private val getRandomTravelerNameUseCase: GetRandomTravelerNameUseCase = mockk()
    private val linkAccountUseCase: LinkAccountUseCase = mockk(relaxed = true)

    private val profileFlow = MutableStateFlow<Profile?>(null)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getProfileFlowUseCase() } returns profileFlow
        every { getRandomTravelerNameUseCase() } returns RANDOM_NAME
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        ProfileScreenViewModel(
            getProfileFlowUseCase = getProfileFlowUseCase,
            updateProfileDisplayNameUseCase = updateProfileDisplayNameUseCase,
            deleteProfileUseCase = deleteProfileUseCase,
            createProfileUseCase = createProfileUseCase,
            getRandomTravelerNameUseCase = getRandomTravelerNameUseCase,
            linkAccountUseCase = linkAccountUseCase,
        )

    private fun UiState<ProfileState>.loaded(): ProfileState =
        assertIs<UiState.Loaded<ProfileState>>(value = this).data

    @Test
    fun `Given the profile flow emits, then the screen shows the profile's display name`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.uiStateFlow.test {
                assertEquals(expected = UiState.Loading, actual = awaitItem())

                profileFlow.value = Profile(userId = USER_ID, displayName = DISPLAY_NAME)
                assertEquals(expected = DISPLAY_NAME, actual = awaitItem().loaded().displayName)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given the profile has stats, then the state exposes rating, achievement progress and per-class records`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.uiStateFlow.test {
                assertEquals(expected = UiState.Loading, actual = awaitItem())

                profileFlow.value =
                    Profile(
                        userId = USER_ID,
                        displayName = DISPLAY_NAME,
                        stats =
                            ProfileStats(
                                overall = GameStats(matchesWon = 3, gauntletsWon = 1),
                                perClass =
                                    mapOf(
                                        CharacterClassType.Mage to
                                            GameStats(matchesWon = 3, gauntletsWon = 1)
                                    ),
                            ),
                        unlockedAchievements = mapOf(AchievementId.WinAMatch to 1L),
                    )

                val state = awaitItem().loaded()
                assertEquals(expected = 175L, actual = state.rating)
                assertEquals(expected = 1, actual = state.achievementsUnlocked)
                assertEquals(expected = 4, actual = state.achievementsTotal)
                assertEquals(
                    expected = listOf(CharacterClassType.Mage),
                    actual = state.perClass.map { it.classType },
                )

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `When the display name is saved, then the update use case is invoked`() = runTest {
        val viewModel = createViewModel()

        viewModel.onUpdateDisplayName(newDisplayName = NEW_NAME)
        runCurrent()

        coVerify(exactly = 1) { updateProfileDisplayNameUseCase(displayName = NEW_NAME) }
    }

    @Test
    fun `When the display name update finishes, then its result is emitted as an event`() =
        runTest {
            coEvery { updateProfileDisplayNameUseCase(displayName = NEW_NAME) } returns
                DisplayNameResult.NAME_TAKEN
            val viewModel = createViewModel()

            viewModel.eventChannelFlow.test {
                viewModel.onUpdateDisplayName(newDisplayName = NEW_NAME)
                assertEquals(
                    expected =
                        ProfileEvent.DisplayNameUpdated(result = DisplayNameResult.NAME_TAKEN),
                    actual = awaitItem(),
                )

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `When the profile is deleted, then a fresh one is provisioned and the deleted event is emitted`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.eventChannelFlow.test {
                profileFlow.value = Profile(userId = USER_ID, displayName = DISPLAY_NAME)

                viewModel.onDeleteProfile()
                assertEquals(expected = ProfileEvent.ProfileDeleted, actual = awaitItem())

                cancelAndIgnoreRemainingEvents()
            }

            coVerify(exactly = 1) { deleteProfileUseCase() }
            coVerify(exactly = 1) { createProfileUseCase(displayName = RANDOM_NAME) }
        }

    @Test
    fun `When linking the account succeeds, then the link result is published`() = runTest {
        coEvery { linkAccountUseCase.withEmail(email = EMAIL, password = PASSWORD) } returns
            LinkAccountResult.SUCCESS
        val viewModel = createViewModel()

        viewModel.linkResultFlow.test {
            assertEquals(expected = null, actual = awaitItem())

            viewModel.onLinkWithEmail(email = EMAIL, password = PASSWORD)
            assertEquals(expected = LinkAccountResult.SUCCESS, actual = awaitItem())

            cancelAndIgnoreRemainingEvents()
        }

        // Linking upgrades the existing account in place — no new profile is created.
        coVerify(exactly = 0) { createProfileUseCase(displayName = any()) }
    }

    private companion object {
        private const val USER_ID = "user-1"
        private const val DISPLAY_NAME = "Gandalf"
        private const val NEW_NAME = "Mithrandir"
        private const val RANDOM_NAME = "a mysterious traveler"
        private const val EMAIL = "wizard@example.com"
        private const val PASSWORD = "you-shall-pass"
    }
}
