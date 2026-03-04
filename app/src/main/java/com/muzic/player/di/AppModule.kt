package com.muzic.player.di

import android.content.Context
import com.muzic.player.data.repository.MediaStoreScanner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMediaStoreScanner(@ApplicationContext context: Context): MediaStoreScanner {
        return MediaStoreScanner(context)
    }
}
