package com.muzic.player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.muzic.player.data.local.dao.FavoriteDao
import com.muzic.player.data.local.dao.PlaylistDao
import com.muzic.player.data.local.entity.FavoriteEntity
import com.muzic.player.data.local.entity.PlaylistEntity
import com.muzic.player.data.local.entity.PlaylistSongCrossRef
import com.muzic.player.data.local.entity.PlaybackHistoryEntity
import com.muzic.player.data.local.dao.PlaybackHistoryDao
import com.muzic.player.data.local.dao.SongDao
import com.muzic.player.data.local.entity.SongEntity

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        FavoriteEntity::class,
        PlaybackHistoryEntity::class,
        SongEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class MuzicDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun songDao(): SongDao
}
