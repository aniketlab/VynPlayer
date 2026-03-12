package com.vyn.player.di

import android.content.Context
import com.vyn.player.player.PlaybackManager
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
        musicRepository: com.vyn.player.data.repository.MusicRepository,
        musicHistoryRepository: com.vyn.player.data.repository.MusicHistoryRepository,
        userPrefs: com.vyn.player.data.preferences.UserPreferencesManager
    ): PlaybackManager {
        return PlaybackManager(context, musicRepository, musicHistoryRepository, userPrefs)
    }
}
