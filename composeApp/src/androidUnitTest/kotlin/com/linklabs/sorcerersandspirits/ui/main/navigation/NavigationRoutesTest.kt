package com.linklabs.sorcerersandspirits.ui.main.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationRoutesTest {

    private val arenaScreen =
        Route.ArenaScreen(
            arguments =
                ArenaScreenViewModelArgumentsSerializable(
                    arenaId = "arena-1",
                    expectedNumberOfPlayers = 2,
                )
        )

    @Test
    fun `Tavern and Arena are home screens`() {
        assertTrue(Route.TavernScreen.isHomeScreen())
        assertTrue(arenaScreen.isHomeScreen())
    }

    @Test
    fun `Profile, Leaderboard, Achievements and Game Rules are not home screens`() {
        assertFalse(Route.ProfileScreen.isHomeScreen())
        assertFalse(Route.LeaderboardScreen.isHomeScreen())
        assertFalse(Route.AchievementsScreen.isHomeScreen())
        assertFalse(Route.GameRulesScreen.isHomeScreen())
    }

    @Test
    fun `popToNearestHomeScreen trims a non-home screen back to the Tavern`() {
        val backStack: NavBackStack<NavKey> = NavBackStack(Route.TavernScreen, Route.ProfileScreen)

        backStack.popToNearestHomeScreen()

        assertEquals(listOf(Route.TavernScreen), backStack.toList())
    }

    @Test
    fun `popToNearestHomeScreen keeps the Arena as the back target during a match`() {
        val backStack: NavBackStack<NavKey> =
            NavBackStack(Route.TavernScreen, arenaScreen, Route.ProfileScreen)

        backStack.popToNearestHomeScreen()

        assertEquals(listOf(Route.TavernScreen, arenaScreen), backStack.toList())
    }

    @Test
    fun `popToNearestHomeScreen is a no-op when already on a home screen`() {
        val backStack: NavBackStack<NavKey> = NavBackStack(Route.TavernScreen, arenaScreen)

        backStack.popToNearestHomeScreen()

        assertEquals(listOf(Route.TavernScreen, arenaScreen), backStack.toList())
    }

    @Test
    fun `popToNearestHomeScreen never empties the stack`() {
        val backStack: NavBackStack<NavKey> = NavBackStack(Route.TavernScreen)

        backStack.popToNearestHomeScreen()

        assertEquals(listOf(Route.TavernScreen), backStack.toList())
    }
}
