package com.muzic.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_songs",
    indices = [
        androidx.room.Index(value = ["album"]),
        androidx.room.Index(value = ["artist"]),
        androidx.room.Index(value = ["dateAdded"])
    ]
)
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val path: String,
    val uriString: String,
    val trackNumber: Int,
    val year: Int,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val mimeType: String,
    val folderName: String,
    val folderPath: String,
    val artworkUrl: String? = null,
    val artistImageUrl: String? = null
)
