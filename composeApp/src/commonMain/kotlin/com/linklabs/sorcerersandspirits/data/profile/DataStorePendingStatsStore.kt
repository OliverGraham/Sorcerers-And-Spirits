package com.linklabs.sorcerersandspirits.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.linklabs.sorcerersandspirits.domain.profile.PendingStatsStore
import com.linklabs.sorcerersandspirits.domain.profile.ProfileStats
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Persists the unflushed stats delta as a single JSON blob in a [Preferences] DataStore, reusing
 * the Firestore [ProfileStatsEntity] mapping so the on-disk shape stays in step with the remote
 * schema. DataStore serializes writes, so the frequent in-match [save]s and the final [clear] do
 * not race.
 */
class DataStorePendingStatsStore(private val dataStore: DataStore<Preferences>) :
    PendingStatsStore {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun load(): ProfileStats {
        val storedDelta = dataStore.data.map { it[PENDING_DELTA_KEY] }.first()
        if (storedDelta == null) return ProfileStats()

        return json.decodeFromString<ProfileStatsEntity>(storedDelta).toDomain()
    }

    override suspend fun save(delta: ProfileStats) {
        val serializedDelta = json.encodeToString(delta.toEntity())
        dataStore.edit { preferences -> preferences[PENDING_DELTA_KEY] = serializedDelta }
    }

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.remove(PENDING_DELTA_KEY) }
    }

    private companion object {
        val PENDING_DELTA_KEY = stringPreferencesKey(name = "pending_stats_delta")
    }
}
