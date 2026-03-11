package com.muzic.player.data.preferences

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

    companion object {
        val AVATAR_URL_KEY = stringPreferencesKey("avatar_url")
        val DISPLAY_NAME_KEY = stringPreferencesKey("display_name")
        val SUBTITLE_KEY = stringPreferencesKey("subtitle")
        val LAST_PLAYED_SONG_ID = androidx.datastore.preferences.core.longPreferencesKey("last_played_song_id")
        val LAST_PLAYBACK_POSITION = androidx.datastore.preferences.core.longPreferencesKey("last_playback_position")
        val LAST_QUEUE_IDS = stringPreferencesKey("last_queue_ids")
        val LAST_QUEUE_INDEX = androidx.datastore.preferences.core.intPreferencesKey("last_queue_index")
        val LAST_SHUFFLE_STATE = androidx.datastore.preferences.core.booleanPreferencesKey("last_shuffle_state")
        val LAST_REPEAT_MODE = androidx.datastore.preferences.core.intPreferencesKey("last_repeat_mode")
        val THEME_MODE_KEY = androidx.datastore.preferences.core.intPreferencesKey("theme_mode")
        val ARTWORK_DOWNLOAD_MODE = intPreferencesKey("artwork_download_mode")
    }

    val themeMode: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE_KEY] ?: 0 // 0 = System, 1 = Light, 2 = Dark
    }

    suspend fun saveThemeMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode
        }
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

    val lastPlayedSongId: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[LAST_PLAYED_SONG_ID]
    }

    val lastPlaybackPosition: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LAST_PLAYBACK_POSITION] ?: 0L
    }

    val lastQueueIds: Flow<List<Long>> = context.dataStore.data.map { preferences ->
        preferences[LAST_QUEUE_IDS]?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
    }

    val lastQueueIndex: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[LAST_QUEUE_INDEX] ?: -1
    }

    val lastShuffleState: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LAST_SHUFFLE_STATE] ?: false
    }

    val lastRepeatMode: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[LAST_REPEAT_MODE] ?: 0
    }

    suspend fun savePlaybackSession(
        songId: Long,
        positionMs: Long,
        queueIds: List<Long>,
        index: Int,
        isShuffle: Boolean,
        repeatMode: Int
    ) {
        context.dataStore.edit { preferences ->
            preferences[LAST_PLAYED_SONG_ID] = songId
            preferences[LAST_PLAYBACK_POSITION] = positionMs
            preferences[LAST_QUEUE_IDS] = queueIds.joinToString(",")
            preferences[LAST_QUEUE_INDEX] = index
            preferences[LAST_SHUFFLE_STATE] = isShuffle
            preferences[LAST_REPEAT_MODE] = repeatMode
        }
    }

    // ─── Artwork Download Setting ───
    // 0 = Off, 1 = WiFi only, 2 = WiFi + Mobile Data
    val artworkDownloadMode: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[ARTWORK_DOWNLOAD_MODE] ?: 2 // default: WiFi + Mobile Data
    }

    suspend fun saveArtworkDownloadMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[ARTWORK_DOWNLOAD_MODE] = mode
        }
    }

    fun isArtworkDownloadAllowed(): Boolean {
        // No ConnectivityManager gating — let OkHttp handle network errors naturally
        return true
    }
}
