package com.vyn.player.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vyn.player.ui.components.bounceClick
import com.vyn.player.data.model.*
import com.vyn.player.ui.screens.library.LibraryViewModel
import com.vyn.player.ui.components.*
import com.vyn.player.ui.theme.*
import com.vyn.player.util.MetadataUtils
import java.util.Calendar

@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToFolders: () -> Unit = {},
    onNavigateToArtists: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    onNavigateToSmartMix: () -> Unit = {},
    onNavigateToHomeRecentlyPlayed: () -> Unit = {},
    onNavigateToHomeMostPlayed: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
    appViewModel: com.vyn.player.ui.MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // ─── TAP TO TOP LISTENER ───
    LaunchedEffect(Unit) {
        appViewModel.scrollToTopRequest.collect { route ->
            if (route == com.vyn.player.ui.navigation.Screen.Home.route) {
                listState.animateScrollToItem(0)
            }
        }
    }

    val calendar = Calendar.getInstance()
    val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val screenWidth = maxWidth
        val adaptivePadding = getAdaptivePadding()
        val spacingSmall = (adaptivePadding.value * 0.5f).dp
        val spacingMedium = (adaptivePadding.value * 0.75f).dp
        val spacingSection = (adaptivePadding.value * 1.5f).dp

    val recentSongsInfo = remember(uiState.recentSongs, uiState.songs) {
        val filtered = uiState.recentSongs.take(10)
        filtered
    }

    val mostPlayedSongs = remember(uiState.topSongs, uiState.songs) {
        val mostPlayedIds = uiState.topSongs.map { it.songId }
        mostPlayedIds.mapNotNull { id -> uiState.songs.find { it.id == id } }.take(10)
    }

    val smartMixSongs = remember(uiState.smartMixSongs) {
        uiState.smartMixSongs.take(4)
    }

    com.vyn.player.ui.components.ObserveScrollState(listState = listState, items = uiState.songs)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        state = listState
    ) {
            item {

            Spacer(modifier = Modifier.height(28.dp))

            // 1. Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = adaptivePadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = uiState.userDisplayName.ifEmpty { "Music Lover" },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = (screenWidth.value * 0.06f).coerceIn(20f, 26f).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacingSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacingMedium))

            // Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = adaptivePadding)
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .bounceClick(onClick = onNavigateToSearch)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Search your music library",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            
            Spacer(modifier = Modifier.height(spacingSection))
            } // Close first item block

            // 2. Quick Filters
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = adaptivePadding),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                    FilterPill(
                        label = "Folders",
                        icon = Icons.Rounded.Folder,
                        onClick = onNavigateToFolders
                    )
                }
                item {
                    FilterPill(
                        label = "Playlists",
                        icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                        onClick = onNavigateToPlaylists
                    )
                }
                item {
                    FilterPill(
                        label = "Favorites",
                        icon = Icons.Rounded.Favorite,
                        onClick = onNavigateToFavorites
                    )
                }
                item {
                    FilterPill(
                        label = "Artists",
                        icon = Icons.Rounded.Person,
                        onClick = onNavigateToArtists
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacingSection))

            }

            // 3. Jump Back In Section
            item {
                if (uiState.recentSongs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = adaptivePadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Jump Back In",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToHomeRecentlyPlayed) {
                        Text(
                            text = "See All",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacingMedium))

                val recentSongs = recentSongsInfo.take(6)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = adaptivePadding),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(recentSongs, key = { it.id }) { song ->
                        SquareSongCard(
                            song = song,
                            onClick = { viewModel.playSong(song, uiState.recentSongs) },
                            modifier = Modifier.animateListEntry(0) // 0 delay for horizontal, let user see it
                        )
                    }
                }
                Spacer(modifier = Modifier.height(spacingSection))
            } else {
                Text(
                    text = "Jump Back In",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = adaptivePadding)
                )
                Spacer(modifier = Modifier.height(spacingSmall))
                Text(
                    text = "Start playing music to see your listening history.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = adaptivePadding)
                )
                Spacer(modifier = Modifier.height(spacingSection))
            }

            }

            // 4. Most Played Section
            item {
                if (mostPlayedSongs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = adaptivePadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Most Played",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToHomeMostPlayed) {
                        Text(
                            text = "See All",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacingMedium))
                
                val topLimited = mostPlayedSongs.take(6)

                LazyRow(
                    contentPadding = PaddingValues(horizontal = adaptivePadding),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(topLimited, key = { it.id }) { song ->
                        SquareSongCard(
                            song = song,
                            onClick = { viewModel.playSong(song, mostPlayedSongs) },
                            modifier = Modifier.animateListEntry(0)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(spacingSection))
                }
            }

            // 5. Smart Mix Section
            item {
                if (uiState.smartMixSongs.isNotEmpty()) {
                    Text(
                        text = "Smart Mix",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = adaptivePadding)
                )
                Text(
                    text = "Generated from your listening habits",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = adaptivePadding)
                )
                
                Spacer(modifier = Modifier.height(spacingMedium))
                
                SmartMixCard(
                    songs = smartMixSongs,
                    onPlayClick = { viewModel.playSmartMix() },
                    onClick = onNavigateToSmartMix,
                    modifier = Modifier
                        .padding(horizontal = adaptivePadding)
                        .animateListEntry(0)
                )
                }
            }

            item {
                Spacer(modifier = Modifier.height(144.dp))
            }
        }
    }
}

@Composable
fun FilterPill(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SquareSongCard(song: Song, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var showActionSheet by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .width(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .bounceCombinedClickable(
                onClick = onClick,
                onLongClick = { showActionSheet = true }
            )
            .padding(4.dp)
    ) {
        SharedArtworkImage(
            song = song,
            contentDescription = null,
            modifier = Modifier.size(122.dp),
            cornerRadius = 16.dp,
            iconSize = 32.dp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = MetadataUtils.cleanTitle(song.title),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = MetadataUtils.cleanArtist(song.artist),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }

    if (showActionSheet) {
        val viewModel: LibraryViewModel = hiltViewModel()
        SongActionSheet(
            song = song,
            onDismissRequest = { showActionSheet = false },
            onFavoriteClick = { viewModel.toggleFavorite(song.id) }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LocalTrackItem(song: Song, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var showActionSheet by remember { mutableStateOf(false) }
    
    val bitRate = if (song.duration > 0) (song.size * 8000L) / song.duration else 0L
    val badgeText = when {
        song.mimeType.contains("flac", ignoreCase = true) -> "FLAC"
        song.mimeType.contains("wav", ignoreCase = true) -> "WAV"
        bitRate >= 320000 -> "320kbps"
        bitRate >= 256000 -> "256kbps"
        bitRate >= 192000 -> "192kbps"
        else -> "MP3"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .bounceCombinedClickable(
                onClick = onClick,
                onLongClick = { showActionSheet = true }
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SharedArtworkImage(
            song = song,
            contentDescription = null,
            modifier = Modifier.size(50.dp),
            cornerRadius = 10.dp,
            iconSize = 20.dp
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = MetadataUtils.cleanTitle(song.title),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = MetadataUtils.cleanArtist(song.artist),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Text(
                text = badgeText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showActionSheet) {
        val viewModel: LibraryViewModel = hiltViewModel()
        SongActionSheet(
            song = song,
            onDismissRequest = { showActionSheet = false },
            onFavoriteClick = { viewModel.toggleFavorite(song.id) }
        )
    }
}

@Composable
fun SmartMixCard(
    songs: List<Song>,
    onPlayClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
            .bounceClick(onClick = onClick)
            .padding(16.dp)
    ) {
            // 2x2 Grid Collage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.8f) // Slightly wider than tall
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            if (songs.size > 0) SharedArtworkImage(song = songs[0], modifier = Modifier.fillMaxSize(), cornerRadius = 0.dp, contentDescription = null)
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            if (songs.size > 2) SharedArtworkImage(song = songs[2], modifier = Modifier.fillMaxSize(), cornerRadius = 0.dp, contentDescription = null)
                        }
                    }
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            if (songs.size > 1) SharedArtworkImage(song = songs[1], modifier = Modifier.fillMaxSize(), cornerRadius = 0.dp, contentDescription = null)
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            if (songs.size > 3) SharedArtworkImage(song = songs[3], modifier = Modifier.fillMaxSize(), cornerRadius = 0.dp, contentDescription = null)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Daily Mix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Curated for you",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Button(
                    onClick = onPlayClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 4.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Play", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
        }
    }
}
