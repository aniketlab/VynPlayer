package com.muzic.player.di

import android.content.Context
import androidx.room.Room
import com.muzic.player.data.local.MuzicDatabase
import com.muzic.player.data.local.dao.FavoriteDao
import com.muzic.player.data.local.dao.PlaylistDao
import com.muzic.player.util.Constants
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
        ).build()
    }

    @Provides
    fun providePlaylistDao(database: MuzicDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun provideFavoriteDao(database: MuzicDatabase): FavoriteDao {
        return database.favoriteDao()
    }
}
