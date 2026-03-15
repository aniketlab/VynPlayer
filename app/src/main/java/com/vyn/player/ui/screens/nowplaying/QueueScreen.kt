package com.vyn.player.ui.screens.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vyn.player.ui.actions.LocalSongActionDispatcher
import com.vyn.player.ui.actions.SongAction
import com.vyn.player.ui.components.SongItem
import com.vyn.player.ui.theme.getAdaptivePadding

@Composable
fun QueueScreen(
    playbackState: com.vyn.player.player.PlaybackState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val queue = playbackState.queue
    val currentIndex = playbackState.currentIndex.coerceAtLeast(0)
    val currentSong = queue.getOrNull(currentIndex) ?: playbackState.currentSong
    val upNextSongs = if (queue.isNotEmpty() && currentIndex in queue.indices) {
        queue.drop(currentIndex + 1)
    } else {
        emptyList()
    }
    val songActionDispatcher = LocalSongActionDispatcher.current
    val adaptivePadding = getAdaptivePadding()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = adaptivePadding - 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Queue",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (queue.isEmpty() || currentSong == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Queue is empty",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item("now_playing_header") {
                    Text(
                        text = "Now Playing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = adaptivePadding, vertical = 12.dp)
                    )
                }

                item("now_playing_song") {
                    SongItem(
                        song = currentSong,
                        isPlaying = true,
                        isPlaybackActive = playbackState.isPlaying,
                        onSongClick = {
                            songActionDispatcher(SongAction.Play(currentSong, queue))
                        },
                        onAction = songActionDispatcher,
                        modifier = Modifier.padding(horizontal = adaptivePadding)
                    )
                }

                if (upNextSongs.isNotEmpty()) {
                    item("up_next_header") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Up Next",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = adaptivePadding, vertical = 12.dp)
                        )
                    }

                    itemsIndexed(upNextSongs, key = { _, song -> song.id }) { index, song ->
                        SongItem(
                            song = song,
                            isPlaying = false,
                            isPlaybackActive = playbackState.isPlaying,
                            onSongClick = {
                                songActionDispatcher(SongAction.Play(song, queue))
                            },
                            onAction = songActionDispatcher,
                            modifier = Modifier.padding(horizontal = adaptivePadding)
                        )
                    }
                }
            }
        }
    }
}