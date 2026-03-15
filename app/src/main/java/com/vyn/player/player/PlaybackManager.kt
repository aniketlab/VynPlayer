package com.vyn.player.player

import androidx.compose.runtime.Immutable
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.vyn.player.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.vyn.player.data.repository.MusicRepository
import com.vyn.player.data.preferences.UserPreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

enum class RepeatMode {
    OFF, ONE, ALL
}

@Immutable
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

@Immutable
data class PlaybackProgress(
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isPlaying: Boolean = false,
    val currentTrack: Song? = null
)

@Singleton
class PlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val musicHistoryRepository: com.vyn.player.data.repository.MusicHistoryRepository,
    private val userPrefs: UserPreferencesManager
) {
    private var exoPlayer: ExoPlayer? = null
    private var mediaController: MediaController? = null
    val queueManager = QueueManager()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _playbackProgress = MutableStateFlow(PlaybackProgress())
    val playbackProgress: StateFlow<PlaybackProgress> = _playbackProgress.asStateFlow()


    // ─── TRACKING VARIABLES ───
    private var lastTrackId: Long? = null
    private var lastDuration: Long = 0L
    private var trackingStartTime: Long = 0L
    private var maxPositionPlayed: Long = 0L
    private var wasManuallySkipped: Boolean = false
    private var currentHistoryId: Long = 0L
    private var savedLibraryQueueSnapshot: List<Song>? = null
    private var savedLibraryQueueIndex: Int = -1
    private var hasTemporaryExternalQueue: Boolean = false

    val player: Player?
        get() = exoPlayer

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        // Collect playback state changes to track listening history
        // History recording handled in onMediaItemTransition
        // Track full playback session (every 5s or when state changes)
        scope.launch {
            var positionJob: Job? = null
            playbackState.collect { state ->
                positionJob?.cancel()
                if (state.currentSong != null) {
                    positionJob = launch {
                        while (true) {
                            val pos = exoPlayer?.currentPosition ?: 0L
                            val queueIds = state.queue.map { it.id }
                            userPrefs.savePlaybackSession(
                                songId = state.currentSong.id,
                                positionMs = pos,
                                queueIds = queueIds,
                                index = state.currentIndex,
                                isShuffle = state.isShuffleEnabled,
                                repeatMode = when (state.repeatMode) {
                                    RepeatMode.OFF -> 0
                                    RepeatMode.ALL -> 1
                                    RepeatMode.ONE -> 2
                                }
                            )
                            // Update persistent recent tracks
                            musicHistoryRepository.updatePlaybackPosition(state.currentSong.id, pos)
                            
                            if (!state.isPlaying) break
                            delay(5000)
                        }
                    }
                }
            }
        }

        // Emit real-time progress updates (every 100ms for perfect sync)
        scope.launch {
            while (true) {
                if (exoPlayer?.isPlaying == true || _playbackState.value.currentSong != null) {
                    val pos = exoPlayer?.currentPosition ?: 0L
                    val dur = exoPlayer?.duration?.takeIf { it > 0 } ?: _playbackState.value.currentSong?.duration ?: 0L
                    
                    // Update tracking
                    if (pos > maxPositionPlayed) maxPositionPlayed = pos
                    
                    // Force a consistent state update for both PlaybackState and Progress flows
                    updateState { it }
                }
                delay(100)
            }
        }
        
        // Restore full playback session on startup
        scope.launch {
            val allSongs = musicRepository.getAllSongs().firstOrNull() ?: return@launch
            val lastId = userPrefs.lastPlayedSongId.firstOrNull() ?: return@launch
            val lastPos = userPrefs.lastPlaybackPosition.firstOrNull() ?: 0L
            val lastQueueIds = userPrefs.lastQueueIds.firstOrNull() ?: emptyList()
            val lastIndex = userPrefs.lastQueueIndex.firstOrNull() ?: -1
            val lastShuffle = userPrefs.lastShuffleState.firstOrNull() ?: false
            val lastRepeatInt = userPrefs.lastRepeatMode.firstOrNull() ?: 0
            
            val lastSong = allSongs.find { it.id == lastId }
            val restoredQueue = if (lastQueueIds.isNotEmpty()) {
                lastQueueIds.mapNotNull { id -> allSongs.find { it.id == id } }
            } else if (lastSong != null) {
                listOf(lastSong)
            } else emptyList()

            if (restoredQueue.isNotEmpty() && !playbackState.value.isPlaying && playbackState.value.currentSong == null) {
                val player = initializePlayer()
                val targetIndex = if (lastIndex >= 0 && lastIndex < restoredQueue.size) lastIndex else {
                    restoredQueue.indexOfFirst { it.id == lastId }.coerceAtLeast(0)
                }
                
                val mediaItems = restoredQueue.map { buildMediaItem(it) }
                player.setMediaItems(mediaItems, targetIndex, lastPos)
                
                // Restore shuffle/repeat
                player.shuffleModeEnabled = lastShuffle
                player.repeatMode = when (lastRepeatInt) {
                    1 -> Player.REPEAT_MODE_ALL
                    2 -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }

                player.prepare()
                
                queueManager.setQueueManual(restoredQueue)
                queueManager.setCurrentIndex(targetIndex)
                
                updateState {
                    it.copy(
                        currentSong = restoredQueue.getOrNull(targetIndex),
                        currentPosition = lastPos,
                        queue = restoredQueue,
                        currentIndex = targetIndex,
                        isShuffleEnabled = lastShuffle,
                        repeatMode = when (lastRepeatInt) {
                            1 -> RepeatMode.ALL
                            2 -> RepeatMode.ONE
                            else -> RepeatMode.OFF
                        }
                    )
                }
            }
        }
    }

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

        // Bind MediaController to keep MediaSessionService alive and show notification
        if (mediaController == null) {
            val sessionToken = SessionToken(context, ComponentName(context, MuzicPlaybackService::class.java))
            val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture.addListener({
                try {
                    mediaController = controllerFuture.get()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }

        return exoPlayer!!
    }

    fun playSong(song: Song) {
        val player = initializePlayer()
        queueManager.setQueue(listOf(song), 0)
        
        val mediaItem = buildMediaItem(song)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        updateState {
            it.copy(
                currentSong = song,
                isPlaying = true,
                queue = queueManager.currentQueue,
                currentIndex = queueManager.currentIndex
            )
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        hasTemporaryExternalQueue = false
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

    fun playTemporaryQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return

        if (!hasTemporaryExternalQueue) {
            savedLibraryQueueSnapshot = _playbackState.value.queue.takeIf { it.isNotEmpty() }
            savedLibraryQueueIndex = _playbackState.value.currentIndex
        }

        hasTemporaryExternalQueue = true
        queueManager.setQueue(songs, startIndex)
        val startSong = queueManager.currentSong ?: return

        val player = initializePlayer()
        val mediaItems = songs.map { buildMediaItem(it) }
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        player.play()

        updateState {
            it.copy(
                currentSong = startSong,
                isPlaying = true,
                queue = songs,
                currentIndex = startIndex,
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

    fun stop() {
        exoPlayer?.stop()
        hasTemporaryExternalQueue = false
        updateState { PlaybackState() }
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
        } else if (_playbackState.value.repeatMode == RepeatMode.ALL) {
            player.seekToDefaultPosition(0)
        } else {
            // Reached end of queue during skip, generate fallback and continue instantly
            autoContinuePlayback()
            return
        }
        
        wasManuallySkipped = true
        val newIndex = player.currentMediaItemIndex
        val nextSong = queueManager.currentQueue.getOrNull(newIndex)
        if (nextSong != null) {
            queueManager.moveToIndex(newIndex)
            updateState {
                it.copy(
                    currentSong = nextSong,
                    currentIndex = newIndex,
                    currentPosition = 0L
                )
            }
        }
    }

    fun skipToPrevious() {
        val player = exoPlayer ?: return
        // If more than 5 seconds into the song, restart it
        if (player.currentPosition > 5000) {
            player.seekTo(0)
            updateState { it.copy(currentPosition = 0L) }
            return
        }
        
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else if (player.currentMediaItemIndex == 0) {
            player.seekTo(0)
        }
        
        wasManuallySkipped = true
        val newIndex = player.currentMediaItemIndex
        val prevSong = queueManager.currentQueue.getOrNull(newIndex)
        if (prevSong != null) {
            queueManager.moveToIndex(newIndex)
            updateState {
                it.copy(
                    currentSong = prevSong,
                    currentIndex = newIndex,
                    currentPosition = 0L
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
        val midState = update(_playbackState.value)
        val finalDuration = exoPlayer?.duration?.takeIf { it > 0 && it != C.TIME_UNSET } 
            ?: midState.currentSong?.duration 
            ?: midState.duration 
            ?: 0L
        val finalPosition = exoPlayer?.currentPosition ?: midState.currentPosition
        val finalIsPlaying = exoPlayer?.isPlaying ?: midState.isPlaying

        val newState = midState.copy(
            duration = finalDuration,
            currentPosition = finalPosition,
            isPlaying = finalIsPlaying
        )
        
        _playbackState.value = newState
        _playbackProgress.value = PlaybackProgress(
            currentPosition = finalPosition,
            duration = finalDuration,
            isPlaying = finalIsPlaying,
            currentTrack = newState.currentSong
        )
    }

    private fun recordStatsForLastTrack() {
        val trackId = lastTrackId ?: return
        val duration = lastDuration
        if (duration <= 0) return

        val completion = (maxPositionPlayed.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        val playDurationMs = System.currentTimeMillis() - trackingStartTime
        
        scope.launch {
            musicHistoryRepository.onSongFinished(
                songId = trackId,
                historyId = currentHistoryId,
                playedDurationMs = playDurationMs,
                wasSkipped = wasManuallySkipped,
                completionPercentage = completion
            )
        }
        
        // Reset
        wasManuallySkipped = false
        maxPositionPlayed = 0L
        currentHistoryId = 0L
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
                    if (hasTemporaryExternalQueue) {
                        restoreLibraryQueueAfterExternalPlayback()
                        return
                    }
                    // Handled by repeat mode
                    // If queue ended and not repeating, auto-continue playback
                    if (exoPlayer?.hasNextMediaItem() != true && _playbackState.value.repeatMode != RepeatMode.ALL) {
                        autoContinuePlayback()
                    }
                }
                Player.STATE_IDLE -> {}
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateState { it.copy(isPlaying = isPlaying) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Record stats for the song that just finished/skipped
            recordStatsForLastTrack()
            
            mediaItem?.let { item ->
                val songId = item.mediaId.toLongOrNull() ?: return
                val song = queueManager.currentQueue.find { it.id == songId }
                
                // Set up tracking for the new song
                lastTrackId = songId
                lastDuration = song?.duration ?: 0L
                trackingStartTime = System.currentTimeMillis()
                maxPositionPlayed = 0L

                // Persistent start recording
                scope.launch {
                    currentHistoryId = musicHistoryRepository.onSongStarted(songId)
                }

                if (song != null) {
                    val index = queueManager.currentQueue.indexOf(song)
                    queueManager.moveToIndex(index)
                    updateState {
                        it.copy(
                            currentSong = song,
                            currentIndex = index,
                            currentPosition = 0L,
                            duration = song.duration
                        )
                    }
                }
                
                // If this is the last item in the queue, prepare the fallback now for seamless transition
                if (exoPlayer?.hasNextMediaItem() == false && _playbackState.value.repeatMode != RepeatMode.ALL) {
                    autoContinuePlayback()
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (hasTemporaryExternalQueue) {
                restoreLibraryQueueAfterExternalPlayback()
                return
            }
            // Skip to next on error
            skipToNext()
        }
    }

    private fun restoreLibraryQueueAfterExternalPlayback() {
        val savedQueue = savedLibraryQueueSnapshot
        val savedIndex = savedLibraryQueueIndex

        hasTemporaryExternalQueue = false
        savedLibraryQueueSnapshot = null
        savedLibraryQueueIndex = -1

        if (savedQueue.isNullOrEmpty()) {
            exoPlayer?.stop()
            updateState { PlaybackState() }
            return
        }

        val restoredIndex = savedIndex.coerceIn(0, savedQueue.lastIndex)
        queueManager.setQueue(savedQueue, restoredIndex)
        val player = initializePlayer()
        player.setMediaItems(savedQueue.map { buildMediaItem(it) }, restoredIndex, 0L)
        player.prepare()
        player.pause()

        updateState {
            it.copy(
                currentSong = savedQueue.getOrNull(restoredIndex),
                isPlaying = false,
                queue = savedQueue,
                currentIndex = restoredIndex,
                currentPosition = 0L,
            )
        }
    }

    private fun autoContinuePlayback() {
        Log.d("PlaybackManager", "Queue finished — triggering fallback playback")
        val currentSong = _playbackState.value.currentSong ?: return
        
        scope.launch {
            val allSongs = musicRepository.getAllSongs().firstOrNull() ?: run {
                Log.e("PlaybackManager", "Failed to get all songs for fallback")
                return@launch
            }
            
            // Generate fallback queue (up to 20 songs)
            val fallbackQueue = mutableListOf<Song>()
            val currentQueueIds = queueManager.currentQueue.map { it.id }.toSet()
            
            // 1. Priority: Same artist songs
            val artistSongs = allSongs.filter { 
                it.artist.equals(currentSong.artist, ignoreCase = true) && it.id !in currentQueueIds
            }.shuffled()
            fallbackQueue.addAll(artistSongs)
            
            // 2. Priority: Same album songs
            if (fallbackQueue.size < 20) {
                val albumSongs = allSongs.filter {
                    it.albumId == currentSong.albumId && it.id !in currentQueueIds && it.id !in fallbackQueue.map { f -> f.id }
                }.shuffled()
                fallbackQueue.addAll(albumSongs)
            }
            
            // 3. Priority: Random songs from library
            if (fallbackQueue.size < 20) {
                val randomSongs = allSongs.filter {
                    it.id !in currentQueueIds && it.id !in fallbackQueue.map { f -> f.id }
                }.shuffled().take(20 - fallbackQueue.size)
                fallbackQueue.addAll(randomSongs)
            }
            
            // Safety guarantee - always return something if library has songs
            if (fallbackQueue.isEmpty() && allSongs.isNotEmpty()) {
                fallbackQueue.addAll(allSongs.shuffled().take(20))
            }
            
            if (fallbackQueue.isNotEmpty()) {
                Log.d("PlaybackManager", "Fallback queue created: ${fallbackQueue.size} songs")
                
                // Append new songs to queue
                val newQueue = queueManager.currentQueue + fallbackQueue
                queueManager.setQueueManual(newQueue)
                
                val mediaItems = fallbackQueue.map { buildMediaItem(it) }
                exoPlayer?.addMediaItems(mediaItems)
                
                updateState { it.copy(queue = queueManager.currentQueue) }
                
                Log.d("PlaybackManager", "Continuing playback")
                
                // If player was fully ended or idle, explicitly restart movement
                if (exoPlayer?.playbackState == Player.STATE_ENDED || exoPlayer?.playbackState == Player.STATE_IDLE) {
                    skipToNext()
                    play()
                }
            } else {
                Log.e("PlaybackManager", "Could not continue playback, fallback generation failed")
            }
        }
    }

    fun release() {
        recordStatsForLastTrack()
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
    }
}
