package com.vyn.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vyn.player.data.local.entity.PlaybackHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: PlaybackHistoryEntity): Long

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

    @Query("SELECT COUNT(id) FROM playback_history WHERE songId = :songId AND playedAt >= :timestamp")
    suspend fun getPlayCountSince(songId: Long, timestamp: Long): Int

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()

    @Query("DELETE FROM playback_history WHERE id NOT IN (SELECT id FROM playback_history ORDER BY playedAt DESC LIMIT 200)")
    suspend fun trimHistory()

    @Query("""
        SELECT DISTINCT songId FROM playback_history 
        WHERE strftime('%H', datetime(playedAt/1000, 'unixepoch', 'localtime')) 
        BETWEEN :startHour AND :endHour
    """)
    suspend fun getSongsPlayedInHourRange(startHour: String, endHour: String): List<Long>
}

data class SongPlayCount(
    val songId: Long,
    val playCount: Int
)
