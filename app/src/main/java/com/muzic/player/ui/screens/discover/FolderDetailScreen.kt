package com.muzic.player.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muzic.player.ui.components.SongItem
import com.muzic.player.ui.components.bounceClick
import com.muzic.player.ui.screens.library.LibraryViewModel
import java.io.File

@Composable
fun FolderDetailScreen(
    folderPath: String,
    onNavigateBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    val folderName = remember(folderPath) {
        File(folderPath).name
    }

    val folderSongs = remember(uiState.songs, folderPath) {
        uiState.songs.filter { it.folderPath == folderPath }
            .sortedBy { it.title.lowercase() }
    }

    val totalDuration = remember(folderSongs) {
        val totalMs = folderSongs.sumOf { it.duration }
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
                text = folderName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (folderSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No songs found in this folder.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 144.dp)) {
                // Folder header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Folder icon
                        Surface(
                            modifier = Modifier.size(100.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = folderName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "${folderSongs.size} songs · $totalDuration",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = folderPath,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.padding(top = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.playSong(folderSongs.first(), folderSongs) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                                modifier = Modifier.bounceClick { viewModel.playSong(folderSongs.first(), folderSongs) }
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play All", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val shuffled = folderSongs.shuffled()
                                    viewModel.playSong(shuffled.first(), shuffled)
                                },
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                                modifier = Modifier.bounceClick {
                                    val shuffled = folderSongs.shuffled()
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
                items(folderSongs, key = { it.id }) { song ->
                    SongItem(
                        song = song,
                        isPlaying = playbackState.currentSong?.id == song.id,
                        onSongClick = { viewModel.playSong(song, folderSongs) },
                        onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
