package com.linklabs.sorcerersandspirits.data.authentication

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.linklabs.sorcerersandspirits.domain.authentication.SessionRecord
import com.linklabs.sorcerersandspirits.domain.authentication.SessionStore
import kotlinx.coroutines.flow.first
import kotlin.concurrent.Volatile

class DataStoreSessionStore(private val dataStore: DataStore<Preferences>) : SessionStore {

    @Volatile
    private var cachedRecord: SessionRecord? = null

    override fun cached(): SessionRecord? = cachedRecord

    override suspend fun restore(): SessionRecord? {
        val preferences = dataStore.data.first()
        val userId = preferences[USER_ID_KEY]
        val displayName = preferences[DISPLAY_NAME_KEY]

        val record =
            if (userId != null && displayName != null) {
                SessionRecord(userId = userId, displayName = displayName)
            } else {
                null
            }

        cachedRecord = record
        return record
    }

    override suspend fun save(userId: String, displayName: String) {
        val newRecord = SessionRecord(userId = userId, displayName = displayName)
        if (cachedRecord == newRecord) return

        cachedRecord = newRecord
        dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[DISPLAY_NAME_KEY] = displayName
        }
    }

    override suspend fun clear() {
        cachedRecord = null
        dataStore.edit { preferences ->
            preferences.remove(USER_ID_KEY)
            preferences.remove(DISPLAY_NAME_KEY)
        }
    }

    private companion object {
        val USER_ID_KEY = stringPreferencesKey(name = "session_user_id")
        val DISPLAY_NAME_KEY = stringPreferencesKey(name = "session_display_name")
    }
}
