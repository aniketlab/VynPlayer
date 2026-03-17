package com.vyn.player.ui.screens.library.tabs

import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vyn.player.data.model.Song
import com.vyn.player.ui.actions.SongAction
import com.vyn.player.ui.components.LocalArtworkRepository
import com.vyn.player.ui.components.SongItem
import com.vyn.player.ui.components.animateListEntry
import com.vyn.player.ui.components.extractArtworkModel
import com.vyn.player.ui.theme.getAdaptivePadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlin.math.abs
import kotlin.math.min

private const val CONTENT_TYPE_HEADER = "song_header"
private const val CONTENT_TYPE_ROW = "song_row"
private const val NORMAL_PREFETCH_COUNT = 5
private const val FAST_PREFETCH_COUNT = 10
private const val FAST_SCROLL_VELOCITY_THRESHOLD = 2.2f

private data class SongSection(
    val letter: String,
    val songs: List<Song>,
)

private data class ScrollSample(
    val timestampNanos: Long,
    val lastVisibleLazyIndex: Int,
    val lastVisibleOffset: Int,
)

private data class PrefetchWindow(
    val startSongIndex: Int,
    val endSongIndex: Int,
)

private data class ScrollSamplePair(
    val previous: ScrollSample?,
    val current: ScrollSample?,
)

@Stable
private data class FastScrollMetadata(
    val sections: List<SongSection>,
    val songIndexToLazyIndex: List<Int>,
    val lazyIndexToSongIndex: List<Int>,
    val songLazyIndexLookup: Set<Int>,
    val totalSongItems: Int,
)

private fun Song.sectionKey(): String {
    val firstChar = title.trim().firstOrNull()?.uppercaseChar() ?: '#'
    return if (firstChar in 'A'..'Z') firstChar.toString() else "#"
}

private fun buildFastScrollMetadata(
    songs: List<Song>,
    hasHeaderContent: Boolean,
): FastScrollMetadata {
    val sections = songs
        .groupBy { it.sectionKey() }
        .toSortedMap(compareBy<String> { if (it == "#") "0" else it })
        .map { SongSection(it.key, it.value) }

    val songIndexToLazyIndex = ArrayList<Int>(songs.size)
    val lazyIndexToSongIndex = ArrayList<Int>(songs.size + sections.size + if (hasHeaderContent) 1 else 0)

    var lazyIndex = 0
    if (hasHeaderContent) {
        lazyIndexToSongIndex += -1
        lazyIndex += 1
    }

    var songIndex = 0
    sections.forEach { section ->
        lazyIndexToSongIndex += -1
        lazyIndex += 1

        repeat(section.songs.size) {
            songIndexToLazyIndex += lazyIndex
            lazyIndexToSongIndex += songIndex
            lazyIndex += 1
            songIndex += 1
        }
    }

    return FastScrollMetadata(
        sections = sections,
        songIndexToLazyIndex = songIndexToLazyIndex,
        lazyIndexToSongIndex = lazyIndexToSongIndex,
        songLazyIndexLookup = songIndexToLazyIndex.toHashSet(),
        totalSongItems = songs.size,
    )
}

@Composable
private fun rememberSmoothFlingBehavior(): FlingBehavior {
    val decay = rememberSplineBasedDecay<Float>()
    return remember(decay) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                var lastValue = 0f
                var remainingVelocity = initialVelocity

                AnimationState(initialValue = 0f, initialVelocity = initialVelocity).animateDecay(decay) {
                    val delta = value - lastValue
                    val consumed = scrollBy(delta)
                    lastValue = value
                    remainingVelocity = velocity

                    if (abs(delta - consumed) > 0.5f) {
                        cancelAnimation()
                    }
                }

                return remainingVelocity
            }
        }
    }
}

@Composable
private fun LibraryListPrefetchEffect(
    listState: LazyListState,
    songs: List<Song>,
    metadata: FastScrollMetadata,
) {
    if (songs.isEmpty()) return

    val context = LocalContext.current.applicationContext
    val artworkRepository = LocalArtworkRepository.current

    LaunchedEffect(listState, songs, metadata) {
        snapshotFlow {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            ScrollSample(
                timestampNanos = System.nanoTime(),
                lastVisibleLazyIndex = lastVisibleItem?.index ?: 0,
                lastVisibleOffset = lastVisibleItem?.offset ?: 0,
            )
        }
            .runningFold(ScrollSamplePair(previous = null, current = null)) { accumulator, current ->
                ScrollSamplePair(previous = accumulator.current, current = current)
            }
            .map { samplePair ->
                val currentSample = samplePair.current ?: return@map null
                val previousSample = samplePair.previous
                val lastVisibleLazyIndex = currentSample.lastVisibleLazyIndex
                val lastVisibleSongIndex = metadata.lazyIndexToSongIndex
                    .getOrNull(lastVisibleLazyIndex.coerceIn(0, metadata.lazyIndexToSongIndex.lastIndex))
                    ?.takeIf { it >= 0 }
                    ?: metadata.lazyIndexToSongIndex
                        .take(lastVisibleLazyIndex.coerceIn(0, metadata.lazyIndexToSongIndex.size))
                        .lastOrNull { it >= 0 }
                    ?: 0

                val velocityRowsPerFrame = if (previousSample != null) {
                    val deltaTimeNanos = (currentSample.timestampNanos - previousSample.timestampNanos).coerceAtLeast(1L)
                    val deltaRows = (currentSample.lastVisibleLazyIndex - previousSample.lastVisibleLazyIndex).toFloat()
                    val deltaOffset = (currentSample.lastVisibleOffset - previousSample.lastVisibleOffset).toFloat() / 400f
                    ((deltaRows + deltaOffset) * 1_000_000_000f) / deltaTimeNanos.toFloat()
                } else {
                    0f
                }

                val prefetchCount = if (abs(velocityRowsPerFrame) >= FAST_SCROLL_VELOCITY_THRESHOLD) {
                    FAST_PREFETCH_COUNT
                } else {
                    NORMAL_PREFETCH_COUNT
                }
                val prefetchStart = (lastVisibleSongIndex + 1).coerceAtMost(songs.lastIndex)
                val prefetchEnd = min(lastVisibleSongIndex + prefetchCount, songs.lastIndex)
                if (prefetchStart > prefetchEnd) null else PrefetchWindow(prefetchStart, prefetchEnd)
            }
            .distinctUntilChanged()
            .collectLatest { window: PrefetchWindow? ->
                if (window == null) return@collectLatest
                songs.subList(window.startSongIndex, window.endSongIndex + 1).forEach { song ->
                    extractArtworkModel(
                        song = song,
                        context = context,
                        artworkRepo = artworkRepository,
                        thumbnailMode = true,
                    )
                }
            }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongsTab(
    songs: List<Song>,
    isLoading: Boolean,
    currentSongId: Long?,
    isPlaybackActive: Boolean = currentSongId != null,
    onSongClick: (Song) -> Unit,
    onSongAction: (SongAction) -> Unit,
    modifier: Modifier = Modifier,
    headerContent: (@Composable () -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
) {
    Box(modifier = modifier.fillMaxSize()) {
        com.vyn.player.ui.components.ObserveScrollState(listState = listState, items = songs)

        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            songs.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No songs found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Text(
                        text = "Add music to your device to get started",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                }
            }

            else -> {
                val adaptivePadding = getAdaptivePadding()
                val metadata = remember(songs, headerContent != null) {
                    buildFastScrollMetadata(songs, headerContent != null)
                }
                val flingBehavior = rememberSmoothFlingBehavior()

                LibraryListPrefetchEffect(
                    listState = listState,
                    songs = songs,
                    metadata = metadata,
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    val songRowModifier = remember(adaptivePadding) {
                        Modifier.padding(horizontal = adaptivePadding)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(top = 8.dp, bottom = 144.dp, end = 12.dp),
                        flingBehavior = flingBehavior,
                    ) {
                        if (headerContent != null) {
                            item(key = "library_header_content", contentType = CONTENT_TYPE_HEADER) {
                                headerContent()
                            }
                        }

                        metadata.sections.forEach { section ->
                            item(key = "header_${section.letter}", contentType = CONTENT_TYPE_HEADER) {
                                Surface(
                                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = section.letter,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = adaptivePadding, vertical = 10.dp),
                                    )
                                }
                            }

                            itemsIndexed(
                                items = section.songs,
                                key = { _, song -> song.id },
                                contentType = { _, _ -> CONTENT_TYPE_ROW },
                            ) { index, song ->
                                SongItem(
                                    song = song,
                                    isPlaying = song.id == currentSongId,
                                    isPlaybackActive = isPlaybackActive,
                                    onSongClick = { onSongClick(song) },
                                    onAction = onSongAction,
                                    isScrolling = false,
                                    modifier = songRowModifier.animateListEntry(index, delay = 15),
                                )
                            }
                        }
                    }

                    ModernScrollbarOverlay(
                        listState = listState,
                        metadata = metadata,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernScrollbarOverlay(
    listState: LazyListState,
    metadata: FastScrollMetadata,
    modifier: Modifier = Modifier,
) {
    if (metadata.totalSongItems <= 40) return

    val density = LocalDensity.current
    var overlayHeightPx by remember { mutableIntStateOf(0) }
    var isVisible by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val currentVisibleSongIndex by remember(metadata, listState) {
        derivedStateOf {
            val firstVisibleLazyIndex = listState.firstVisibleItemIndex
            val mappedIndex = metadata.lazyIndexToSongIndex
                .getOrNull(firstVisibleLazyIndex.coerceIn(0, metadata.lazyIndexToSongIndex.lastIndex))
            when {
                mappedIndex != null && mappedIndex >= 0 -> mappedIndex
                metadata.totalSongItems > 0 -> metadata.totalSongItems - 1
                else -> 0
            }
        }
    }

    val thumbProgress by remember(metadata, currentVisibleSongIndex) {
        derivedStateOf {
            if (metadata.totalSongItems <= 1) 0f
            else currentVisibleSongIndex.toFloat() / (metadata.totalSongItems - 1).toFloat()
        }
    }

    val visibleItemsCount by remember(listState, metadata) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.count { it.index in metadata.songLazyIndexLookup }
                .coerceAtLeast(1)
        }
    }

    val thumbHeightPx by remember(overlayHeightPx, visibleItemsCount, metadata.totalSongItems, density) {
        derivedStateOf {
            if (overlayHeightPx == 0 || metadata.totalSongItems == 0) {
                with(density) { 54.dp.roundToPx() }
            } else {
                (overlayHeightPx * (visibleItemsCount.toFloat() / metadata.totalSongItems.toFloat())).toInt()
                    .coerceIn(with(density) { 48.dp.roundToPx() }, with(density) { 96.dp.roundToPx() })
            }
        }
    }

    LaunchedEffect(listState, isDragging) {
        if (isDragging) {
            isVisible = true
            return@LaunchedEffect
        }
        
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collectLatest { scrolling ->
                if (scrolling) {
                    isVisible = true
                } else {
                    delay(800)
                    isVisible = false
                }
            }
    }

    val thumbAlpha by animateFloatAsState(
        targetValue = if (isVisible) 0.72f else 0f,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "libraryScrollbarThumbAlpha",
    )
    val trackAlpha by animateFloatAsState(
        targetValue = if (isVisible) 0.10f else 0f,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "libraryScrollbarTrackAlpha",
    )

    val thumbOffsetDp = with(density) {
        val offsetPx = ((overlayHeightPx - thumbHeightPx).coerceAtLeast(0) * thumbProgress)
        offsetPx.toDp()
    }
    val thumbHeightDp = with(density) { thumbHeightPx.toDp() }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp)
            .padding(end = 4.dp, top = 8.dp, bottom = 112.dp)
            .onSizeChanged { overlayHeightPx = it.height }
            .pointerInput(metadata, overlayHeightPx) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        if (overlayHeightPx <= 0) return@detectVerticalDragGestures
                        val scrollPercent = (offset.y / overlayHeightPx.toFloat()).coerceIn(0f, 1f)
                        val songIdx = (scrollPercent * (metadata.totalSongItems - 1)).toInt()
                            .coerceIn(0, metadata.totalSongItems - 1)
                        val lazyIdx = metadata.songIndexToLazyIndex.getOrNull(songIdx) ?: 0
                        coroutineScope.launch { listState.scrollToItem(lazyIdx) }
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        if (overlayHeightPx <= 0) return@detectVerticalDragGestures
                        val scrollPercent = (change.position.y / overlayHeightPx.toFloat()).coerceIn(0f, 1f)
                        val songIdx = (scrollPercent * (metadata.totalSongItems - 1)).toInt()
                            .coerceIn(0, metadata.totalSongItems - 1)
                        val lazyIdx = metadata.songIndexToLazyIndex.getOrNull(songIdx) ?: 0
                        coroutineScope.launch { listState.scrollToItem(lazyIdx) }
                    }
                )
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(999.dp))
                .alpha(trackAlpha)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = thumbOffsetDp)
                .width(4.dp)
                .height(thumbHeightDp)
                .clip(RoundedCornerShape(999.dp))
                .alpha(thumbAlpha)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)),
        )
    }
}