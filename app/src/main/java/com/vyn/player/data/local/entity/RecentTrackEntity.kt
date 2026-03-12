package com.vyn.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_tracks")
data class RecentTrackEntity(
    @PrimaryKey
    val songId: Long,
    val lastPlayedTimestamp: Long = System.currentTimeMillis(),
    val lastPlaybackPosition: Long = 0L
)
