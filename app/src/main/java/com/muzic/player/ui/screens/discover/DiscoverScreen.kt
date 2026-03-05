package com.muzic.player.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.muzic.player.data.model.Song
import com.muzic.player.data.model.Album
import com.muzic.player.ui.screens.library.LibraryViewModel
import com.muzic.player.ui.theme.*
import com.muzic.player.ui.components.bounceClick

@Composable
fun DiscoverScreen(
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    
    val songs = uiState.songs
    
    // Simulate statistics from local library
    val recentlyAdded = remember(songs) { songs.reversed().take(12) }
    
    val mostPlayed = remember(songs, uiState.topSongs) {
        val top = uiState.topSongs.mapNotNull { count -> songs.find { it.id == count.songId } }
        if (top.isNotEmpty()) top.take(12) else songs.take(12)
    }
    
    val randomPicks = remember(songs) { songs.shuffled().take(15) }
    val randomAlbums = remember(uiState.albums) { uiState.albums.shuffled().take(10) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 144.dp)
    ) {
        item {
            Text(
                text = "Discover",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp, top = 28.dp, bottom = 24.dp)
            )
        }

        if (songs.isNotEmpty()) {
            item {
                DiscoverSection(
                    title = "Recently Added",
                    songs = recentlyAdded,
                    isPlayingSong = { id -> playbackState.currentSong?.id == id },
                    onSongClick = { song -> viewModel.playSong(song, recentlyAdded) }
                )
            }
            item {
                DiscoverSection(
                    title = "Most Played",
                    songs = mostPlayed,
                    isPlayingSong = { id -> playbackState.currentSong?.id == id },
                    onSongClick = { song -> viewModel.playSong(song, mostPlayed) }
                )
            }
            item {
                DiscoverSection(
                    title = "Random Picks For You",
                    songs = randomPicks,
                    isPlayingSong = { id -> playbackState.currentSong?.id == id },
                    onSongClick = { song -> viewModel.playSong(song, randomPicks) }
                )
            }
            if (randomAlbums.isNotEmpty()) {
                item {
                    DiscoverAlbumSection(
                        title = "Albums You May Like",
                        albums = randomAlbums,
                        onAlbumClick = { album -> 
                            val matchingSongs = songs.filter { it.album == album.name }
                            if (matchingSongs.isNotEmpty()) {
                                viewModel.playSong(matchingSongs.first(), matchingSongs)
                            }
                        }
                    )
                }
            }
        } else {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No music found to discover. Add some local files!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun DiscoverSection(
    title: String,
    songs: List<Song>,
    isPlayingSong: (Long) -> Boolean,
    onSongClick: (Song) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)
        )
        
        BoxWithConstraints {
            val cardWidth = if (maxWidth > 600.dp) 180.dp else 148.dp
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(songs, key = { it.id }) { song ->
                    DiscoverSongCard(
                        song = song,
                        isPlaying = isPlayingSong(song.id),
                        cardWidth = cardWidth,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun DiscoverSongCard(
    song: Song,
    isPlaying: Boolean,
    cardWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(cardWidth)
            .clip(RoundedCornerShape(16.dp))
            .bounceClick(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(cardWidth),
            contentAlignment = Alignment.Center
        ) {
            com.muzic.player.ui.components.MuzicImage(
                model = song.albumArtUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 16.dp,
                iconSize = 40.dp,
                fallbackText = song.artist
            )
            
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Equalizer,
                        contentDescription = "Playing",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DiscoverAlbumSection(
    title: String,
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)
        )
        
        BoxWithConstraints {
            val cardWidth = if (maxWidth > 600.dp) 180.dp else 148.dp
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(albums, key = { it.id }) { album ->
                    DiscoverAlbumCard(
                        album = album,
                        cardWidth = cardWidth,
                        onClick = { onAlbumClick(album) }
                    )
                }
            }
        }
    }
}

@Composable
fun DiscoverAlbumCard(
    album: Album,
    cardWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(cardWidth)
            .clip(RoundedCornerShape(16.dp))
            .bounceClick(onClick = onClick)
    ) {
        com.muzic.player.ui.components.MuzicImage(
            model = album.albumArtUri,
            contentDescription = null,
            modifier = Modifier.size(cardWidth),
            cornerRadius = 16.dp,
            iconSize = 40.dp,
            fallbackText = album.name
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
