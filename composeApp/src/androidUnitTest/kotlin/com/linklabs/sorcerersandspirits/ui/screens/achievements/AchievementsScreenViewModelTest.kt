package com.linklabs.sorcerersandspirits.ui.screens.achievements

import app.cash.turbine.test
import com.linklabs.sorcerersandspirits.domain.achievement.AchievementCatalog
import com.linklabs.sorcerersandspirits.domain.achievement.AchievementId
import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetProfileFlowUseCase
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AchievementsScreenViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getProfileFlowUseCase: GetProfileFlowUseCase = mockk()
    private val profileFlow = MutableStateFlow<Profile?>(null)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getProfileFlowUseCase() } returns profileFlow
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Given the profile emits, then every catalog achievement is listed with its unlock state`() =
        runTest {
            val viewModel =
                AchievementsScreenViewModel(getProfileFlowUseCase = getProfileFlowUseCase)

            viewModel.uiStateFlow.test {
                assertEquals(expected = UiState.Loading, actual = awaitItem())

                profileFlow.value =
                    Profile(
                        userId = "user-1",
                        displayName = "Gandalf",
                        unlockedAchievements = mapOf(AchievementId.WinAMatch to 1L),
                    )

                val state = assertIs<UiState.Loaded<AchievementsState>>(value = awaitItem()).data
                assertEquals(expected = AchievementCatalog.all.size, actual = state.totalCount)
                assertEquals(expected = 1, actual = state.unlockedCount)
                assertEquals(
                    expected = setOf(AchievementId.WinAMatch),
                    actual = state.achievements.filter { it.isUnlocked }.map { it.id }.toSet(),
                )

                cancelAndIgnoreRemainingEvents()
            }
        }
}
