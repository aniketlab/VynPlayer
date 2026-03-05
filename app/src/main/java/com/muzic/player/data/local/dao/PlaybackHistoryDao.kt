package com.muzic.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.muzic.player.data.local.entity.PlaybackHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: PlaybackHistoryEntity)

    // Gets the most recently played distinct song ids
    @Query("""
        SELECT songId FROM playback_history 
        GROUP BY songId 
        ORDER BY MAX(playedAt) DESC 
        LIMIT :limit
    """)
    fun getRecentSongs(limit: Int): Flow<List<Long>>

    @Query("""
        SELECT songId, COUNT(songId) as playCount 
        FROM playback_history 
        GROUP BY songId 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    fun getTopSongs(limit: Int): Flow<List<SongPlayCount>>

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}

data class SongPlayCount(
    val songId: Long,
    val playCount: Int
)
