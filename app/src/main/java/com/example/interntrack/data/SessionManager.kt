package com.example.interntrack.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

class SessionManager(private val context: Context) {
    companion object {
        private val USER_UID_KEY = stringPreferencesKey("user_uid")
    }

    val userId: Flow<String?> = context.dataStore.data.map { preferences ->
        try {
            val id = preferences[USER_UID_KEY]
            if (id == null || id == "") null else id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveSession(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_UID_KEY] = userId
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences[USER_UID_KEY] = ""
        }
    }
}
