package com.muzic.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muzic.player.player.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val playbackManager: PlaybackManager
) : ViewModel() {

    val playbackState = playbackManager.playbackState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.muzic.player.player.PlaybackState()
    )

    fun togglePlayPause() = playbackManager.togglePlayPause()
    fun skipToNext() = playbackManager.skipToNext()
}
