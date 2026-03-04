package com.muzic.player.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.muzic.player.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class RepeatMode {
    OFF, ONE, ALL
}

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentSong: Song? = null,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffleEnabled: Boolean = false,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isBuffering: Boolean = false
)

@Singleton
class PlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var exoPlayer: ExoPlayer? = null
    val queueManager = QueueManager()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    val player: Player?
        get() = exoPlayer

    @OptIn(UnstableApi::class)
    fun initializePlayer(): ExoPlayer {
        if (exoPlayer != null) return exoPlayer!!

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        exoPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
            .apply {
                addListener(playerListener)
                // Enable gapless playback
                playWhenReady = false
            }

        return exoPlayer!!
    }

    fun playSong(song: Song) {
        val player = initializePlayer()
        val mediaItem = buildMediaItem(song)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        updateState {
            it.copy(
                currentSong = song,
                isPlaying = true,
                currentIndex = queueManager.currentIndex
            )
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        queueManager.setQueue(songs, startIndex)
        val startSong = queueManager.currentSong ?: return

        val player = initializePlayer()
        val mediaItems = songs.map { buildMediaItem(it) }
        player.setMediaItems(mediaItems, startIndex, 0)
        player.prepare()
        player.play()

        updateState {
            it.copy(
                currentSong = startSong,
                isPlaying = true,
                queue = songs,
                currentIndex = startIndex
            )
        }
    }

    fun play() {
        exoPlayer?.play()
        updateState { it.copy(isPlaying = true) }
    }

    fun pause() {
        exoPlayer?.pause()
        updateState { it.copy(isPlaying = false) }
    }

    fun togglePlayPause() {
        if (_playbackState.value.isPlaying) pause() else play()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        updateState { it.copy(currentPosition = positionMs) }
    }

    fun skipToNext() {
        val player = exoPlayer ?: return
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            val nextSong = queueManager.moveToNext()
            updateState {
                it.copy(
                    currentSong = nextSong,
                    currentIndex = queueManager.currentIndex
                )
            }
        }
    }

    fun skipToPrevious() {
        val player = exoPlayer ?: return
        // If more than 3 seconds into the song, restart it
        if (player.currentPosition > 3000) {
            player.seekTo(0)
            return
        }
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            val prevSong = queueManager.moveToPrevious()
            updateState {
                it.copy(
                    currentSong = prevSong,
                    currentIndex = queueManager.currentIndex
                )
            }
        }
    }

    fun setRepeatMode(mode: RepeatMode) {
        val playerRepeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        exoPlayer?.repeatMode = playerRepeatMode
        updateState { it.copy(repeatMode = mode) }
    }

    fun cycleRepeatMode() {
        val nextMode = when (_playbackState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        setRepeatMode(nextMode)
    }

    fun toggleShuffle() {
        queueManager.toggleShuffle()
        val isShuffled = queueManager.isShuffled
        exoPlayer?.shuffleModeEnabled = isShuffled
        updateState {
            it.copy(
                isShuffleEnabled = isShuffled,
                queue = queueManager.currentQueue
            )
        }
    }

    fun addToQueue(song: Song) {
        queueManager.addToQueue(song)
        val mediaItem = buildMediaItem(song)
        exoPlayer?.addMediaItem(mediaItem)
        updateState { it.copy(queue = queueManager.currentQueue) }
    }

    fun playNext(song: Song) {
        queueManager.addNextInQueue(song)
        val mediaItem = buildMediaItem(song)
        val insertIndex = queueManager.currentIndex + 1
        exoPlayer?.addMediaItem(insertIndex, mediaItem)
        updateState { it.copy(queue = queueManager.currentQueue) }
    }

    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }

    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0L
    }

    fun release() {
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun buildMediaItem(song: Song): MediaItem {
        return MediaItem.Builder()
            .setUri(song.uri)
            .setMediaId(song.id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.albumArtUri)
                    .build()
            )
            .build()
    }

    private fun updateState(update: (PlaybackState) -> PlaybackState) {
        _playbackState.value = update(_playbackState.value)
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> updateState { it.copy(isBuffering = true) }
                Player.STATE_READY -> {
                    updateState {
                        it.copy(
                            isBuffering = false,
                            duration = exoPlayer?.duration ?: 0L
                        )
                    }
                }
                Player.STATE_ENDED -> {
                    // Handled by repeat mode
                }
                Player.STATE_IDLE -> {}
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateState { it.copy(isPlaying = isPlaying) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let { item ->
                val songId = item.mediaId.toLongOrNull() ?: return
                val song = queueManager.currentQueue.find { it.id == songId }
                if (song != null) {
                    val index = queueManager.currentQueue.indexOf(song)
                    queueManager.moveToIndex(index)
                    updateState {
                        it.copy(
                            currentSong = song,
                            currentIndex = index,
                            currentPosition = 0L
                        )
                    }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            // Skip to next on error
            skipToNext()
        }
    }
}
