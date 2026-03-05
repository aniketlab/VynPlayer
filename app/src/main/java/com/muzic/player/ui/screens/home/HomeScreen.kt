package com.muzic.player.ui.screens.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.muzic.player.ui.components.bounceClick
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.muzic.player.data.model.*
import com.muzic.player.ui.screens.library.LibraryViewModel
import com.muzic.player.ui.components.*
import com.muzic.player.ui.theme.*
import com.muzic.player.util.PermissionHelper
import java.util.Calendar

@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToArtist: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Header Spacer
            Spacer(modifier = Modifier.height(28.dp))

            // Greeting Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = adaptivePadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WELCOME BACK",
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 2.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = greeting,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = (screenWidth.value * 0.07f).coerceIn(24f, 32f).sp,
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
                        onClick = onNavigateToSearch,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

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

            Spacer(modifier = Modifier.height(spacingSection))

            // Recently Played
            if (uiState.recentSongs.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(horizontal = adaptivePadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Recently Played",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(spacingMedium))

                val recentSongs = uiState.recentSongs.take(6)
                val columns = if (screenWidth > 600.dp) 3 else 2
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = adaptivePadding),
                    verticalArrangement = Arrangement.spacedBy(spacingMedium)
                ) {
                    recentSongs.chunked(columns).forEach { rowSongs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacingMedium)
                        ) {
                            rowSongs.forEach { song ->
                                RecentlyPlayedCard(
                                    song = song,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.playSong(song, uiState.recentSongs) }
                                )
                            }
                            repeat(columns - rowSongs.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacingSection))

            // Recommended (Albums)
            if (uiState.albums.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = adaptivePadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recommended for You",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "See all",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(spacingMedium))

                val cardWidth = (screenWidth.value * 0.4f).coerceIn(140f, 180f).dp
                LazyRow(
                    contentPadding = PaddingValues(horizontal = adaptivePadding),
                    horizontalArrangement = Arrangement.spacedBy(adaptivePadding)
                ) {
                    items(uiState.albums.take(10)) { album ->
                        Column(modifier = Modifier
                            .width(cardWidth)
                            .clip(RoundedCornerShape(20.dp))
                            .bounceClick {
                                val matchingSongs = uiState.songs.filter { it.album == album.name }
                                if (matchingSongs.isNotEmpty()) {
                                    viewModel.playSong(matchingSongs.first(), matchingSongs)
                                }
                            }
                        ) {
                            MuzicImage(
                                model = album.albumArtUri,
                                contentDescription = album.name,
                                modifier = Modifier.size(cardWidth),
                                fallbackText = album.name,
                                cornerRadius = 20.dp
                            )
                            Spacer(modifier = Modifier.height(spacingSmall))
                            Text(
                                text = album.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = album.artist,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacingSection))

            // Top Artists
            if (uiState.artists.isNotEmpty()) {
                Text(
                    text = "Top Artists",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = adaptivePadding)
                )

                Spacer(modifier = Modifier.height(spacingMedium))

                val artistSize = (screenWidth.value * 0.2f).coerceIn(72f, 96f).dp
                LazyRow(
                    contentPadding = PaddingValues(horizontal = adaptivePadding),
                    horizontalArrangement = Arrangement.spacedBy(adaptivePadding)
                ) {
                    items(uiState.artists.take(8)) { artist ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(artistSize + 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .bounceClick {
                                    onNavigateToArtist(artist.name)
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(artistSize)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = artist.name.take(1).uppercase(),
                                    fontSize = (artistSize.value * 0.35f).sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = artist.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacingSection))

            // Playlists
            if (uiState.playlists.isNotEmpty()) {
                Text(
                    text = "Your Playlists",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = adaptivePadding)
                )

                Spacer(modifier = Modifier.height(spacingMedium))

                val playlistWidth = (screenWidth.value * 0.35f).coerceIn(120f, 160f).dp
                LazyRow(
                    contentPadding = PaddingValues(horizontal = adaptivePadding),
                    horizontalArrangement = Arrangement.spacedBy(spacingMedium)
                ) {
                    items(uiState.playlists) { playlistItem ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            modifier = Modifier
                                .width(playlistWidth)
                                .height(72.dp)
                                .bounceClick {
                                    // Normally we would navigate to the playlist detail screen, but as we don't have
                                    // the navigation callback in the HomeScreen parameters, we will just play the songs.
                                }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = playlistItem.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(144.dp))
        }
    }
}

// ═══════════════════════════════════════
// 3. RECENTLY PLAYED CARD
// ═══════════════════════════════════════
@Composable
fun RecentlyPlayedCard(
    song: Song,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .bounceClick(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Album art — 48dp square
        MuzicImage(
            model = song.albumArtUri,
            contentDescription = null,
            modifier = Modifier.size(42.dp),
            fallbackText = song.artist,
            cornerRadius = 8.dp,
            iconSize = 18.dp
        )

        // Song title — vertically centered
        Text(
            text = song.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    val adaptivePadding = getAdaptivePadding()
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = adaptivePadding)
    )
}

@Composable
fun EmptySectionText(text: String) {
    val adaptivePadding = getAdaptivePadding()
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = adaptivePadding)
    )
}
