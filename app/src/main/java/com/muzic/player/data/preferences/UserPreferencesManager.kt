package com.muzic.player.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val AVATAR_URL_KEY = stringPreferencesKey("avatar_url")
        val DISPLAY_NAME_KEY = stringPreferencesKey("display_name")
        val SUBTITLE_KEY = stringPreferencesKey("subtitle")
    }

    val userAvatarUrl: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[AVATAR_URL_KEY]
    }

    val userDisplayName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DISPLAY_NAME_KEY] ?: "Music Lover"
    }

    val userSubtitle: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SUBTITLE_KEY] ?: "Music Enthusiast"
    }

    suspend fun saveUserProfile(avatarUrl: String?, displayName: String, subtitle: String) {
        context.dataStore.edit { preferences ->
            if (avatarUrl != null) {
                preferences[AVATAR_URL_KEY] = avatarUrl
            } else {
                preferences.remove(AVATAR_URL_KEY)
            }
            preferences[DISPLAY_NAME_KEY] = displayName
            preferences[SUBTITLE_KEY] = subtitle
        }
    }
}
