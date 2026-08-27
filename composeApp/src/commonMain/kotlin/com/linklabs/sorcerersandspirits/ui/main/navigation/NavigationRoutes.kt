package com.linklabs.sorcerersandspirits.ui.main.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.linklabs.sorcerersandspirits.domain.computer.ComputerOpponent
import com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel.ArenaScreenViewModelArguments
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface Route : NavKey {
    @Serializable data object TavernScreen : Route

    @Serializable
    data class ArenaScreen(val arguments: ArenaScreenViewModelArgumentsSerializable) : Route

    @Serializable data object ProfileScreen : Route

    @Serializable data object LeaderboardScreen : Route

    @Serializable data object AchievementsScreen : Route

    @Serializable data object GameRulesScreen : Route
}

/** Necessary for KMP and to work with iOS */
internal val navigationRoutesConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(
                subclass = Route.TavernScreen::class,
                serializer = Route.TavernScreen.serializer(),
            )
            subclass(
                subclass = Route.ArenaScreen::class,
                serializer = Route.ArenaScreen.serializer(),
            )
            subclass(
                subclass = Route.ProfileScreen::class,
                serializer = Route.ProfileScreen.serializer(),
            )
            subclass(
                subclass = Route.LeaderboardScreen::class,
                serializer = Route.LeaderboardScreen.serializer(),
            )
            subclass(
                subclass = Route.AchievementsScreen::class,
                serializer = Route.AchievementsScreen.serializer(),
            )
            subclass(
                subclass = Route.GameRulesScreen::class,
                serializer = Route.GameRulesScreen.serializer(),
            )
        }
    }
}

internal fun NavKey.isHomeScreen(): Boolean =
    this is Route.TavernScreen || this is Route.ArenaScreen

internal fun NavBackStack<NavKey>.popToNearestHomeScreen() {
    while (size > 1 && !last().isHomeScreen()) {
        removeLastOrNull()
    }
}

internal fun NavBackStack<NavKey>.popToTavern() {
    while (size > 1 && last() !is Route.TavernScreen) {
        removeLastOrNull()
    }
}

@Serializable
data class ArenaScreenViewModelArgumentsSerializable(
    val arenaId: String,
    val expectedNumberOfPlayers: Int,
    val computerOpponents: List<ComputerOpponentSerializable> = emptyList(),
)

@Serializable data class ComputerOpponentSerializable(val userId: String, val displayName: String)

internal fun ArenaScreenViewModelArgumentsSerializable.toDomain() = ArenaScreenViewModelArguments(
    arenaId = arenaId,
    expectedNumberOfPlayers = expectedNumberOfPlayers,
    computerOpponents =
    computerOpponents.map { opponent ->
        ComputerOpponent(userId = opponent.userId, displayName = opponent.displayName)
    },
)

fun ArenaScreenViewModelArguments.toSerializable() = ArenaScreenViewModelArgumentsSerializable(
    arenaId = arenaId,
    expectedNumberOfPlayers = expectedNumberOfPlayers,
    computerOpponents =
    computerOpponents.map { opponent ->
        ComputerOpponentSerializable(
            userId = opponent.userId,
            displayName = opponent.displayName,
        )
    },
)
