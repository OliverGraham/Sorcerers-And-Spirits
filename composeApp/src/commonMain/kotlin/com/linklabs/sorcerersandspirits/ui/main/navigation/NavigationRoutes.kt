package com.linklabs.sorcerersandspirits.ui.main.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaScreenViewModelArguments
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface Route : NavKey {
    @Serializable data object TavernScreen : Route

    @Serializable
    data class ArenaScreen(val arguments: ArenaScreenViewModelArgumentsSerializable) : Route
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
        }
    }
}

@Serializable
data class ArenaScreenViewModelArgumentsSerializable(
    val arenaId: String,
    val expectedNumberOfPlayers: Int,
)

internal fun ArenaScreenViewModelArgumentsSerializable.toDomain() =
    ArenaScreenViewModelArguments(
        arenaId = arenaId,
        expectedNumberOfPlayers = expectedNumberOfPlayers,
    )

fun ArenaScreenViewModelArguments.toSerializable() =
    ArenaScreenViewModelArgumentsSerializable(
        arenaId = arenaId,
        expectedNumberOfPlayers = expectedNumberOfPlayers,
    )
