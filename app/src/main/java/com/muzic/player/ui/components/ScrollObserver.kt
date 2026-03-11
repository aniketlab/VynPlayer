package com.muzic.player.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import coil.request.ImageRequest
import com.muzic.player.data.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import com.muzic.player.data.repository.ArtworkRepository
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observes the scrolling state of a LazyList and informs the ArtworkRepository.
 * This ensures that artwork background prefetching is paused during scrolling
 * to keep the UI buttery smooth.
 */
@Composable
fun ObserveScrollState(
    listState: LazyListState,
    items: List<Song> = emptyList(),
    artworkRepository: ArtworkRepository? = LocalArtworkRepository.current
) {
    if (artworkRepository == null) return
    val context = LocalContext.current

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling ->
                artworkRepository.setScrolling(isScrolling)
            }
    }

    if (items.isNotEmpty()) {
        LaunchedEffect(listState) {
            var lastIndex = 0
            val currentJobs = mutableListOf<Job>()

            snapshotFlow { 
                val first = listState.firstVisibleItemIndex
                val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: first
                Triple(first, last, listState.isScrollInProgress)
            }.collect { (first, last, isScrolling) ->
                if (!isScrolling) return@collect
                
                val isScrollingDown = first > lastIndex
                lastIndex = first

                // Cancel prefetch tasks if scroll direction changes
                currentJobs.forEach { it.cancel() }
                currentJobs.clear()

                val prefetchStart = if (isScrollingDown) last + 1 else first - 30
                val prefetchEnd = if (isScrollingDown) last + 30 else first - 1

                val safeStart = prefetchStart.coerceIn(0, items.size)
                val safeEnd = prefetchEnd.coerceIn(0, items.size)

                if (safeStart < safeEnd) {
                    currentJobs.add(launch(Dispatchers.IO) {
                        for (i in safeStart until safeEnd) {
                            if (!isActive) break
                            val song = items[i]
                            val file = artworkRepository.getCachedAlbumArtwork(song.album, song.artist)
                            if (file != null && file.exists()) {
                                val req = ImageRequest.Builder(context)
                                    .data(file)
                                    .size(512)
                                    .memoryCacheKey(file.absolutePath)
                                    .build()
                                context.imageLoader.enqueue(req)
                            }
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun ObserveScrollState(
    gridState: LazyGridState,
    artworkRepository: ArtworkRepository? = LocalArtworkRepository.current
) {
    if (artworkRepository == null) return

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling ->
                artworkRepository.setScrolling(isScrolling)
            }
    }
}
