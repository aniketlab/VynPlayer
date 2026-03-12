package com.vyn.player.di

import android.content.Context
import androidx.room.Room
import com.vyn.player.data.local.MuzicDatabase
import com.vyn.player.data.local.dao.FavoriteDao
import com.vyn.player.data.local.dao.PlaylistDao
import com.vyn.player.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MuzicDatabase {
        return Room.databaseBuilder(
            context,
            MuzicDatabase::class.java,
            Constants.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun providePlaylistDao(database: MuzicDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun provideFavoriteDao(database: MuzicDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    fun providePlaybackHistoryDao(database: MuzicDatabase): com.vyn.player.data.local.dao.PlaybackHistoryDao {
        return database.playbackHistoryDao()
    }

    @Provides
    fun provideSongDao(database: MuzicDatabase): com.vyn.player.data.local.dao.SongDao {
        return database.songDao()
    }

    @Provides
    fun provideSongStatsDao(database: MuzicDatabase): com.vyn.player.data.local.dao.SongStatsDao {
        return database.songStatsDao()
    }

    @Provides
    fun provideSmartMixDao(database: MuzicDatabase): com.vyn.player.data.local.dao.SmartMixDao {
        return database.smartMixDao()
    }

    @Provides
    fun provideRecentTrackDao(database: MuzicDatabase): com.vyn.player.data.local.dao.RecentTrackDao {
        return database.recentTrackDao()
    }

    @Provides
    fun provideAlbumArtworkDao(database: MuzicDatabase): com.vyn.player.data.local.dao.AlbumArtworkDao {
        return database.albumArtworkDao()
    }
}
