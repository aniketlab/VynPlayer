package com.vyn.player.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.vyn.player.data.model.Song
import com.vyn.player.player.PlaybackManager
import com.vyn.player.player.PlaybackState
import com.vyn.player.ui.actions.SongAction
import com.vyn.player.ui.actions.SongActionHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    val playbackState = playbackManager.playbackState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PlaybackState()
    )

    init {
        observeCurrentSong()
    }

    private fun observeCurrentSong() {
        viewModelScope.launch {
            playbackState
                .map { it.currentSong }
                .distinctUntilChanged()
                .flatMapLatest { song ->
                    if (song == null) {
                        flowOf(false)
                    } else {
                        preloadArtwork(song)
                        musicRepository.isFavorite(song.id)
                    }
                }
                .collect { isFav ->
                    _isFavorite.value = isFav
                }
        }
    }

    private fun preloadArtwork(song: Song) {
        if (song.isExternalSource) return

        viewModelScope.launch(Dispatchers.IO) {
            val request = ImageRequest.Builder(context)
                .data(song.albumArtUri)
                .size(300)
                .crossfade(true)
                .build()
            withContext(Dispatchers.Main) {
                context.imageLoader.enqueue(request)
            }
        }
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
