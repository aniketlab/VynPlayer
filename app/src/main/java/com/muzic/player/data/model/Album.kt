package com.muzic.player.data.model

import android.net.Uri

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val songCount: Int,
    val year: Int = 0
) {
    val albumArtUri: Uri
        get() = Uri.parse("content://media/external/audio/albumart/$id")
}
