package com.muzic.player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.muzic.player.data.local.dao.FavoriteDao
import com.muzic.player.data.local.dao.PlaylistDao
import com.muzic.player.data.local.entity.FavoriteEntity
import com.muzic.player.data.local.entity.PlaylistEntity
import com.muzic.player.data.local.entity.PlaylistSongCrossRef

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        FavoriteEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class MuzicDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
}
