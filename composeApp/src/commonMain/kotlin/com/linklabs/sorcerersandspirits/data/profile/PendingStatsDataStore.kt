package com.linklabs.sorcerersandspirits.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

const val PENDING_STATS_DATASTORE_FILE = "pending_stats.preferences_pb"

/**
 * Builds the single [Preferences] DataStore that persists the unflushed stats delta. The on-disk
 * location differs per platform, so each platform module supplies [producePath].
 */
fun createPendingStatsDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = {
        producePath().toPath()
    })
