package com.vyn.player.ui.actions

import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.vyn.player.data.model.Song
import com.vyn.player.data.repository.MusicRepository
import com.vyn.player.player.PlaybackManager
import com.vyn.player.util.TimeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackManager: PlaybackManager,
    private val musicRepository: MusicRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _selectedSong = MutableStateFlow<Song?>(null)
    val selectedSong: StateFlow<Song?> = _selectedSong.asStateFlow()

    private val _showSongDetails = MutableStateFlow(false)
    val showSongDetails: StateFlow<Boolean> = _showSongDetails.asStateFlow()

    fun handle(action: SongAction) {
        when (action) {
            is SongAction.Play -> playSong(action.song, action.queue)
            is SongAction.PlayNext -> {
                playbackManager.playNext(action.song)
                Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
            }

            is SongAction.AddToPlaylist -> {
                Toast.makeText(context, "Playlist feature coming soon", Toast.LENGTH_SHORT).show()
            }

            is SongAction.Share -> shareSong(action.song)
            is SongAction.ShowDetails -> {
                _selectedSong.value = action.song
                _showSongDetails.value = true
            }

            is SongAction.ToggleFavorite -> {
                scope.launch {
                    musicRepository.toggleFavorite(action.song.id)
                }
            }
        }
    }

    fun dismissSongDetails() {
        _showSongDetails.value = false
    }

    private fun playSong(song: Song, queue: List<Song>?) {
        val songList = queue ?: listOf(song)
        val index = songList.indexOf(song).coerceAtLeast(0)
        playbackManager.playQueue(songList, index)
    }

    private fun shareSong(song: Song) {
        val songFile = File(song.path)
        if (!songFile.exists()) {
            Toast.makeText(context, "Audio file not found", Toast.LENGTH_SHORT).show()
            return
        }

        val shareUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            songFile
        )

        val shareText = buildString {
            appendLine("🎵 ${song.title}")
            appendLine("👤 ${song.artist}")
            appendLine("💿 ${song.album}")
            appendLine()
            append("Shared from VYN Player")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            clipData = ClipData.newUri(context.contentResolver, song.title, shareUri)
            putExtra(Intent.EXTRA_SUBJECT, song.title)
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_STREAM, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(intent, "Share song").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}