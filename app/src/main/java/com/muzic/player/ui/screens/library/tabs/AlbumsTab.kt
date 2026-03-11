package com.muzic.player.ui.screens.library.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import com.muzic.player.ui.components.animateListEntry
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muzic.player.data.model.Album
import com.muzic.player.ui.components.AlbumCard
import com.muzic.player.ui.theme.*

@Composable
fun AlbumsTab(
    albums: List<Album>,
    isLoading: Boolean,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
) {
    Box(modifier = modifier.fillMaxSize()) {
        com.muzic.player.ui.components.ObserveScrollState(gridState)
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            albums.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Album,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No albums found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            else -> {
                val adaptivePadding = getAdaptivePadding()
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    contentPadding = PaddingValues(
                        start = adaptivePadding,
                        top = 16.dp,
                        end = adaptivePadding,
                        bottom = 144.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(adaptivePadding),
                    verticalArrangement = Arrangement.spacedBy(adaptivePadding)
                ) {
                    itemsIndexed(
                        items = albums,
                        key = { _, it -> it.id }
                    ) { index, album ->
                        AlbumCard(
                            album = album,
                            onClick = { onAlbumClick(album) },
                            isScrolling = gridState.isScrollInProgress,
                            modifier = Modifier.animateListEntry(index, delay = 15)
                        )
                    }
                }
            }
        }
    }
}
