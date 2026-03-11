package com.muzic.player.di

import android.content.Context
import com.muzic.player.player.PlaybackManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun providePlaybackManager(
        @ApplicationContext context: Context,
        musicRepository: com.muzic.player.data.repository.MusicRepository,
        musicHistoryRepository: com.muzic.player.data.repository.MusicHistoryRepository,
        userPrefs: com.muzic.player.data.preferences.UserPreferencesManager
    ): PlaybackManager {
        return PlaybackManager(context, musicRepository, musicHistoryRepository, userPrefs)
    }
}
