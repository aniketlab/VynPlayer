package com.vyn.player.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.vyn.player.data.model.Song
import com.vyn.player.data.repository.ArtworkRepository
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observes scrolling state and informs ArtworkRepository.
 * 
 * NOTE: This does NOT trigger artwork downloads.
 * All artwork downloading is handled by the background EnrichmentWorker
 * which starts at app launch and processes gradually.
 * 
 * Scroll state is only used for MetadataCorrectionRepository pause/resume.
 */
@Composable
fun ObserveScrollState(
    listState: LazyListState,
    items: List<Song> = emptyList(),
    artworkRepository: ArtworkRepository? = LocalArtworkRepository.current
) {
    if (artworkRepository == null) return

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling ->
                artworkRepository.setScrolling(isScrolling)
            }
    }
    // No scroll-based prefetch — artwork worker runs independently in the background
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
