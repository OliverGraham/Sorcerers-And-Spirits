package com.linklabs.sorcerersandspirits.domain.connectivity

import kotlinx.coroutines.flow.Flow

/** Reports whether the device currently has a usable network connection. */
interface ConnectivityObserver {
    /** Emits the current connectivity state and every subsequent change. */
    val isOnline: Flow<Boolean>
}
