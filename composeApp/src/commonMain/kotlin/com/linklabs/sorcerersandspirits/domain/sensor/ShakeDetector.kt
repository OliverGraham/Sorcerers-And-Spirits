package com.linklabs.sorcerersandspirits.domain.sensor

import kotlinx.coroutines.flow.Flow

/** Reports when the user physically shakes the device. */
interface ShakeDetector {
    /**
     * Emits once each time a shake is detected. Collectors only receive events while they are
     * actively collecting, so the platform sensor is registered on collection and released when
     * collection stops.
     */
    val shakes: Flow<Unit>
}
