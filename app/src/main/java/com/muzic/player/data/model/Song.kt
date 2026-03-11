package com.muzic.player.data.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val path: String,
    val uri: Uri,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val size: Long = 0,
    val dateAdded: Long = 0,
    val dateModified: Long = 0,
    val mimeType: String = "",
    val folderName: String = "",
    val folderPath: String = "",
    val isFavorite: Boolean = false,
    val artworkUrl: String? = null,
    val artistImageUrl: String? = null
) {
    val albumArtUri: Uri
        get() = Uri.parse("content://media/external/audio/albumart/$albumId")
}
