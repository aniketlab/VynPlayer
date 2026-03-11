package com.muzic.player.data.repository

import com.muzic.player.data.local.dao.PlaybackHistoryDao
import com.muzic.player.data.local.dao.RecentTrackDao
import com.muzic.player.data.local.dao.SongStatsDao
import com.muzic.player.data.local.entity.PlaybackHistoryEntity
import com.muzic.player.data.local.entity.RecentTrackEntity
import com.muzic.player.data.local.entity.SongStatsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicHistoryRepository @Inject constructor(
    private val songStatsDao: SongStatsDao,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val recentTrackDao: RecentTrackDao
) {
    /**
     * Called when a song starts playing.
     * Updates lastPlayedTimestamp and inserts a new PlayHistory record.
     */
    suspend fun onSongStarted(songId: Long): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        
        // 1. Update lastPlayedTimestamp in SongStats
        val currentStats = songStatsDao.getStatsForSong(songId) ?: SongStatsEntity(songId = songId)
        songStatsDao.insertOrUpdate(currentStats.copy(lastPlayedTimestamp = now))
        
        // 2. Insert into Playback History
        val historyId = playbackHistoryDao.insert(
            PlaybackHistoryEntity(
                songId = songId,
                playedAt = now,
                playedDuration = 0L
            )
        )
        
        // 3. Trim history
        playbackHistoryDao.trimHistory()
        
        // 4. Update Recent Track entry for "Jump Back In"
        recentTrackDao.insertOrUpdate(
            RecentTrackEntity(
                songId = songId,
                lastPlayedTimestamp = now,
                lastPlaybackPosition = 0L
            )
        )
        recentTrackDao.trimRecentTracks()
        
        historyId
    }

    /**
     * Called periodically during playback to track position.
     */
    suspend fun updatePlaybackPosition(songId: Long, position: Long) = withContext(Dispatchers.IO) {
        val existing = recentTrackDao.getRecentTrack(songId)
        if (existing != null) {
            recentTrackDao.insertOrUpdate(existing.copy(lastPlaybackPosition = position))
        }
    }

    /**
     * Called when a song finishes or the user skips.
     * Updates the final duration in history and updates aggregate stats.
     */
    suspend fun onSongFinished(
        songId: Long,
        historyId: Long,
        playedDurationMs: Long,
        wasSkipped: Boolean,
        completionPercentage: Float
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        
        // 1. Update PlayHistory with final duration
        // We'll need a way to update by ID. Let's assume we can re-insert with the same ID or add an update method.
        playbackHistoryDao.insert(
            PlaybackHistoryEntity(
                id = historyId,
                songId = songId,
                playedAt = now, // Or keep original
                playedDuration = playedDurationMs
            )
        )

        // 2. Update SongStats
        val stats = songStatsDao.getStatsForSong(songId) ?: SongStatsEntity(songId = songId)
        
        var newPlayCount = stats.playCount
        var newSkipCount = stats.skipCount
        
        // Rule: playCount increases if song plays for more than 20 seconds
        if (playedDurationMs > 20000) {
            newPlayCount++
        }
        
        // Rule: skipCount increases if user skips before 30% completion
        if (wasSkipped && completionPercentage < 0.3f) {
            newSkipCount++
        }
        
        // Update completion rate average
        val newCompletionRate = if (stats.playCount == 0) {
            completionPercentage
        } else {
            (stats.completionRate * stats.playCount + completionPercentage) / (stats.playCount + 1)
        }
        
        // Update obsession count (7 days)
        val last7DaysCount = playbackHistoryDao.getPlayCountSince(songId, now - 7 * 24 * 60 * 60 * 1000L)

        songStatsDao.insertOrUpdate(
            stats.copy(
                playCount = newPlayCount,
                skipCount = newSkipCount,
                completionRate = newCompletionRate,
                last7DaysPlayCount = last7DaysCount
            )
        )
    }

    fun getRecentTrackIds(limit: Int = 50) = playbackHistoryDao.getRecentSongs(limit)
    
    fun getTopTrackStats(limit: Int = 50) = playbackHistoryDao.getTopSongs(limit)
}
