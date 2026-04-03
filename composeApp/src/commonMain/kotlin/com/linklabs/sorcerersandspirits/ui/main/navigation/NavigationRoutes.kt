package com.linklabs.sorcerersandspirits.ui.main.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface Route : NavKey {
    @Serializable data object TavernScreen : Route

    @Serializable data class ArenaScreen(val arenaId: String) : Route
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
