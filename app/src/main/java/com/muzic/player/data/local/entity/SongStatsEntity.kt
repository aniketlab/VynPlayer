package com.muzic.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_stats")
data class SongStatsEntity(
    @PrimaryKey
    val songId: Long,
    val playCount: Int = 0,
    val skipCount: Int = 0,
    val completionRate: Float = 0f, // 0.0 to 1.0
    val lastPlayedTimestamp: Long = 0L,
    val last7DaysPlayCount: Int = 0 // For obsession detection
)
