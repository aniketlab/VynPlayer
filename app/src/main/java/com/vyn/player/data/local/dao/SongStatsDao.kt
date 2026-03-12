package com.vyn.player.data.local.dao

import androidx.room.*
import com.vyn.player.data.local.entity.SongStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongStatsDao {
    @Query("SELECT * FROM song_stats WHERE songId = :songId")
    suspend fun getStatsForSong(songId: Long): SongStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: SongStatsEntity)

    @Query("SELECT * FROM song_stats")
    suspend fun getAllStats(): List<SongStatsEntity>
    
    @Query("SELECT * FROM song_stats")
    fun getAllStatsFlow(): Flow<List<SongStatsEntity>>
}
