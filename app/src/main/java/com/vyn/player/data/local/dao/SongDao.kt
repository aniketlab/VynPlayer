package com.vyn.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vyn.player.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM cached_songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM cached_songs WHERE id = :songId")
    suspend fun getSongById(songId: Long): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Query("DELETE FROM cached_songs")
    suspend fun clearCache()

    @Query("SELECT COUNT(*) FROM cached_songs")
    suspend fun getCount(): Int

    @Query("UPDATE cached_songs SET artist = :newArtist, album = :newAlbum, artworkUrl = :newArtworkUrl, artistImageUrl = :newArtistImageUrl WHERE id = :songId")
    suspend fun updateSongMetadata(songId: Long, newArtist: String, newAlbum: String, newArtworkUrl: String?, newArtistImageUrl: String?)

    @Query("SELECT MAX(dateModified) FROM cached_songs")
    suspend fun getMaxDateModified(): Long?
}
