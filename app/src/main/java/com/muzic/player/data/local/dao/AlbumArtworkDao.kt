package com.muzic.player.data.local.dao

import androidx.room.*
import com.muzic.player.data.local.entity.AlbumArtworkEntity

@Dao
interface AlbumArtworkDao {
    @Query("SELECT * FROM album_artwork_table WHERE albumKey = :key")
    suspend fun getArtwork(key: String): AlbumArtworkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtwork(artwork: AlbumArtworkEntity)

    @Query("DELETE FROM album_artwork_table")
    suspend fun clear()
}
