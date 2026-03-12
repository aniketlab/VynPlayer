package com.vyn.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "album_artwork_table")
data class AlbumArtworkEntity(
    @PrimaryKey val albumKey: String, // format: "artist_album" or similar
    val albumName: String,
    val artistName: String,
    val artworkPath: String,
    val lastUpdated: Long = System.currentTimeMillis()
)
