package com.muzic.player.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.muzic.player.data.model.Song
import com.muzic.player.player.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val musicRepository: com.muzic.player.data.repository.MusicRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded = _isPlayerExpanded.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite = _isFavorite.asStateFlow()

    init {
        observeCurrentSong()
    }

    private fun observeCurrentSong() {
        viewModelScope.launch {
            playbackManager.playbackState.collect { state ->
                state.currentSong?.let { song ->
                    preloadArtwork(song)
                    musicRepository.isFavorite(song.id).collect { isFav ->
                        _isFavorite.value = isFav
                    }
                }
            }
        }
    }

    private fun preloadArtwork(song: Song) {
        val request = ImageRequest.Builder(context)
            .data(song.albumArtUri)
            .size(512)
            .build()
        context.imageLoader.enqueue(request)
    }

    fun setPlayerExpanded(expanded: Boolean) {
        if (expanded) {
            playbackState.value.currentSong?.let { preloadArtwork(it) }
        }
        _isPlayerExpanded.value = expanded
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            playbackManager.playbackState.value.currentSong?.let { song ->
                musicRepository.toggleFavorite(song.id)
            }
        }
    }

    private val _scrollToTopRequest = MutableSharedFlow<String>()
    val scrollToTopRequest = _scrollToTopRequest.asSharedFlow()

    fun requestScrollToTop(route: String) {
        viewModelScope.launch {
            _scrollToTopRequest.emit(route)
        }
    }

    val playbackState = playbackManager.playbackState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.muzic.player.player.PlaybackState()
    )

    val playbackProgress = playbackManager.playbackProgress.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.muzic.player.player.PlaybackProgress()
    )

    fun togglePlayPause() = playbackManager.togglePlayPause()
    fun skipToNext() = playbackManager.skipToNext()
    fun skipToPrevious() = playbackManager.skipToPrevious()
    fun playNext(song: com.muzic.player.data.model.Song) = playbackManager.playNext(song)
    fun stopPlayback() = playbackManager.stop()

    fun seekTo(positionMs: Long) = playbackManager.seekTo(positionMs)
    fun cycleRepeatMode() = playbackManager.cycleRepeatMode()
    fun toggleShuffle() = playbackManager.toggleShuffle()
}
