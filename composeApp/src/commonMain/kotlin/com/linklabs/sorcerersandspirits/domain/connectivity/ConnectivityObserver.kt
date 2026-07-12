package com.linklabs.sorcerersandspirits.domain.connectivity

import kotlinx.coroutines.flow.Flow

/** Reports whether the device currently has a usable network connection. */
interface ConnectivityObserver {
    val isOnline: Flow<Boolean>
}
