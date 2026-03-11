package com.muzic.player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.muzic.player.data.local.dao.FavoriteDao
import com.muzic.player.data.local.dao.PlaylistDao
import com.muzic.player.data.local.dao.PlaybackHistoryDao
import com.muzic.player.data.local.dao.SongDao
import com.muzic.player.data.local.dao.SongStatsDao
import com.muzic.player.data.local.dao.SmartMixDao
import com.muzic.player.data.local.dao.RecentTrackDao
import com.muzic.player.data.local.dao.AlbumArtworkDao
import com.muzic.player.data.local.entity.*
import androidx.room.TypeConverters

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        FavoriteEntity::class,
        PlaybackHistoryEntity::class,
        SongEntity::class,
        SongStatsEntity::class,
        SmartMixEntity::class,
        RecentTrackEntity::class,
        AlbumArtworkEntity::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MuzicDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun songDao(): SongDao
    abstract fun songStatsDao(): SongStatsDao
    abstract fun smartMixDao(): SmartMixDao
    abstract fun recentTrackDao(): RecentTrackDao
    abstract fun albumArtworkDao(): AlbumArtworkDao
}
