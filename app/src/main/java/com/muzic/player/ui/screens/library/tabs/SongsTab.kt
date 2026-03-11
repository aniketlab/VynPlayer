package com.muzic.player.ui.screens.library.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.muzic.player.ui.components.animateListEntry
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muzic.player.data.model.Song
import com.muzic.player.ui.components.SongItem
import com.muzic.player.ui.theme.*
import coil.imageLoader
import coil.request.ImageRequest
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun SongsTab(
    songs: List<Song>,
    isLoading: Boolean,
    currentSongId: Long?,
    onSongClick: (Song) -> Unit,
    onFavoriteClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
) {
    Box(modifier = modifier.fillMaxSize()) {
        com.muzic.player.ui.components.ObserveScrollState(listState = listState, items = songs)
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            songs.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No songs found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Add music to your device to get started",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
            else -> {
                val adaptivePadding = getAdaptivePadding()
                val context = LocalContext.current

                val chunkSize = 50
                val displayedItemsCountState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(chunkSize) }

                LaunchedEffect(songs) {
                    displayedItemsCountState.intValue = chunkSize
                }

                LaunchedEffect(listState) {
                    androidx.compose.runtime.snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .collect { lastVisibleIndex ->
                            if (lastVisibleIndex != null && lastVisibleIndex >= displayedItemsCountState.intValue - 15) {
                                if (displayedItemsCountState.intValue < songs.size) {
                                    displayedItemsCountState.intValue = (displayedItemsCountState.intValue + chunkSize).coerceAtMost(songs.size)
                                }
                            }
                        }
                }

                val displayedSongs = androidx.compose.runtime.remember(songs, displayedItemsCountState.intValue) {
                    songs.take(displayedItemsCountState.intValue)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(vertical = 8.dp),
                    flingBehavior = androidx.compose.foundation.gestures.ScrollableDefaults.flingBehavior()
                ) {
                    // Song count header
                    item {
                        Text(
                            text = "${songs.size} songs",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = adaptivePadding, vertical = 8.dp)
                        )
                    }

                    itemsIndexed(
                        items = displayedSongs,
                        key = { _, it -> it.id }
                    ) { index, song ->
                        SongItem(
                            song = song,
                            isPlaying = song.id == currentSongId,
                            onSongClick = { onSongClick(song) },
                            onFavoriteClick = { onFavoriteClick(song.id) },
                            isScrolling = listState.isScrollInProgress,
                            modifier = Modifier
                                .padding(horizontal = adaptivePadding)
                                .animateListEntry(index, delay = 15)
                        )
                    }

                    // Bottom spacer for mini player
                    item {
                        Spacer(modifier = Modifier.height(144.dp))
                    }
                }
            }
        }
    }
}
