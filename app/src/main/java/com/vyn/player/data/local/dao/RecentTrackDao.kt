package com.vyn.player.data.local.dao

import androidx.room.*
import com.vyn.player.data.local.entity.RecentTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentTrackDao {
    @Query("SELECT * FROM recent_tracks ORDER BY lastPlayedTimestamp DESC LIMIT :limit")
    fun getRecentTracks(limit: Int): Flow<List<RecentTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(track: RecentTrackEntity)

    @Query("SELECT * FROM recent_tracks WHERE songId = :songId")
    suspend fun getRecentTrack(songId: Long): RecentTrackEntity?

    @Query("DELETE FROM recent_tracks WHERE songId NOT IN (SELECT songId FROM recent_tracks ORDER BY lastPlayedTimestamp DESC LIMIT 50)")
    suspend fun trimRecentTracks()
}
