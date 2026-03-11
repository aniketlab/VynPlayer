package com.muzic.player.ui.screens.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muzic.player.data.repository.MusicRepository
import com.muzic.player.player.PlaybackManager
import com.muzic.player.player.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    val playbackManager: PlaybackManager,
    private val musicRepository: MusicRepository
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState

    val currentPosition: StateFlow<Long> = playbackManager.playbackState
        .map { it.currentPosition }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    init {
        observeCurrentSong()
    }

    private fun observeCurrentSong() {
        viewModelScope.launch {
            playbackManager.playbackState.collect { state ->
                state.currentSong?.let { song ->
                    musicRepository.isFavorite(song.id).collect { isFav ->
                        _isFavorite.value = isFav
                    }
                }
            }
        }
    }

    fun togglePlayPause() = playbackManager.togglePlayPause()
    fun skipToNext() = playbackManager.skipToNext()
    fun skipToPrevious() = playbackManager.skipToPrevious()
    fun seekTo(positionMs: Long) = playbackManager.seekTo(positionMs)
    fun cycleRepeatMode() = playbackManager.cycleRepeatMode()
    fun toggleShuffle() = playbackManager.toggleShuffle()

    fun toggleFavorite() {
        viewModelScope.launch {
            playbackState.value.currentSong?.let { song ->
                musicRepository.toggleFavorite(song.id)
            }
        }
    }
}
