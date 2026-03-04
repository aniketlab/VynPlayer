package com.muzic.player.util

object Constants {
    // Notification
    const val NOTIFICATION_CHANNEL_ID = "muzic_playback_channel"
    const val NOTIFICATION_ID = 1001

    // Database
    const val DATABASE_NAME = "muzic_database"

    // Preferences
    const val PREFERENCES_NAME = "muzic_preferences"
    const val PREF_REPEAT_MODE = "repeat_mode"
    const val PREF_SHUFFLE_ENABLED = "shuffle_enabled"
    const val PREF_LAST_PLAYED_SONG_ID = "last_played_song_id"
    const val PREF_LAST_POSITION = "last_position"

    // Supported formats
    val SUPPORTED_AUDIO_EXTENSIONS = setOf(
        "mp3", "flac", "wav", "aac", "ogg", "m4a", "opus", "alac"
    )

    // Scan batch size
    const val SCAN_BATCH_SIZE = 500
}
