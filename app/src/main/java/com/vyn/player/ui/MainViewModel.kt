package com.vyn.player.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.vyn.player.data.model.Song
import com.vyn.player.player.PlaybackManager
import com.vyn.player.ui.actions.SongAction
import com.vyn.player.ui.actions.SongActionHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val musicRepository: com.vyn.player.data.repository.MusicRepository,
    private val songActionHandler: SongActionHandler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded = _isPlayerExpanded.asStateFlow()

    private val _isQueueScreenVisible = MutableStateFlow(false)
    val isQueueScreenVisible = _isQueueScreenVisible.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite = _isFavorite.asStateFlow()

    val selectedSong = songActionHandler.selectedSong
    val showSongDetails = songActionHandler.showSongDetails

    init {
        observeCurrentSong()
    }

    private fun observeCurrentSong() {
        viewModelScope.launch {
            playbackManager.playbackState.collectLatest { state ->
                state.currentSong?.let { song ->
                    preloadArtwork(song)
                    musicRepository.isFavorite(song.id).collectLatest { isFav ->
                        _isFavorite.value = isFav
                    }
                } ?: run {
                    _isFavorite.value = false
                }
            }
        }
    }

    private fun preloadArtwork(song: Song) {
        if (song.isExternalSource) return

        val request = ImageRequest.Builder(context)
            .data(song.albumArtUri)
            .size(512)
            .build()
        context.imageLoader.enqueue(request)
    }

    fun setPlayerExpanded(expanded: Boolean) {
        if (expanded) {
            playbackState.value.currentSong?.let { preloadArtwork(it) }
        } else {
            _isQueueScreenVisible.value = false
        }
        _isPlayerExpanded.value = expanded
    }

    fun setQueueScreenVisible(visible: Boolean) {
        _isQueueScreenVisible.value = visible
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            playbackManager.playbackState.value.currentSong?.let { song ->
                _isFavorite.value = !_isFavorite.value
                musicRepository.toggleFavorite(song.id)
            }
        }
    }

    fun handleSongAction(action: SongAction) {
        songActionHandler.handle(action)
    }

    fun dismissSongDetails() {
        songActionHandler.dismissSongDetails()
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
        com.vyn.player.player.PlaybackState()
    )

    val playbackProgress = playbackManager.playbackProgress.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.vyn.player.player.PlaybackProgress()
    )

    fun togglePlayPause() = playbackManager.togglePlayPause()
    fun skipToNext() = playbackManager.skipToNext()
    fun skipToPrevious() = playbackManager.skipToPrevious()
    fun playNext(song: com.vyn.player.data.model.Song) = playbackManager.playNext(song)
    fun stopPlayback() = playbackManager.stop()

    fun seekTo(positionMs: Long) = playbackManager.seekTo(positionMs)
    fun cycleRepeatMode() = playbackManager.cycleRepeatMode()
    fun toggleShuffle() = playbackManager.toggleShuffle()
}
