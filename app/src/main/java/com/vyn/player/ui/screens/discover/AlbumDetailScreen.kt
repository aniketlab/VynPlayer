package com.vyn.player.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vyn.player.ui.components.SongItem
import com.vyn.player.ui.components.bounceClick
import com.vyn.player.ui.screens.library.LibraryViewModel

@Composable
fun AlbumDetailScreen(
    albumId: Long,
    albumName: String,
    onNavigateBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    val albumSongs = remember(uiState.songs, albumName) {
        uiState.songs.filter { it.album.equals(albumName, ignoreCase = true) }
            .sortedBy { it.trackNumber }
    }

    val albumArtist = remember(albumSongs) {
        albumSongs.firstOrNull()?.artist ?: "Unknown Artist"
    }

    val totalDuration = remember(albumSongs) {
        val totalMs = albumSongs.sumOf { it.duration }
        val minutes = totalMs / 60000
        if (minutes > 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = albumName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (albumSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No songs found in this album.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 144.dp)) {
                // Album header with artwork
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Album Art
                        com.vyn.player.ui.components.MuzicImage(
                            model = albumSongs.firstOrNull()?.albumArtUri,
                            contentDescription = albumName,
                            modifier = Modifier.size(180.dp),
                            cornerRadius = 16.dp,
                            iconSize = 48.dp,
                            fallbackText = albumName,
                            albumName = albumName,
                            artistName = albumArtist
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = albumName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = albumArtist,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "${albumSongs.size} songs · $totalDuration",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.playSong(albumSongs.first(), albumSongs) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                                modifier = Modifier.bounceClick { viewModel.playSong(albumSongs.first(), albumSongs) }
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play All", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val shuffled = albumSongs.shuffled()
                                    viewModel.playSong(shuffled.first(), shuffled)
                                },
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                                modifier = Modifier.bounceClick {
                                    val shuffled = albumSongs.shuffled()
                                    viewModel.playSong(shuffled.first(), shuffled)
                                }
                            ) {
                                Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shuffle", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Song list
                items(albumSongs, key = { it.id }) { song ->
                    SongItem(
                        song = song,
                        isPlaying = playbackState.currentSong?.id == song.id,
                        isPlaybackActive = playbackState.isPlaying,
                        onSongClick = { viewModel.playSong(song, albumSongs) },
                        onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
